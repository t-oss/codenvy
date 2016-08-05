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
 * @deprecated use {@link OrganizationMongoDatabaseProvider}, for more information see {@link MongoDatabaseProvider}
 */
@Deprecated
@Singleton
public class OrganizationMongoDBProvider extends MongoDBProvider {

    protected static final String DB_URL      = "organization.storage.db.url";
    protected static final String DB_NAME     = "organization.storage.db.name";
    protected static final String DB_USERNAME = "organization.storage.db.username";
    protected static final String DB_PASSWORD = "organization.storage.db.password";

    @Inject
    public OrganizationMongoDBProvider(@Named(DB_URL) String dbUrl,
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
