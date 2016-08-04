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

import com.codenvy.api.workspace.server.spi.WorkerDao;
import com.codenvy.api.workspace.server.model.impl.WorkerImpl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.server.event.WorkspaceRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Removes permissions related to workspace when it was removed
 *
 * @author Sergii Leschenko
 */
@Singleton
public class WorkspacePermissionsRemover implements EventSubscriber<WorkspaceRemovedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspacePermissionsRemover.class);

    private final EventService eventService;
    private final WorkerDao    workerDao;

    @Inject
    public WorkspacePermissionsRemover(EventService eventService,
                                       WorkerDao workerDao) {
        this.eventService = eventService;
        this.workerDao = workerDao;
    }

    @Override
    public void onEvent(WorkspaceRemovedEvent event) {
        final List<WorkerImpl> workers;
        try {
            workers = workerDao.getWorkers(event.getWorkspaceId());
        } catch (ServerException e) {
            LOG.error("Can't workers of workspace '" + event.getWorkspaceId() + "'", e);
            return;
        }

        for (WorkerImpl worker : workers) {
            try {
                workerDao.removeWorker(worker.getWorkspaceId(), worker.getUserId());
            } catch (ServerException e) {
                LOG.error(String.format("Can't remove worker with user '%s' and workspace '%s'", worker.getUserId(), worker.getWorkspaceId()), e);
            } catch (NotFoundException ignored) {
                // do nothing. Worker is already removed
            }
        }
    }

    @PostConstruct
    void subscribe() {
        eventService.subscribe(this);
    }

    @PreDestroy
    void unsubscribe() {
        eventService.unsubscribe(this);
    }
}
