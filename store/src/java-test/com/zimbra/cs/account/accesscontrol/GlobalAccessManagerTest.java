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
import com.zimbra.cs.account.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GlobalAccessManager}.
 *
 * GlobalAccessManager's constructor catches ServiceException from ACLAccessManager,
 * so it always succeeds even without RightManager XML files in the test environment.
 */
public class GlobalAccessManagerTest {

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

    // ---------------------------------------------------------------
    // Test fixture
    // ---------------------------------------------------------------

    private static GlobalAccessManager manager;

    @BeforeClass
    public static void setUpClass() {
        // Constructor catches ServiceException internally — always succeeds
        manager = new GlobalAccessManager();
    }

    // ---------------------------------------------------------------
    // Constructor always succeeds
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_alwaysSucceeds() {
        assertNotNull(manager);
    }

    // ---------------------------------------------------------------
    // isDomainAdminOnly
    // ---------------------------------------------------------------

    @Test
    public void testIsDomainAdminOnly_alwaysReturnsFalse() {
        AuthToken at = Mockito.mock(AuthToken.class);
        assertFalse(manager.isDomainAdminOnly(at));
    }

    // ---------------------------------------------------------------
    // targetTypesForGrantSearch
    // ---------------------------------------------------------------

    @Test
    public void testTargetTypesForGrantSearch_containsAccount() {
        Set<TargetType> types = manager.targetTypesForGrantSearch();
        assertTrue(types.contains(TargetType.account));
    }

    @Test
    public void testTargetTypesForGrantSearch_containsCalresource() {
        Set<TargetType> types = manager.targetTypesForGrantSearch();
        assertTrue(types.contains(TargetType.calresource));
    }

    @Test
    public void testTargetTypesForGrantSearch_containsDl() {
        Set<TargetType> types = manager.targetTypesForGrantSearch();
        assertTrue(types.contains(TargetType.dl));
    }

    @Test
    public void testTargetTypesForGrantSearch_containsGroup() {
        Set<TargetType> types = manager.targetTypesForGrantSearch();
        assertTrue(types.contains(TargetType.group));
    }

    @Test
    public void testTargetTypesForGrantSearch_exactlyFourTypes() {
        Set<TargetType> types = manager.targetTypesForGrantSearch();
        assertEquals(4, types.size());
    }

    // ---------------------------------------------------------------
    // canDo(MailTarget, Entry, Right, boolean) — admin right
    // ---------------------------------------------------------------

    @Test
    public void testCanDo_globalAdmin_adminRight_asAdminTrue_returnsTrue() {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        Right right = adminRight();
        assertTrue(manager.canDo((MailTarget) admin, (Entry) null, right, true));
    }

    @Test
    public void testCanDo_regularAccount_adminRight_asAdminTrue_returnsFalse() {
        MockAccount regular = new MockAccount("user@test.com");
        Right right = adminRight();
        assertFalse(manager.canDo((MailTarget) regular, (Entry) null, right, true));
    }

    @Test
    public void testCanDo_globalAdmin_adminRight_asAdminFalse_returnsFalse() {
        // asAdmin=false → isGlobalAdmin always false
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        Right right = adminRight();
        assertFalse(manager.canDo((MailTarget) admin, (Entry) null, right, false));
    }

    // ---------------------------------------------------------------
    // canDo(MailTarget, Entry, Right, boolean) — user right
    // ---------------------------------------------------------------

    @Test
    public void testCanDo_userRight_mAclAccessManagerNull_returnsFalse() {
        // mAclAccessManager is null (ACLAccessManager construction failed without RightManager)
        // → returns false for user rights
        MockAccount regular = new MockAccount("user@test.com");
        Right right = userRight();
        // result is false either because mAclAccessManager is null or because ACL check fails
        boolean result = manager.canDo((MailTarget) regular, (Entry) null, right, true);
        assertFalse(result);
    }

    @Test
    public void testCanDo_nullRight_adminAccount_asAdminTrue_returnsTrue() {
        // rightNeeded == null → isUserRight() won't be called → falls to isGlobalAdmin check
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        assertTrue(manager.canDo((MailTarget) admin, (Entry) null, null, true));
    }

