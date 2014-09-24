/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.analytics.metrics.runs;

import com.codenvy.analytics.metrics.AbstractLongValueResulted;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.MetricType;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import javax.annotation.security.RolesAllowed;

/** @author Anatoliy Bazko */
@RolesAllowed(value = {"user", "system/admin", "system/manager"})
public class RunsWithTimeout extends AbstractLongValueResulted {

    public RunsWithTimeout() {
        super(MetricType.RUNS_WITH_TIMEOUT, PROJECT_ID);
    }

    @Override
    public DBObject[] getSpecificDBOperations(Context clauses) {
        String field = getTrackedFields()[0];

        DBObject match = new BasicDBObject();
        match.put(LIFETIME, new BasicDBObject("$ne", -1));

        DBObject group = new BasicDBObject();
        group.put(ID, null);
        group.put(field, new BasicDBObject("$sum", "$" + field));

        return new DBObject[]{new BasicDBObject("$match", match),
                              new BasicDBObject("$group", group)};
    }


    @Override
    public String getStorageCollectionName() {
        return getStorageCollectionName(MetricType.RUNS);
    }

    @Override
    public String getDescription() {
        return "The number of runs with timeout";
    }
}