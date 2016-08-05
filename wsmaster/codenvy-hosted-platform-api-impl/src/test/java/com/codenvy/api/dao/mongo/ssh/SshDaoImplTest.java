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

import com.github.fakemongo.Fongo;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.codecs.configuration.CodecRegistry;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link SshDaoImpl}.
 *
 * @author Sergii Leschenko
 */
@Listeners(value = MockitoTestNGListener.class)
public class SshDaoImplTest {
    private static final String OWNER = "user123";
    MongoCollection<SshPairImpl> collection;
    SshDaoImpl                    sshDao;

    @BeforeMethod
    public void setUpDb() {
        final Fongo fongo = new Fongo("Ssh test server");
        final CodecRegistry defaultRegistry = MongoClient.getDefaultCodecRegistry();
        final MongoDatabase database = fongo.getDatabase("ssh")
                                            .withCodecRegistry(fromRegistries(defaultRegistry,
                                                                              fromCodecs(new UsersSshPairCodec(defaultRegistry))));
        collection = database.getCollection("ssh", SshPairImpl.class);
        sshDao = new SshDaoImpl(database, "ssh");
    }

    @Test
    public void shouldCreateSshPair() throws Exception {
        SshPairImpl sshPair = createSshPair(OWNER);

        sshDao.create(sshPair);

        final SshPairImpl result = collection.find(and(eq("owner", OWNER),
                                                       eq("service", sshPair.getService()),
                                                       eq("name", sshPair.getName()))).first();
        assertEquals(result, sshPair);
    }


    @Test(expectedExceptions = NullPointerException.class,
          expectedExceptionsMessageRegExp = "Ssh pair must not be null")
    public void shouldThrowExceptionIfSshPairIsNull() throws Exception {
        sshDao.create(null);
    }

    @Test(expectedExceptions = NullPointerException.class,
          expectedExceptionsMessageRegExp = "Owner must not be null")
    public void shouldThrowExceptionIfOwnerIsNull() throws Exception {
        sshDao.create(createSshPair(null));
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "Ssh pair with service 'service' and name 'name' already exists")
    public void shouldThrowExceptionWhenSshPairWithSuchOwnerAndServiceAndNameAlreadyExists() throws Exception {
        final MongoWriteException exception = mock(MongoWriteException.class);
        final WriteError writeError = mock(WriteError.class);
        when(exception.getError()).thenReturn(writeError);
        when(writeError.getCategory()).thenReturn(ErrorCategory.DUPLICATE_KEY);
        final MongoDatabase db = mockDatabase(col -> doThrow(exception).when(col).insertOne(any()));

        new SshDaoImpl(db, "ssh").create(createSshPair(OWNER));
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowExceptionWhenMongoWriteExceptionWasThrew() throws Exception {
        final MongoWriteException exception = mock(MongoWriteException.class);
        final WriteError writeError = mock(WriteError.class);
        when(exception.getError()).thenReturn(writeError);
        final MongoDatabase db = mockDatabase(col -> doThrow(exception).when(col).insertOne(any()));

        new SshDaoImpl(db, "ssh").create(createSshPair(OWNER));
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowExceptionWhenMongoExceptionWasThrew() throws Exception {
        final MongoDatabase db = mockDatabase(col -> doThrow(mock(MongoException.class)).when(col).insertOne(any()));

        new SshDaoImpl(db, "ssh").create(createSshPair(OWNER));
    }

    @Test
    public void shouldRemoveSshPair() throws Exception {
        SshPairImpl sshPair = createSshPair(OWNER);
        sshDao.create(sshPair);

        sshDao.remove(OWNER, sshPair.getService(), sshPair.getName());

        assertEquals(collection.count(and(eq("owner", OWNER), eq("service", sshPair.getService()), eq("name", sshPair.getName()))), 0);
    }

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Ssh pair with service 'service' and name 'name' was not found.")
    public void shouldThrowNotFoundExceptionWhenSshPairDoesNotExistsOnRemove() throws Exception {
        sshDao.remove(OWNER, "service", "name");
    }

    @Test
    public void testGetSnapshotByOwnerAndServiceAndName() throws Exception {
        SshPairImpl sshPair = createSshPair(OWNER);
        sshDao.create(sshPair);

        final SshPairImpl result = sshDao.get(OWNER, sshPair.getService(), sshPair.getName());

        assertEquals(result, sshPair);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testGetSnapshotByIdWhenSnapshotDoesNotExist() throws Exception {
        sshDao.get(OWNER, "service", "name");
    }

    @Test
    public void testGetSnapshotByOwnerAndService() throws Exception {
        SshPairImpl sshPair = createSshPair(OWNER);
        sshDao.create(sshPair);

        final List<SshPairImpl> result = sshDao.get(OWNER, sshPair.getService());

        assertEquals(result.size(), 1);
        assertEquals(result.get(0), sshPair);
    }

    private SshPairImpl createSshPair(String owner) {
        return new SshPairImpl(owner,
                               "service",
                               "name",
                               "publicKey",
                               "privateKey");
    }

    private MongoDatabase mockDatabase(Consumer<MongoCollection<SshPairImpl>> consumer) {
        @SuppressWarnings("unchecked")
        final MongoCollection<SshPairImpl> collection = mock(MongoCollection.class);
        consumer.accept(collection);

        final MongoDatabase database = mock(MongoDatabase.class);
        when(database.getCollection("ssh", SshPairImpl.class)).thenReturn(collection);

        return database;
    }
}
