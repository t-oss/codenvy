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
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

/**
 * @author Mykola Morhun
 */
@Listeners(value = {MockitoTestNGListener.class})
public class MachineBackupManagerTest {

    private static final String WORKSPACE_ID                   = "workspaceId";
    private static final String BACKUP_SCRIPT                  = "/tmp/backup.sh";
    private static final String RESTORE_SCRIPT                 = "/tmp/restore.sh";
    private static final int    MAX_BACKUP_DURATION_SEC        = 10;
    private static final int    MAX_RESTORE_DURATION_SEC       = 10;
    private static final String BACKUPS_ROOT_PATH              = "/tmp/che/backups";
    private static final String SRC_PATH                       = "srcPath";
    private static final String SRC_ADDRESS                    = "srcAddress";
    private static final String DEST_PATH                      = "/tmp/restore.sh";
    private static final String DEST_ADDRESS                   = "/tmp/restore.sh";
    private static final String USER_ID                        = "1000";
    private static final String USER_GID                       = "1000";
    private static final String PATH_TO_WORKSPACE              = "00/00/00/";
    private static final String ABSOLUTE_PATH_TO_WORKSPACE_DIR = BACKUPS_ROOT_PATH + PATH_TO_WORKSPACE + WORKSPACE_ID;

    private static final String[] BACKUP_WORKSPACE_COMMAND              =
            {BACKUP_SCRIPT, SRC_PATH, SRC_ADDRESS, ABSOLUTE_PATH_TO_WORKSPACE_DIR, "false"};
    private static final String[] BACKUP_WORKSPACE_WITH_CLEANUP_COMMAND =
            {BACKUP_SCRIPT, SRC_PATH, SRC_ADDRESS, ABSOLUTE_PATH_TO_WORKSPACE_DIR, "true"};
    private static final String[] RESTORE_WORKSPACE_COMMAND             =
            {RESTORE_SCRIPT, ABSOLUTE_PATH_TO_WORKSPACE_DIR, RESTORE_SCRIPT, RESTORE_SCRIPT, USER_ID, USER_GID};

    private static final int FAKE_BACKUP_TIME_MS = 2000;

    @Mock
    private WorkspaceIdHashLocationFinder workspaceIdHashLocationFinder;

    private ArgumentCaptor<String[]> cmdCaptor;

    private ExecutorService executor;

    private MachineBackupManager backupManager;

