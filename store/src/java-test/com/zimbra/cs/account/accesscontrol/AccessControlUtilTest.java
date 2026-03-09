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
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AccessControlUtil}.
 *
 * Uses lightweight inner-class mock accounts to control isIsAdminAccount /
 * isIsDelegatedAdminAccount without needing LDAP.  MockProvisioning is used
 * for the email-address look-up paths.
 */
public class AccessControlUtilTest {

    // ---------------------------------------------------------------
    // Inner mock-account helpers
    // ---------------------------------------------------------------

    /** Regular (non-admin, non-delegated) account stub. */
    private static class MockAccount extends Account {
        private final String id = UUID.randomUUID().toString();

        MockAccount(String name) {
            super(name, null, null, null, null);
        }

        @Override public String getId()  { return id; }

        @Override public boolean isIsAdminAccount()           { return false; }
        @Override public boolean isIsDelegatedAdminAccount()  { return false; }
    }

    /** Global-admin account stub. */
    private static class MockAdminAccount extends MockAccount {
        MockAdminAccount(String name) { super(name); }
        @Override public boolean isIsAdminAccount() { return true; }
    }

    /** Delegated-admin account stub. */
    private static class MockDelegatedAdminAccount extends MockAccount {
        MockDelegatedAdminAccount(String name) { super(name); }
        @Override public boolean isIsDelegatedAdminAccount() { return true; }
    }

    // ---------------------------------------------------------------
    // Right helpers
    // ---------------------------------------------------------------

