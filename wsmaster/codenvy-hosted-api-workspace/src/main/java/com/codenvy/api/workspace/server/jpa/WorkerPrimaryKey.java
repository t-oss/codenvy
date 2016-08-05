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
package com.codenvy.api.workspace.server.jpa;

import com.codenvy.api.workspace.server.model.impl.WorkerImpl;

import java.io.Serializable;
import java.util.Objects;

/**
 * Primary key for {@link WorkerImpl} entity.
 *
 * @author Max Shaposhnik
 */
public class WorkerPrimaryKey implements Serializable {

    private String userId;
    private String workspaceId;

    public WorkerPrimaryKey() {
    }

    public WorkerPrimaryKey(String workspaceId, String userId) {
        this.userId = userId;
        this.workspaceId = workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WorkerPrimaryKey)) return false;
        WorkerPrimaryKey that = (WorkerPrimaryKey)obj;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(workspaceId, that.workspaceId);

    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(userId);
        hash = 31 * hash + Objects.hashCode(workspaceId);
        return hash;
    }
}