    @BeforeMethod
    private void setup() throws ServerException, InterruptedException, IOException, TimeoutException {
        backupManager = spy(new MachineBackupManager(BACKUP_SCRIPT,
                                                     RESTORE_SCRIPT,
                                                     MAX_BACKUP_DURATION_SEC,
                                                     MAX_RESTORE_DURATION_SEC,
                                                     new File(BACKUPS_ROOT_PATH),
                                                     workspaceIdHashLocationFinder));

        when(workspaceIdHashLocationFinder.calculateDirPath(any(File.class), any(String.class)))
                .thenReturn(new File(ABSOLUTE_PATH_TO_WORKSPACE_DIR));

        cmdCaptor = ArgumentCaptor.forClass(String[].class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                sleep(FAKE_BACKUP_TIME_MS);
                return null;
            }
        }).when(backupManager).execute(anyObject(), anyInt());

        executor = Executors.newFixedThreadPool(5);
    }

    @AfterMethod
    private void cleanup() {
        executor.shutdownNow();
    }

    @Test
    public void shouldBeAbleBackupWorkspace() throws ServerException, InterruptedException, IOException, TimeoutException {
        doNothing().when(backupManager).execute(anyObject(), anyInt());
        injectWorkspaceLock(WORKSPACE_ID);

        backupManager.backupWorkspace(WORKSPACE_ID, SRC_PATH, SRC_ADDRESS);

        verify(backupManager).execute(cmdCaptor.capture(), eq(MAX_BACKUP_DURATION_SEC));

        String[] command = cmdCaptor.getValue();
        assertArrayEquals(BACKUP_WORKSPACE_COMMAND, command);
    }

    @Test
    public void shouldNotBackupWorkspaceAfterBackupWithCleanup() throws InterruptedException,
                                                                        IOException,
                                                                        TimeoutException,
                                                                        ServerException {
        doNothing().when(backupManager).execute(anyObject(), anyInt());

        injectWorkspaceLock(WORKSPACE_ID);
        backupManager.backupWorkspaceAndCleanup(WORKSPACE_ID, SRC_PATH, SRC_ADDRESS);

        backupManager.backupWorkspace(WORKSPACE_ID, SRC_PATH, SRC_ADDRESS);

        verify(backupManager).execute(anyObject(), anyInt());
    }

    @Test
    public void shouldBeAbleBackupWorkspaceWithCleanup() throws ServerException, InterruptedException, IOException, TimeoutException {
        doNothing().when(backupManager).execute(anyObject(), anyInt());

        injectWorkspaceLock(WORKSPACE_ID);
        backupManager.backupWorkspaceAndCleanup(WORKSPACE_ID, SRC_PATH, SRC_ADDRESS);

        verify(backupManager).execute(cmdCaptor.capture(), eq(MAX_BACKUP_DURATION_SEC));

        String[] command = cmdCaptor.getValue();
        assertArrayEquals(BACKUP_WORKSPACE_WITH_CLEANUP_COMMAND, command);
    }

    @Test
    public void shouldBeAbleRestoreWorkspace() throws ServerException, InterruptedException, IOException, TimeoutException {
        doNothing().when(backupManager).execute(anyObject(), anyInt());

        backupManager.restoreWorkspaceBackup(WORKSPACE_ID, DEST_PATH, USER_ID, USER_GID, DEST_ADDRESS);

        verify(backupManager).execute(cmdCaptor.capture(), eq(MAX_RESTORE_DURATION_SEC));

        String[] command = cmdCaptor.getValue();
        assertArrayEquals(RESTORE_WORKSPACE_COMMAND, command);
    }

    @Test
    public void shouldIgnoreNewBackupRequestIfPreviousOneHasBeenJustStarted() throws InterruptedException, IOException, TimeoutException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        executeNewBackup();

        awaitFinalization();

        verify(backupManager, times(1)).execute(anyObject(), anyInt());
    }

    @Test
    public void shouldIgnoreNewBackupRequestIfOldHasNotFinishedYet() throws InterruptedException, IOException, TimeoutException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 2);
        executeNewBackup();

        awaitFinalization();

        verify(backupManager, times(1)).execute(anyObject(), anyInt());
    }

    @Test
    public void shouldIgnoreAllNewBackupRequestIfOldHasNotFinishedYet() throws InterruptedException, IOException, TimeoutException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();

        awaitFinalization();

        verify(backupManager, times(1)).execute(anyObject(), anyInt());
    }

    @Test
    public void shouldIgnoreNewRestoreRequestIfPreviousOneHasBeenJustStarted() throws InterruptedException, IOException, TimeoutException {
        executeNewRestore();
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(1)).execute(anyObject(), anyInt());
    }

    @Test
    public void shouldIgnoreNewRestoreRequestIfOldHasNotFinishedYet() throws InterruptedException, IOException, TimeoutException {
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 2);
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(1)).execute(anyObject(), anyInt());
    }

    @Test
    public void shouldIgnoreAllNewRestoreRequestIfOldHasNotFinishedYet() throws InterruptedException, TimeoutException, IOException {
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(1)).execute(anyObject(), anyInt());
    }

    @Test
    public void shouldIgnoreRestoreIfBackupHasNotFinishedYet() throws InterruptedException, IOException, TimeoutException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 2);
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(1)).execute(cmdCaptor.capture(), anyInt());
        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldIgnoreOthersBackupsAndRestoresWhileBackupInProgress() throws InterruptedException, TimeoutException, IOException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(1)).execute(cmdCaptor.capture(), anyInt());
        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldIgnoreOthersBackupsAndRestoresWhileRestoreInProgress() throws InterruptedException, TimeoutException, IOException {
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(1)).execute(cmdCaptor.capture(), anyInt());
        assertEquals(cmdCaptor.getValue()[0], RESTORE_SCRIPT);
    }

    @Test
    public void shouldIgnoreBackupIfRestoreHasNotFinishedYet() throws InterruptedException, IOException, TimeoutException {
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 2);
        executeNewBackup();

        awaitFinalization();

        verify(backupManager, times(1)).execute(cmdCaptor.capture(), anyInt());
        assertEquals(cmdCaptor.getValue()[0], RESTORE_SCRIPT);
    }

    @Test
    public void shouldProcessOnlyOneBackupAtTheSameTime() throws InterruptedException, IOException, TimeoutException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        executeNewRestore();
        executeNewBackup();
        executeNewRestore();
        executeNewBackup();

        awaitFinalization();

        verify(backupManager, times(1)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldProcessOnlyOneRestoreAtTheSameTime() throws InterruptedException, IOException, TimeoutException {
        executeNewRestore();
        executeNewBackup();
        executeNewRestore();
        executeNewBackup();
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(1)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], RESTORE_SCRIPT);
    }

    @Test
    public void shouldBackupWithCleanupAfterFinishOfCurrentBackup() throws InterruptedException, TimeoutException, IOException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 2);
        executeNewBackupWithCleanup();

        awaitFinalization();

        verify(backupManager, times(2)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldBackupWithCleanupAfterFinishOfCurrentRestore() throws InterruptedException, TimeoutException, IOException {
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 2);
        executeNewBackupWithCleanup();

        awaitFinalization();

        verify(backupManager, times(2)).execute(anyObject(), anyInt());
    }

    @Test
    public void shouldLogErrorIfTwoBackupsWithCleanupRunningSimultaneously() throws Exception {
        Logger logger = mock(Logger.class);

        Field loggerField = backupManager.getClass()
                                         .getSuperclass()
                                         .getDeclaredField("LOG");

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(loggerField, loggerField.getModifiers() & ~Modifier.FINAL);

        loggerField.setAccessible(true);
        loggerField.set(null, logger);

        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackupWithCleanup();
        sleep(FAKE_BACKUP_TIME_MS / 2);
        executeNewBackupWithCleanup();

        awaitFinalization();

        verify(backupManager, times(1)).execute(anyObject(), anyInt());
        verify(logger).error(anyString(), Matchers.<String>anyVararg());
    }

    @Test
    public void shouldBackupWithCleanupAfterFinishOfCurrentBackupButNotStartAnotherBackup() throws InterruptedException,
                                                                                                   TimeoutException,
                                                                                                   IOException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackupWithCleanup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();

        awaitFinalization();

        verify(backupManager, times(2)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldBackupWithCleanupAfterFinishOfCurrentBackupButNotStartRestore() throws InterruptedException,
                                                                                             TimeoutException,
                                                                                             IOException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackupWithCleanup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(2)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldBackupWithCleanupAfterFinishOfCurrentRestoreButNotStartBackup() throws InterruptedException,
                                                                                             TimeoutException,
                                                                                             IOException {
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackupWithCleanup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();

        awaitFinalization();

        verify(backupManager, times(2)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldBackupWithCleanupAfterFinishOfCurrentRestoreButNotStartAnotherRestore() throws InterruptedException,
                                                                                                     TimeoutException,
                                                                                                     IOException {
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackupWithCleanup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(2)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldIgnoreAllBackupsAndRestoresWhileBackupWithCleanupInQueueAfterBackup() throws InterruptedException,
                                                                                                   TimeoutException,
                                                                                                   IOException {
        injectWorkspaceLock(WORKSPACE_ID);

        executeNewBackup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackupWithCleanup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(2)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    @Test
    public void shouldIgnoreAllBackupsAndRestoresWhileBackupWithCleanupInQueueAfterRestore() throws InterruptedException,
                                                                                                    TimeoutException,
                                                                                                    IOException {
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackupWithCleanup();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        executeNewRestore();
        sleep(FAKE_BACKUP_TIME_MS / 4);
        executeNewBackup();
        executeNewRestore();

        awaitFinalization();

        verify(backupManager, times(2)).execute(cmdCaptor.capture(), anyInt());

        assertEquals(cmdCaptor.getValue()[0], BACKUP_SCRIPT);
    }

    private void executeNewBackup() {
        executor.execute(() -> {
            try {
                backupManager.backupWorkspace(WORKSPACE_ID, SRC_PATH, SRC_ADDRESS);
            } catch (ServerException e) {
                e.printStackTrace();
            }
        });
    }

    private void executeNewBackupWithCleanup() {
        executor.execute(() -> {
            try {
                backupManager.backupWorkspaceAndCleanup(WORKSPACE_ID, SRC_PATH, SRC_ADDRESS);
            } catch (ServerException e) {
                e.printStackTrace();
            }
        });
    }

    private void executeNewRestore() {
        executor.execute(() -> {
            try {
                backupManager.restoreWorkspaceBackup(WORKSPACE_ID, DEST_PATH, USER_ID, USER_GID, DEST_ADDRESS);
            } catch (ServerException e) {
                e.printStackTrace();
            }
        });
    }

    private void awaitFinalization() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(2 * FAKE_BACKUP_TIME_MS + 1000, TimeUnit.MILLISECONDS)) {
            fail("Operation is hanged up. Terminated.");
        }
    }

    /**
     * Adds lock for specified workspace into concurrent hash map of {@link MachineBackupManager} class
     * It allows emulate restore of workspace without invoking it explicit
     *
     * @param workspaceId
     *         id of workspace for which lock will be injected
     */
    private void injectWorkspaceLock(String workspaceId) {
        try {
            Field locks = MachineBackupManager.class.getDeclaredField("workspacesBackupLocks");
            locks.setAccessible(true);
            ((ConcurrentHashMap<String, ReentrantLock>)locks.get(backupManager)).put(workspaceId, new ReentrantLock());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
