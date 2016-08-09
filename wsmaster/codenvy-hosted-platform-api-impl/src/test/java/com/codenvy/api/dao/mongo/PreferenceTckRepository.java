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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;

import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.eclipse.che.commons.test.tck.repository.TckRepositoryException;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Map;

import static com.codenvy.api.dao.mongo.MongoUtil.asDBList;

/**
 * @author Anton Korneta
 */
public class PreferenceTckRepository implements TckRepository<Pair<String, Map<String,String>>> {

    @Inject
    @Named("mongo.db.organization")
    private DB db;

    @Override
    public void createAll(Collection<? extends Pair<String, Map<String, String>>> entities) throws TckRepositoryException {
        final DBCollection collection = db.getCollection("preference.collection");
        for (Pair<String, Map<String, String>> entity : entities) {
            final BasicDBObject preferencesDocument = new BasicDBObject("_id", entity.first).append("preferences", asDBList(entity.second));
            collection.save(preferencesDocument);
        }
    }

    @Override
    public void removeAll() throws TckRepositoryException {
        db.getCollection("preference.collection").remove(new BasicDBObject());
    }
}
