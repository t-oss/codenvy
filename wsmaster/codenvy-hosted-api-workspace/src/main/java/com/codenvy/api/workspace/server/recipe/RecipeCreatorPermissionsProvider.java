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
package com.codenvy.api.workspace.server.recipe;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.api.machine.server.model.impl.AclEntryImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds acl entry for creator before recipe creation
 *
 * @author Sergii Leschenko
 */
@Singleton
public class RecipeCreatorPermissionsProvider implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        final RecipeImpl recipe = (RecipeImpl)methodInvocation.getArguments()[0];
        final String creator = recipe.getCreator();
        List<AclEntryImpl> acl = recipe.getAcl();
        if (acl == null) {
            acl = new ArrayList<>();
        } else {
            acl.removeIf(aclEntry -> aclEntry.getUser().equals(creator));
        }
        acl.add(new AclEntryImpl(creator, new ArrayList<>(new RecipeDomain().getAllowedActions())));
        recipe.setAcl(acl);
        return methodInvocation.proceed();
    }
}
