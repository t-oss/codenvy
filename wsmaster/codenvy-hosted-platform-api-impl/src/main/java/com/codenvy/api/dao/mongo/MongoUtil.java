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

import org.bson.Document;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Utils for Mongo.
 *
 * @author Yevhenii Voevodin
 */
public final class MongoUtil {

    /**
     * Checks if the {@link MongoWriteException writeEx} category is {@link ErrorCategory#DUPLICATE_KEY}
     * and if it is - throws {@link ConflictException} with given {@code dkExErrorMessage},
     * otherwise throws {@link ServerException} with the message based on the {@code writeEx}.
     *
     * <p>The most typical usage of this method is <i>update/create</i> dao methods,
     * which need to throw {@link ConflictException} in the case of duplicate key(e.g. workspace with
     * such name already exists) but to do this they need to extract actual write exception category
     * and write the common code.
     *
     * <p>Example:
     * <pre>{@code
     *  try {
     *      collection.insertOne(workspace);
     *  } catch (MongoWriteException writeEx) {
     *      if (writeEx.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
     *          throw new ConflictException("Workspace with the name " + name + " already exists");
     *      }
     *      throw new ServerException(writeEx.getMessage(), writeEx);
     *  } catch (MongoException mongoEx) {
     *      throw new ServerException(mongoEx.getMessage(), mongoEx);
     *  }
     * }</pre>
     *
     * <p>The goal of this method is to simplify this process by extracting and rethrowing appropriate exceptions.
     * Example:
     * <pre>{@code
     *  try {
     *      collection.insertOne(workspace);
     *  } catch (MongoWriteException writeEx) {
     *      handleWriteConflict(writeEx, "Workspace with the name " + name + " already exists");
     *  } catch (MongoException mongoEx) {
     *      throw new ServerException(mongoEx.getMessage(), mongoEx);
     *  }
     * }</pre>
     *
     * @param writeEx
     *         write exception which category should be checked
     * @param dkExErrorMessage
     *         the message of the conflict exception to throw when {@code writeEx} category is {@link ErrorCategory#DUPLICATE_KEY}
     * @throws ConflictException
     *         when {@code writeEx} category is {@link ErrorCategory#DUPLICATE_KEY}.
     *         Exception message is equal to the {@code dkExErrorMessage}
     * @throws ServerException
     *         when {@code writeEx} category is different from the {@link ErrorCategory#DUPLICATE_KEY}.
     *         Exception message is based on the {@code writeEx} message
     */
    public static void handleWriteConflict(MongoWriteException writeEx, String dkExErrorMessage) throws ConflictException, ServerException {
        if (writeEx.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
            throw new ConflictException(dkExErrorMessage);
        }
        throw new ServerException(writeEx.getMessage(), writeEx);
    }

    /**
     * Converts map to database list
     */
    public static BasicDBList asDBList(Map<String, String> src) {
        final BasicDBList list = new BasicDBList();
        for (Map.Entry<String, String> entry : src.entrySet()) {
            list.add(new BasicDBObject().append("name", entry.getKey())
                                        .append("value", entry.getValue()));
        }
        return list;
    }

    /**
     * Converts database list to Map
     */
    public static Map<String, String> asMap(Object src) {
        if (!(src instanceof BasicDBList)) {
            throw new IllegalArgumentException("BasicDBList was expected");
        }
        final BasicDBList list = (BasicDBList)src;
        final Map<String, String> map = new HashMap<>();
        for (Object obj : list) {
            final BasicDBObject attribute = (BasicDBObject)obj;
            map.put(attribute.getString("name"), attribute.getString("value"));
        }
        return map;
    }

    /**
     * Converts list of documents to a map.
     *
     * <pre>
     *     List of documents:
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
     *
     *     Will be converted to map:
     *     {
     *         "attribute1" : "value1",
     *         "attribute2" : "value2"
     *     }
     * </pre>
     *
     * @param documents
     *         list of documents
     * @return map view of given list
     * @see #mapAsDocumentsList(Map)
     */
    public static Map<String, String> documentsListAsMap(List<Document> documents) {
        return documents.stream().collect(toMap(d -> d.getString("name"), d -> d.getString("value")));
    }

    /**
     * Converts map to list of documents.
     *
     * @return list representation of given map
     * @see #documentsListAsMap(List)
     */
    public static List<Document> mapAsDocumentsList(Map<String, ?> map) {
        return map.entrySet()
                  .stream()
                  .map(entry -> new Document("name", entry.getKey()).append("value", entry.getValue()))
                  .collect(toList());
    }

    public static List<String> asStringList(Object src) {
        if (!(src instanceof BasicDBList)) {
            throw new IllegalArgumentException("BasicDBList was expected");
        }
        final BasicDBList basicList = (BasicDBList)src;
        final List<String> result = new ArrayList<>(basicList.size());
        for (Object obj : basicList) {
            result.add(obj.toString());
        }
        return result;
    }

    public static BasicDBList asDBList(List<?> list) {
        final BasicDBList dbList = new BasicDBList();
        dbList.addAll(list);
        return dbList;
    }

    private MongoUtil() {}
}
