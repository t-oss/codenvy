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
package com.codenvy.api.dao.ldap;


import org.eclipse.che.api.user.server.model.impl.ProfileImpl;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.eclipse.che.commons.test.tck.repository.TckRepositoryException;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import java.util.Collection;

import static com.codenvy.api.dao.ldap.LdapCloser.wrapCloseable;

/**
 * LDAP specific implementation of {@link TckRepository}.
 * Since that user and profile is one entity in the context of LDAP
 * create all operation wil proceed upda of profiles and remove all
 *
 * @author Anton Korneta.
 */
public class ProfileTckRepository implements TckRepository<ProfileImpl> {

    @Inject
    private ProfileAttributesMapper mapper;

    @Inject
    private InitialLdapContextFactory contextFactory;

    @Override
    public void createAll(Collection<? extends ProfileImpl> entities) throws TckRepositoryException {
        try {
            for (ProfileImpl entity : entities) {
                try (LdapCloser.CloseableSupplier<InitialLdapContext> ctxSup = wrapCloseable(contextFactory.createContext())) {
                    final InitialLdapContext ctx = ctxSup.get();
                    final Attributes attributes = ctx.getAttributes(mapper.getProfileDn(entity.getUserId()));
                    final ProfileImpl existingProfile = mapper.asProfile(attributes);
                    final ModificationItem[] mods = mapper.createModifications(existingProfile.getAttributes(), entity.getAttributes());
                    ctx.modifyAttributes(mapper.getProfileDn(entity.getUserId()), mods);
                }
            }
        } catch (NamingException x) {
            throw new TckRepositoryException(x.getLocalizedMessage(), x);
        }
    }

    @Override
    public void removeAll() throws TckRepositoryException {
        // References to the users LDAP entity, UserTckRepository will remove it
    }
}
