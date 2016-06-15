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
package com.codenvy.auth.sso.server;


import com.codenvy.auth.sso.server.organization.UserCreator;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.Constants;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.commons.lang.NameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Sergii Kabashniuk
 */
public class OrgServiceUserCreator implements UserCreator {
    private static final Logger LOG = LoggerFactory.getLogger(OrgServiceUserCreator.class);

    private final UserManager userManager;
    private final UserProfileDao profileDao;
    private final PreferenceDao preferenceDao;
    private final boolean userSelfCreationAllowed;

    @Inject
    public OrgServiceUserCreator(UserManager userManager,
                                 UserProfileDao profileDao,
                                 PreferenceDao preferenceDao,
                                 @Named("user.self.creation.allowed") boolean userSelfCreationAllowed) {
        this.userManager = userManager;
        this.profileDao = profileDao;
        this.preferenceDao = preferenceDao;
        this.userSelfCreationAllowed = userSelfCreationAllowed;
    }

    @Override
    public User createUser(String email, String userName, String firstName, String lastName) throws IOException {
        //TODO check this method should only call if user is not exists.
        try {
            return userManager.getByAlias(email);
        } catch (NotFoundException e) {
            if (!userSelfCreationAllowed) {
                throw new IOException("Currently only admins can create accounts. Please contact our Admin Team for further info.");
            }

            String id = NameGenerator.generate(User.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);

            final Map<String, String> attributes = new HashMap<>();
            attributes.put("firstName", firstName);
            attributes.put("lastName", lastName);
            attributes.put("email", email);

            Profile profile = new Profile()
                    .withId(id)
                    .withUserId(id)
                    .withAttributes(attributes);
            String password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            try {

                while (!createNonReservedUser(id, userName, email, password)) {
                    userName = NameGenerator.generate(userName, 4);
                }
                profileDao.create(profile);

                final Map<String, String> preferences = new HashMap<>();
                preferences.put("codenvy:created", Long.toString(System.currentTimeMillis()));
                preferences.put("resetPassword", "true");
                preferenceDao.setPreferences(id, preferences);

                return new User().withId(id).withName(userName).withEmail(email).withPassword(password);
            } catch (ConflictException | ServerException | NotFoundException e1) {
                throw new IOException(e1.getLocalizedMessage(), e1);
            }
        } catch (ServerException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public User createTemporary() throws IOException {

        String id = NameGenerator.generate(User.class.getSimpleName(), Constants.ID_LENGTH);
        try {
            String testName;
            while (true) {
                testName = NameGenerator.generate("AnonymousUser_", 6);
                try {
                    userManager.getByName(testName);
                } catch (NotFoundException e) {
                    break;
                } catch (ApiException e) {
                    throw new IOException(e.getLocalizedMessage(), e);
                }
            }


            final String anonymousUser = testName;
            // generate password and delete all "-" symbols which are generated by randomUUID()
            String password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);


            final User user = new User().withId(id).withName(anonymousUser)
                                        .withPassword(password);
            userManager.create(user, true);

            profileDao.create(new Profile()
                                      .withId(id)
                                      .withUserId(id));

            final Map<String, String> preferences = new HashMap<>();
            preferences.put("temporary", String.valueOf(true));
            preferences.put("codenvy:created", Long.toString(System.currentTimeMillis()));
            preferenceDao.setPreferences(id, preferences);

            LOG.info("Temporary user {} created", anonymousUser);
            return user;
        } catch (ApiException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    private boolean createNonReservedUser(String id, String username, String email, String password) throws ServerException{
        try {
            userManager.create(new User().withId(id)
                                         .withName(username)
                                         .withEmail(email)
                                         .withPassword(password), false);
            return true;
        } catch (ServerException e) {
            throw e;
        } catch (ConflictException e) {
            return false;
        }
    }
}
