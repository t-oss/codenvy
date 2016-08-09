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

import com.google.inject.AbstractModule;

import org.eclipse.che.api.workspace.server.jpa.JpaStackDao;
import org.eclipse.che.api.workspace.server.jpa.JpaWorkspaceDao;
import org.eclipse.che.api.workspace.server.spi.StackDao;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;

/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 *
 */
public class OnPremisesWorkspaceJpaModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StackDao.class).to(JpaStackDao.class);
        bind(WorkspaceDao.class).to(OnPremisesJpaWorkspaceDao.class);
        bind(JpaWorkspaceDao.RemoveWorkspaceBeforeUserRemovedEventSubscriber.class).asEagerSingleton();
        bind(JpaWorkspaceDao.RemoveSnapshotsBeforeWorkspaceRemovedEventSubscriber.class).asEagerSingleton();

    }
}
