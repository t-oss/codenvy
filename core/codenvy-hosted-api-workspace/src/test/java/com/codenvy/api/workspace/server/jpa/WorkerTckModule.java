package com.codenvy.api.workspace.server.jpa;

import com.codenvy.api.workspace.server.model.impl.WorkerImpl;
import com.codenvy.api.workspace.server.spi.WorkerDao;
import com.google.inject.TypeLiteral;
import com.google.inject.persist.jpa.JpaPersistModule;

import org.eclipse.che.api.core.jdbc.jpa.guice.JpaInitializer;
import org.eclipse.che.commons.test.tck.TckModule;
import org.eclipse.che.commons.test.tck.repository.JpaTckRepository;
import org.eclipse.che.commons.test.tck.repository.TckRepository;

/**
 * @author Max Shaposhnik
 */
public class WorkerTckModule extends TckModule {

    @Override
    protected void configure() {
        bind(WorkerDao.class).to(JpaWorkerDao.class);
        bind(new TypeLiteral<TckRepository<WorkerImpl>>() {
        }).toInstance(new JpaTckRepository<>(WorkerImpl.class));

        install(new JpaPersistModule("main"));
        bind(JpaInitializer.class).asEagerSingleton();
        bind(org.eclipse.che.api.core.h2.jdbc.jpa.eclipselink.H2ExceptionHandler.class);
    }
}
