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
package com.codenvy.api.dao.jpa;

import com.codenvy.api.user.server.dao.AdminUserDao;
import com.google.inject.persist.Transactional;

import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.jpa.JpaUserDao;
import org.eclipse.che.api.user.server.model.impl.UserImpl;

import java.util.List;

import static com.google.api.client.repackaged.com.google.common.base.Preconditions.checkArgument;

/**
 * @author Anton Korneta.
 */
public class JpaAdminUserDao extends JpaUserDao implements AdminUserDao {

    @Override
    @Transactional
    public Page<UserImpl> getAll(int maxItems, int skipCount) throws ServerException {
        checkArgument(maxItems >= 0, "The number of items to return can't be negative.");
        checkArgument(skipCount >= 0, "The number of items to skip can't be negative.");
        try {
            final List<UserImpl> users = managerProvider.get()
                                                        .createQuery("SELECT u FROM Usr u", UserImpl.class)
                                                        .setMaxResults(maxItems)
                                                        .setFirstResult(skipCount)
                                                        .getResultList();
            return new Page<>(users, maxItems, skipCount, users.size());
        } catch (RuntimeException x) {
            throw new ServerException(x.getLocalizedMessage(), x);
        }
    }
}