    private static Right userRight() {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.isUserRight()).thenReturn(true);
        return r;
    }

    private static Right adminRight() {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.isUserRight()).thenReturn(false);
        return r;
    }

    // ---------------------------------------------------------------
    // Test fixture — accounts pre-registered in MockProvisioning
    // ---------------------------------------------------------------

    private static MockProvisioning prov;
    private static Account knownAccount;
    private static Account knownAccount2;

    @BeforeClass
    public static void setUpClass() throws ServiceException {
        prov = new MockProvisioning();
        Provisioning.setInstance(prov);

        Map<String, Object> attrs1 = new HashMap<String, Object>();
        attrs1.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        knownAccount = prov.createAccount("known@accesscontrolutil.test", "secret", attrs1);

        Map<String, Object> attrs2 = new HashMap<String, Object>();
        attrs2.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        knownAccount2 = prov.createAccount("known2@accesscontrolutil.test", "secret", attrs2);
    }

    // ---------------------------------------------------------------
    // isGlobalAdmin(Account)
    // ---------------------------------------------------------------

    @Test
    public void testIsGlobalAdmin_nullAccount_returnsFalse() {
        assertFalse(AccessControlUtil.isGlobalAdmin((Account) null));
    }

    @Test
    public void testIsGlobalAdmin_regularAccount_returnsFalse() {
        assertFalse(AccessControlUtil.isGlobalAdmin(new MockAccount("user@test.com")));
    }

    @Test
    public void testIsGlobalAdmin_adminAccount_returnsTrue() {
        assertTrue(AccessControlUtil.isGlobalAdmin(new MockAdminAccount("admin@test.com")));
    }

    // ---------------------------------------------------------------
    // isGlobalAdmin(MailTarget, boolean)
    // ---------------------------------------------------------------

    @Test
    public void testIsGlobalAdmin_mailTarget_asAdminFalse_returnsFalse() {
        // even for a global admin, asAdmin=false must return false
        assertFalse(AccessControlUtil.isGlobalAdmin(
                (MailTarget) new MockAdminAccount("admin@test.com"), false));
    }

    @Test
    public void testIsGlobalAdmin_mailTarget_nullTarget_returnsFalse() {
        assertFalse(AccessControlUtil.isGlobalAdmin((MailTarget) null, true));
    }

    @Test
    public void testIsGlobalAdmin_mailTarget_nonAccountMailTarget_returnsFalse() {
        // MailTarget that is NOT an Account — should return false regardless
        MailTarget target = Mockito.mock(MailTarget.class);
        assertFalse(AccessControlUtil.isGlobalAdmin(target, true));
    }

    @Test
    public void testIsGlobalAdmin_mailTarget_regularAccountAsAdmin_returnsFalse() {
        assertFalse(AccessControlUtil.isGlobalAdmin(
                (MailTarget) new MockAccount("user@test.com"), true));
    }

    @Test
    public void testIsGlobalAdmin_mailTarget_adminAccountAsAdmin_returnsTrue() {
        assertTrue(AccessControlUtil.isGlobalAdmin(
                (MailTarget) new MockAdminAccount("admin@test.com"), true));
    }

    // ---------------------------------------------------------------
    // isGlobalAdmin(Account, boolean)
    // ---------------------------------------------------------------

    @Test
    public void testIsGlobalAdmin_accountBool_asAdminFalse_returnsFalse() {
        assertFalse(AccessControlUtil.isGlobalAdmin(new MockAdminAccount("admin@test.com"), false));
    }

    @Test
    public void testIsGlobalAdmin_accountBool_nullAsAdminTrue_returnsFalse() {
        assertFalse(AccessControlUtil.isGlobalAdmin((Account) null, true));
    }

    @Test
    public void testIsGlobalAdmin_accountBool_regularAsAdminTrue_returnsFalse() {
        assertFalse(AccessControlUtil.isGlobalAdmin(new MockAccount("user@test.com"), true));
    }

    @Test
    public void testIsGlobalAdmin_accountBool_adminAsAdminTrue_returnsTrue() {
        assertTrue(AccessControlUtil.isGlobalAdmin(new MockAdminAccount("admin@test.com"), true));
    }

    // ---------------------------------------------------------------
    // isDelegatedAdmin (package-private)
    // ---------------------------------------------------------------

    @Test
    public void testIsDelegatedAdmin_asAdminFalse_returnsFalse() {
        assertFalse(AccessControlUtil.isDelegatedAdmin(
                new MockDelegatedAdminAccount("da@test.com"), false));
    }

    @Test
    public void testIsDelegatedAdmin_nullAccount_returnsFalse() {
        assertFalse(AccessControlUtil.isDelegatedAdmin(null, true));
    }

    @Test
    public void testIsDelegatedAdmin_regularAccount_returnsFalse() {
        assertFalse(AccessControlUtil.isDelegatedAdmin(new MockAccount("user@test.com"), true));
    }

    @Test
    public void testIsDelegatedAdmin_delegatedAdmin_asAdminTrue_returnsTrue() {
        assertTrue(AccessControlUtil.isDelegatedAdmin(
                new MockDelegatedAdminAccount("da@test.com"), true));
    }

    // ---------------------------------------------------------------
    // authTokenToAccount
    // ---------------------------------------------------------------

    @Test
    public void testAuthTokenToAccount_nullToken_userRight_returnsAnonymous() {
        Account result = AccessControlUtil.authTokenToAccount(null, userRight());
        assertSame(GuestAccount.ANONYMOUS_ACCT, result);
    }

    @Test
    public void testAuthTokenToAccount_nullToken_adminRight_returnsNull() {
        Account result = AccessControlUtil.authTokenToAccount(null, adminRight());
        assertNull(result);
    }

    @Test
    public void testAuthTokenToAccount_zimbraUser_returnsAccount() throws ServiceException {
        Account expected = new MockAccount("zimbra@test.com");
        AuthToken token = Mockito.mock(AuthToken.class);
        Mockito.when(token.isZimbraUser()).thenReturn(true);
        Mockito.when(token.getAccount()).thenReturn(expected);

        Account result = AccessControlUtil.authTokenToAccount(token, userRight());
        assertSame(expected, result);
    }

    @Test
    public void testAuthTokenToAccount_zimbraUser_returnsAccountForAdminRight() throws ServiceException {
        // zimbra-user token returns the account regardless of the right type
        Account expected = new MockAccount("zimbra2@test.com");
        AuthToken token = Mockito.mock(AuthToken.class);
        Mockito.when(token.isZimbraUser()).thenReturn(true);
        Mockito.when(token.getAccount()).thenReturn(expected);

        Account result = AccessControlUtil.authTokenToAccount(token, adminRight());
        assertSame(expected, result);
    }

    @Test
    public void testAuthTokenToAccount_nonZimbraUser_userRight_returnsGuestAccount() {
        AuthToken token = Mockito.mock(AuthToken.class);
        Mockito.when(token.isZimbraUser()).thenReturn(false);
        Mockito.when(token.getExternalUserEmail()).thenReturn("guest@external.com");
        Mockito.when(token.getDigest()).thenReturn("digest123");
        Mockito.when(token.getAccessKey()).thenReturn(null);

        Account result = AccessControlUtil.authTokenToAccount(token, userRight());
        assertNotNull(result);
        assertTrue(result instanceof GuestAccount);
    }

    @Test
    public void testAuthTokenToAccount_nonZimbraUser_adminRight_returnsNull() {
        AuthToken token = Mockito.mock(AuthToken.class);
        Mockito.when(token.isZimbraUser()).thenReturn(false);

        Account result = AccessControlUtil.authTokenToAccount(token, adminRight());
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // emailAddrToAccount
    // ---------------------------------------------------------------

    @Test
    public void testEmailAddrToAccount_nullEmail_userRight_returnsAnonymous() {
        Account result = AccessControlUtil.emailAddrToAccount(null, userRight());
        assertSame(GuestAccount.ANONYMOUS_ACCT, result);
    }

    @Test
    public void testEmailAddrToAccount_nullEmail_adminRight_returnsNull() {
        Account result = AccessControlUtil.emailAddrToAccount(null, adminRight());
        assertNull(result);
    }

    @Test
    public void testEmailAddrToAccount_knownEmail_returnsRegisteredAccount() {
        Account result = AccessControlUtil.emailAddrToAccount(
                "known@accesscontrolutil.test", userRight());
        assertSame(knownAccount, result);
    }

    @Test
    public void testEmailAddrToAccount_unknownEmail_userRight_returnsGuestAccount() {
        Account result = AccessControlUtil.emailAddrToAccount(
                "nobody@unknown.example", userRight());
        assertNotNull(result);
        assertTrue(result instanceof GuestAccount);
        assertEquals("nobody@unknown.example", result.getName());
    }

    @Test
    public void testEmailAddrToAccount_unknownEmail_adminRight_returnsNull() {
        Account result = AccessControlUtil.emailAddrToAccount(
                "nobody@unknown.example", adminRight());
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // emailAddrToMailTarget
    // ---------------------------------------------------------------

    @Test
    public void testEmailAddrToMailTarget_nullEmail_userRight_returnsAnonymous() {
        MailTarget result = AccessControlUtil.emailAddrToMailTarget(null, userRight());
        assertSame(GuestAccount.ANONYMOUS_ACCT, result);
    }

    @Test
    public void testEmailAddrToMailTarget_nullEmail_adminRight_returnsNull() {
        MailTarget result = AccessControlUtil.emailAddrToMailTarget(null, adminRight());
        assertNull(result);
    }

    @Test
    public void testEmailAddrToMailTarget_knownEmail_returnsRegisteredAccount() {
        MailTarget result = AccessControlUtil.emailAddrToMailTarget(
                "known2@accesscontrolutil.test", userRight());
        assertSame(knownAccount2, result);
    }

    @Test
    public void testEmailAddrToMailTarget_unknownEmail_userRight_returnsGuestAccount() {
        MailTarget result = AccessControlUtil.emailAddrToMailTarget(
                "stranger@unknown.example", userRight());
        assertNotNull(result);
        assertTrue(result instanceof GuestAccount);
        assertEquals("stranger@unknown.example", result.getName());
    }

    @Test
    public void testEmailAddrToMailTarget_unknownEmail_adminRight_returnsNull() {
        MailTarget result = AccessControlUtil.emailAddrToMailTarget(
                "stranger@unknown.example", adminRight());
        assertNull(result);
    }
}
