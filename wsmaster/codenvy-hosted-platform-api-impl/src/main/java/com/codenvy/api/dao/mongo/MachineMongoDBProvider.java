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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides single instance of database to all consumers.
 *
 * @deprecated use {@link MachineMongoDatabaseProvider}, for more information see {@link MongoDatabaseProvider}
 */
@Deprecated
@Singleton
public class MachineMongoDBProvider extends MongoDBProvider {
    protected static final String DB_URL      = "storage.machine.db.url";
    protected static final String DB_NAME     = "storage.machine.db.name";
    protected static final String DB_USERNAME = "storage.machine.db.username";
    protected static final String DB_PASSWORD = "storage.machine.db.password";

    @Inject
    public MachineMongoDBProvider(@Named(DB_URL) String dbUrl,
                                  @Named(DB_NAME) String dbName,
                                  @Named(DB_USERNAME) String username,
                                  @Named(DB_PASSWORD) String password) {
        super(dbUrl, dbName, username, password);
    }

    @Override
    public DB get() {
        return super.get();
    }
}
