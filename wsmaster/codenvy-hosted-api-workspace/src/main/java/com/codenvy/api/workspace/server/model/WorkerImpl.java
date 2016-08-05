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
package com.codenvy.api.workspace.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Sergii Leschenko
 */
public class WorkerImpl implements Worker {
    private String       user;
    private String       workspace;
    private List<String> actions;

    public WorkerImpl(String user, String workspace, List<String> actions) {
        this.user = user;
        this.workspace = workspace;
        this.actions = new ArrayList<>();
        if (actions != null) {
            this.actions.addAll(actions);
        }
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getWorkspace() {
        return workspace;
    }

    @Override
    public List<String> getActions() {
        return actions;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WorkerImpl)) return false;
        final WorkerImpl other = (WorkerImpl)obj;
        return Objects.equals(user, other.user)
               && Objects.equals(workspace, other.workspace)
               && actions.equals(other.actions);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(user);
        hash = 31 * hash + Objects.hashCode(workspace);
        hash = 31 * hash + actions.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "WorkerImpl{" +
               "user='" + user + '\'' +
               ", workspace='" + workspace + '\'' +
               ", actions=" + actions +
               '}';
    }

}
