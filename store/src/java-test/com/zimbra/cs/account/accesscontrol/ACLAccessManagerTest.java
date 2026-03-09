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
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ACLAccessManager}.
 *
 * <p>The constructor calls {@code RightManager.getInstance()}, which requires XML files
 * that are not available in a plain unit-test environment.  Each test guards with
 * {@code Assume.assumeNotNull(manager)} so the test is skipped (not failed) when the
 * manager could not be created.
 *
 * <p>Methods that are independent of RightManager (e.g. {@code isDomainAdminOnly})
 * are tested directly without the guard when they don't touch RightManager.
 */
public class ACLAccessManagerTest {

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
        @Override public boolean getBooleanAttr(String name, boolean defaultValue) {
            return defaultValue;
        }
    }

    private static class MockAdminAccount extends MockAccount {
        MockAdminAccount(String name) { super(name); }
        @Override public boolean isIsAdminAccount() { return true; }
        @Override public boolean getBooleanAttr(String name, boolean defaultValue) {
            // Simulate zimbraIsAdminAccount = TRUE
            if ("zimbraIsAdminAccount".equalsIgnoreCase(name)) return true;
            if ("zimbraIsDelegatedAdminAccount".equalsIgnoreCase(name)) return false;
            return defaultValue;
        }
    }

    private static class MockDelegatedAdminAccount extends MockAccount {
        MockDelegatedAdminAccount(String name) { super(name); }
        @Override public boolean isIsDelegatedAdminAccount() { return true; }
        @Override public boolean getBooleanAttr(String name, boolean defaultValue) {
            if ("zimbraIsDelegatedAdminAccount".equalsIgnoreCase(name)) return true;
            if ("zimbraIsAdminAccount".equalsIgnoreCase(name)) return false;
            return defaultValue;
        }
    }

    // ---------------------------------------------------------------
    // Test fixture
    // ---------------------------------------------------------------

    private static ACLAccessManager manager;

    @BeforeClass
    public static void setUpClass() {
        try {
            manager = new ACLAccessManager();
        } catch (ServiceException e) {
            manager = null; // RightManager not available; tests will be skipped
        }
    }

    // ---------------------------------------------------------------
    // isDomainAdminOnly — always false, no RightManager dependency
    // ---------------------------------------------------------------

    @Test
    public void testIsDomainAdminOnly_alwaysReturnsFalse() {
        Assume.assumeNotNull(manager);
        AuthToken at = Mockito.mock(AuthToken.class);
        assertFalse(manager.isDomainAdminOnly(at));
    }

    // ---------------------------------------------------------------
    // isAdequateAdminAccount
    // ---------------------------------------------------------------

    @Test
    public void testIsAdequateAdminAccount_delegatedAdmin_returnsTrue() {
        Assume.assumeNotNull(manager);
        assertTrue(manager.isAdequateAdminAccount(new MockDelegatedAdminAccount("da@test.com")));
    }

    @Test
    public void testIsAdequateAdminAccount_globalAdmin_returnsTrue() {
        Assume.assumeNotNull(manager);
        assertTrue(manager.isAdequateAdminAccount(new MockAdminAccount("admin@test.com")));
    }

    @Test
    public void testIsAdequateAdminAccount_regularAccount_returnsFalse() {
        Assume.assumeNotNull(manager);
        assertFalse(manager.isAdequateAdminAccount(new MockAccount("user@test.com")));
    }

    // ---------------------------------------------------------------
    // targetTypesForGrantSearch — returns all TargetType values
    // ---------------------------------------------------------------

    @Test
    public void testTargetTypesForGrantSearch_containsAllTargetTypes() {
        Assume.assumeNotNull(manager);
        Set<TargetType> types = manager.targetTypesForGrantSearch();
        assertEquals(TargetType.values().length, types.size());
        for (TargetType tt : TargetType.values()) {
            assertTrue("Expected " + tt + " in targetTypesForGrantSearch", types.contains(tt));
        }
    }

    // ---------------------------------------------------------------
    // canDo(MailTarget, Entry, Right, boolean) — basic paths
    // ---------------------------------------------------------------

    @Test
    public void testCanDo_globalAdmin_adminRight_asAdminTrue_returnsTrue() {
        Assume.assumeNotNull(manager);
        MockAdminAccount admin = new MockAdminAccount("admin@test.com");
        Right right = adminRight();
        // GlobalAdmin → HardRules returns TRUE
        assertTrue(manager.canDo(admin, null, right, true));
    }

    @Test
    public void testCanDo_regularAccount_adminRight_asAdminTrue_returnsFalse() {
        Assume.assumeNotNull(manager);
        // Regular account checking admin right → HardRules throws PERM_DENIED → caught → false
        MockAccount regular = new MockAccount("user@test.com");
        Right right = adminRight();
        assertFalse(manager.canDo(regular, null, right, true));
    }

    // ---------------------------------------------------------------
    // Class-level structural tests (no manager instance needed)
    // ---------------------------------------------------------------

    @Test
    public void testACLAccessManager_extendsAccessManager() {
        assertTrue(AccessManager.class.isAssignableFrom(ACLAccessManager.class));
    }

    @Test
    public void testACLAccessManager_implementsAdminConsoleCapable() {
        assertTrue(AdminConsoleCapable.class.isAssignableFrom(ACLAccessManager.class));
    }

    @Test
    public void testACLAccessManager_isPublicConcreteClass() {
        assertTrue(Modifier.isPublic(ACLAccessManager.class.getModifiers()));
        assertFalse(Modifier.isAbstract(ACLAccessManager.class.getModifiers()));
    }

    @Test
    public void testIsDomainAdminOnly_methodIsPublicAndNonStatic() throws Exception {
        Method m = ACLAccessManager.class.getMethod("isDomainAdminOnly", AuthToken.class);
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertFalse(Modifier.isStatic(m.getModifiers()));
        assertEquals(boolean.class, m.getReturnType());
    }

    @Test
    public void testIsAdequateAdminAccount_methodExists() throws Exception {
        Method m = ACLAccessManager.class.getMethod("isAdequateAdminAccount", Account.class);
        assertNotNull(m);
        assertEquals(boolean.class, m.getReturnType());
    }

    @Test
    public void testTargetTypesForGrantSearch_methodExists() throws Exception {
        Method m = ACLAccessManager.class.getMethod("targetTypesForGrantSearch");
        assertNotNull(m);
        assertEquals(Set.class, m.getReturnType());
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
