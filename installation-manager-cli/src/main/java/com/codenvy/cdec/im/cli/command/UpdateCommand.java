/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.cdec.im.cli.command;

import com.codenvy.cli.command.builtin.AbsCommand;

import org.apache.karaf.shell.commands.Command;
import org.fusesource.jansi.Ansi;

import java.io.IOException;

import static org.fusesource.jansi.Ansi.Color.GREEN;

/**
 * Parameters and execution of 'CDEC:update' command.
 *
 * @author Alexander Reshetnyak
 */
@Command(scope = "cdec", name = "update", description = "Update CDEC...")
public class UpdateCommand extends AbsCommand {

    /**
     * Update...
     */
    protected Object doExecute() throws IOException {
        init();

        /*// not logged in
        if (!checkifEnabledRemotes()) {
            return null;
        }*/

        Ansi buffer = Ansi.ansi();
        buffer.fg(GREEN);
        buffer.a("Update CDEC is not yet implement...");
        buffer.reset();
        System.out.println(buffer.toString());

        return null;
    }
}