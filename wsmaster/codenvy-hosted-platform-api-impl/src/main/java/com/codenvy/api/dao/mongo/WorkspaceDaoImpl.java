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

import com.codenvy.api.workspace.server.spi.WorkerDao;
import com.codenvy.api.workspace.server.model.Worker;
import com.codenvy.api.workspace.server.model.impl.WorkerImpl;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import org.bson.Document;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.api.dao.mongo.MongoUtil.handleWriteConflict;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link WorkspaceDao} based on MongoDB storage.
 *
 * <p>Workspace collection document scheme:
 * <pre>
 *
 * {
 *      "id" : "workspace123",
 *      "namespace" : "user123",
 *      "config" : {
 *          "name" : "my-workspace",
 *          "description" : "This is workspace description",
 *          "defaultEnv" : "dev-env",
 *          "commands" : [
 *              {
 *                  "name" : "mci",
 *                  "commandLine" : "maven clean install",
 *                  "type" : "maven",
 *                  "attributes" : [
 *                      {
 *                          "name" : "attribute1",
 *                          "value" : "value1"
 *                      }
 *                   ]
 *              }
 *          ],
 *          "projects" : [
 *              {
 *                  "name" : "my-project",
 *                  "path" : "/path/to/project",
 *                  "description" : "This is project description",
 *                  "type" : "project-type",
 *                  "source" : {
 *                      "type" : "storage-type",
 *                      "location" : "storage-location",
 *                      "parameters" : [
 *                          {
 *                              "name" : "parameter1",
 *                              "value" : "parameter-value"
 *                          }
 *                      ]
 *                  },
 *                  "mixins" : [ "mixinType1", "mixinType2" ],
 *                  "attributes" : [
 *                      {
 *                          "name" : "project-attribute-1",
 *                          "value" : [ "value1", "value2" ]
 *                      }
 *                  ]
 *              }
 *          ],
 *          "environments" : [
 *              {
 *                  "name" : "dev-env",
 *                  "recipe" : {
 *                      "type" : "dockerfile",
 *                      "script" : "FROM codenvy/jdk7\nCMD tail -f /dev/null"
 *                  },
 *                  "machineConfigs" : [
 *                      {
 *                          "dev" : true,
 *                          "name" : "dev",
 *                          "type" : "machine-type",
 *                          "limits" : {
 *                              "ram" : 512
 *                          },
 *                          "source" : {
 *                              "type" : "recipe",
 *                              "location" : "recipe-url"
 *                          },
 *                          "servers" : [
 *                              {
 *                                  "ref" : "some_reference",
 *                                  "port" : "9090/udp",
 *                                  "protocol" : "some_protocol",
 *                                  "path" : "/some/path"
 *                              }
 *                          ],
 *                          envVariables : [
 *                              {
 *                                  "name" : "var_name1",
 *                                  "value" : "var_value1"
 *                              }
 *                          ]
 *                      }
 *                  ]
 *              }
 *          ],
 *      },
 *      "attributes" : [
 *              {
 *                  "name" : "attribute1",
 *                  "value" : "value1"
 *              }
 *      ]
 * }
 * </pre>
 *
 * <p>Note that for string maps - list of objects used instead of single object,
 * actually the reason is that MongoDB does not support keys which contain DOT<i>.</i>
 * <pre>
 *     //map as single object
 *     {
 *         "attribute1" : "value1",
 *         "attribute2" : "value2"
 *     }
 *
 *     //map as list of objects
 *     [
 *          {
 *              "name" : "attribute1",
 *              "value" : "value1"
 *          },
 *          {
 *              "name" : "attribute2",
 *              "value" : "value2"
 *          }
 *     ]
 * </pre>
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class WorkspaceDaoImpl implements WorkspaceDao {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceDaoImpl.class);

    private final MongoCollection<WorkspaceImpl> collection;
    private final WorkerDao                      workerDao;

    @Inject
    public WorkspaceDaoImpl(@Named("mongo.db.organization") MongoDatabase database,
                            @Named("organization.storage.db.workspace2.collection") String collectionName,
                            WorkerDao workerDao) {
        this.workerDao = workerDao;
        collection = database.getCollection(collectionName, WorkspaceImpl.class);
        collection.createIndex(new Document("config.name", 1).append("namespace", 1), new IndexOptions().unique(true));
    }

    @Override
    public WorkspaceImpl create(WorkspaceImpl workspace) throws ConflictException, ServerException {
        requireNonNull(workspace, "Workspace must not be null");
        requireNonNull(workspace.getConfig(), "Workspace config must not be null");
        try {
            collection.insertOne(workspace);
        } catch (MongoWriteException writeEx) {
            handleWriteConflict(writeEx, format("Workspace with id '%s' or name '%s' in namespace '%s' already exists",
                                                workspace.getId(),
                                                workspace.getConfig().getName(),
                                                workspace.getNamespace()));
        } catch (MongoException mongoEx) {
            throw new ServerException(mongoEx.getMessage(), mongoEx);
        }
        return workspace;
    }

    @Override
    public WorkspaceImpl update(WorkspaceImpl update) throws NotFoundException, ConflictException, ServerException {
        requireNonNull(update, "Workspace update must not be null");
        requireNonNull(update.getConfig(), "Workspace update config must not be null");
        try {
            if (collection.findOneAndReplace(eq("_id", update.getId()), update) == null) {
                throw new NotFoundException("Workspace with id '" + update.getId() + "' was not found");
            }
        } catch (MongoWriteException writeEx) {
            handleWriteConflict(writeEx, format("Workspace with id '%s' or name '%s' in namespace '%s' already exists",
                                                update.getId(),
                                                update.getConfig().getName(),
                                                update.getNamespace()));
        } catch (MongoException mongoEx) {
            throw new ServerException(mongoEx.getMessage(), mongoEx);
        }
        return update;
    }

    @Override
    public void remove(String id) throws ConflictException, ServerException {
        requireNonNull(id, "Workspace identifier must not be null");

        collection.findOneAndDelete(eq("_id", id));
    }

    @Override
    public WorkspaceImpl get(String id) throws NotFoundException, ServerException {
        requireNonNull(id, "Workspace identifier must not be null");

        final FindIterable<WorkspaceImpl> findIt = collection.find(eq("_id", id));
        if (findIt.first() == null) {
            throw new NotFoundException("Workspace with id '" + id + "' was not found");
        }
        return findIt.first();
    }

    @Override
    public WorkspaceImpl get(String name, String namespace) throws NotFoundException, ServerException {
        requireNonNull(name, "Workspace name must not be null");
        requireNonNull(namespace, "Workspace namespace must not be null");

        final FindIterable<WorkspaceImpl> findIt = collection.find(and(eq("config.name", name), eq("namespace", namespace)));
        if (findIt.first() == null) {
            throw new NotFoundException(format("Workspace with name '%s' in namespace '%s' was not found", name, namespace));
        }
        return findIt.first();
    }

    @Override
    public List<WorkspaceImpl> getByNamespace(String namespace) throws ServerException {
        requireNonNull(namespace, "Workspace namespace must not be null");

        return collection.find(eq("namespace", namespace)).into(new ArrayList<>());
    }

    @Override
    public List<WorkspaceImpl> getWorkspaces(String userId) throws ServerException {
        List<WorkspaceImpl> workspaces = new ArrayList<>();
        final List<WorkerImpl> workers = workerDao.getWorkersByUser(userId);
        for (Worker worker : workers) {
            if (worker.getActions().contains("read")) {
                try {
                    workspaces.add(get(worker.getWorkspaceId()));
                } catch (NotFoundException e) {
                    LOG.warn(String.format("There is worker with workspace '%s' but this workspace doesn't exist",
                                           worker.getWorkspaceId()));
                }
            }
        }
        return workspaces;
    }

}
