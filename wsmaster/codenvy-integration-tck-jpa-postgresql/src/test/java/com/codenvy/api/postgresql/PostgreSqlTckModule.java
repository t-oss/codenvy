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
package com.codenvy.api.postgresql;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.jpa.JpaPersistModule;

import org.eclipse.che.api.core.jdbc.jpa.eclipselink.EntityListenerInjectionManagerInitializer;
import org.eclipse.che.api.core.jdbc.jpa.guice.JpaInitializer;
import org.eclipse.che.api.machine.server.jpa.JpaRecipeDao;
import org.eclipse.che.api.machine.server.jpa.JpaSnapshotDao;
import org.eclipse.che.api.machine.server.model.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.RecipeDao;
import org.eclipse.che.api.machine.server.spi.SnapshotDao;
import org.eclipse.che.api.ssh.server.jpa.JpaSshDao;
import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;
import org.eclipse.che.api.ssh.server.spi.SshDao;
import org.eclipse.che.api.user.server.jpa.JpaPreferenceDao;
import org.eclipse.che.api.user.server.jpa.JpaProfileDao;
import org.eclipse.che.api.user.server.jpa.JpaUserDao;
import org.eclipse.che.api.user.server.jpa.PreferenceEntity;
import org.eclipse.che.api.user.server.model.impl.ProfileImpl;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.user.server.spi.PreferenceDao;
import org.eclipse.che.api.user.server.spi.ProfileDao;
import org.eclipse.che.api.user.server.spi.UserDao;
import org.eclipse.che.api.workspace.server.jpa.JpaStackDao;
import org.eclipse.che.api.workspace.server.jpa.JpaWorkspaceDao;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.spi.StackDao;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.test.tck.TckModule;
import org.eclipse.che.commons.test.tck.repository.JpaTckRepository;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.eclipse.che.commons.test.tck.repository.TckRepositoryException;
import org.eclipse.che.security.PasswordEncryptor;
import org.eclipse.che.security.SHA512PasswordEncryptor;
import org.postgresql.ds.PGPoolingDataSource;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Map;

/**
 * Module for testing JPA DAO on PostgreSQL Database.
 *
 * @author Mihail Kuznyetsov
 */
public class PostgreSqlTckModule extends TckModule {

    @Override
    protected void configure() {

        try {
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.naming.java.javaURLContextFactory");
            System.setProperty(Context.URL_PKG_PREFIXES,
                               "org.apache.naming");

            InitialContext ic = new InitialContext();

            try {
                ic.lookup("java:/comp/env/jdbc/codenvy");
                // Create initial context
            } catch (NameNotFoundException e) {

                try {
                    ic.createSubcontext("java:");
                    ic.createSubcontext("java:/comp");
                    ic.createSubcontext("java:/comp/env");
                    ic.createSubcontext("java:/comp/env/jdbc");

                    PGPoolingDataSource source = new PGPoolingDataSource();
                    source.setDataSourceName("A Data Source");
                    source.setServerName(System.getProperty("postgresql.host"));
                    source.setPortNumber(Integer.parseInt(System.getProperty("postgresql.port")));
                    source.setDatabaseName("codenvy");
                    source.setUser("postgres");
                    source.setPassword("postgres");
                    source.setMaxConnections(10);
                    ic.bind("java:/comp/env/jdbc/codenvy", source);

                } catch (NamingException ex) {
                    ex.printStackTrace();
                }
            } catch (NamingException e) {
                e.printStackTrace();
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }

        install(new JpaPersistModule("main"));
        bind(JpaInitializer.class).asEagerSingleton();
        bind(EntityListenerInjectionManagerInitializer.class).asEagerSingleton();

        //repositories
        //api-user
        bind(new TypeLiteral<TckRepository<UserImpl>>() {}).to(UserJpaTckRepository.class);
        bind(new TypeLiteral<TckRepository<ProfileImpl>>() {
        }).toInstance(new JpaTckRepository<>(ProfileImpl.class));
        bind(new TypeLiteral<TckRepository<Pair<String, Map<String, String>>>>() {
        }).toInstance(new PreferenceJpaTckRepository());



        //api-workspace
        bind(new TypeLiteral<TckRepository<WorkspaceImpl>>() {
        }).toInstance(new JpaTckRepository<>(WorkspaceImpl.class));
        bind(new TypeLiteral<TckRepository<StackImpl>>() {
        }).toInstance(new JpaTckRepository<>(StackImpl.class));
        //api-machine
        bind(new TypeLiteral<TckRepository<RecipeImpl>>() {
        }).toInstance(new JpaTckRepository<>(RecipeImpl.class));
        bind(new TypeLiteral<TckRepository<SnapshotImpl>>() {
        }).toInstance(new JpaTckRepository<>(SnapshotImpl.class));
        //api ssh
        bind(new TypeLiteral<TckRepository<SshPairImpl>>() {
        }).toInstance(new JpaTckRepository<>(SshPairImpl.class));

        //dao
        //api-user
        bind(UserDao.class).to(JpaUserDao.class);
        bind(ProfileDao.class).to(JpaProfileDao.class);
        bind(PreferenceDao.class).to(JpaPreferenceDao.class);
        //api-workspace
        bind(WorkspaceDao.class).to(JpaWorkspaceDao.class);
        bind(StackDao.class).to(JpaStackDao.class);
        //api-machine
        bind(RecipeDao.class).to(JpaRecipeDao.class);
        bind(SnapshotDao.class).to(JpaSnapshotDao.class);
        //api-ssh
        bind(SshDao.class).to(JpaSshDao.class);

        // SHA-512 ecnryptor is faster than PBKDF2 so it is better for testing
        bind(PasswordEncryptor.class).to(SHA512PasswordEncryptor.class).in(Singleton.class);
        bind(org.eclipse.che.api.core.postgresql.jdbc.jpa.eclipselink.PostgreSqlExceptionHandler.class);
    }


    @Transactional
    public static class PreferenceJpaTckRepository implements TckRepository<Pair<String, Map<String, String>>> {

        @Inject
        private Provider<EntityManager> managerProvider;

        @Override
        public void createAll(Collection<? extends Pair<String, Map<String, String>>> entities) throws TckRepositoryException {
            final EntityManager manager = managerProvider.get();
            for (Pair<String, Map<String, String>> pair : entities) {
                manager.persist(new UserImpl(pair.first, "email_" + pair.first, "name_" + pair.first));
            }
        }

        @Override
        public void removeAll() throws TckRepositoryException {
            final EntityManager manager = managerProvider.get();
            manager.createQuery("SELECT prefs FROM Preference prefs", PreferenceEntity.class)
                   .getResultList()
                   .forEach(manager::remove);
        }
    }


    @Transactional
    public static class UserJpaTckRepository implements TckRepository<UserImpl> {

        @Inject
        private Provider<EntityManager> managerProvider;

        @Inject
        private PasswordEncryptor encryptor;

        @Override
        public void createAll(Collection<? extends UserImpl> entities) throws TckRepositoryException {
            final EntityManager manager = managerProvider.get();
            entities.stream()
                    .map(user -> new UserImpl(user.getId(),
                                              user.getEmail(),
                                              user.getName(),
                                              encryptor.encrypt(user.getPassword()),
                                              user.getAliases()))
                    .forEach(manager::persist);
        }

        @Override
        public void removeAll() throws TckRepositoryException {
            managerProvider.get()
                           .createQuery("DELETE FROM \"User\"")
                           .executeUpdate();
        }
    }
}
