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

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import javax.inject.Provider;
import javax.inject.Singleton;

import static com.mongodb.MongoCredential.createCredential;
import static java.util.Collections.singletonList;

/**
 * Provides single instance of database to all consumers.
 *
 * @deprecated as we moved to <i>Mongo DB 3.0</i> we need to use new MongoDB driver API see {@link MongoDatabaseProvider}
 */

@Deprecated
@Singleton
public class MongoDBProvider implements Provider<DB> {
    private final String dbUrl;
    private final String dbName;
    private final String username;
    private final String password;

    private volatile DB db;

    public MongoDBProvider(String dbUrl, String dbName, String username, String password) {
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    @Override
    public DB get() {
        if (db == null) {
            synchronized (this) {
                if (db == null) {
                    MongoCredential credential = createCredential(username, dbName, password.toCharArray());
                    MongoClient mongoClient = new MongoClient(new ServerAddress(dbUrl), singletonList(credential));
                    db = mongoClient.getDB(dbName);
                }
            }
        }
        return db;
    }
}
