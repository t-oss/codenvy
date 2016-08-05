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
package com.codenvy.api.workspace.server.dao;

import com.codenvy.api.workspace.server.model.WorkerImpl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import java.util.List;

/**
 * Defines data access object contract for {@link WorkerImpl}.
 *
 * @author Sergii Leschenko
 */
public interface WorkerDao {

    /**
     * Stores (adds or updates) worker.
     *
     * @param worker
     *         worker to store
     * @throws NullPointerException
     *         when {@code worker} is null
     * @throws ServerException
     *         when any other error occurs during worker storing
     */
    void store(WorkerImpl worker) throws ServerException;

    /**
     * Gets worker by user and workspace
     *
     * @param workspace
     *         workspace identifier
     * @param user
     *         user identifier
     * @return worker instance, never null
     * @throws NullPointerException
     *         when {@code workspace} or {@code user} is null
     * @throws NotFoundException
     *         when worker with given {@code workspace} and {@code user} was not found
     * @throws ServerException
     *         when any other error occurs during worker fetching
     */
    WorkerImpl getWorker(String workspace, String user) throws NotFoundException, ServerException;

    /**
     * Removes worker
     *
     * <p>Doesn't throw an exception when worker with given {@code workspace} and {@code user} does not exist
     *
     * @param workspace
     *         workspace identifier
     * @param user
     *         user identifier
     * @throws NullPointerException
     *         when {@code workspace} or {@code user} is null
     * @throws ServerException
     *         when any other error occurs during worker removing
     */
    void removeWorker(String workspace, String user) throws ServerException, NotFoundException;

    /**
     * Gets workers by workspace
     *
     * @param workspace
     *         workspace identifier
     * @return list of workers instance
     * @throws NullPointerException
     *         when {@code workspace} is null
     * @throws ServerException
     *         when any other error occurs during worker fetching
     */
    List<WorkerImpl> getWorkers(String workspace) throws ServerException;

    /**
     * Gets workers by user
     *
     * @param user
     *         workspace identifier
     * @return list of workers instance
     * @throws NullPointerException
     *         when {@code user} is null
     * @throws ServerException
     *         when any other error occurs during worker fetching
     */
    List<WorkerImpl> getWorkersByUser(String user) throws ServerException;
}
