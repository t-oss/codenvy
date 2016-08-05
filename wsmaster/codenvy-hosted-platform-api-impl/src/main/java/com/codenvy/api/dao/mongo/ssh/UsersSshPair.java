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
package com.codenvy.api.dao.mongo.ssh;

import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;
import org.eclipse.che.api.ssh.shared.model.SshPair;

import java.util.Objects;

/**
 * Defines ssh pair owned by user
 *
 * @author Sergii Leschenko
 */
public class UsersSshPair extends SshPairImpl {
    private final String owner;

    public UsersSshPair(String owner, SshPair sshPair) {
        super(sshPair);
        this.owner = owner;
    }

    public UsersSshPair(String owner, String service, String name, String publicKey, String privateKey) {
        super(service, name, publicKey, privateKey);
        this.owner = owner;
    }

    /**
     * Returns the id of the user who is the owner of the ssh pair
     */
    public String getOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsersSshPair)) return false;
        if (!super.equals(o)) return false;
        UsersSshPair other = (UsersSshPair)o;
        return Objects.equals(owner, other.owner);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(owner);
        return result;
    }

    @Override
    public String toString() {
        return "UsersSshPair{" +
               "owner='" + owner + '\'' +
               ", service='" + getService() + '\'' +
               ", name='" + getName() + '\'' +
               ", publicKey='" + getPublicKey() + '\'' +
               ", privateKey='" + getPrivateKey() + '\'' +
               '}';
    }
}
