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
package com.codenvy.api.workspace.server;

import com.codenvy.api.permission.server.dao.PermissionsStorage;
import com.codenvy.api.workspace.server.filters.MachinePermissionsFilter;
import com.codenvy.api.workspace.server.filters.RecipeScriptDownloadPermissionFilter;
import com.codenvy.api.workspace.server.filters.WorkspacePermissionsFilter;
import com.codenvy.api.workspace.server.filters.RecipePermissionsFilter;
import com.codenvy.api.workspace.server.filters.AclSetPermissionsFilter;
import com.codenvy.api.workspace.server.filters.StackPermissionsFilter;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * @author Sergii Leschenko
 */
public class WorkspaceApiModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(WorkspacePermissionsFilter.class);
//        bind(RecipePermissionsFilter.class);
        bind(MachinePermissionsFilter.class);
//        bind(StackPermissionsFilter.class);
//        bind(AclSetPermissionsFilter.class);
        bind(RecipeScriptDownloadPermissionFilter.class);

        bind(WorkspaceCreatorPermissionsProvider.class).asEagerSingleton();
        bind(WorkspacePermissionsRemover.class).asEagerSingleton();

        Multibinder<PermissionsStorage> storages = Multibinder.newSetBinder(binder(),
                                                                            PermissionsStorage.class);
        storages.addBinding().to(WorkspacePermissionStorage.class);
    }
}
