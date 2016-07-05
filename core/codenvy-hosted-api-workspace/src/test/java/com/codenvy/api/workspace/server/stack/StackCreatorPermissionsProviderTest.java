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
package com.codenvy.api.workspace.server.stack;

import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.api.machine.server.model.impl.AclEntryImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link StackCreatorPermissionsProvider}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class StackCreatorPermissionsProviderTest {
    @Mock
    MethodInvocation                   invocation;
    @Mock
    StackImpl                          stack;
    @Captor
    ArgumentCaptor<List<AclEntryImpl>> aclCaptor;

    StackCreatorPermissionsProvider provider;

    @BeforeMethod
    public void setUp() {
        provider = new StackCreatorPermissionsProvider();

        when(invocation.getArguments()).thenReturn(new Object[] {stack});
    }

    @Test
    public void shouldCreateAclEntryForCreatorIfThereIsNotAclAtAll() throws Throwable {
        when(stack.getAcl()).thenReturn(null);
        when(stack.getCreator()).thenReturn("user123");

        provider.invoke(invocation);

        verify(stack).setAcl(aclCaptor.capture());
        final List<AclEntryImpl> acl = aclCaptor.getValue();
        AclEntryImpl creatorAclEntry = acl.stream()
                                          .findAny()
                                          .orElseGet(null);
        assertNotNull(creatorAclEntry);
        assertEquals(creatorAclEntry.getUser(), "user123");
        assertTrue(creatorAclEntry.getActions().containsAll(new StackDomain().getAllowedActions()));
    }

    @Test
    public void shouldAddAclEntryForCreator() throws Throwable {
        when(stack.getAcl()).thenReturn(new ArrayList<>());
        when(stack.getCreator()).thenReturn("user123");

        provider.invoke(invocation);

        verify(stack).setAcl(aclCaptor.capture());
        final List<AclEntryImpl> acl = aclCaptor.getValue();
        AclEntryImpl creatorAclEntry = acl.stream()
                                          .findAny()
                                          .orElseGet(null);
        assertNotNull(creatorAclEntry);
        assertEquals(creatorAclEntry.getUser(), "user123");
        assertTrue(creatorAclEntry.getActions().containsAll(new StackDomain().getAllowedActions()));
    }

    @Test
    public void shouldAddAllActionToAclEntryForCreator() throws Throwable {
        final AclEntryImpl aclEntry = new AclEntryImpl("user123", new ArrayList<>(singletonList("read")));
        when(stack.getAcl()).thenReturn(new ArrayList<>(singletonList(aclEntry)));
        when(stack.getCreator()).thenReturn("user123");

        provider.invoke(invocation);

        verify(stack).setAcl(aclCaptor.capture());
        final List<AclEntryImpl> acl = aclCaptor.getValue();
        AclEntryImpl creatorAclEntry = acl.stream()
                                          .findAny()
                                          .orElseGet(null);
        assertNotNull(creatorAclEntry);
        assertEquals(creatorAclEntry.getUser(), "user123");
        assertTrue(creatorAclEntry.getActions().containsAll(new StackDomain().getAllowedActions()));
    }

}
