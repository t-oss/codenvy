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
package com.codenvy.api.workspace.server.jpa;


import com.codenvy.api.workspace.server.model.impl.WorkerImpl;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.eclipse.che.api.user.server.event.BeforeUserRemovedEvent;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.event.BeforeWorkspaceRemovedEvent;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

import static org.testng.Assert.assertTrue;

/**
 * JPA-specific (non-TCK compliant) tests of {@link JpaWorkerDao}
 * @author Max Shaposhnik
 */
public class JpaWorkerDaoTest {

    private EntityManager manager;

    private JpaWorkerDao workerDao;

    private JpaWorkerDao.RemoveWorkersBeforeWorkspaceRemovedEventSubscriber removeWorkersBeforeWorkspaceRemovedEventSubscriber;

    private JpaWorkerDao.RemoveWorkersBeforeUserRemovedEventSubscriber removeWorkersBeforeUserRemovedEventSubscriber;

    WorkerImpl[] workers;

    @BeforeClass
    public void setupEntities() throws Exception {
        workers = new WorkerImpl[]{new WorkerImpl("ws1", "user1", Arrays.asList("read", "use", "run")),
                                   new WorkerImpl("ws1", "user2", Arrays.asList("read", "use")),
                                   new WorkerImpl("ws2", "user1", Arrays.asList("read", "run")),
                                   new WorkerImpl("ws2", "user2", Arrays.asList("read", "use", "run", "configure"))};

        Injector injector = Guice.createInjector(new WorkerTckModule(), new WorkerJpaModule());
        manager = injector.getInstance(EntityManager.class);
        workerDao = injector.getInstance(JpaWorkerDao.class);
        removeWorkersBeforeWorkspaceRemovedEventSubscriber = injector.getInstance(
                JpaWorkerDao.RemoveWorkersBeforeWorkspaceRemovedEventSubscriber.class);

        removeWorkersBeforeUserRemovedEventSubscriber = injector.getInstance(
                JpaWorkerDao.RemoveWorkersBeforeUserRemovedEventSubscriber.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        manager.getTransaction().begin();
        for (WorkerImpl worker : workers) {
            manager.persist(worker);
        }
        manager.getTransaction().commit();
        manager.clear();
    }

    @AfterMethod
    private void cleanup() {
        manager.getTransaction().begin();
        manager.createQuery("SELECT e FROM Worker e", WorkerImpl.class)
               .getResultList()
               .forEach(manager::remove);
        manager.getTransaction().commit();
        manager.getEntityManagerFactory().close();
    }

    @Test
    public void shouldRemoveWorkersWhenWorkspaceIsRemoved() throws Exception {
        BeforeWorkspaceRemovedEvent event =  new BeforeWorkspaceRemovedEvent(new WorkspaceImpl("ws1", "ns", new WorkspaceConfigImpl()));
        removeWorkersBeforeWorkspaceRemovedEventSubscriber.onEvent(event);
        assertTrue(workerDao.getWorkers("ws1").isEmpty());
    }

    @Test
    public void shouldRemoveWorkersWhenUserIsRemoved() throws Exception {
        BeforeUserRemovedEvent event =  new BeforeUserRemovedEvent(new UserImpl("user1", "email@co.com", "user"));
        removeWorkersBeforeUserRemovedEventSubscriber.onEvent(event);
        assertTrue(workerDao.getWorkersByUser("user1").isEmpty());
    }

}
