/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.PermCacheManager.CachedPerms;
import com.zimbra.cs.account.accesscontrol.PermissionCache.CachedPermission;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link PermissionCache}: the cache-key derivation contract (grantee
 * identifier + admin flag + can-delegate flag, including GuestAccount digest/access-key keys),
 * the cacheability gate, and the put/get round-trip through the real PermCacheManager.
 */
public class PermissionCacheTest {

    private static Right cacheableUserRight;

    private static Right nonCacheableRight;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        // Initialize the right registry so user rights (and cacheability flags) are populated.
        RightManager rm = RightManager.getInstance();
        for (Right r : rm.getAllUserRights().values()) {
            if (r.isCacheable()) {
                cacheableUserRight = r;
            } else if (nonCacheableRight == null) {
                nonCacheableRight = r;
            }
        }
        assertNotNull("expected at least one cacheable user right", cacheableUserRight);
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String name, boolean admin, boolean delegatedAdmin) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        if (admin) {
            attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        }
        if (delegatedAdmin) {
            attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        }
        return prov.createAccount(name, "pw", attrs);
    }

    @Test
    public void buildCacheKeyRegularUserAccountEndsWithUserAndNoDelegateFlags() throws Exception {
        // Arrange
        Account grantee = createAccount("cachekey-user@example.com", false, false);

        // Act
        String key = PermissionCache.buildCacheKey(grantee, cacheableUserRight, false);

        // Assert — <accountId><adminFlag '0'><canDelegate '0'>
        assertNotNull(key);
        assertEquals(grantee.getId() + "00", key);

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void buildCacheKeyAdminAccountUsesGlobalAdminFlag2() throws Exception {
        // Arrange
        Account grantee = createAccount("cachekey-admin@example.com", true, false);

        // Act
        String key = PermissionCache.buildCacheKey(grantee, cacheableUserRight, false);

        // Assert — global admin flag is '2'
        assertEquals(grantee.getId() + "20", key);

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void buildCacheKeyCanDelegateNeededSetsTrailingFlagOne() throws Exception {
        // Arrange
        Account grantee = createAccount("cachekey-deleg@example.com", false, false);

        // Act
        String key = PermissionCache.buildCacheKey(grantee, cacheableUserRight, true);

        // Assert — trailing can-delegate flag is '1'
        assertEquals(grantee.getId() + "01", key);

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void buildCacheKeyNonCacheableRightReturnsNull() throws Exception {
        // Arrange
        Account grantee = createAccount("cachekey-nc@example.com", false, false);

        // Act / Assert — uncacheable rights are never keyed
        if (nonCacheableRight != null) {
            assertNull(PermissionCache.buildCacheKey(grantee, nonCacheableRight, false));
        }

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void buildCacheKeyGuestWithDigestAppendsGSuffix() throws Exception {
        // Arrange — GuestAccount with a username/password yields a digest-based key ending in 'G'
        GuestAccount guest = new GuestAccount("guest@elsewhere.com", "secretpw");

        // Act
        String key = PermissionCache.buildCacheKey(guest, cacheableUserRight, false);

        // Assert
        assertNotNull("guest with digest must be cacheable", guest.getDigest());
        assertNotNull(key);
        assertTrue("guest key must embed digest and 'G' suffix", key.contains("G"));
        // adminFlag is '0' for non-Account grantees, canDelegate '0'
        assertTrue(key.endsWith("00"));
    }

    @Test
    public void cachePutThenGetAllowedRoundTripsAllowed() throws Exception {
        // Arrange
        Account grantee = createAccount("cacheflow@example.com", false, false);
        Account target = createAccount("cachetarget@example.com", false, false);

        // Act — put ALLOWED, then read it back
        PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.TRUE);
        CachedPermission got = PermissionCache.cacheGet(grantee, target, cacheableUserRight, false);

        // Assert
        assertEquals(CachedPermission.ALLOWED, got);

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void cachePutThenGetDeniedRoundTripsDenied() throws Exception {
        // Arrange
        Account grantee = createAccount("cacheflow2@example.com", false, false);
        Account target = createAccount("cachetarget2@example.com", false, false);

        // Act
        PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.FALSE);
        CachedPermission got = PermissionCache.cacheGet(grantee, target, cacheableUserRight, false);

        // Assert
        assertEquals(CachedPermission.DENIED, got);

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void cacheGetNeverPutReturnsNotCached() throws Exception {
        // Arrange — fresh grantee/target with nothing cached
        Account grantee = createAccount("cachemiss@example.com", false, false);
        Account target = createAccount("cachemisstarget@example.com", false, false);
        PermissionCache.invalidateCache(target);

        // Act
        CachedPermission got = PermissionCache.cacheGet(grantee, target, cacheableUserRight, false);

        // Assert
        assertEquals(CachedPermission.NOT_CACHED, got);

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void cachePutNullAllowedThenGetReturnsNoMatchingAcl() throws Exception {
        // Arrange
        Account grantee = createAccount("cachenull@example.com", false, false);
        Account target = createAccount("cachenulltarget@example.com", false, false);

        // Act — null "allowed" maps to NO_MATCHING_ACL
        PermissionCache.cachePut(grantee, target, cacheableUserRight, false, null);
        CachedPermission got = PermissionCache.cacheGet(grantee, target, cacheableUserRight, false);

        // Assert
        assertEquals(CachedPermission.NO_MATCHING_ACL, got);

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void invalidateCacheTargetAfterPutEvictsEntry() throws Exception {
        // Arrange — cache an entry, then invalidate that target
        Account grantee = createAccount("cacheinval@example.com", false, false);
        Account target = createAccount("cacheinvaltarget@example.com", false, false);
        PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.TRUE);
        assertEquals(CachedPermission.ALLOWED,
                PermissionCache.cacheGet(grantee, target, cacheableUserRight, false));

        // Act
        PermissionCache.invalidateCache(target);

        // Assert — entry gone
        assertEquals(CachedPermission.NOT_CACHED,
                PermissionCache.cacheGet(grantee, target, cacheableUserRight, false));

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void getHitRateReturnsNonNegativeRate() {
        // Act — hit rate is a percentage; just assert it is within a sane range
        double rate = PermissionCache.getHitRate();

        // Assert
        assertTrue("hit rate must be >= 0", rate >= 0.0);
        assertTrue("hit rate must be <= 100", rate <= 100.0);
    }

    @Test
    public void invalidateCacheNoArgClearsPreviouslyCachedEntry() throws Exception {
        // Arrange — seed a cache entry, confirm it is present
        Account grantee = createAccount("globalinval@example.com", false, false);
        Account target = createAccount("globalinvaltarget@example.com", false, false);
        PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.TRUE);
        assertEquals(CachedPermission.ALLOWED,
                PermissionCache.cacheGet(grantee, target, cacheableUserRight, false));

        // Act — global invalidation clears grantee, effective-rights and perm caches
        PermissionCache.invalidateCache();

        // Assert — the previously cached permission is gone
        assertEquals(CachedPermission.NOT_CACHED,
                PermissionCache.cacheGet(grantee, target, cacheableUserRight, false));

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void invalidateAllCacheFlushesProvAndPermCache() throws Exception {
        // Arrange — seed a cache entry
        Account grantee = createAccount("allinval@example.com", false, false);
        Account target = createAccount("allinvaltarget@example.com", false, false);
        PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.FALSE);
        assertEquals(CachedPermission.DENIED,
                PermissionCache.cacheGet(grantee, target, cacheableUserRight, false));

        // Act — invalidateAllCache also flushes provisioning LDAP-entry caches
        PermissionCache.invalidateAllCache();

        // Assert — permission cache cleared, no exception from prov.flushCache
        assertEquals(CachedPermission.NOT_CACHED,
                PermissionCache.cacheGet(grantee, target, cacheableUserRight, false));

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    /*
     * Build a key-grantee GuestAccount (null digest, non-null access key) without Mockito,
     * which is not on the unit-test classpath. The two private GuestAccount fields are set directly
     * via reflection, matching what GuestAccount(AuthToken) does for a key grantee.
     */
    private static GuestAccount keyGuest(String accessKey) throws Exception {
        GuestAccount guest = new GuestAccount(GuestAccount.EMAIL_ADDRESS_PUBLIC, null);
        Field digestField = GuestAccount.class.getDeclaredField("digest");
        digestField.setAccessible(true);
        digestField.set(guest, null);
        Field accessKeyField = GuestAccount.class.getDeclaredField("accessKey");
        accessKeyField.setAccessible(true);
        accessKeyField.set(guest, accessKey);
        return guest;
    }

    @Test
    public void buildCacheKeyGuestWithAccessKeyNoDigestAppendsKSuffix() throws Exception {
        // Arrange — a key-grantee GuestAccount: null digest, non-null access key
        GuestAccount guest = keyGuest("ACCESSKEY123");

        // Act
        String key = PermissionCache.buildCacheKey(guest, cacheableUserRight, false);

        // Assert — identifier is <accessKey>K, then adminFlag '0' and canDelegate '0'
        assertNull("precondition: digest must be null", guest.getDigest());
        assertEquals("ACCESSKEY123", guest.getAccessKey());
        assertNotNull(key);
        assertEquals("ACCESSKEY123K00", key);
    }

    @Test
    public void buildCacheKeyGuestWithAccessKeyCanDelegateSetsTrailingOne() throws Exception {
        // Arrange — key grantee with canDelegateNeeded true
        GuestAccount guest = keyGuest("KEYABC");

        // Act
        String key = PermissionCache.buildCacheKey(guest, cacheableUserRight, true);

        // Assert — <accessKey>K + adminFlag '0' + canDelegate '1'
        assertEquals("KEYABCK01", key);
    }

    @Test
    public void buildCacheKeyGuestNoDigestNoAccessKeyReturnsNull() throws Exception {
        // Covers buildCacheKey L187 (digest != null) and L199 (id == null): a guest with neither a
        // digest nor an access key has no identifier, so the key MUST be null. If the L199 guard
        // were flipped, a non-null (garbage) key would be returned instead.
        GuestAccount guest = keyGuest(null);

        // Act
        String key = PermissionCache.buildCacheKey(guest, cacheableUserRight, false);

        // Assert
        assertNull("precondition: digest null", guest.getDigest());
        assertNull("precondition: access key null", guest.getAccessKey());
        assertNull("no identifier -> null cache key", key);
    }

    @Test
    public void buildCacheKeyDelegatedAdminAccountUsesFlagOne() throws Exception {
        // Arrange — delegated (but not global) admin maps to admin flag '1'
        Account grantee = createAccount("cachekey-deladmin@example.com", false, true);

        // Act
        String key = PermissionCache.buildCacheKey(grantee, cacheableUserRight, false);

        // Assert
        assertEquals(grantee.getId() + "10", key);

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void cacheGetDebugLoggingEnabledStillReturnsCachedValue() throws Exception {
        // Arrange — enable acl debug so the debug-log branch in cacheGet executes
        Account grantee = createAccount("debugget@example.com", false, false);
        Account target = createAccount("debuggettarget@example.com", false, false);
        Log acl = ZimbraLog.acl;
        Log.Level prior = acl.getLevel();
        acl.setLevel(Log.Level.debug);
        try {
            PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.TRUE);

            // Act — debug branch builds a log message but must not change the result
            CachedPermission got = PermissionCache.cacheGet(grantee, target, cacheableUserRight, false);

            // Assert
            assertEquals(CachedPermission.ALLOWED, got);
        } finally {
            acl.setLevel(prior);
        }

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void cachePutDebugLoggingEnabledStillStoresValue() throws Exception {
        // Arrange — enable acl debug so the debug-log branch in cachePut executes
        Account grantee = createAccount("debugput@example.com", false, false);
        Account target = createAccount("debugputtarget@example.com", false, false);
        Log acl = ZimbraLog.acl;
        Log.Level prior = acl.getLevel();
        acl.setLevel(Log.Level.debug);
        try {
            // Act — put under debug logging, then read back outside the assertion
            PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.FALSE);

            // Assert — value is stored despite the extra logging
            assertEquals(CachedPermission.DENIED,
                    PermissionCache.cacheGet(grantee, target, cacheableUserRight, false));
        } finally {
            acl.setLevel(prior);
        }

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    // ============================================================================================
    // CachedPermission enum contract — pins getResult() (L50) and getCacheMask() (L54) to exact
    // values so a false/zero return mutation is caught.
    // ============================================================================================

    @Test
    public void cachedPermissionGetResultExactBooleanPerConstant() {
        // ALLOWED -> Boolean.TRUE, DENIED -> Boolean.FALSE, NO_MATCHING_ACL -> null.
        // (NOT_CACHED.getResult() asserts this!=NOT_CACHED, so it is intentionally not called.)
        assertEquals(Boolean.TRUE, CachedPermission.ALLOWED.getResult());
        assertEquals(Boolean.FALSE, CachedPermission.DENIED.getResult());
        assertNull(CachedPermission.NO_MATCHING_ACL.getResult());
        // Distinctness guard: a mutated getResult() returning a constant would collapse these.
        assertFalse(CachedPermission.ALLOWED.getResult().equals(CachedPermission.DENIED.getResult()));
    }

    @Test
    public void cachedPermissionGetCacheMaskExactShortPerConstant() {
        // The mask backs the on-disk/byte encoding; an all-zero (PrimitiveReturns) mutation would
        // make ALLOWED/DENIED indistinguishable.
        assertEquals(CachedPerms.MASK_NO_MATCHING_ACL, CachedPermission.NOT_CACHED.getCacheMask());
        assertEquals(CachedPerms.MASK_NO_MATCHING_ACL, CachedPermission.NO_MATCHING_ACL.getCacheMask());
        assertEquals(CachedPerms.MASK_ALLOWED, CachedPermission.ALLOWED.getCacheMask());
        assertEquals(CachedPerms.MASK_DENIED, CachedPermission.DENIED.getCacheMask());
        // Concrete numeric values (MASK_ALLOWED=1, MASK_DENIED=2): catches a return-0 mutation.
        assertEquals((short) 1, CachedPermission.ALLOWED.getCacheMask());
        assertEquals((short) 2, CachedPermission.DENIED.getCacheMask());
    }

    // ============================================================================================
    // cacheGet/cachePut return-value and gate behavior.
    // ============================================================================================

    @Test
    public void cacheGetNonCacheableRightReturnsNotCachedNotNull() throws Exception {
        // Covers cacheGet L101 (cacheKey == null) and L103 (return NOT_CACHED, not null): an
        // uncacheable right yields a null cache key, so cacheGet returns the NOT_CACHED enum.
        if (nonCacheableRight == null) {
            return; // no uncacheable right in registry; nothing to assert
        }
        Account grantee = createAccount("ncget@example.com", false, false);
        Account target = createAccount("ncgettarget@example.com", false, false);

        CachedPermission got = PermissionCache.cacheGet(grantee, target, nonCacheableRight, false);

        assertNotNull("cacheGet must never return null", got);
        assertSame(CachedPermission.NOT_CACHED, got);

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void cacheDisabledPutThenGetReturnsNotCached() throws Exception {
        // Covers cacheGet L96 and cachePut L120 (the !cacheEnabled gate): with caching disabled,
        // cachePut is a no-op and cacheGet returns NOT_CACHED even after a put. Toggle the private
        // static flag via reflection and restore it afterward.
        Account grantee = createAccount("disabledcache@example.com", false, false);
        Account target = createAccount("disabledcachetarget@example.com", false, false);

        Field enabled = PermissionCache.class.getDeclaredField("cacheEnabled");
        enabled.setAccessible(true);
        boolean prior = enabled.getBoolean(null);
        try {
            enabled.setBoolean(null, false);

            PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.TRUE);
            CachedPermission got = PermissionCache.cacheGet(grantee, target, cacheableUserRight, false);

            // With caching disabled the put never stored anything and the get short-circuits.
            assertSame(CachedPermission.NOT_CACHED, got);
        } finally {
            enabled.setBoolean(null, prior);
        }

        // Sanity: with caching re-enabled the same key now round-trips, proving the gate (not some
        // unrelated miss) drove the NOT_CACHED result above.
        PermissionCache.cachePut(grantee, target, cacheableUserRight, false, Boolean.TRUE);
        assertSame(CachedPermission.ALLOWED,
                PermissionCache.cacheGet(grantee, target, cacheableUserRight, false));

        prov.deleteAccount(grantee.getId());
        prov.deleteAccount(target.getId());
    }
}
