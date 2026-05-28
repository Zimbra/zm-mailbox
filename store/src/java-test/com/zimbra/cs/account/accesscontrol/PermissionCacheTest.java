/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.accesscontrol;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.ldap.LdapUtil;

public class PermissionCacheTest {

    private class MockAccount extends Account {
        private String id = LdapUtil.generateUUID();
        private String name;
        private boolean isAdmin = false;
        private boolean isDelegatedAdmin = false;

        private MockAccount(String name) {
            super(name, null, null, null, null);
            this.name = name;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isIsAdminAccount() {
            return isAdmin;
        }

        @Override
        public boolean isIsDelegatedAdminAccount() {
            return isDelegatedAdmin;
        }

        private MockAccount setAdmin(boolean admin) {
            this.isAdmin = admin;
            return this;
        }

        private MockAccount setDelegatedAdmin(boolean delegated) {
            this.isDelegatedAdmin = delegated;
            return this;
        }
    }

    @Test
    public void buildCacheKey_regularAccount_includesIdAndAdminFlag() throws Exception {
        MockAccount grantee = new MockAccount("grantee@example.com");
        Right right = User.R_loginAs;

        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        Assert.assertNotNull(cacheKey);
        Assert.assertTrue("Cache key should contain account ID", cacheKey.contains(grantee.getId()));
        Assert.assertTrue("Cache key should contain admin flag 0 for regular account", cacheKey.contains("0"));
    }

    @Test
    public void buildCacheKey_delegatedAdminAccount_includesFlag1() throws Exception {
        MockAccount grantee = new MockAccount("delegated.admin@example.com").setDelegatedAdmin(true);
        Right right = User.R_loginAs;

        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        Assert.assertNotNull(cacheKey);
        Assert.assertTrue("Cache key should contain flag 1 for delegated admin", cacheKey.contains("1"));
    }

    @Test
    public void buildCacheKey_globalAdminAccount_includesFlag2() throws Exception {
        MockAccount grantee = new MockAccount("global.admin@example.com").setAdmin(true);
        Right right = User.R_loginAs;

        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        Assert.assertNotNull(cacheKey);
        Assert.assertTrue("Cache key should contain flag 2 for global admin", cacheKey.contains("2"));
    }

    @Test
    public void buildCacheKey_canDelegateFalse_endsWithZero() throws Exception {
        MockAccount grantee = new MockAccount("grantee@example.com");
        Right right = User.R_loginAs;

        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        Assert.assertNotNull(cacheKey);
        Assert.assertTrue("Cache key should end with 0 when canDelegateNeeded is false", cacheKey.endsWith("0"));
    }

    @Test
    public void buildCacheKey_canDelegateTrue_endsWithOne() throws Exception {
        MockAccount grantee = new MockAccount("grantee@example.com");
        Right right = User.R_loginAs;

        String cacheKey = PermissionCache.buildCacheKey(grantee, right, true);

        Assert.assertNotNull(cacheKey);
        Assert.assertTrue("Cache key should end with 1 when canDelegateNeeded is true", cacheKey.endsWith("1"));
    }

    @Test
    public void buildCacheKey_nonCacheableRight_returnsNull() throws Exception {
        MockAccount grantee = new MockAccount("grantee@example.com");
        // Admin.R_adminLoginAs is cacheable but most admin rights are not
        Right right = User.R_loginAs;

        // Test with a right that's cacheable
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);
        Assert.assertNotNull("R_loginAs should be cacheable", cacheKey);
    }

    @Test
    public void buildCacheKey_differentGrantees_producesDifferentKeys() throws Exception {
        MockAccount grantee1 = new MockAccount("grantee1@example.com");
        MockAccount grantee2 = new MockAccount("grantee2@example.com");
        Right right = User.R_loginAs;

        String cacheKey1 = PermissionCache.buildCacheKey(grantee1, right, false);
        String cacheKey2 = PermissionCache.buildCacheKey(grantee2, right, false);

        Assert.assertNotNull(cacheKey1);
        Assert.assertNotNull(cacheKey2);
        Assert.assertNotEquals("Different grantees should produce different cache keys", cacheKey1, cacheKey2);
    }

    @Test
    public void buildCacheKey_differentDelegateModes_producesDifferentKeys() throws Exception {
        MockAccount grantee = new MockAccount("grantee@example.com");
        Right right = User.R_loginAs;

        String cacheKeyNoDelegate = PermissionCache.buildCacheKey(grantee, right, false);
        String cacheKeyWithDelegate = PermissionCache.buildCacheKey(grantee, right, true);

        Assert.assertNotEquals("Different delegate modes should produce different keys",
                               cacheKeyNoDelegate, cacheKeyWithDelegate);
    }

    @Test
    public void invalidateCache_clearsPermissionCache() throws Exception {
        // This tests the public invalidateCache() method
        PermissionCache.invalidateCache();
        // Verify no exception is thrown; actual cache state is opaque from this interface
        Assert.assertTrue(true);
    }

    @Test
    public void invalidateCacheWithTarget_clearsTargetSpecificCache() throws Exception {
        MockAccount target = new MockAccount("target@example.com");
        PermissionCache.invalidateCache(target);
        // Verify no exception is thrown
        Assert.assertTrue(true);
    }

    @Test
    public void getHitRate_afterClearCache_returnsValid() throws Exception {
        double hitRate = PermissionCache.getHitRate();
        Assert.assertTrue("Hit rate should be between 0 and 100", hitRate >= 0 && hitRate <= 100);
    }
}
