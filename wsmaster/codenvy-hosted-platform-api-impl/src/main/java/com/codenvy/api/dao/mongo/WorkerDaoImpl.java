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
package com.codenvy.api.dao.mongo;

import com.codenvy.api.workspace.server.dao.WorkerDao;
import com.codenvy.api.workspace.server.model.WorkerImpl;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;

import org.bson.Document;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;

/**
 * Workers collection document scheme:
 * <pre>
 *
 * {
 *     "user" : "user123",
 *     "workspace" : "workspace123",
 *     "actions" : [
 *         "read",
 *         "write",
 *         ...
 *     ]
 * }
 *
 * @author Sergii Leschenko
 */
@Singleton
public class WorkerDaoImpl implements WorkerDao {
    private final MongoCollection<WorkerImpl> collection;

    @Inject
    public WorkerDaoImpl(@Named("mongo.db.organization") MongoDatabase database,
                         @Named("organization.storage.db.worker.collection") String collectionName) {
        collection = database.getCollection(collectionName, WorkerImpl.class);
        collection.createIndex(new Document("user", 1).append("workspace", 1), new IndexOptions().unique(true));
    }

    @Override
    public void store(WorkerImpl worker) throws ServerException {
        try {
            collection.replaceOne(and(eq("user", worker.getUser()),
                                      eq("workspace", worker.getWorkspace())),
                                  worker,
                                  new UpdateOptions().upsert(true));
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public WorkerImpl getWorker(String workspace, String user) throws NotFoundException, ServerException {
        WorkerImpl found;
        try {
            found = collection.find(and(eq("user", user),
                                        eq("workspace", workspace)))
                              .first();
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }

        if (found == null) {
            throw new NotFoundException(format("Worker with user '%s' and workspace '%s' was not found", user, workspace));
        }

        return found;
    }

    @Override
    public void removeWorker(String workspace, String user) throws ServerException, NotFoundException {
        try {
            final DeleteResult deleteResult = collection.deleteOne(and(eq("user", user),
                                                                       eq("workspace", workspace)));

            if (deleteResult.getDeletedCount() == 0) {
                throw new NotFoundException(format("Worker with user '%s' and workspace '%s' was not found", user, workspace));
            }
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<WorkerImpl> getWorkers(String workspace) throws ServerException {
        try {
            return collection.find(eq("workspace", workspace))
                             .into(new ArrayList<>());
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<WorkerImpl> getWorkersByUser(String user) throws ServerException {
        try {
            return collection.find(eq("user", user))
                             .into(new ArrayList<>());
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }
}
