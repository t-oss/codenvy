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

import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcernException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import org.bson.Document;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.machine.server.spi.SnapshotDao;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.model.impl.SnapshotImpl;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link SnapshotDao} based on MongoDB storage
 *
 * @author Sergii kabashniuk
 */
public class SnapshotDaoImpl implements SnapshotDao {

    private final MongoCollection<SnapshotImpl> collection;

    @Inject
    public SnapshotDaoImpl(@Named("mongo.db.organization") MongoDatabase database,
                           @Named("organization.storage.db.snapshot.collection") String collectionName) {
        collection = database.getCollection(collectionName, SnapshotImpl.class);
        collection
                .createIndex(new Document("workspaceId", 1).append("envName", 1).append("machineName", 1), new IndexOptions().unique(true));
    }

    @Override
    public SnapshotImpl getSnapshot(String workspaceId, String envName, String machineName) throws NotFoundException, SnapshotException {
        requireNonNull(workspaceId, "Workspace id must not be null");
        requireNonNull(envName, "Environment name must not be null");
        requireNonNull(machineName, "Machine name must not be null");

        final FindIterable<SnapshotImpl> findIt =
                collection.find(and(eq("workspaceId", workspaceId), eq("envName", envName), eq("machineName", machineName)));
        if (findIt.first() == null) {
            throw new NotFoundException(format("Snapshot with workspace id '%s' & environment '%s' & machine name  '%s' not found",
                                               workspaceId,
                                               envName,
                                               machineName));
        }
        return findIt.first();
    }

    @Override
    public SnapshotImpl getSnapshot(String snapshotId) throws NotFoundException, SnapshotException {
        requireNonNull(snapshotId, "Snapshot identifier must not be null");

        final FindIterable<SnapshotImpl> findIt = collection.find(eq("_id", snapshotId));
        if (findIt.first() == null) {
            throw new NotFoundException("Snapshot with id '" + snapshotId + "' not found");
        }
        return findIt.first();
    }

    @Override
    public void saveSnapshot(SnapshotImpl snapshot) throws SnapshotException {
        requireNonNull(snapshot, "Snapshot must not be null");
        try {
            collection.insertOne(snapshot);
        } catch (MongoWriteException | WriteConcernException e) {
            throw new SnapshotException(format("Snapshot with id '%s' or combination of "
                                               + "workspace id '%s' & environment '%s' & machine name  '%s'  already exists",
                                               snapshot.getId(),
                                               snapshot.getWorkspaceId(),
                                               snapshot.getEnvName(),
                                               snapshot.getMachineName()));
        } catch (MongoException mongoEx) {
            throw new SnapshotException(mongoEx.getMessage(), mongoEx);
        }
    }

    @Override
    public List<SnapshotImpl> findSnapshots(String namespace, String workspaceId) throws SnapshotException {
        requireNonNull(namespace, "Snapshot namespace must not be null");
        requireNonNull(workspaceId, "Workspace id must not be null");

        return collection.find(and(eq("workspaceId", workspaceId), eq("namespace", namespace))).into(new ArrayList<>());
    }

    @Override
    public void removeSnapshot(String snapshotId) throws NotFoundException, SnapshotException {
        requireNonNull(snapshotId, "Snapshot identifier must not be null");

        collection.findOneAndDelete(eq("_id", snapshotId));
    }
}
