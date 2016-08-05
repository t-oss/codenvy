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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;

import org.bson.Document;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.codenvy.api.dao.mongo.MongoUtil.asDBList;
import static com.codenvy.api.dao.mongo.MongoUtil.asMap;
import static com.codenvy.api.dao.mongo.MongoUtil.documentsListAsMap;
import static com.codenvy.api.dao.mongo.MongoUtil.handleWriteConflict;
import static com.codenvy.api.dao.mongo.MongoUtil.mapAsDocumentsList;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Eugene Voevodin
 */
public class MongoUtilTest {

    @Test
    public void shouldBeAbleToConvertMapToDBList() {
        final Map<String, String> src = new HashMap<>(4);
        src.put("test-key-1", "test-value-1");
        src.put("test-key-2", "test-value-2");

        final BasicDBList list = asDBList(src);

        assertEquals(list.size(), 2);
        final BasicDBObject testKey1 = new BasicDBObject("name", "test-key-1").append("value", "test-value-1");
        final BasicDBObject testKey2 = new BasicDBObject("name", "test-key-2").append("value", "test-value-2");
        assertEquals(new HashSet<>(list), new HashSet<>(asList(testKey1, testKey2)));
    }

    @Test
    public void shouldBeAbleToConvertDBListToMap() {
        final BasicDBObject testKey1 = new BasicDBObject("name", "test-key-1").append("value", "test-value-1");
        final BasicDBObject testKey2 = new BasicDBObject("name", "test-key-2").append("value", "test-value-2");
        final BasicDBList list = new BasicDBList();
        list.addAll(asList(testKey1, testKey2));

        final Map<String, String> map = asMap(list);
        assertEquals(map.size(), 2);
        assertEquals(map.get("test-key-1"), "test-value-1");
        assertEquals(map.get("test-key-2"), "test-value-2");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenArgumentIsNotBasicDBList() {
        asMap(new Object());
    }

    @Test
    public void testMapAsDocumentsList() throws Exception {
        final Map<String, String> source = new HashMap<>(4);
        source.put("key1", "value1");
        source.put("key2", "value2");

        final List<Document> result = mapAsDocumentsList(source);

        assertEquals(result.size(), 2);
        assertEquals(result.get(0), new Document("name", "key1").append("value", "value1"));
        assertEquals(result.get(1), new Document("name", "key2").append("value", "value2"));
    }

    @Test
    public void testDocumentsListAsMap() throws Exception {
        final List<Document> source = new ArrayList<>(2);
        source.add(new Document("name", "key1").append("value", "value1"));
        source.add(new Document("name", "key2").append("value", "value2"));

        final Map<String, String> result = documentsListAsMap(source);

        assertEquals(result.size(), 2);
        assertEquals(result.get("key1"), "value1");
        assertEquals(result.get("key2"), "value2");
    }

    @Test(expectedExceptions = ConflictException.class, expectedExceptionsMessageRegExp = "duplicate key")
    public void shouldThrowConflictExceptionWhenWriteExceptionIsDuplicateKey() throws Exception {
        handleWriteConflict(mockWriteEx(ErrorCategory.DUPLICATE_KEY), "duplicate key");
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowServerExceptionWhenWriteExceptionIsNotDuplicateKey() throws Exception {
        handleWriteConflict(mockWriteEx(ErrorCategory.EXECUTION_TIMEOUT), "duplicate key");
    }

    public static MongoWriteException mockWriteEx(ErrorCategory category) {
        final WriteError writeErrMock = mock(WriteError.class);
        when(writeErrMock.getCategory()).thenReturn(category);
        final MongoWriteException writeExMock = mock(MongoWriteException.class);
        when(writeExMock.getError()).thenReturn(writeErrMock);
        return writeExMock;
    }
}
