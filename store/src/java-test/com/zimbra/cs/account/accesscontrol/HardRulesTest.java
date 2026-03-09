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
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.MailTarget;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link HardRules} and its inner enum {@link HardRules.HardRule}.
 */
public class HardRulesTest {

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

    private static class MockAdminAccount extends MockAccount {
        MockAdminAccount(String name) { super(name); }
        @Override public boolean isIsAdminAccount() { return true; }
    }

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
    // isForbiddenAttr
    // ---------------------------------------------------------------

    @Test
    public void testIsForbiddenAttr_zimbraIsAdminAccount_returnsTrue() {
        assertTrue(HardRules.isForbiddenAttr("zimbraIsAdminAccount"));
    }

    @Test
    public void testIsForbiddenAttr_uppercaseVariant_returnsTrue() {
        // check is case-insensitive
        assertTrue(HardRules.isForbiddenAttr("ZIMBRAISADMINACCOUNT"));
    }

    @Test
    public void testIsForbiddenAttr_mixedCase_returnsTrue() {
        assertTrue(HardRules.isForbiddenAttr("ZimbraIsAdminAccount"));
    }

    @Test
    public void testIsForbiddenAttr_regularAttr_returnsFalse() {
        assertFalse(HardRules.isForbiddenAttr("zimbraMailAlias"));
    }

    @Test
    public void testIsForbiddenAttr_emptyString_returnsFalse() {
        assertFalse(HardRules.isForbiddenAttr(""));
    }

    // ---------------------------------------------------------------
    // checkForbiddenAttr
    // ---------------------------------------------------------------

    @Test(expected = ServiceException.class)
    public void testCheckForbiddenAttr_forbiddenAttr_throwsServiceException() throws ServiceException {
        HardRules.checkForbiddenAttr("zimbraIsAdminAccount");
    }

    @Test
    public void testCheckForbiddenAttr_allowedAttr_noException() throws ServiceException {
        HardRules.checkForbiddenAttr("zimbraMailAlias"); // must not throw
    }

    @Test(expected = ServiceException.class)
    public void testCheckForbiddenAttr_forbiddenAttr_uppercase_throwsServiceException() throws ServiceException {
        HardRules.checkForbiddenAttr("ZIMBRAISADMINACCOUNT");
    }

    // ---------------------------------------------------------------
    // checkHardRules — global admin → TRUE
    // ---------------------------------------------------------------

