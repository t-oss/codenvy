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
package com.codenvy.api.event.user;

import org.eclipse.che.api.core.notification.EventOrigin;

/**
 * Inform that account with certain id was removed.
 * Should be sending after successfully removing of account.
 *
 * @author Sergii Leschenko
 */
@EventOrigin("account")
public class RemoveAccountEvent {
    private String accountId;

    public RemoveAccountEvent(String id) {
        this.accountId = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
