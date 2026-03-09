/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DomainAccessManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DomainACLAccessManager}.
 *
 * <p>The constructor calls {@code RightManager.getInstance()}.  Tests are skipped when
 * the manager cannot be created (no XML files in the unit-test classpath).
 *
 * <p>The self-access path in {@code canDo(MailTarget, Entry, Right, boolean, boolean)}
 * is testable without any ACL/LDAP plumbing by passing the same account as both grantee
 * and target.
 */
public class DomainACLAccessManagerTest {

    // ---------------------------------------------------------------
    // Inner mock-account helpers
    // ---------------------------------------------------------------

    private static class MockAccount extends Account {
        private final String id = UUID.randomUUID().toString();

        MockAccount(String name) {
            super(name, null, null, null, null);
        }

        @Override public String getId()                              { return id; }
        @Override public boolean isIsAdminAccount()                 { return false; }
        @Override public boolean isIsDelegatedAdminAccount()        { return false; }
    }

    // ---------------------------------------------------------------
    // Test fixture
    // ---------------------------------------------------------------

    private static DomainACLAccessManager manager;

    @BeforeClass
    public static void setUpClass() {
        try {
            manager = new DomainACLAccessManager();
        } catch (ServiceException e) {
            manager = null; // RightManager not available; tests will be skipped
        }
    }

    // ---------------------------------------------------------------
    // canDo(MailTarget, Entry, Right, boolean, boolean) — self-access
    // ---------------------------------------------------------------

    /**
     * When grantee and target are the same account, canDo must return true immediately
     * (self-access rule) without consulting any ACL store.
     */
    @Test
    public void testCanDo_selfAccess_returnsTrue() {
        Assume.assumeNotNull(manager);

        MockAccount account = new MockAccount("user@test.com");
        Right right = mockRight();

        // same object used as both grantee (MailTarget) and target (Entry)
        boolean result = manager.canDo((MailTarget) account, (Entry) account, right, false, false);
        assertTrue(result);
    }

    @Test
    public void testCanDo_selfAccess_asAdminTrue_returnsTrue() {
        Assume.assumeNotNull(manager);

        MockAccount account = new MockAccount("admin@test.com");
        Right right = mockRight();

        boolean result = manager.canDo((MailTarget) account, (Entry) account, right, true, false);
        assertTrue(result);
    }

    // ---------------------------------------------------------------
    // canDo — null grantee is treated as anonymous
    // ---------------------------------------------------------------

    @Test
    public void testCanDo_nullGrantee_nonSelfTarget_returnsDefaultGrant() {
        Assume.assumeNotNull(manager);

        MockAccount target = new MockAccount("target@test.com");
        Right right = mockRightWithNoDefault();

        // null grantee → GuestAccount.ANONYMOUS_ACCT; target is a different account
        // no matching ACL, no configured default → falls through to defaultGrant=false
        boolean result = manager.canDo((MailTarget) null, (Entry) target, right, false, false);
        assertFalse(result);
    }

    @Test
    public void testCanDo_nullGrantee_nonSelfTarget_defaultGrantTrue_returnsTrue() {
        Assume.assumeNotNull(manager);

        MockAccount target = new MockAccount("target@test.com");
        Right right = mockRightWithNoDefault();

        // no matching ACL, no configured default → falls through to defaultGrant=true
        boolean result = manager.canDo((MailTarget) null, (Entry) target, right, false, true);
        assertTrue(result);
    }

    // ---------------------------------------------------------------
    // canDo — different grantee and target accounts (not self-access)
    // ---------------------------------------------------------------

    @Test
    public void testCanDo_differentAccounts_noAcl_returnsDefaultGrant() {
        Assume.assumeNotNull(manager);

        MockAccount grantee = new MockAccount("grantee@test.com");
        MockAccount target  = new MockAccount("target@test.com");
        Right right = mockRightWithNoDefault();

        // Different accounts, no ACL → falls back to defaultGrant=false
        boolean result = manager.canDo((MailTarget) grantee, (Entry) target, right, false, false);
        assertFalse(result);
    }

    // ---------------------------------------------------------------
    // canDo(String, Entry, Right, boolean, boolean) — null email
    // ---------------------------------------------------------------

    @Test
    public void testCanDo_stringGrantee_nullEmail_nonSelfTarget_returnsDefaultGrant() {
        Assume.assumeNotNull(manager);

        MockAccount target = new MockAccount("target@test.com");
        Right right = mockRightWithNoDefault();

        // null email → granteeAcct = ANONYMOUS_ACCT; not self; no ACL → defaultGrant
        boolean result = manager.canDo((String) null, (Entry) target, right, false, false);
        assertFalse(result);
    }

    // ---------------------------------------------------------------
    // Class hierarchy — structural tests (no manager instance needed)
    // ---------------------------------------------------------------

    @Test
    public void testDomainACLAccessManager_extendsDomainAccessManager() {
        assertTrue(DomainAccessManager.class.isAssignableFrom(DomainACLAccessManager.class));
    }

    @Test
    public void testDomainACLAccessManager_isPublicConcreteClass() {
        assertTrue(java.lang.reflect.Modifier.isPublic(DomainACLAccessManager.class.getModifiers()));
        assertFalse(java.lang.reflect.Modifier.isAbstract(DomainACLAccessManager.class.getModifiers()));
    }

    // ---------------------------------------------------------------
    // canDo: right with a default of true (Boolean.TRUE) overrides defaultGrant
    // ---------------------------------------------------------------

    @Test
    public void testCanDo_differentAccounts_rightDefaultTrue_returnsTrue() {
        Assume.assumeNotNull(manager);
        MockAccount grantee = new MockAccount("grantee2@test.com");
        MockAccount target  = new MockAccount("target2@test.com");

        // Right with getDefault() = TRUE: no ACL → falls back to right's default
        Right rightWithDefault = Mockito.mock(Right.class);
        Mockito.when(rightWithDefault.getDefault()).thenReturn(Boolean.TRUE);
        Mockito.when(rightWithDefault.getName()).thenReturn("defaultTrueRight");
        Mockito.when(rightWithDefault.isUserRight()).thenReturn(true);

        boolean result = manager.canDo((MailTarget) grantee, (Entry) target,
                rightWithDefault, false, false);
        assertTrue(result);
    }

    @Test
    public void testCanDo_differentAccounts_rightDefaultFalse_returnsFalse() {
        Assume.assumeNotNull(manager);
        MockAccount grantee = new MockAccount("grantee3@test.com");
        MockAccount target  = new MockAccount("target3@test.com");

        Right rightWithDefault = Mockito.mock(Right.class);
        Mockito.when(rightWithDefault.getDefault()).thenReturn(Boolean.FALSE);
        Mockito.when(rightWithDefault.getName()).thenReturn("defaultFalseRight");
        Mockito.when(rightWithDefault.isUserRight()).thenReturn(true);

        boolean result = manager.canDo((MailTarget) grantee, (Entry) target,
                rightWithDefault, false, true); // defaultGrant=true but right.getDefault()=false wins
        assertFalse(result);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** A minimal Right mock whose getDefault() returns null (no configured default). */
    private static Right mockRightWithNoDefault() {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.getDefault()).thenReturn(null);
        Mockito.when(r.getName()).thenReturn("mockRight");
        Mockito.when(r.isUserRight()).thenReturn(true);
        return r;
    }

    private static Right mockRight() {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.getName()).thenReturn("mockRight");
        Mockito.when(r.isUserRight()).thenReturn(true);
        return r;
    }
}
