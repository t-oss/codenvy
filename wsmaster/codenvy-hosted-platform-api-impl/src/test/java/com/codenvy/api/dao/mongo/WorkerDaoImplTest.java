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

import com.codenvy.api.workspace.server.model.impl.WorkerImpl;
import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.util.Collections.singletonList;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link WorkerDaoImpl}
 *
 * @author Sergii Leschenko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkerDaoImplTest {

    private MongoCollection<WorkerImpl> collection;
    private WorkerDaoImpl               workerDao;

    @BeforeMethod
    public void setUpDb() throws Exception {
        final Fongo fongo = new Fongo("Permissions test server");
        final CodecRegistry defaultRegistry = MongoClient.getDefaultCodecRegistry();
        final MongoDatabase database = fongo.getDatabase("worker")
                                            .withCodecRegistry(fromRegistries(defaultRegistry,
                                                                              fromCodecs(new WorkerImplCodec(defaultRegistry))));
        collection = database.getCollection("worker", WorkerImpl.class);
        workerDao = new WorkerDaoImpl(database, "worker");
    }

    @Test
    public void shouldStoreWorker() throws Exception {
        final WorkerImpl worker = createWorker();

        workerDao.store(worker);

        final WorkerImpl result = collection.find(and(eq("user", worker.getUserId()),
                                                      eq("workspace", worker.getWorkspaceId())))
                                            .first();
        assertEquals(result, worker);
    }

    @Test
    public void shouldUpdateWorkerWhenItHasAlreadyExisted() throws Exception {
        WorkerImpl worker = createWorker();
        workerDao.store(worker);

        WorkerImpl newWorker = new WorkerImpl(worker.getWorkspaceId(), worker.getUserId(), singletonList("configure"));
        workerDao.store(newWorker);

        final WorkerImpl result = collection.find(and(eq("user", newWorker.getUserId()),
                                                      eq("workspace", newWorker.getWorkspaceId())))
                                            .first();
        assertEquals(result, newWorker);
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowServerExceptionWhenMongoExceptionWasThrewOnWorkerStoring() throws Exception {
        final MongoDatabase db = mockDatabase(col -> doThrow(mock(MongoException.class)).when(col).replaceOne(any(), any(), any()));

        new WorkerDaoImpl(db, "worker").store(createWorker());
    }

    @Test
    public void shouldRemoveWorker() throws Exception {
        final WorkerImpl worker = createWorker();
        collection.insertOne(worker);

        workerDao.removeWorker(worker.getWorkspaceId(), worker.getUserId());

        assertEquals(collection.count(and(eq("user", worker.getUserId()),
                                          eq("worksapce", worker.getWorkspaceId()))),
                     0);
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowServerExceptionWhenMongoExceptionWasThrewOnWorkerRemoving() throws Exception {
        final MongoDatabase db = mockDatabase(col -> doThrow(mock(MongoException.class)).when(col).deleteOne(any()));

        new WorkerDaoImpl(db, "worker").removeWorker("workspace123", "user");
    }

    @Test
    public void shouldBeAbleToGetWorkersByUser() throws Exception {
        final WorkerImpl worker = createWorker();
        collection.insertOne(worker);
        collection.insertOne(new WorkerImpl("workspace123", "anotherUser", singletonList("read")));

        List<WorkerImpl> workersByUser = workerDao.getWorkersByUser(worker.getUserId());

        assertEquals(workersByUser.size(), 1);
        assertEquals(workersByUser.get(0), worker);
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowServerExceptionWhenMongoExceptionWasThrewOnGettingWorkersByUser() throws Exception {
        final MongoDatabase db = mockDatabase(col -> doThrow(mock(MongoException.class)).when(col).find((Bson)any()));

        new WorkerDaoImpl(db, "worker").getWorkersByUser("user");
    }

    @Test
    public void shouldBeAbleToGetWorker() throws Exception {
        final WorkerImpl worker = createWorker();
        collection.insertOne(worker);
        collection.insertOne(new WorkerImpl("workspace123", "anotherUser", singletonList("read")));

        WorkerImpl workersByUser = workerDao.getWorker(worker.getWorkspaceId(), worker.getUserId());

        assertEquals(workersByUser, worker);
    }

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Worker with user 'tempUser' and workspace 'fakeWorkspace' was not found")
    public void shouldThrowNotFoundExceptionWhenRequestedWorkerDoesNotExist() throws Exception {
        workerDao.getWorker("fakeWorkspace", "tempUser");
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowServerExceptionWhenMongoExceptionWasThrewOnGettingWorker() throws Exception {
        final MongoDatabase db = mockDatabase(col -> doThrow(mock(MongoException.class)).when(col).find((Bson)any()));

        new WorkerDaoImpl(db, "worker").getWorker("workspace123", "user123");
    }


    @Test
    public void shouldBeAbleToGetWorkersByWorkspace() throws Exception {
        final WorkerImpl worker = createWorker();
        collection.insertOne(worker);
        WorkerImpl anotherWorker = new WorkerImpl("workspace123", "user", singletonList("read"));
        collection.insertOne(anotherWorker);

        final List<WorkerImpl> result = workerDao.getWorkers(worker.getWorkspaceId());

        assertEquals(result.size(), 2);
        assertTrue(result.contains(worker));
        assertTrue(result.contains(anotherWorker));
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowServerExceptionWhenMongoExceptionWasThrewOnGettingWorkersByWorkspace() throws Exception {
        final MongoDatabase db = mockDatabase(col -> doThrow(mock(MongoException.class)).when(col).find((Bson)any()));

        new WorkerDaoImpl(db, "worker").getWorkers("workspace123");
    }

    private WorkerImpl createWorker() {
        return new WorkerImpl("workspace123", "user123",
                              Arrays.asList("read", "use", "run"));
    }

    private MongoDatabase mockDatabase(Consumer<MongoCollection<WorkerImpl>> consumer) {
        @SuppressWarnings("unchecked")
        final MongoCollection<WorkerImpl> collection = mock(MongoCollection.class);
        consumer.accept(collection);

        final MongoDatabase database = mock(MongoDatabase.class);
        when(database.getCollection("worker", WorkerImpl.class)).thenReturn(collection);

        return database;
    }
}
