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
package com.codenvy.api.dao.ldap;

import org.eclipse.che.commons.lang.IoUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class BaseTest {

    File               server;
    EmbeddedLdapServer embeddedLdapServer;

    @BeforeMethod
    public void startServer() throws Exception {
        URL u = Thread.currentThread().getContextClassLoader().getResource(".");
        assertNotNull(u);
        File target = new File(u.toURI()).getParentFile();
        server = new File(target, "server");
        assertTrue(server.mkdirs(), "Unable create directory for temporary data");
        embeddedLdapServer = EmbeddedLdapServer.start(server);
    }

    @AfterMethod
    public void stopServer() throws Exception {
        embeddedLdapServer.stop();
        assertTrue(IoUtil.deleteRecursive(server), "Unable remove temporary data");
    }
}
