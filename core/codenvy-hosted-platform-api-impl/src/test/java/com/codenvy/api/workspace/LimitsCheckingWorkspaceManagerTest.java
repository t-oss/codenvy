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

import com.codenvy.api.workspace.LimitsCheckingWorkspaceManager.WorkspaceCallback;
import com.google.common.collect.ImmutableList;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.json.SystemInfo;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.codenvy.api.workspace.TestObjects.createConfig;
import static com.codenvy.api.workspace.TestObjects.createRuntime;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LimitsCheckingWorkspaceManager}.
 *
 * @author Yevhenii Voevodin
 */
public class LimitsCheckingWorkspaceManagerTest {

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum workspaces allowed per user is set to '2' and you are currently at that limit. " +
                                            "This value is set by your admin with the 'limits.user.workspaces.count' property")
    public void shouldNotBeAbleToCreateNewWorkspaceIfLimitIsExceeded() throws Exception {
        final DockerConnector dockerConnector= mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "0 B / 3 GiB"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2, // <- workspaces max count
                                                                                              "2gb",
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(ImmutableList.of(mock(WorkspaceImpl.class), mock(WorkspaceImpl.class))) // <- currently used 2
                                                                                         .when(manager)
                                                                                         .getByNamespace(anyString());

        manager.checkCountAndPropagateCreation("user123", null);
    }

    @Test
    public void shouldNotCheckAllowedWorkspacesPerUserWhenItIsSetToMinusOne() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(-1, // <- workspaces max count
                                                                                              "2gb",
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(ImmutableList.of(mock(WorkspaceImpl.class), mock(WorkspaceImpl.class))) // <- currently used 2
                                                                                         .when(manager)
                                                                                         .getByNamespace(anyString());
        final WorkspaceCallback callback = mock(WorkspaceCallback.class);

        manager.checkCountAndPropagateCreation("user123", callback);

        verify(callback).call();
        verify(manager, never()).getWorkspaces(any());
    }


    @Test
    public void shouldCallCreateCallBackIfEverythingIsOkayWithLimits() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2, // <- workspaces max count
                                                                                              "2gb",
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(emptyList()).when(manager).getByNamespace(anyString()); // <- currently used 0

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkCountAndPropagateCreation("user123", callback);

        verify(callback).call();
    }

    @Test(expectedExceptions = LimitExceededException.class,
            expectedExceptionsMessageRegExp = "There are 1 running workspaces consuming 2GB RAM. Your current RAM " +
                                              "limit is 2GB. This workspaces requires an additional 1GB. You can stop other workspaces to free resources.")
    public void shouldNotBeAbleToStartNewWorkspaceIfUserRamLimitIsExceeded() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "0 B / 3 GiB"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        manager.checkRamAndPropagateStart(null, createConfig("1gb"), null, "user123", null);
    }

    @Test
    public void shouldSendErrorEventWhenStartingNewWorkspaceIfUserRamLimitIsExceeded() throws Exception {
        final EventService eventService = mock(EventService.class);
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "0 B / 3 GiB"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              eventService,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        try {
            manager.checkRamAndPropagateStart("workspaceId", createConfig("1gb"), null, "user123", null);
        } catch (ServerException ignored) {
        }

        verify(eventService).publish(anyObject());
    }

    @Test
    public void shouldSkipWorkspacesRamCheckIfItIsSetToMinusOne() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "0 B / 3 GiB"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "-1", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb
        final WorkspaceCallback callback = mock(WorkspaceCallback.class);

        manager.checkRamAndPropagateStart(null, createConfig("1gb"), null, "user123", callback);

        verify(callback).call();
        verify(manager, never()).getWorkspaces(any());
    }


    @Test
    public void shouldCallStartCallbackIfEverythingIsOkayWithLimits() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "0 B / 3 GiB"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkRamAndPropagateStart(null, createConfig("1gb"), null, "user123", callback);

        verify(callback).call();
    }

    @Test
    public void shouldCallStartCallbackIfDockerSystemAndDriverStatusInfoIsNull() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(null);
        when(systemInfo.getSystemStatus()).thenReturn(null);
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkRamAndPropagateStart(null, createConfig("1gb"), null, "user123", callback);

        verify(callback).call();
    }

    @Test
    public void shouldCallStartCallbackIfFailedToGetDockerSystemInfo() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        when(dockerConnector.getSystemInfo()).thenThrow(IOException.class);
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkRamAndPropagateStart(null, createConfig("1gb"), null, "user123", callback);

        verify(callback).call();
    }

    @Test
    public void shouldCallStartCallbackIfFailedToRecognizeDockerSystemInfo() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{"Unrecognized value", "Unrecognized value"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkRamAndPropagateStart(null, createConfig("1gb"), null, "user123", callback);

        verify(callback).call();
    }

    @Test
    public void shouldCallStartCallbackIfFailedToRecognizeSystemRamValues() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "Unrecognized value"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkRamAndPropagateStart(null, createConfig("1gb"), null, "user123", callback);

        verify(callback).call();
    }

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "Low RAM. Your workspace cannot be started until the system has more RAM available.")
    public void shouldNotBeAbleToStartWorkspaceWhichExceedsSystemRamLimit() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][]{{" └ Reserved Memory", "800 MiB / 1 GiB"},
                                                                     {" └ Reserved Memory", "0.99 GiB / 1 GiB"},
                                                                     {" └ Reserved Memory", "0.95 GiB / 1 GiB"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "2gb", // <- workspaces env ram limit
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkRamAndPropagateStart(null, createConfig("1gb"), null, "user123", callback);
    }

    @Test
    public void shouldSendErrorEventWhenStartingWorkspaceWhichExceedsSystemRamLimit() throws Exception{
        final EventService eventService = mock(EventService.class);
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "2.9 GiB / 3 GiB"}});
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "2gb", // <- workspaces env ram limit
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              eventService,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        try {
            manager.checkRamAndPropagateStart("workspaceId", createConfig("1gb"), null, "user123", callback);
        } catch (ServerException ignored) {
        }

        verify(eventService).publish(anyObject());
    }

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum RAM per workspace is set to '2048mb' and you requested '3072mb'. " +
                                            "This value is set by your admin with the 'limits.workspace.env.ram' property")
    public void shouldNotBeAbleToCreateWorkspaceWhichExceedsUserRamLimit() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "2.9 GiB / 3 GiB"}});
        final WorkspaceConfig config = createConfig("3gb");
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "2gb", // <- workspaces env ram limit
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));

        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldNotCheckWorkspaceRamLimitIfItIsSetToMinusOne() throws Exception {
        final WorkspaceConfig config = createConfig("3gb");
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "-1", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));

        manager.checkMaxEnvironmentRam(config);
    }

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum RAM per workspace is set to '2048mb' and you requested '2304mb'. " +
                                            "This value is set by your admin with the 'limits.workspace.env.ram' property")
    public void shouldNotBeAbleToCreateWorkspaceWithMultipleMachinesWhichExceedsRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("1gb", "1gb", "256mb");

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "2gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldBeAbleToCreateWorkspaceWithMultipleMachinesWhichDoesNotExceedRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("1gb", "1gb", "256mb");

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "3gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              false,
                                                                                              false));
        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldCheckRamLimitOfCreatorUserInsteadOfCurrent() throws Exception {
        final DockerConnector dockerConnector = mock(DockerConnector.class);
        final SystemInfo systemInfo = mock(SystemInfo.class);
        when(dockerConnector.getSystemInfo()).thenReturn(systemInfo);
        when(systemInfo.getDriverStatus()).thenReturn(new String[][] {{" └ Reserved Memory", "0 B / 3 GiB"}});
        final UserManager userManager = mock(UserManager.class);
        final WorkspaceImpl ws = createRuntime("1gb", "1gb");
        final UserImpl user = new UserImpl("id", "email", ws.getNamespace());
        user.setName(ws.getNamespace());
        doReturn(user).when(userManager).getByName(eq(ws.getNamespace()));

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              dockerConnector,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              userManager,
                                                                                              false,
                                                                                              false));

        doReturn(ws).when(manager).getWorkspace(anyString()); // <- currently running 2gb
        doReturn(ws).when(manager).checkRamAndPropagateStart(anyObject(), anyObject(), anyString(), anyString(), anyObject());

        manager.startWorkspace(ws.getId(), "envName", "accountId", true);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(manager).checkRamAndPropagateStart(anyObject(), anyObject(), anyString(), argument.capture(), anyObject());
        verify((WorkspaceManager)manager).startWorkspace(ws.getId(), "envName", "accountId", true);
        Assert.assertEquals(argument.getValue(), ws.getNamespace());
    }

    @Test(expectedExceptions = ServerException.class,
          expectedExceptionsMessageRegExp = "Unable to start workspace .*, because its namespace owner is " +
                                            "unavailable and it is impossible to check resources consumption.")
    public void shouldPreventStartIfCreatorNotExistsAnymore() throws Exception {
        final UserManager userManager = mock(UserManager.class);
        final WorkspaceImpl ws = createRuntime("1gb", "1gb");
        doThrow(new NotFoundException("Nope")).when(userManager).getByName(eq(ws.getNamespace()));


        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              userManager,
                                                                                              false,
                                                                                              false));
        doReturn(ws).when(manager).getWorkspace(anyString()); // <- currently running 2gb

        manager.startWorkspace(ws.getId(), null, null, null);
    }
}
