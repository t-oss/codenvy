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

import com.github.fakemongo.Fongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;


/**
 * Base test for <i>XDao</i> implementations based on mongo
 *
 * @author Eugene Voevodin
 */
public class BaseDaoTest {

    private static final String DB_NAME = "test1";

    protected DBCollection  collection;
    protected MongoClient   client;
    protected DB            db;
    protected MongoDatabase database;

    public void setUp(String collectionName) {
        final Fongo fongo = new Fongo("test server");
        client = fongo.getMongo();
        db = client.getDB(DB_NAME);
        collection = db.getCollection(collectionName);
    }
}
