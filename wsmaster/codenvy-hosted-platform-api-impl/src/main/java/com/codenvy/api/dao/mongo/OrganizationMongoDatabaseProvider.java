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

import org.bson.codecs.configuration.CodecProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Provides {@link com.mongodb.client.MongoDatabase} single instance for <i>organization</i> database.
 *
 * @author Eugene Voevodin
 */
@Singleton
public class OrganizationMongoDatabaseProvider extends MongoDatabaseProvider {

    @Inject
    public OrganizationMongoDatabaseProvider(@Named("organization.storage.db.url") String dbUrl,
                                             @Named("organization.storage.db.name") String dbName,
                                             @Named("organization.storage.db.username") String username,
                                             @Named("organization.storage.db.password") String password,
                                             Set<CodecProvider> codecProviders) {
        super(dbUrl, dbName, username, password, codecProviders);
    }
}
