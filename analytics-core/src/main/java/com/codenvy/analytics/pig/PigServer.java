/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */


package com.codenvy.analytics.pig;

import com.codenvy.analytics.Configurator;
import com.codenvy.analytics.Utils;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.pig.scripts.ScriptType;
import com.codenvy.analytics.storage.DataStorageContainer;

import org.apache.pig.ExecType;
import org.apache.pig.data.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Pig-latin script executor.
 *
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
 */
public class PigServer {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PigServer.class);

    /** Embedded Pig server */
    private static org.apache.pig.PigServer server;

    private static final String ANALYTICS_SCRIPTS_DIR_PROPERTY = "analytics.scripts.dir";
    private static final String ANALYTICS_BIN_DIR_PROPERTY     = "analytics.bin.dir";
    public static final  String ANALYTICS_LOGS_DIRECTORY       = "analytics.logs.directory";

    public static final String SCRIPTS_DIR    = System.getProperty(ANALYTICS_SCRIPTS_DIR_PROPERTY);
    public static final String BIN_DIR        = System.getProperty(ANALYTICS_BIN_DIR_PROPERTY);
    public static final String LOGS_DIRECTORY = Configurator.getString(ANALYTICS_LOGS_DIRECTORY);

    private static final String PIG_SERVER_EMBEDDED = "pig.server.embedded";

    /** Pig relation containing execution result. */
    private static final String FINAL_RELATION = "result";

    /** Imported macro files. Pig sever doesn't allow to import the same macro file twice. */
    private static final Set<String> importedMacros = new HashSet<>();

    static {
        for (Map.Entry<String, String> entry : Configurator.getAll("pig.server.property").entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        try {
            server = new org.apache.pig.PigServer(ExecType.LOCAL);
            server.registerJar(PigServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new IllegalStateException("Pig server can't be instantiated", e);
        }

        LOG.info("Pig server is started");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.shutdown();
                LOG.info("Pig server is shutdown");
            }
        });
    }

    /**
     * Run the script. Mostly for testing purpose.
     *
     * @param scriptType
     *         specific script type to execute
     * @param context
     *         contains all necessary value parameters required by given {@link com.codenvy.analytics.pig.scripts
     *         .ScriptType}
     * @throws IOException
     *         if something gone wrong or if a required parameter is absent
     */
    public static void execute(ScriptType scriptType, Map<String, String> context) throws IOException {
        LOG.info("Script execution " + scriptType + " is started: " + context.toString());

        context = validateAndAdjustContext(scriptType, context);
        if (scriptType.isLogRequired() && Parameters.LOG.get(context).isEmpty()) {
            return;
        }

        if (Configurator.getBoolean(PIG_SERVER_EMBEDDED)) {
            executeOnEmbeddedServer(scriptType, context);
        } else {
            executeOnDedicatedServer(scriptType, context);
        }
    }

    private static void executeOnEmbeddedServer(ScriptType scriptType, Map<String, String> context) throws IOException {
        String script = readScriptContent(scriptType);
        try (InputStream scriptContent = new ByteArrayInputStream(script.getBytes())) {
            server.registerScript(scriptContent, context);
        } finally {
            LOG.info("Execution " + scriptType + " has finished");
        }
    }

    private static synchronized void executeOnDedicatedServer(ScriptType scriptType, Map<String, String> context)
            throws IOException {
        try {
            String command = prepareRunCommand(scriptType, context);
            Process process = Runtime.getRuntime().exec(command);

            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    logProcessOutput(process);
                    throw new IOException("The process has finished with wrong code " + exitCode);
                }
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        } finally {
            LOG.info("Execution " + scriptType + " has finished");
        }
    }

    private static void logProcessOutput(Process process) throws IOException {
        try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            for (; ; ) {
                String inLine = in.readLine();
                String errLine = err.readLine();

                if (inLine != null) {
                    LOG.info(inLine);
                } else if (errLine != null) {
                    LOG.info(errLine);
                } else {
                    break;
                }
            }
        }
    }

    private static String prepareRunCommand(ScriptType scriptType, Map<String, String> context) {
        StringBuilder builder = new StringBuilder();

        builder.append(new File(BIN_DIR, "run_pig.sh").getAbsolutePath());

        for (Map.Entry<String, String> entry : context.entrySet()) {
            builder.append(' ');
            builder.append("-param ");
            builder.append(entry.getKey());
            builder.append("=");
            builder.append("'").append(entry.getValue()).append("'");
        }

        builder.append(' ');
        builder.append(getScriptFileName(scriptType).getAbsolutePath());

        return builder.toString();
    }

    /**
     * Run the script and returns the result. Mostly for testing purpose.
     *
     * @param scriptType
     *         specific script type to execute
     * @param context
     *         contains all necessary value parameters required by given {@link com.codenvy.analytics.pig.scripts
     *         .ScriptType}
     * @throws IOException
     *         if something gone wrong or if a required parameter is absent
     */
    public static Iterator<Tuple> executeAndReturn(ScriptType scriptType,
                                                   Map<String, String> context) throws IOException {
        LOG.info("Script execution " + scriptType + " is started: " + context.toString());

        context = validateAndAdjustContext(scriptType, context);
        if (scriptType.isLogRequired() && Parameters.LOG.get(context).isEmpty()) {
            return Collections.emptyIterator();
        }

        String script = readScriptContent(scriptType);
        script = removeRedundantCode(script);

        try (InputStream scriptContent = new ByteArrayInputStream(script.getBytes())) {
            server.registerScript(scriptContent, context);
            return server.openIterator(FINAL_RELATION);
        } finally {
            LOG.info("Execution " + scriptType + " has finished");
        }
    }

    /** Checks if all parameters that are needed to script execution are added to context; */
    private static Map<String, String> validateAndAdjustContext(ScriptType scriptType,
                                                                Map<String, String> context) throws IOException {
        context = Utils.clone(context);

        Parameters.STORAGE_URL.put(context, DataStorageContainer.getStorageUrl());
        if (!Parameters.LOG.exists(context) && scriptType.isLogRequired()) {
            setOptimizedPaths(context);
        }

        for (Parameters param : scriptType.getParams()) {
            if (!param.exists(context)) {
                throw new IOException("Key field " + param + " is absent in execution context");
            }

            param.validate(param.get(context), context);
        }

        return context;
    }


    /** @return the script file name */
    private static File getScriptFileName(ScriptType scriptType) {
        return new File(SCRIPTS_DIR, scriptType.toString().toLowerCase() + ".pig");
    }

    /**
     * Selects sub directories with data to inspect based on given date parameters.
     *
     * @throws IOException
     *         if any exception is occurred
     */
    private static void setOptimizedPaths(Map<String, String> context) throws IOException {
        try {
            String path = LogLocationOptimizer.generatePaths(new File(LOGS_DIRECTORY).getAbsolutePath(),
                                                             Parameters.FROM_DATE.get(context),
                                                             Parameters.TO_DATE.get(context));
            Parameters.LOG.put(context, path);
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    /** Reads script from file. */
    private static String readScriptContent(ScriptType scriptType) throws IOException {
        File scriptFile = getScriptFileName(scriptType);
        if (!scriptFile.exists()) {
            throw new IOException("Resource " + scriptFile.getAbsolutePath() + " not found");
        }

        try (InputStream scriptContent = new BufferedInputStream(new FileInputStream(scriptFile))) {
            String script = getStreamContentAsString(scriptContent);
            script = fixImport(script);

            return script;
        }
    }

    /** All commands after {@link #FINAL_RELATION} is considering as redundant. */
    private static String removeRedundantCode(String script) throws IOException {
        int pos = script.indexOf(FINAL_RELATION);
        if (pos < 0) {
            return script;
        }

        int endLine = script.indexOf(";", pos);
        if (endLine < 0) {
            throw new IOException("");
        }

        return script.substring(0, endLine + 1);
    }

    /** Set the absolute paths to script in imports. */
    private static String fixImport(String script) throws IOException {
        int lastPos = 0;
        final String regex = "IMPORT\\s'(.+\\.pig)';";
        final StringBuilder builder = new StringBuilder();

        Pattern importPattern = Pattern.compile(regex);

        Matcher matcher = importPattern.matcher(script);
        while (matcher.find()) {
            builder.append(script.substring(lastPos, matcher.start()));

            File importFile = getMacroFile(regex, script, matcher);
            if (!importedMacros.contains(importFile.getAbsolutePath())) {
                builder.append("IMPORT '");
                builder.append(importFile.getAbsolutePath().replace("\\", "/"));
                builder.append("';");

                importedMacros.add(importFile.getAbsolutePath());
            }

            lastPos = matcher.end();
        }
        builder.append(script.substring(lastPos));

        return builder.toString();
    }

    /** Extracts relative path to pig script out of IMPORT command. */
    private static File getMacroFile(final String regex, String scriptContent, Matcher matcher)
            throws IOException {
        String importCommand = scriptContent.substring(matcher.start(), matcher.end());
        String importFileName = importCommand.replaceAll(regex, "$1");

        File importFile = new File(SCRIPTS_DIR, importFileName);
        if (!importFile.exists()) {
            throw new IOException("Resource " + importFile + " not found");
        }
        return importFile;
    }

    /** Reads a stream until its end and returns its content as a byte array. */
    private static String getStreamContentAsString(InputStream is) throws IOException, IllegalArgumentException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];

            for (int read = is.read(data); read > -1; read = is.read(data)) {
                output.write(data, 0, read);
            }

            return new String(output.toByteArray(), "UTF-8");
        }
    }
}
