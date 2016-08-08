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

import com.google.inject.persist.Transactional;

import com.codenvy.api.workspace.server.model.impl.WorkerImpl;
import com.codenvy.api.workspace.server.spi.WorkerDao;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Jpa implementation of {@link WorkerDao},
 *
 * @author Max Shaposhnik
 */
@Singleton
public class JpaWorkerDao implements WorkerDao {

    @Inject
    private Provider<EntityManager> managerProvider;

    @Override
    public void store(WorkerImpl worker) throws ServerException {
        requireNonNull(worker, "Worker required");
        try {
            doCreate(worker);
        } catch (RuntimeException e) {
            throw new ServerException(e);
        }
    }

    @Override
    @Transactional
    public WorkerImpl getWorker(String workspaceId, String userId) throws NotFoundException, ServerException {
        requireNonNull(workspaceId, "Workspace identifier required");
        requireNonNull(userId, "User identifier required");
        try {
            final WorkerImpl result = managerProvider.get().find(WorkerImpl.class, new WorkerPrimaryKey(workspaceId, userId));
            if (result == null) {
                throw new NotFoundException(String.format("Worker of workspace '%s' with id '%s' was not found.", workspaceId, userId));
            }
            return result;
        } catch (RuntimeException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void removeWorker(String workspaceId, String userId) throws ServerException, NotFoundException {
        requireNonNull(workspaceId, "Workspace identifier required");
        requireNonNull(userId, "User identifier required");
        try {
            doRemove(workspaceId, userId);
        } catch (RuntimeException e) {
            throw new ServerException(e);
        }
    }

    @Override
    @Transactional
    public List<WorkerImpl> getWorkers(String workspaceId) throws ServerException {
        requireNonNull(workspaceId, "Workspace identifier required");
        try {
            return managerProvider.get()
                                  .createNamedQuery("Worker.getByWorkspaceId", WorkerImpl.class)
                                  .setParameter("workspaceId", workspaceId)
                                  .getResultList();
        } catch (RuntimeException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    @Transactional
    public List<WorkerImpl> getWorkersByUser(String userId) throws ServerException {
        requireNonNull(userId, "User identifier required");
        try {
            return managerProvider.get()
                                  .createNamedQuery("Worker.getByUserId", WorkerImpl.class)
                                  .setParameter("userId", userId)
                                  .getResultList();
        } catch (RuntimeException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    @Transactional
    protected void doCreate(WorkerImpl entity) {
        managerProvider.get().merge(entity);
    }

    @Transactional
    protected void doRemove(String workspaceId, String userId) throws NotFoundException {
        EntityManager manager = managerProvider.get();
        final WorkerImpl entity = manager.find(WorkerImpl.class, new WorkerPrimaryKey(workspaceId, userId));
        if (entity == null) {
            throw new NotFoundException(String.format("Worker of workspace '%s' with id '%s' was not found.", workspaceId, userId));
        }
        manager.remove(entity);
    }
}
