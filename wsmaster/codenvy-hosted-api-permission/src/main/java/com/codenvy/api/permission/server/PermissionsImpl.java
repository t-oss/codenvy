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
package com.codenvy.api.permission.server;

import com.codenvy.api.permission.shared.Permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents users' permissions to access to some resources
 *
 * @author Sergii Leschenko
 */
public class PermissionsImpl implements Permissions {
    private String       user;
    private String       domain;
    private String       instance;
    private List<String> actions;

    public PermissionsImpl(Permissions permissions) {
        this(permissions.getUser(), permissions.getDomain(), permissions.getInstance(), permissions.getActions());
    }

    public PermissionsImpl(String user, String domain, String instance, List<String> actions) {
        this.user = user;
        this.domain = domain;
        this.instance = instance;
        this.actions = new ArrayList<>(actions);
    }

    /**
     * Returns used id
     */
    @Override
    public String getUser() {
        return user;
    }

    /**
     * Returns domain id
     */
    @Override
    public String getDomain() {
        return domain;
    }

    /**
     * Returns instance id
     */
    @Override
    public String getInstance() {
        return instance;
    }

    /**
     * List of actions which user can perform for particular instance
     */
    @Override
    public List<String> getActions() {
        return actions;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PermissionsImpl)) return false;
        final PermissionsImpl other = (PermissionsImpl)obj;
        return Objects.equals(user, other.user) &&
               Objects.equals(domain, other.domain) &&
               Objects.equals(instance, other.instance) &&
               Objects.equals(actions, other.actions);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(user);
        hash = 31 * hash + Objects.hashCode(domain);
        hash = 31 * hash + Objects.hashCode(instance);
        hash = 31 * hash + Objects.hashCode(actions);
        return hash;
    }

    @Override
    public String toString() {
        return "Permissions{" +
               "user='" + user + '\'' +
               ", domain='" + domain + '\'' +
               ", instance='" + instance + '\'' +
               ", actions=" + actions +
               '}';
    }
}
