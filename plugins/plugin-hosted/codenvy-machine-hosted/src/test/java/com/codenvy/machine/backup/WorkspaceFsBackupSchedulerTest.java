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
package com.codenvy.machine.backup;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.machine.MachineStatus;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.LimitsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineRuntimeInfoImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineSourceImpl;
import org.eclipse.che.api.machine.server.model.impl.ServerConfImpl;
import org.eclipse.che.api.machine.server.model.impl.ServerImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceNode;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Thread.sleep;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Garagatyi
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkspaceFsBackupSchedulerTest {
    private static final int FAKE_BACKUP_TIME_MS = 1500;

    @Mock
    private MachineManager machineManager;
    @Mock
    private Recipe         recipe;

    @Mock
    private MachineBackupManager backupManager;

    private WorkspaceFsBackupScheduler scheduler;

    MachineImpl machine1;

    MachineImpl machine2;

    final List<MachineImpl> machines = new ArrayList<>();

    @Mock
    private Instance machineInstance;

    @Mock
    private InstanceNode node;

    @BeforeMethod
    public void setUp() throws Exception {
        scheduler = spy(new WorkspaceFsBackupScheduler(machineManager, backupManager, 5 * 60));

        when(machineManager.getMachines()).thenReturn(machines);
        when(machineInstance.getStatus()).thenReturn(MachineStatus.RUNNING);

        machine1 = new MachineImpl(new MachineConfigImpl(true,
                                                         "displayName1",
                                                         "type1",
                                                         new MachineSourceImpl("sourcetype1", "location1", "content1"),
                                                         new LimitsImpl(1024),
                                                         Arrays.asList(new ServerConfImpl("ref1",
                                                                                          "8080",
                                                                                          "https",
                                                                                          "some/path"),
                                                                       new ServerConfImpl("ref2",
                                                                                          "9090/udp",
                                                                                          "someprotocol",
                                                                                          "/some/path")),
                                                         Collections.singletonMap("key1", "value1")),
                                   "id1",
                                   "workspaceId1",
                                   "envName1",
                                   "owner1",
                                   MachineStatus.RUNNING,
                                   new MachineRuntimeInfoImpl(singletonMap("var1", "value1"),
                                                              singletonMap("prop1", "pvalue1"),
                                                              singletonMap("8080", new ServerImpl("ref1",
                                                                                                  "http",
                                                                                                  "address1",
                                                                                                  "some/path",
                                                                                                  "url1"))));

        machine2 = new MachineImpl(new MachineConfigImpl(true,
                                                         "displayName2",
                                                         "type2",
                                                         new MachineSourceImpl("sourcetype2", "location2", "content2"),
                                                         new LimitsImpl(1024),
                                                         Arrays.asList(new ServerConfImpl("ref1",
                                                                                          "8080",
                                                                                          "https",
                                                                                          "some/path"),
                                                                       new ServerConfImpl("ref2",
                                                                                          "9090/udp",
                                                                                          "someprotocol",
                                                                                          "/some/path")),
                                                         Collections.singletonMap("key1", "value1")),
                                   "id2",
                                   "workspaceId2",
                                   "envName1",
                                   "owner2",
                                   MachineStatus.RUNNING,
                                   new MachineRuntimeInfoImpl(singletonMap("var2", "value2"),
                                                              singletonMap("prop2", "pvalue2"),
                                                              singletonMap("8080", new ServerImpl("ref2",
                                                                                                  "https",
                                                                                                  "address2",
                                                                                                  "/some/path",
                                                                                                  "url2"))));

        machines.add(machine1);
        machines.add(machine2);

        doNothing().when(scheduler).backupWorkspaceInMachine(any(MachineImpl.class));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        machines.clear();
    }

    @Test
    public void shouldBackupWs() throws Exception {
        // given
        when(machineManager.getInstance("id1")).thenReturn(machineInstance);
        when(machineInstance.getNode()).thenReturn(node);
        when(node.getHost()).thenReturn("192.168.0.1");
        when(node.getProjectsFolder()).thenReturn("/workspace1");
        doCallRealMethod().when(scheduler).backupWorkspaceInMachine(any(MachineImpl.class));

        // when
        scheduler.backupWorkspaceInMachine(machine1);

        // then
        verify(machineManager).getInstance("id1");
        verify(scheduler).backupWorkspaceInMachine(any(MachineImpl.class));
        verify(backupManager).backupWorkspace("workspaceId1", "/workspace1", "192.168.0.1");
    }

    @Test
    public void shouldBackupWorkspaceFsOfMachines() throws Exception {
        // when
        scheduler.scheduleBackup();

        // then
        verify(scheduler, timeout(2000)).backupWorkspaceInMachine(eq(machine1));
        verify(scheduler, timeout(2000)).backupWorkspaceInMachine(eq(machine2));
    }

    @Test
    public void shouldBackupOtherMachinesIfBackupOfPreviousFails() throws Exception {
        // given
        doThrow(new ServerException("server exception")).when(scheduler).backupWorkspaceInMachine(eq(machine1));
        doNothing().when(scheduler).backupWorkspaceInMachine(eq(machine2));

        // when
        scheduler.scheduleBackup();

        // then
        // add this verification with timeout to ensure that thread executor had enough time before verification of call
        verify(scheduler, timeout(2000)).backupWorkspaceInMachine(eq(machine1));
        InOrder inOrder = inOrder(scheduler);
        // ensure that backup of first machine was started and its fails doesn't affect backup of next machine
        inOrder.verify(scheduler).backupWorkspaceInMachine(eq(machine1));
        inOrder.verify(scheduler).backupWorkspaceInMachine(eq(machine2));
    }

    @Test
    public void shouldNotBackupWorkspaceOfNonDevMachines() throws Exception {
        // given
        machines.clear();
        machines.add(machine1);
        machines.add(MachineImpl.builder()
                                .fromMachine(machine2)
                                .setConfig(MachineConfigImpl.builder()
                                                            .fromConfig(machine2.getConfig())
                                                            .setDev(false)
                                                            .build())
                                .build());

        // when
        scheduler.scheduleBackup();

        // then
        // add this verification with timeout to ensure that thread executor had enough time before verification of call
        verify(scheduler, timeout(2000)).backupWorkspaceInMachine(eq(machine1));
        verify(scheduler, never()).backupWorkspaceInMachine(eq(machine2));
    }

    @Test
    public void shouldNotBackupMachinesWithNonRunningStatus() throws Exception {
        // given
        final MachineImpl creatingMachine = new MachineImpl(machine2);
        creatingMachine.setStatus(MachineStatus.CREATING);
        machines.add(creatingMachine);
        final MachineImpl destroyingMachine = new MachineImpl(machine2);
        destroyingMachine.setStatus(MachineStatus.DESTROYING);
        machines.add(destroyingMachine);

        // when
        scheduler.scheduleBackup();

        // then
        // add this verification with timeout to ensure that thread executor had enough time before verification of call
        verify(scheduler, timeout(2000)).backupWorkspaceInMachine(eq(machine1));
        verify(scheduler, timeout(1000).never()).backupWorkspaceInMachine(eq(creatingMachine));
        verify(scheduler, timeout(1000).never()).backupWorkspaceInMachine(eq(destroyingMachine));
    }

    @Test
    public void shouldNotBackupAnythingIfMachinesListRetrievalFails() throws Exception {
        // given
        when(machineManager.getMachines()).thenThrow(new MachineException(""));

        // when
        scheduler.scheduleBackup();

        // then
        verify(machineManager).getMachines();
        verify(scheduler, never()).backupWorkspaceInMachine(any(MachineImpl.class));
    }

    @Test
    public void shouldNotBackupMachineIfElapsedTimeFromLastSyncTooSmall() throws Exception {
        // given
        machines.clear();
        machines.add(machine1);
        scheduler.scheduleBackup();

        // add this verification with timeout to ensure that thread executor had enough time before verification of call
        verify(scheduler, timeout(2000)).backupWorkspaceInMachine(eq(machine1));

        // when
        // second synchronization
        scheduler.scheduleBackup();

        verify(machineManager, times(2)).getMachines();
        verify(scheduler, times(2)).isTimeToBackup(machine1.getId());
        verify(scheduler, timeout(2000)).backupWorkspaceInMachine(eq(machine1));
    }

    @Test
    public void shouldBackupMachineFsIfLastSyncTimeoutIsExpired() throws Exception {
        // given
        machines.clear();
        machines.add(machine1);
        scheduler = spy(new WorkspaceFsBackupScheduler(machineManager, backupManager, 0));
        doNothing().when(scheduler).backupWorkspaceInMachine(any(MachineImpl.class));

        scheduler.scheduleBackup();

        // wait until previous backup finish
        sleep(100);

        // when
        // second synchronization
        scheduler.scheduleBackup();

        // then
        verify(machineManager, times(2)).getMachines();
        verify(scheduler, timeout(2000).times(2)).backupWorkspaceInMachine(eq(machine1));
    }

    @Test
    public void shouldNotBackupMachineFsIfPreviousBackupIsStillRunning() throws Exception {
        // given
        machines.clear();
        machines.add(machine1);
        scheduler = spy(new WorkspaceFsBackupScheduler(machineManager, backupManager, 0));
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                sleep(FAKE_BACKUP_TIME_MS);
                return null;
            }
        }).when(scheduler).backupWorkspaceInMachine(any(MachineImpl.class));

        scheduler.scheduleBackup();
        sleep(FAKE_BACKUP_TIME_MS / 2);
        // run next backup while previous is still running
        scheduler.scheduleBackup();

        // then
        verify(machineManager, times(2)).getMachines();
        verify(scheduler, timeout(2000).times(1)).backupWorkspaceInMachine(eq(machine1));
    }

}