    // ---------------------------------------------------------------
    // getGetAttrsChecker(Account, Entry, boolean)
    // ---------------------------------------------------------------

    @Test
    public void testGetGetAttrsChecker_globalAdmin_asAdminTrue_returnsAllowAll() throws ServiceException {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        AccessManager.AttrRightChecker checker = manager.getGetAttrsChecker(admin, null, true);
        assertNotNull(checker);
        // AllowedAttrs.ALLOW_ALL_ATTRS() allows any attr
        assertTrue(checker.allowAttr("anyAttribute"));
    }

    @Test
    public void testGetGetAttrsChecker_regularAccount_asAdminTrue_returnsDenyAll() throws ServiceException {
        MockAccount regular = new MockAccount("user@test.com");
        AccessManager.AttrRightChecker checker = manager.getGetAttrsChecker(regular, null, true);
        assertNotNull(checker);
        // AllowedAttrs.DENY_ALL_ATTRS() denies any attr
        assertFalse(checker.allowAttr("anyAttribute"));
    }

    @Test
    public void testGetGetAttrsChecker_globalAdmin_asAdminFalse_returnsDenyAll() throws ServiceException {
        // asAdmin=false → isGlobalAdmin returns false → DENY_ALL_ATTRS
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        AccessManager.AttrRightChecker checker = manager.getGetAttrsChecker(admin, null, false);
        assertNotNull(checker);
        assertFalse(checker.allowAttr("anyAttribute"));
    }

    // ---------------------------------------------------------------
    // isAdequateAdminAccount — only checks zimbraIsAdminAccount (not delegated)
    // ---------------------------------------------------------------

    @Test
    public void testIsAdequateAdminAccount_globalAdmin_returnsTrue() {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        assertTrue(manager.isAdequateAdminAccount(admin));
    }

    @Test
    public void testIsAdequateAdminAccount_regularAccount_returnsFalse() {
        MockAccount regular = new MockAccount("user@test.com");
        assertFalse(manager.isAdequateAdminAccount(regular));
    }

    // ---------------------------------------------------------------
    // canGetAttrs(Account, Entry, Set<String>, boolean)
    // ---------------------------------------------------------------

    @Test
    public void testCanGetAttrs_globalAdmin_asAdminTrue_returnsTrue() throws ServiceException {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        assertTrue(manager.canGetAttrs(admin, null, new HashSet<String>(), true));
    }

    @Test
    public void testCanGetAttrs_regularAccount_asAdminTrue_returnsFalse() throws ServiceException {
        MockAccount regular = new MockAccount("user@test.com");
        assertFalse(manager.canGetAttrs(regular, null, new HashSet<String>(), true));
    }

    @Test
    public void testCanGetAttrs_globalAdmin_asAdminFalse_returnsFalse() throws ServiceException {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        assertFalse(manager.canGetAttrs(admin, null, new HashSet<String>(), false));
    }

    // ---------------------------------------------------------------
    // canSetAttrs(Account, Entry, Set<String>, boolean)
    // ---------------------------------------------------------------

    @Test
    public void testCanSetAttrs_withSet_globalAdmin_asAdminTrue_returnsTrue() throws ServiceException {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        assertTrue(manager.canSetAttrs(admin, null, new HashSet<String>(), true));
    }

    @Test
    public void testCanSetAttrs_withSet_regularAccount_returnsFalse() throws ServiceException {
        MockAccount regular = new MockAccount("user@test.com");
        assertFalse(manager.canSetAttrs(regular, null, new HashSet<String>(), true));
    }

    // ---------------------------------------------------------------
    // canSetAttrs(Account, Entry, Map<String, Object>, boolean)
    // ---------------------------------------------------------------

    @Test
    public void testCanSetAttrs_withMap_globalAdmin_asAdminTrue_returnsTrue() throws ServiceException {
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        assertTrue(manager.canSetAttrs(admin, null, new HashMap<String, Object>(), true));
    }

    @Test
    public void testCanSetAttrs_withMap_regularAccount_returnsFalse() throws ServiceException {
        MockAccount regular = new MockAccount("user@test.com");
        assertFalse(manager.canSetAttrs(regular, null, new HashMap<String, Object>(), true));
    }

    // ---------------------------------------------------------------
    // Helper right factories
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
}
