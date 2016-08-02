/*
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.api.workspace;

import com.codenvy.api.ErrorCodes;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Striped;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.WorkspaceRuntimes;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.lang.Size;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.everrest.core.impl.provider.json.JsonUtils;
import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPED;
import static org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent.EventType.ERROR;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Manager that checks limits and delegates all its operations to the {@link WorkspaceManager}.
 * Doesn't contain any logic related to start/stop or any kind of operations different from limits checks.
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class LimitsCheckingWorkspaceManager extends WorkspaceManager {

    private static final Logger LOG = LoggerFactory.getLogger(LimitsCheckingWorkspaceManager.class);

    private static final DecimalFormat DECIMAL_FORMAT                          = new DecimalFormat("#0.#");
    private static final Striped<Lock> CREATE_LOCKS                            = Striped.lazyWeakLock(100);
    private static final Striped<Lock> START_LOCKS                             = Striped.lazyWeakLock(100);
    private static final String        SYSTEM_RAM_LIMIT_EXCEEDED_ERROR_MESSAGE = "Low Resources. System's resources are not allowing" +
                                                                                 " any workspaces to be started.";
    private static final String        USER_RAM_LIMIT_EXCEEDED_ERROR_MESSAGE   = "There are %d running workspaces consuming " +
                                                                                 "%sGB RAM. Your current RAM limit is %sGB. " +
                                                                                 "This workspaces requires an additional %sGB. " +
                                                                                 "You can stop other workspaces to free resources.";

    private final DockerConnector dockerConnector;
    private final EventService    eventService;
    private final UserManager     userManager;
    private final int             workspacesPerUser;
    private final long            maxRamPerEnv;
    private final long            ramPerUser;

    private ScheduledExecutorService executor;

    @Inject
    public LimitsCheckingWorkspaceManager(@Named("limits.user.workspaces.count") int workspacesPerUser,
                                          @Named("limits.user.workspaces.ram") String ramPerUser,
                                          @Named("limits.workspace.env.ram") String maxRamPerEnv,
                                          DockerConnector dockerConnector,
                                          WorkspaceDao workspaceDao,
                                          WorkspaceRuntimes runtimes,
                                          EventService eventService,
                                          MachineManager machineManager,
                                          UserManager userManager,
                                          @Named("workspace.runtime.auto_snapshot") boolean defaultAutoSnapshot,
                                          @Named("workspace.runtime.auto_restore") boolean defaultAutoRestore) {
        super(workspaceDao, runtimes, eventService, machineManager, defaultAutoSnapshot, defaultAutoRestore);
        this.dockerConnector = dockerConnector;
        this.eventService = eventService;
        this.userManager = userManager;
        this.workspacesPerUser = workspacesPerUser;
        this.maxRamPerEnv = "-1".equals(maxRamPerEnv) ? -1 : Size.parseSizeToMegabytes(maxRamPerEnv);
        this.ramPerUser = "-1".equals(ramPerUser) ? -1 : Size.parseSizeToMegabytes(ramPerUser);
    }

    @Override
    public WorkspaceImpl createWorkspace(WorkspaceConfig config,
                                         String namespace,
                                         @Nullable String accountId) throws ServerException,
                                                                            ConflictException,
                                                                            NotFoundException {
        checkMaxEnvironmentRam(config);
        checkNamespaceValidity(namespace, "Unable to create workspace because its namespace owner is " +
                                          "unavailable and it is impossible to check resources limit.");
        return checkCountAndPropagateCreation(namespace, () -> super.createWorkspace(config, namespace, accountId));
    }

    @Override
    public WorkspaceImpl createWorkspace(WorkspaceConfig config,
                                         String namespace,
                                         Map<String, String> attributes,
                                         @Nullable String accountId) throws ServerException,
                                                                            NotFoundException,
                                                                            ConflictException {
        checkMaxEnvironmentRam(config);
        checkNamespaceValidity(namespace, "Unable to create workspace because its namespace owner is " +
                                          "unavailable and it is impossible to check resources limit.");
        return checkCountAndPropagateCreation(namespace, () -> super.createWorkspace(config, namespace, attributes, accountId));
    }

    @Override
    public WorkspaceImpl startWorkspace(String workspaceId,
                                        @Nullable String envName,
                                        @Nullable String accountId,
                                        @Nullable Boolean restore) throws NotFoundException,
                                                                          ServerException,
                                                                          ConflictException {
        final WorkspaceImpl workspace = getWorkspace(workspaceId);
        checkNamespaceValidity(workspace.getNamespace(), String.format(
                "Unable to start workspace %s, because its namespace owner is " +
                "unavailable and it is impossible to check resources consumption.",
                workspaceId));
        return checkRamAndPropagateStart(workspaceId,
                                         workspace.getConfig(),
                                         envName,
                                         workspace.getNamespace(),
                                         () -> super.startWorkspace(workspaceId, envName, accountId, restore));
    }

    @Override
    public WorkspaceImpl startWorkspace(WorkspaceConfig config,
                                        String namespace,
                                        boolean isTemporary,
                                        @Nullable String accountId) throws ServerException,
                                                                           NotFoundException,
                                                                           ConflictException {
        checkMaxEnvironmentRam(config);
        return checkRamAndPropagateStart(null,
                                         config,
                                         config.getDefaultEnv(),
                                         namespace,
                                         () -> super.startWorkspace(config, namespace, isTemporary, accountId));
    }

    @Override
    public WorkspaceImpl updateWorkspace(String id, Workspace update) throws ConflictException,
                                                                             ServerException,
                                                                             NotFoundException {
        checkMaxEnvironmentRam(update.getConfig());
        return super.updateWorkspace(id, update);
    }

    /**
     * Defines callback which should be called when all necessary checks are performed.
     * Helps to propagate actions to the super class.
     */
    @FunctionalInterface
    @VisibleForTesting
    interface WorkspaceCallback<T extends WorkspaceImpl> {
        T call() throws ConflictException, NotFoundException, ServerException;
    }

    /**
     * Checks that whole system ram limit is not exceeded, and starting workspace won't exceed user's RAM limit.
     * Throws {@link LimitExceededException} in the case of RAM constraint violation, otherwise
     * performs {@code callback.call()} and returns its result.
     */
    @VisibleForTesting
    <T extends WorkspaceImpl> T checkRamAndPropagateStart(@Nullable String workspaceId,
                                                          WorkspaceConfig config,
                                                          String envName,
                                                          String namespace,
                                                          WorkspaceCallback<T> callback) throws ServerException,
                                                                                                NotFoundException, ConflictException {
        // check system's RAM
        if (systemRamLimitExceeded()) {
            trackRamLimitIfNotTracked();
            if (workspaceId != null) {
                publishError(workspaceId, SYSTEM_RAM_LIMIT_EXCEEDED_ERROR_MESSAGE);
            }
            throw new LimitExceededException(SYSTEM_RAM_LIMIT_EXCEEDED_ERROR_MESSAGE, ErrorCodes.SYSTEM_RAM_LIMIT_EXCEEDED);
        }

        // check user's RAM
        if (ramPerUser < 0) {
            return callback.call();
        }
        Optional<? extends Environment> envOptional = findEnv(config.getEnvironments(), envName);
        if (!envOptional.isPresent()) {
            envOptional = findEnv(config.getEnvironments(), config.getDefaultEnv());
        }
        // It is important to lock in this place because:
        // if ram per user limit is 2GB and user currently using 1GB, then if he sends 2 separate requests to start a new
        // 1 GB workspace , it may start both of them, because currently allocated ram check is not atomic one
        final Lock lock = START_LOCKS.get(namespace);
        lock.lock();
        try {
            final List<WorkspaceImpl> workspacesPerUser = getByNamespace(namespace);
            final long runningWorkspaces = workspacesPerUser.stream().filter(ws -> STOPPED != ws.getStatus()).count();
            final long currentlyUsedRamMB = workspacesPerUser.stream().filter(ws -> STOPPED != ws.getStatus())
                                                             .map(ws -> ws.getConfig()
                                                                          .getEnvironment(ws.getRuntime().getActiveEnv())
                                                                          .get()
                                                                          .getMachineConfigs())
                                                             .mapToLong(this::sumRam)
                                                             .sum();
            final long currentlyFreeRamMB = ramPerUser - currentlyUsedRamMB;
            final long allocating = sumRam(envOptional.get().getMachineConfigs());
            if (allocating > currentlyFreeRamMB) {
                final String usedRamGb = DECIMAL_FORMAT.format(currentlyUsedRamMB / 1024D);
                final String limitRamGb = DECIMAL_FORMAT.format(ramPerUser / 1024D);
                final String requiredRamGb = DECIMAL_FORMAT.format(allocating / 1024D);
                if (workspaceId != null) {
                    publishError(workspaceId,
                                 format(USER_RAM_LIMIT_EXCEEDED_ERROR_MESSAGE, runningWorkspaces, usedRamGb, limitRamGb, requiredRamGb));
                }
                throw new LimitExceededException(format(USER_RAM_LIMIT_EXCEEDED_ERROR_MESSAGE,
                                                        runningWorkspaces,
                                                        usedRamGb,
                                                        limitRamGb,
                                                        requiredRamGb),
                                                 ImmutableMap.of("workspaces_count", Long.toString(runningWorkspaces),
                                                                 "used_ram", usedRamGb,
                                                                 "limit_ram", limitRamGb,
                                                                 "required_ram", requiredRamGb,
                                                                 "ram_unit", "GB"));
            }
            return callback.call();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sends message about system ram limit exceeded and starts scheduled task
     * that checks existence of more than 10% of free ram in the system.
     * If the check discovered that it is more than 10% of free ram in the system
     * it sends message about that and stops scheduler.
     * If this method called when scheduler is already in progress it will do nothing.
     */
    private void trackRamLimitIfNotTracked() {
        if (executor == null || executor.isShutdown()) {
            sendMessage("system_ram_limit_exceeded");

            executor = Executors.newSingleThreadScheduledExecutor(
                    new ThreadFactoryBuilder().setNameFormat("RAMLimitCheckScheduler-%d").setDaemon(true).build());

            executor.scheduleAtFixedRate(() -> {
                if (!systemRamLimitExceeded()) {
                    sendMessage("system_ram_limit_not_exceeded");
                    executor.shutdown();
                }
            }, 0, 10, SECONDS);
        }
    }

    private void publishError(String workspaceId, String errorMessage) {
        eventService.publish(newDto(WorkspaceStatusEvent.class)
                                     .withEventType(ERROR)
                                     .withWorkspaceId(workspaceId)
                                     .withError(errorMessage));
    }

    /**
     * Checks that created workspace won't exceed user's workspaces limit.
     * Throws {@link BadRequestException} in the case of workspace limit constraint violation, otherwise
     * performs {@code callback.call()} and returns its result.
     */
    @VisibleForTesting
    <T extends WorkspaceImpl> T checkCountAndPropagateCreation(String namespace,
                                                               WorkspaceCallback<T> callback) throws ServerException,
                                                                                                     NotFoundException,
                                                                                                     ConflictException {
        if (workspacesPerUser < 0) {
            return callback.call();
        }
        // It is important to lock in this place because:
        // if workspace per user limit is 10 and user has 9, then if he sends 2 separate requests to create
        // a new workspace, it may create both of them, because workspace count check is not atomic one
        final Lock lock = CREATE_LOCKS.get(namespace);
        lock.lock();
        try {
            final List<WorkspaceImpl> workspaces = getByNamespace(namespace);
            if (workspaces.size() >= workspacesPerUser) {
                throw new LimitExceededException(format("The maximum workspaces allowed per user is set to '%d' and " +
                                                        "you are currently at that limit. This value is set by your admin with the " +
                                                        "'limits.user.workspaces.count' property",
                                                        workspacesPerUser),
                                                 ImmutableMap.of("workspace_max_count", Integer.toString(workspacesPerUser)));
            }
            return callback.call();
        } finally {
            lock.unlock();
        }
    }

    @VisibleForTesting
    void checkMaxEnvironmentRam(WorkspaceConfig config) throws LimitExceededException {
        if (maxRamPerEnv < 0) {
            return;
        }
        for (Environment environment : config.getEnvironments()) {
            final long workspaceRam = environment.getMachineConfigs()
                                                 .stream()
                                                 .filter(machineCfg -> machineCfg.getLimits() != null)
                                                 .mapToInt(machineCfg -> machineCfg.getLimits().getRam())
                                                 .sum();
            if (workspaceRam > maxRamPerEnv) {
                throw new LimitExceededException(format("The maximum RAM per workspace is set to '%dmb' and you requested '%dmb'. " +
                                                        "This value is set by your admin with the 'limits.workspace.env.ram' property",
                                                        maxRamPerEnv,
                                                        workspaceRam),
                                                 ImmutableMap.of("environment_max_ram", Long.toString(maxRamPerEnv),
                                                                 "environment_max_ram_unit", "mb",
                                                                 "environment_ram", Long.toString(workspaceRam),
                                                                 "environment_ram_unit", "mb"));
            }
        }
    }

    private long sumRam(List<? extends MachineConfig> machineConfigs) {
        return machineConfigs.stream()
                             .mapToInt(m -> m.getLimits().getRam())
                             .sum();
    }

    private Optional<? extends Environment> findEnv(List<? extends Environment> environments, String envName) {
        return environments.stream()
                           .filter(env -> env.getName().equals(envName))
                           .findFirst();
    }


    private void checkNamespaceValidity(String namespace, String errorMsg) throws ServerException {
        try {
            userManager.getByName(namespace);
        } catch (NotFoundException e) {
            throw new ServerException(errorMsg);
        }
    }

    private boolean systemRamLimitExceeded() {
        String ramUsage = null;
        try {
            ramUsage = dockerConnector.getSystemInfo().ramUsage();
        } catch (IOException e) {
            LOG.error("A problem occurred while getting system information from docker", e);
        }
        if (ramUsage == null) {
            return false;
        }
        String[] ramValues = ramUsage.split("/ ");
        if (ramValues.length < 2) {
            LOG.error("A problem occurred while parsing system information from docker");
            return false;
        }
        long ramUsed = getBytesAmountFromString(ramValues[0]);
        long ramTotal = getBytesAmountFromString(ramValues[1]);
        return ((ramUsed * 100) / ramTotal > 90);
    }

    private void sendMessage(String line) {
        final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
        bm.setChannel("resources_chanel");
        bm.setBody(JsonUtils.getJsonString(line));
        try {
            WSConnectionContext.sendMessage(bm);
        } catch (Exception e) {
            LOG.error("A problem occurred while sending websocket message", e);
        }
    }

    private long getBytesAmountFromString(String string) {
        if (string.contains("KiB")) {
            return getValue(string) * 1024;
        }else if (string.contains("MiB")){
            return getValue(string) * 1024 * 1024 ;
        } else if (string.contains("GiB")) {
            return getValue(string) * 1024 * 1024 * 1024;
        } else if (string.contains("TiB")) {
            return getValue(string) * 1024 * 1024 * 1024 * 1024;
        } else {
            return getValue(string);
        }
    }

    private long getValue(String string) {
        return Math.round(Float.parseFloat(string.substring(0, string.indexOf(" "))));
    }
}
