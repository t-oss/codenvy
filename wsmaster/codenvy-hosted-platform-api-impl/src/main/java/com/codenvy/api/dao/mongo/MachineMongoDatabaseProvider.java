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
 * Provides {@link com.mongodb.client.MongoDatabase} single instance for <i>machines</i> database.
 *
 * @author Eugene Voevodin
 */
@Singleton
public class MachineMongoDatabaseProvider extends MongoDatabaseProvider {

    @Inject
    public MachineMongoDatabaseProvider(@Named("storage.machine.db.url") String dbUrl,
                                        @Named("storage.machine.db.name") String dbName,
                                        @Named("storage.machine.db.username") String username,
                                        @Named("storage.machine.db.password") String password,
                                        Set<CodecProvider> codecProviders) {
        super(dbUrl, dbName, username, password, codecProviders);
    }
}
