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
package com.codenvy.api.workspace.server.spi.tck;

import com.codenvy.api.workspace.server.model.impl.WorkerImpl;
import com.codenvy.api.workspace.server.spi.WorkerDao;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.commons.test.tck.TckModuleFactory;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.eclipse.che.commons.test.tck.repository.TckRepositoryException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Compatibility test for {@link WorkerDao}
 *
 * @author Max Shaposhnik
 */

@Guice(moduleFactory = TckModuleFactory.class)
@Test(suiteName = "WorkerDaoTck")
public class WorkerDaoTest {

    @Inject
    private WorkerDao workerDao;

    @Inject
    private TckRepository<WorkerImpl> workerRepository;

    WorkerImpl[] workers;

    @BeforeMethod
    public void setUp() throws TckRepositoryException {
        workers = new WorkerImpl[]{new WorkerImpl("ws1", "user1", Arrays.asList("read", "use", "run")),
                                   new WorkerImpl("ws1", "user2", Arrays.asList("read", "use")),
                                   new WorkerImpl("ws2", "user1", Arrays.asList("read", "run")),
                                   new WorkerImpl("ws2", "user2", Arrays.asList("read", "use", "run", "configure"))};
        workerRepository.createAll(Arrays.asList(workers));

    }

    @AfterMethod
    public void cleanUp() throws TckRepositoryException {
        workerRepository.removeAll();
    }

    /* WorkerDao.store() tests */
    @Test
    public void shouldStoreWorker() throws Exception {
        WorkerImpl worker = new WorkerImpl("ws", "user", Arrays.asList("read", "write", "start"));
        workerDao.store(worker);
        assertEquals(workerDao.getWorker("ws", "user"), worker);
    }

    @Test
    public void shouldReplaceExistingWorkerOnStoring() throws Exception {
        WorkerImpl replace = new WorkerImpl("ws1", "user1", Collections.singletonList("read"));
        workerDao.store(replace);
        assertEquals(workerDao.getWorker("ws1", "user1"), replace);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowExceptionWhenStoringArgumentIsNull() throws Exception {
        workerDao.store(null);
    }

    /* WorkerDao.getWorker() tests */
    @Test
    public void shouldGetWorkerByWorkspaceIdAndUserId() throws Exception {
        assertEquals(workerDao.getWorker("ws1", "user1"), workers[0]);
        assertEquals(workerDao.getWorker("ws2", "user2"), workers[3]);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowExceptionWhenGetWorkerWorkspaceIdArgumentIsNull() throws Exception {
        workerDao.getWorker(null, "user1");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowExceptionWhenGetWorkerUserIdArgumentIsNull() throws Exception {
        workerDao.getWorker("ws1", null);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldThrowNotFoundExceptionOnGetIfWorkerWithSuchWorkspaceIdOrUserIdDoesNotExist() throws Exception {
        workerDao.getWorker("ws9", "user1");
    }

    /* WorkerDao.getWorkers() tests */
    @Test
    public void shouldGetWorkersByWorkspaceId() throws Exception {
        List<WorkerImpl> actual = workerDao.getWorkers("ws1");
        List<WorkerImpl> expected = Arrays.asList(workers).subList(0, 2);
        assertEquals(actual.size(), expected.size());
        assertTrue(new HashSet<>(actual).equals(new HashSet<>(expected)));
    }

    public void shouldGetWorkersByUserId() throws Exception {
        List<WorkerImpl> actual = workerDao.getWorkersByUser("user1");
        List<WorkerImpl> expected = Arrays.asList(workers[0], workers[2]);
        assertEquals(actual.size(), expected.size());
        assertTrue(new HashSet<>(actual).equals(new HashSet<>(expected)));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowExceptionWhenGetWorkersByWorkspaceArgumentIsNull() throws Exception {
        workerDao.getWorkers(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowExceptionWhenGetWorkersByUserArgumentIsNull() throws Exception {
        workerDao.getWorkersByUser(null);
    }

    @Test
    public void shouldReturnEmptyListIfWorkersWithSuchWorkspaceIdDoesNotFound() throws Exception {
        assertEquals(0, workerDao.getWorkers("unexisted_ws").size());
    }

    @Test
    public void shouldReturnEmptyListIfWorkersWithSuchUserIdDoesNotFound() throws Exception {
        assertEquals(0, workerDao.getWorkersByUser("unexisted_user").size());
    }

    /* WorkerDao.removeWorker() tests */
    @Test
    public void shouldRemoveWorker() throws Exception {
        workerDao.removeWorker("ws1", "user1");
        assertEquals(1, workerDao.getWorkersByUser("user1").size());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowExceptionWhenRemoveWorkerWorkspaceIdArgumentIsNull() throws Exception {
        workerDao.removeWorker(null, "user1");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowExceptionWhenRemoveWorkerUserIdArgumentIsNull() throws Exception {
        workerDao.removeWorker("ws1", null);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldThrowNotFoundExceptionOnRemoveIfWorkerWithSuchWorkspaceIdDoesNotExist() throws Exception {
        workerDao.removeWorker("unexisted_ws", "user1");
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldThrowNotFoundExceptionOnRemoveIfWorkerWithSuchUserIdDoesNotExist() throws Exception {
        workerDao.removeWorker("ws1", "unexisted_user");
    }
}
