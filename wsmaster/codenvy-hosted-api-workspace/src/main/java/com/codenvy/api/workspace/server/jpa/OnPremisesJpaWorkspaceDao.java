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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.jpa.JpaWorkspaceDao;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * @author Max Shaposhnik
 *
 */
@Singleton
public class OnPremisesJpaWorkspaceDao extends JpaWorkspaceDao {

    @Inject
    private Provider<EntityManager> manager;

    @Override
    @Transactional
    public List<WorkspaceImpl> getWorkspaces(String userId) throws ServerException {
        final String query = "SELECT ws FROM Worker worker, Workspace ws " +
                             "          WHERE worker.workspaceId = ws.id " +
                             "          AND worker.userId = :userId ";
        try {
            return manager.get()
                          .createQuery(query, WorkspaceImpl.class)
                          .setParameter("userId", userId)
                          .getResultList();
        } catch (RuntimeException x) {
            throw new ServerException(x.getLocalizedMessage(), x);
        }
    }
}
