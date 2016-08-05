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
package com.codenvy.api.dao.mongo.ssh;

import com.mongodb.ErrorCategory;
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
import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;
import org.eclipse.che.api.ssh.server.spi.SshDao;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link SshDao} based on MongoDB storage.
 * <pre>
 *  Ssh pairs collection document schema:
 *
 * {
 *     "owner" : "userId...",
 *     "service" : "service...",
 *     "name" : "name...",
 *     "publicKey" : "publicKey...",
 *     "privateKey" : "privateKey..."
 * }
 * </pre>
 *
 * @author Sergii Leschenko
 */
@Singleton
public class SshDaoImpl implements SshDao {

    private final MongoCollection<UsersSshPair> collection;

    @Inject
    public SshDaoImpl(@Named("mongo.db.organization") MongoDatabase database,
                      @Named("organization.storage.db.ssh.collection") String collectionName) {
        collection = database.getCollection(collectionName, UsersSshPair.class);
        collection.createIndex(new Document("owner", 1).append("service", 1).append("name", 1), new IndexOptions().unique(true));
    }

    @Override
    public void create(String owner, SshPairImpl sshPair) throws ServerException, ConflictException {
        requireNonNull(owner, "Owner must not be null");
        requireNonNull(sshPair, "Ssh pair must not be null");

        try {
            collection.insertOne(new UsersSshPair(owner, sshPair));
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new ConflictException(format("Ssh pair with service '%s' and name '%s' already exists",
                                                   sshPair.getService(),
                                                   sshPair.getName()));
            }
            throw new ServerException(e.getMessage(), e);
        } catch (MongoException mongoEx) {
            throw new ServerException(mongoEx.getMessage(), mongoEx);
        }
    }

    @Override
    public List<SshPairImpl> get(String owner, String service) throws ServerException {
        requireNonNull(owner, "Owner must not be null");
        requireNonNull(service, "Service must not be null");

        return collection.find(and(eq("owner", owner), eq("service", service)))
                         .into(new ArrayList<>());
    }

    @Override
    public SshPairImpl get(String owner, String service, String name) throws ServerException, NotFoundException {
        requireNonNull(owner, "Owner must not be null");
        requireNonNull(service, "Service must not be null");
        requireNonNull(name, "Name must not be null");

        final FindIterable<UsersSshPair> findIt = collection.find(and(eq("owner", owner), eq("service", service), eq("name", name)));
        if (findIt.first() == null) {
            throw new NotFoundException(format("Ssh pair with service '%s' and name '%s' was not found.", service, name));
        }
        return findIt.first();
    }

    @Override
    public void remove(String owner, String service, String name) throws ServerException, NotFoundException {
        requireNonNull(owner, "Owner must not be null");
        requireNonNull(service, "Service must not be null");
        requireNonNull(name, "Name must not be null");

        if (collection.findOneAndDelete(and(eq("owner", owner), eq("service", service), eq("name", name))) == null) {
            throw new NotFoundException(format("Ssh pair with service '%s' and name '%s' was not found.", service, name));
        }
    }
}
