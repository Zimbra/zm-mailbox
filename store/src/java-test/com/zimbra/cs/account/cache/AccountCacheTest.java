/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

package com.zimbra.cs.account.cache;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.stats.Counter;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link AccountCache} using real {@link Account} objects
 * created through the in-memory MockProvisioning harness. Covers caching by id,
 * name, alias, foreign principal and old mail address; case folding; staleness
 * eviction; remove/replace/clear workflows; and hit-rate accounting.
 */
public class AccountCacheTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account newAccount(String email, Map<String, Object> attrs) throws Exception {
        prov.createAccount(email, "secret", attrs);
        return prov.get(AccountBy.name, email);
    }

    private static Counter hitRateCounterOf(AccountCache cache) throws Exception {
        Field f = AccountCache.class.getDeclaredField("mHitRate");
        f.setAccessible(true);
        return (Counter) f.get(cache);
    }

    @SuppressWarnings("unchecked")
    private static AccountCache.CacheEntry cacheEntryById(AccountCache cache, String id) throws Exception {
        Field f = AccountCache.class.getDeclaredField("mIdCache");
        f.setAccessible(true);
        Map<String, AccountCache.CacheEntry> idCache =
                (Map<String, AccountCache.CacheEntry>) f.get(cache);
        return idCache.get(id);
    }

    @Test
    public void putThenGetByIdAndNameReturnsSameEntry() throws Exception {
        // Arrange
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-basic@example.com", new HashMap<String, Object>());

        // Act
        cache.put(acct);

        // Assert
        assertSame("getById returns cached account", acct, cache.getById(acct.getId()));
        assertSame("getByName returns cached account", acct, cache.getByName(acct.getName()));
        assertEquals("one id entry", 1, cache.getSize());
    }

    @Test
    public void getByNameAliasKeyResolvesViaAliasCache() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "alias@example.com");
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-alias@example.com", attrs);

        // Act
        cache.put(acct);
        Account viaAlias = cache.getByName("alias@example.com");

        // Assert
        assertSame("alias lookup hits the alias cache", acct, viaAlias);
    }

    @Test
    public void getByNameOldMailAddressResolvesViaOldNameCache() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraOldMailAddress, "former@example.com");
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-old@example.com", attrs);

        // Act
        cache.put(acct);
        Account viaOld = cache.getByName("former@example.com");

        // Assert
        assertSame("old-name lookup hits the old-name cache", acct, viaOld);
    }

    @Test
    public void getByForeignPrincipalCachedFpReturnsEntry() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraForeignPrincipal, "kerberos5:fp@REALM");
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-fp@example.com", attrs);

        // Act
        cache.put(acct);
        Account viaFp = cache.getByForeignPrincipal("kerberos5:fp@REALM");

        // Assert
        assertSame("foreign principal lookup returns the account", acct, viaFp);
    }

    @Test
    public void getByNameUpperCaseKeyIsCaseInsensitive() throws Exception {
        // Arrange
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-case@example.com", new HashMap<String, Object>());
        cache.put(acct);

        // Act
        Account found = cache.getByName("AC-CASE@EXAMPLE.COM");

        // Assert
        assertSame("name lookup folds to lower case", acct, found);
    }

    @Test
    public void getByIdMissingKeyReturnsNull() throws Exception {
        // Arrange
        AccountCache cache = new AccountCache(100, 0);

        // Act / Assert
        assertNull("absent id yields null", cache.getById("nope"));
    }

    @Test
    public void removeWithAliasAndFpClearsAllIndexes() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "rmalias@example.com");
        attrs.put(Provisioning.A_zimbraForeignPrincipal, "kerberos5:rmfp@REALM");
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-rm@example.com", attrs);
        cache.put(acct);

        // Act
        cache.remove(acct);

        // Assert
        assertNull("id index cleared", cache.getById(acct.getId()));
        assertNull("name index cleared", cache.getByName(acct.getName()));
        assertNull("alias index cleared", cache.getByName("rmalias@example.com"));
        assertNull("fp index cleared", cache.getByForeignPrincipal("kerberos5:rmfp@REALM"));
        assertEquals("size back to zero", 0, cache.getSize());
    }

    @Test
    public void removeNullEntryIsNoOp() throws Exception {
        // Arrange
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-rmnull@example.com", new HashMap<String, Object>());
        cache.put(acct);

        // Act
        cache.remove(null);

        // Assert
        assertSame("null remove leaves entry", acct, cache.getById(acct.getId()));
    }

    @Test
    public void replaceExistingEntryKeepsSingleEntry() throws Exception {
        // Arrange
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-replace@example.com", new HashMap<String, Object>());
        cache.put(acct);

        // Act
        cache.replace(acct);

        // Assert
        assertSame("replace re-caches account", acct, cache.getById(acct.getId()));
        assertEquals("no duplicate id entries", 1, cache.getSize());
    }

    /**
     * Targets the {@code remove(entry)} call inside {@code replace(entry)} (L126).
     * After replacing an account that carries every index key, the cache must still
     * resolve that account through all five indexes with a single id entry. The
     * {@code remove}-then-{@code put} pair must leave each index pointing at the
     * account and never duplicate the id row.
     */
    @Test
    public void replaceAccountWithAllIndexesReestablishesEverySingleIndex() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "44444444-4444-4444-4444-444444444444");
        attrs.put(Provisioning.A_zimbraMailAlias, "rpl-alias@example.com");
        attrs.put(Provisioning.A_zimbraForeignPrincipal, "kerberos5:rplfp@REALM");
        attrs.put(Provisioning.A_zimbraOldMailAddress, "rpl-old@example.com");
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-rpl@example.com", attrs);
        cache.put(acct);

        // Act
        cache.replace(acct);

        // Assert: every index still resolves and id row is not duplicated.
        assertSame("id index after replace", acct,
                cache.getById("44444444-4444-4444-4444-444444444444"));
        assertSame("name index after replace", acct, cache.getByName("ac-rpl@example.com"));
        assertSame("alias index after replace", acct, cache.getByName("rpl-alias@example.com"));
        assertSame("old-name index after replace", acct, cache.getByName("rpl-old@example.com"));
        assertSame("fp index after replace", acct,
                cache.getByForeignPrincipal("kerberos5:rplfp@REALM"));
        assertEquals("single id entry after replace", 1, cache.getSize());
    }

    /**
     * Further pins {@code replace} (L126): replacing an account that is NOT yet
     * cached must invoke {@code put} so the account becomes resolvable, and the
     * preceding {@code remove} on an absent entry must be a harmless no-op that
     * leaves an unrelated cached account untouched.
     */
    @Test
    public void replaceUncachedAccountPutsItWithoutDisturbingOthers() throws Exception {
        Map<String, Object> a = new HashMap<String, Object>();
        a.put(Provisioning.A_zimbraId, "55555555-5555-5555-5555-555555555555");
        Map<String, Object> b = new HashMap<String, Object>();
        b.put(Provisioning.A_zimbraId, "66666666-6666-6666-6666-666666666666");
        AccountCache cache = new AccountCache(100, 0);
        Account existing = newAccount("ac-rpl-keep@example.com", a);
        Account fresh = newAccount("ac-rpl-new@example.com", b);
        cache.put(existing);

        // Act: replace an account that was never put.
        cache.replace(fresh);

        // Assert: put ran (fresh now resolvable) and the pre-existing entry survived.
        assertSame("replace put the previously-uncached account", fresh,
                cache.getById("66666666-6666-6666-6666-666666666666"));
        assertSame("unrelated entry untouched by replace", existing,
                cache.getById("55555555-5555-5555-5555-555555555555"));
        assertEquals("both accounts cached", 2, cache.getSize());
    }

    @Test
    public void clearPopulatedCacheEmptiesEverything() throws Exception {
        // Arrange: give each account a distinct id so the id-keyed cache holds
        // two separate entries (MockProvisioning otherwise assigns both the same
        // well-known default id, which would collapse them to one).
        Map<String, Object> attrs1 = new HashMap<String, Object>();
        attrs1.put(Provisioning.A_zimbraId, "11111111-1111-1111-1111-111111111111");
        Map<String, Object> attrs2 = new HashMap<String, Object>();
        attrs2.put(Provisioning.A_zimbraId, "22222222-2222-2222-2222-222222222222");
        AccountCache cache = new AccountCache(100, 0);
        cache.put(newAccount("ac-c1@example.com", attrs1));
        cache.put(newAccount("ac-c2@example.com", attrs2));
        assertEquals(2, cache.getSize());

        // Act
        cache.clear();

        // Assert
        assertEquals("clear empties the cache", 0, cache.getSize());
    }

    /**
     * Targets the five {@code clear()} calls (mNameCache, mIdCache, mAliasCache,
     * mForeignPrincipalCache, mOldNameCache). Each cache index is probed through a
     * distinct lookup path so dropping any single {@code clear()} leaves that one
     * index populated and fails the matching assertion.
     */
    @Test
    public void clearEveryIndexIsEmptied() throws Exception {
        // Arrange: one account that lands in all five maps at once.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "33333333-3333-3333-3333-333333333333");
        attrs.put(Provisioning.A_zimbraMailAlias, "clr-alias@example.com");
        attrs.put(Provisioning.A_zimbraForeignPrincipal, "kerberos5:clrfp@REALM");
        attrs.put(Provisioning.A_zimbraOldMailAddress, "clr-old@example.com");
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-clr@example.com", attrs);
        cache.put(acct);

        // Sanity: every index resolves before clear.
        assertSame(acct, cache.getById("33333333-3333-3333-3333-333333333333"));
        assertSame(acct, cache.getByName("ac-clr@example.com"));
        assertSame(acct, cache.getByName("clr-alias@example.com"));
        assertSame(acct, cache.getByName("clr-old@example.com"));
        assertSame(acct, cache.getByForeignPrincipal("kerberos5:clrfp@REALM"));

        // Act
        cache.clear();

        // Assert: each index independently emptied.
        assertNull("id index cleared (L77)", cache.getById("33333333-3333-3333-3333-333333333333"));
        assertNull("name index cleared (L76)", cache.getByName("ac-clr@example.com"));
        assertNull("alias index cleared (L78)", cache.getByName("clr-alias@example.com"));
        assertNull("old-name index cleared (L80)", cache.getByName("clr-old@example.com"));
        assertNull("foreign-principal index cleared (L79)",
                cache.getByForeignPrincipal("kerberos5:clrfp@REALM"));
        assertEquals("size zero after clear", 0, cache.getSize());
    }

    @Test
    public void getByIdStaleEntryEvictsAndReturnsNull() throws Exception {
        // Arrange: negative TTL makes lifetime already in the past.
        AccountCache cache = new AccountCache(100, -1000);
        Account acct = newAccount("ac-ttl@example.com", new HashMap<String, Object>());
        cache.put(acct);

        // Act
        Account found = cache.getById(acct.getId());

        // Assert
        assertNull("stale entry evicted on read", found);
        assertEquals("eviction removed it", 0, cache.getSize());
    }

    /**
     * Targets the {@code mHitRate.increment(0)} call on the stale-eviction branch of
     * {@code get()} (L135). A single stale read must record exactly one counter
     * sample with a zero value; if the increment is dropped the counter stays at
     * count 0. Asserted directly against the underlying {@link Counter} via
     * reflection because a zero-valued sample is invisible to the averaged hit rate.
     */
    @Test
    public void getByIdStaleReadRecordsZeroValuedHitSample() throws Exception {
        // Arrange: negative TTL => entry already stale, but mRefreshTTL != 0 so the
        // staleness branch is actually taken.
        AccountCache cache = new AccountCache(100, -1000);
        Account acct = newAccount("ac-stalehr@example.com", new HashMap<String, Object>());
        cache.put(acct);
        Counter hitRate = hitRateCounterOf(cache);
        long countBefore = hitRate.getCount();
        long totalBefore = hitRate.getTotal();

        // Act: one stale read travels the L133-136 branch (remove + increment(0) + null).
        Account found = cache.getById(acct.getId());

        // Assert
        assertNull("stale read returns null", found);
        assertEquals("stale read records exactly one counter sample (L135)",
                1, hitRate.getCount() - countBefore);
        assertEquals("the stale sample contributes zero to the total (L135)",
                0, hitRate.getTotal() - totalBefore);
    }

    /**
     * Targets {@code CacheEntry.isStale()} (L57): the BooleanTrueReturnVals mutant
     * (always return true) and the conditional itself. A clearly-fresh entry must
     * report not-stale and a clearly-past entry must report stale, driving both
     * sides of the {@code mLifetime < now} branch to distinct results.
     */
    @Test
    public void cacheEntryIsStaleReflectsLifetimeVersusNow() throws Exception {
        Account acct = newAccount("ac-isstale@example.com", new HashMap<String, Object>());

        // Fresh: expiry far in the future -> not stale (mLifetime < now is false).
        AccountCache.CacheEntry fresh = new AccountCache.CacheEntry(acct, 60L * 60L * 1000L);
        assertFalse("entry expiring an hour from now is not stale", fresh.isStale());

        // Stale: pin lifetime well into the past -> stale (mLifetime < now is true).
        AccountCache.CacheEntry stale = new AccountCache.CacheEntry(acct, 0L);
        stale.mLifetime = System.currentTimeMillis() - 60L * 60L * 1000L;
        assertTrue("entry expired an hour ago is stale", stale.isStale());
    }

    /**
     * End-to-end staleness through {@code get()}: a fresh, positive-TTL entry must
     * survive a read (not evicted, returns the account and counts as a hit), while a
     * past-dated entry is evicted and returns null. This pins the not-stale side of
     * L57 to an observable cache outcome that the always-true mutant would break.
     */
    @Test
    public void getByIdFreshPositiveTtlEntryIsNotEvicted() throws Exception {
        AccountCache cache = new AccountCache(100, 60L * 60L * 1000L);
        Account acct = newAccount("ac-fresh@example.com", new HashMap<String, Object>());
        cache.put(acct);

        // Fresh read: isStale() must be false -> account returned, still cached.
        assertSame("fresh entry survives read", acct, cache.getById(acct.getId()));
        assertEquals("fresh entry not evicted", 1, cache.getSize());
        assertSame("second read still hits", acct, cache.getById(acct.getId()));

        // Now force the same entry stale and confirm the other branch evicts it.
        AccountCache.CacheEntry ce = cacheEntryById(cache, acct.getId());
        ce.mLifetime = System.currentTimeMillis() - 1000L;
        assertNull("stale entry now evicted", cache.getById(acct.getId()));
        assertEquals("eviction shrank the cache", 0, cache.getSize());
    }

    @Test
    public void getHitRateHitsAndMissesReflectsAccess() throws Exception {
        // Arrange
        AccountCache cache = new AccountCache(100, 0);
        Account acct = newAccount("ac-hr@example.com", new HashMap<String, Object>());
        cache.put(acct);

        // Act
        cache.getById(acct.getId());   // hit
        cache.getById("missing");      // miss

        // Assert
        double rate = cache.getHitRate();
        assertTrue("hit rate averaged between 0 and 100, got " + rate,
                rate > 0.0 && rate < 100.0);
    }
}
