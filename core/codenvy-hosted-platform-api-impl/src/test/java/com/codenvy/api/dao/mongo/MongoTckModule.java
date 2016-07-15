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
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.mongodb.DB;
import com.mongodb.FongoDB;

import org.eclipse.che.api.user.server.spi.PreferenceDao;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.test.tck.TckModule;
import org.eclipse.che.commons.test.tck.repository.TckRepository;

import java.util.Map;

/**
 * @author Anton Korneta
 */
public class MongoTckModule extends TckModule {

    @Override
    protected void configure() {
        final FongoDB db = new Fongo("test server").getDB("test1");
        bind(DB.class).annotatedWith(Names.named("mongo.db.organization")).toInstance(db);
        bind(String.class).annotatedWith(Names.named("organization.storage.db.preferences.collection"))
                          .toInstance("preference.collection");

        bind(new TypeLiteral<TckRepository<Pair<String, Map<String, String>>>>() {}).to(PreferenceTckRepository.class);

        bind(PreferenceDao.class).to(PreferenceDaoImpl.class);
    }
}
