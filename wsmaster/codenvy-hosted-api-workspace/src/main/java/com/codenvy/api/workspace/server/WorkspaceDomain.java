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
package com.codenvy.api.workspace.server;

import com.codenvy.api.permission.server.AbstractPermissionsDomain;
import com.google.common.collect.ImmutableList;

/**
 * Domain for storing workspaces' permissions
 *
 * @author Sergii Leschenko
 */
public class WorkspaceDomain extends AbstractPermissionsDomain {
    public static final String READ      = "read";
    public static final String RUN       = "run";
    public static final String USE       = "use";
    public static final String CONFIGURE = "configure";
    public static final String DELETE    = "delete";

    public static final String DOMAIN_ID = "workspace";

    public WorkspaceDomain() {
        super(DOMAIN_ID, ImmutableList.of(READ,
                                          RUN,
                                          USE,
                                          CONFIGURE,
                                          DELETE));
    }
}
