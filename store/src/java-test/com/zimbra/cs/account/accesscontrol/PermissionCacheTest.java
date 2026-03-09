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

import com.zimbra.cs.account.Account;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PermissionCache}.
 *
 * Tests the package-private {@code CachedPermission} enum and
 * {@code buildCacheKey} static method without requiring LDAP.
 */
public class PermissionCacheTest {

    // ---------------------------------------------------------------
    // CachedPermission.getResult()
    // ---------------------------------------------------------------

    @Test
    public void testCachedPermission_ALLOWED_getResult_returnsTrue() {
        assertEquals(Boolean.TRUE, PermissionCache.CachedPermission.ALLOWED.getResult());
    }

    @Test
    public void testCachedPermission_DENIED_getResult_returnsFalse() {
        assertEquals(Boolean.FALSE, PermissionCache.CachedPermission.DENIED.getResult());
    }

    @Test
    public void testCachedPermission_NO_MATCHING_ACL_getResult_returnsNull() {
        assertNull(PermissionCache.CachedPermission.NO_MATCHING_ACL.getResult());
    }

    // ---------------------------------------------------------------
    // CachedPermission.getCacheMask()
    // ---------------------------------------------------------------

    @Test
    public void testCachedPermission_NOT_CACHED_getCacheMask_isZero() {
        assertEquals(0, PermissionCache.CachedPermission.NOT_CACHED.getCacheMask());
    }

    @Test
    public void testCachedPermission_distinctMasks() {
        // All non-NOT_CACHED values have non-zero masks that are distinguishable
        short allowedMask = PermissionCache.CachedPermission.ALLOWED.getCacheMask();
        short deniedMask  = PermissionCache.CachedPermission.DENIED.getCacheMask();
        short noAclMask   = PermissionCache.CachedPermission.NO_MATCHING_ACL.getCacheMask();

        assertNotEquals(allowedMask, deniedMask);
        assertNotEquals(allowedMask, noAclMask);
        assertNotEquals(deniedMask,  noAclMask);
    }

    // ---------------------------------------------------------------
    // CachedPermission enum completeness
    // ---------------------------------------------------------------

    @Test
    public void testCachedPermission_hasFourValues() {
        assertEquals(4, PermissionCache.CachedPermission.values().length);
    }

    // ---------------------------------------------------------------
    // buildCacheKey — non-cacheable right returns null
    // ---------------------------------------------------------------

    @Test
    public void testBuildCacheKey_nonCacheableRight_returnsNull() {
        UserRight right = new UserRight("testRight");
        // mCacheIndex defaults to NOT_CACHEABLE → isCacheable() = false
        assertFalse(right.isCacheable());
        MockAccount grantee = new MockAccount("user@example.com", "acct-id", false, false);
        assertNull(PermissionCache.buildCacheKey(grantee, right, false));
    }

    // ---------------------------------------------------------------
    // buildCacheKey — admin right without RightManager returns null
    // ---------------------------------------------------------------

    @Test
    public void testBuildCacheKey_cacheableAdminRight_withoutRightManager_returnsNull() throws Exception {
        // PresetRight is an admin right (isUserRight()=false).
        // Admin.R_adminLoginAs is null without RightManager.init().
        // Condition: !isUserRight() && R_adminLoginAs != right → null
        PresetRight adminRight = new PresetRight("adminRight");
        adminRight.setCacheable();

        MockAccount grantee = new MockAccount("admin@example.com", "admin-id", true, false);
        assertNull(PermissionCache.buildCacheKey(grantee, adminRight, false));
    }

    // ---------------------------------------------------------------
    // buildCacheKey — cacheable user right, various admin flags
    // ---------------------------------------------------------------

    @Test
    public void testBuildCacheKey_cacheableUserRight_regularAccount_returnsIdZeroZero() {
        UserRight right = new UserRight("loginAs");
        right.setCacheable();

        MockAccount grantee = new MockAccount("user@example.com", "test-id", false, false);
        String key = PermissionCache.buildCacheKey(grantee, right, false);

        assertNotNull(key);
        assertEquals("test-id00", key);
    }

    @Test
    public void testBuildCacheKey_cacheableUserRight_delegatedAdmin_returnsIdOneZero() {
        UserRight right = new UserRight("loginAs");
        right.setCacheable();

        MockAccount grantee = new MockAccount("dadmin@example.com", "dadmin-id", false, true);
        String key = PermissionCache.buildCacheKey(grantee, right, false);

        assertNotNull(key);
        assertEquals("dadmin-id10", key);
    }

    @Test
    public void testBuildCacheKey_cacheableUserRight_globalAdmin_returnsIdTwoZero() {
        UserRight right = new UserRight("loginAs");
        right.setCacheable();

        MockAccount grantee = new MockAccount("gadmin@example.com", "gadmin-id", true, false);
        String key = PermissionCache.buildCacheKey(grantee, right, false);

        assertNotNull(key);
        assertEquals("gadmin-id20", key);
    }

    @Test
    public void testBuildCacheKey_canDelegateNeededTrue_appendsOne() {
        UserRight right = new UserRight("loginAs");
        right.setCacheable();

        MockAccount grantee = new MockAccount("user@example.com", "test-id", false, false);
        String key = PermissionCache.buildCacheKey(grantee, right, true);

        assertNotNull(key);
        assertEquals("test-id01", key);
    }

    @Test
    public void testBuildCacheKey_canDelegateNeededFalse_appendsZero() {
        UserRight right = new UserRight("loginAs");
        right.setCacheable();

        MockAccount grantee = new MockAccount("user@example.com", "test-id", false, false);
        String key = PermissionCache.buildCacheKey(grantee, right, false);

        assertNotNull(key);
        assertTrue(key.endsWith("0"));
    }

    // ---------------------------------------------------------------
    // Inner MockAccount
    // ---------------------------------------------------------------

    private static class MockAccount extends Account {
        private final boolean globalAdmin;
        private final boolean delegatedAdmin;

        MockAccount(String name, String id, boolean globalAdmin, boolean delegatedAdmin) {
            super(name, id, new HashMap<String, Object>(), new HashMap<String, Object>(), null);
            this.globalAdmin = globalAdmin;
            this.delegatedAdmin = delegatedAdmin;
        }

        @Override
        public boolean isIsAdminAccount() {
            return globalAdmin;
        }

        @Override
        public boolean isIsDelegatedAdminAccount() {
            return delegatedAdmin;
        }
    }
}