    @Test
    public void testCheckHardRules_globalAdmin_asAdminTrue_returnsTrue() throws ServiceException {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        Boolean result = HardRules.checkHardRules(admin, true, null, adminRight());
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testCheckHardRules_globalAdmin_asAdminTrue_userRight_returnsTrue() throws ServiceException {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        Boolean result = HardRules.checkHardRules(admin, true, null, userRight());
        assertEquals(Boolean.TRUE, result);
    }

    // ---------------------------------------------------------------
    // checkHardRules — user right → null (hard rules not applicable)
    // ---------------------------------------------------------------

    @Test
    public void testCheckHardRules_regularAccount_userRight_returnsNull() throws ServiceException {
        MockAccount regular = new MockAccount("user@test.com");
        Boolean result = HardRules.checkHardRules(regular, true, null, userRight());
        assertNull(result);
    }

    @Test
    public void testCheckHardRules_nullRight_returnsNull() throws ServiceException {
        // right == null is treated as admin right (isAdminRight = true), but account is not admin
        // this case: regular account + null right → PERM_DENIED
        MockAccount regular = new MockAccount("user@test.com");
        try {
            HardRules.checkHardRules(regular, true, null, null);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    // ---------------------------------------------------------------
    // checkHardRules — admin right, non-delegated-admin → PERM_DENIED
    // ---------------------------------------------------------------

    @Test
    public void testCheckHardRules_regularAccount_adminRight_throwsPERMDENIED() throws ServiceException {
        MockAccount regular = new MockAccount("user@test.com");
        try {
            HardRules.checkHardRules(regular, true, null, adminRight());
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            // The exception should encode NOT_EFFECTIVE_DELEGATED_ADMIN_ACCOUNT
            HardRules.HardRule rule = HardRules.HardRule.ruleVolated(e);
            assertEquals(HardRules.HardRule.NOT_EFFECTIVE_DELEGATED_ADMIN_ACCOUNT, rule);
        }
    }

    // ---------------------------------------------------------------
    // checkHardRules — non-Account MailTarget → PERM_DENIED
    // ---------------------------------------------------------------

    @Test
    public void testCheckHardRules_nonAccountMailTarget_adminRight_throwsPERMDENIED() throws ServiceException {
        MailTarget nonAccountGrantee = Mockito.mock(MailTarget.class);
        try {
            HardRules.checkHardRules(nonAccountGrantee, true, null, adminRight());
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            HardRules.HardRule rule = HardRules.HardRule.ruleVolated(e);
            assertEquals(HardRules.HardRule.NOT_EFFECTIVE_DELEGATED_ADMIN_ACCOUNT, rule);
        }
    }

    // ---------------------------------------------------------------
    // checkHardRules — delegated admin + non-admin target → null
    // ---------------------------------------------------------------

    @Test
    public void testCheckHardRules_delegatedAdmin_regularTarget_returnsNull() throws ServiceException {
        MockDelegatedAdminAccount da = new MockDelegatedAdminAccount("da@test.com");
        MockAccount regularTarget = new MockAccount("target@test.com");
        Boolean result = HardRules.checkHardRules(da, true, regularTarget, adminRight());
        assertNull(result);
    }

    // ---------------------------------------------------------------
    // checkHardRules — delegated admin + global-admin target → PERM_DENIED
    // ---------------------------------------------------------------

    @Test
    public void testCheckHardRules_delegatedAdmin_globalAdminTarget_throwsPERMDENIED() throws ServiceException {
        MockDelegatedAdminAccount da = new MockDelegatedAdminAccount("da@test.com");
        MockAdminAccount adminTarget = new MockAdminAccount("admin@test.com");
        try {
            HardRules.checkHardRules(da, true, adminTarget, adminRight());
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            HardRules.HardRule rule = HardRules.HardRule.ruleVolated(e);
            assertEquals(HardRules.HardRule.DELEGATED_ADMIN_CANNOT_ACCESS_GLOBAL_ADMIN, rule);
        }
    }

    // ---------------------------------------------------------------
    // checkHardRules — asAdmin=false (no admin check at all)
    // ---------------------------------------------------------------

    @Test
    public void testCheckHardRules_globalAdmin_asAdminFalse_adminRight_returnsNull() throws ServiceException {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        // asAdmin=false → isGlobalAdmin returns false → falls through to admin-right check
        // → isDelegatedAdmin also false (asAdmin=false) → PERM_DENIED
        try {
            HardRules.checkHardRules(admin, false, null, adminRight());
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    // ---------------------------------------------------------------
    // HardRule.ruleViolated
    // ---------------------------------------------------------------

    @Test
    public void testRuleViolated_withEncodedRule_returnsCorrectRule() throws ServiceException {
        MockAccount regular = new MockAccount("user@test.com");
        try {
            HardRules.checkHardRules(regular, true, null, adminRight());
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            HardRules.HardRule rule = HardRules.HardRule.ruleVolated(e);
            assertNotNull(rule);
            assertEquals(HardRules.HardRule.NOT_EFFECTIVE_DELEGATED_ADMIN_ACCOUNT, rule);
        }
    }

    @Test
    public void testRuleViolated_nonPermDeniedException_returnsNull() throws ServiceException {
        ServiceException e = ServiceException.FAILURE("some failure", null);
        HardRules.HardRule rule = HardRules.HardRule.ruleVolated(e);
        assertNull(rule);
    }

    @Test
    public void testRuleViolated_permDeniedWithoutArgs_returnsNull() throws ServiceException {
        ServiceException e = ServiceException.PERM_DENIED("no rule embedded");
        HardRules.HardRule rule = HardRules.HardRule.ruleVolated(e);
        assertNull(rule);
    }

    @Test
    public void testRuleViolated_delegatedAdminCannotAccessGlobalAdmin() throws ServiceException {
        MockDelegatedAdminAccount da = new MockDelegatedAdminAccount("da@test.com");
        MockAdminAccount adminTarget = new MockAdminAccount("admin@test.com");
        try {
            HardRules.checkHardRules(da, true, adminTarget, adminRight());
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            HardRules.HardRule rule = HardRules.HardRule.ruleVolated(e);
            assertEquals(HardRules.HardRule.DELEGATED_ADMIN_CANNOT_ACCESS_GLOBAL_ADMIN, rule);
        }
    }

    // ---------------------------------------------------------------
    // HardRule enum — values count
    // ---------------------------------------------------------------

    @Test
    public void testHardRule_twoValues() {
        assertEquals(2, HardRules.HardRule.values().length);
    }
}
