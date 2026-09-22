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

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.cache.DomainCache.GetFromDomainCacheOption;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
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
 * Functional tests for {@link DomainCache} using real {@link Domain} objects
 * created through the in-memory MockProvisioning harness. Covers positive
 * caching by id / name / virtual-hostname / foreign-name / krb5-realm, the
 * POSITIVE/NEGATIVE/BOTH lookup options, the negative (non-existing) cache,
 * staleness eviction, remove/replace/clear workflows, and hit-rate accounting.
 */
public class DomainCacheTest {

    private Provisioning prov;

    private int counter;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        counter++;
    }

    /* Creates a uniquely-named domain so per-method runs never collide (createDomain throws on dup). */
    private Domain newDomain(Map<String, Object> attrs) throws Exception {
        String name = "dc" + counter + "-" + System.nanoTime() + ".example.com";
        return prov.createDomain(name, attrs);
    }

    @Test
    public void putThenGetByIdPositiveReturnsCachedDomain() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());

        // Act
        cache.put(DomainBy.id, domain.getId(), domain);

        // Assert
        assertSame("getById POSITIVE returns the cached domain", domain,
                cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE));
        assertEquals("one id entry cached", 1, cache.getSize());
    }

    @Test
    public void getByNameCaseInsensitivePositiveReturnsCachedDomain() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());

        // Act
        cache.put(DomainBy.name, domain.getName(), domain);
        Domain found = cache.getByName(domain.getName().toUpperCase(),
                GetFromDomainCacheOption.POSITIVE);

        // Assert
        assertSame("name lookup folds case to lower", domain, found);
    }

    @Test
    public void getByVirtualHostnamePositiveReturnsCachedDomain() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, "vhost.example.com");
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(attrs);

        // Act
        cache.put(DomainBy.virtualHostname, "vhost.example.com", domain);
        Domain found = cache.getByVirtualHostname("VHOST.EXAMPLE.COM",
                GetFromDomainCacheOption.POSITIVE);

        // Assert
        assertSame("virtual hostname lookup folds case and returns domain", domain, found);
    }

    @Test
    public void getByForeignNamePositiveReturnsCachedDomain() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraForeignName, "webmail:foreign.example.com");
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(attrs);

        // Act
        cache.put(DomainBy.foreignName, "webmail:foreign.example.com", domain);
        Domain found = cache.getByForeignName("webmail:foreign.example.com",
                GetFromDomainCacheOption.POSITIVE);

        // Assert
        assertSame("foreign name lookup returns the domain", domain, found);
    }

    @Test
    public void getByKrb5RealmPositiveReturnsCachedDomain() throws Exception {
        // Arrange
        // put() stores the krb5 realm under its raw attribute value as the key,
        // but getByKrb5Realm(POSITIVE) folds the lookup key to lower-case. So the
        // stored realm must already be lower-case for a positive hit to match.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, "myrealm");
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(attrs);

        // Act
        cache.put(DomainBy.krb5Realm, "myrealm", domain);
        Domain found = cache.getByKrb5Realm("MYREALM", GetFromDomainCacheOption.POSITIVE);

        // Assert
        assertSame("krb5 realm lookup folds case to lower and returns the domain", domain, found);
    }

    @Test
    public void putNullEntryPopulatesNegativeCache() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);

        // Act
        cache.put(DomainBy.name, "ghost.example.com", null);

        // Assert
        assertNotNull("negative cache holds a non-existing-domain placeholder",
                cache.getByName("ghost.example.com", GetFromDomainCacheOption.NEGATIVE));
        assertNull("positive lookup of a never-cached domain is null",
                cache.getByName("ghost.example.com", GetFromDomainCacheOption.POSITIVE));
        assertEquals("negative put does not grow the positive id cache", 0, cache.getSize());
    }

    @Test
    public void getByIdBothFallsBackToNegativeCache() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.id, "ghost-id", null);

        // Act
        Domain found = cache.getById("ghost-id", GetFromDomainCacheOption.BOTH);

        // Assert
        assertNotNull("BOTH falls back to the negative cache when positive misses", found);
    }

    @Test
    public void putRealDomainCleansMatchingNegativeEntry() throws Exception {
        // Arrange: first cache a negative entry for the name, then put the real domain.
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.name, domain.getName(), null);
        assertNotNull("negative entry present before real put",
                cache.getByName(domain.getName(), GetFromDomainCacheOption.NEGATIVE));

        // Act
        cache.put(DomainBy.name, domain.getName(), domain);

        // Assert
        assertNull("real put cleans the negative entry",
                cache.getByName(domain.getName(), GetFromDomainCacheOption.NEGATIVE));
        assertSame("positive entry now present", domain,
                cache.getByName(domain.getName(), GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void removeFromNegativeCacheExistingKeyRemovesPlaceholder() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.id, "neg-id", null);

        // Act
        cache.removeFromNegativeCache(DomainBy.id, "neg-id");

        // Assert
        assertNull("negative placeholder removed",
                cache.getById("neg-id", GetFromDomainCacheOption.NEGATIVE));
    }

    @Test
    public void removeCachedDomainClearsAllIndexes() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, "rmvhost.example.com");
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, "RMREALM");
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(attrs);
        cache.put(DomainBy.id, domain.getId(), domain);

        // Act
        cache.remove(domain);

        // Assert
        assertNull("id index cleared", cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE));
        assertNull("name index cleared", cache.getByName(domain.getName(), GetFromDomainCacheOption.POSITIVE));
        assertNull("vhost index cleared",
                cache.getByVirtualHostname("rmvhost.example.com", GetFromDomainCacheOption.POSITIVE));
        assertNull("krb5 index cleared",
                cache.getByKrb5Realm("RMREALM", GetFromDomainCacheOption.POSITIVE));
        assertEquals("size back to zero", 0, cache.getSize());
    }

    @Test
    public void removeNullEntryIsNoOp() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.id, domain.getId(), domain);

        // Act
        cache.remove(null);

        // Assert
        assertSame("null remove leaves entry", domain,
                cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void replaceCachedDomainKeepsSingleEntry() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.id, domain.getId(), domain);

        // Act
        cache.replace(domain);

        // Assert
        assertSame("replace re-caches the domain by id", domain,
                cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE));
        assertEquals("no duplicate id entries", 1, cache.getSize());
    }

    @Test
    public void clearPopulatedCacheEmptiesPositiveAndNegative() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.id, domain.getId(), domain);
        cache.put(DomainBy.name, "ghost.example.com", null);

        // Act
        cache.clear();

        // Assert
        assertEquals("positive cache emptied", 0, cache.getSize());
        assertNull("negative cache emptied",
                cache.getByName("ghost.example.com", GetFromDomainCacheOption.NEGATIVE));
    }

    @Test
    public void getByIdStaleEntryEvictsAndReturnsNull() throws Exception {
        // Arrange: negative TTL forces immediate staleness.
        DomainCache cache = new DomainCache(100, -1000, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.id, domain.getId(), domain);

        // Act
        Domain found = cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE);

        // Assert
        assertNull("stale entry evicted on read", found);
        assertEquals("eviction removed it", 0, cache.getSize());
    }

    @Test
    public void putNullEntryByIdPopulatesNegativeIdCache() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);

        // Act -- negative put under the id key
        cache.put(DomainBy.id, "neg-by-id", null);

        // Assert -- NEGATIVE id lookup hits, BOTH falls back to it, positive misses
        assertNotNull("negative id placeholder present",
                cache.getById("neg-by-id", GetFromDomainCacheOption.NEGATIVE));
        assertNotNull("BOTH falls back to negative id cache",
                cache.getById("neg-by-id", GetFromDomainCacheOption.BOTH));
        assertNull("positive id lookup misses",
                cache.getById("neg-by-id", GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void putNullEntryByVirtualHostnamePopulatesNegativeVhostCache() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);

        // Act -- exercises the virtualHostname branch of NegativeCache.put
        cache.put(DomainBy.virtualHostname, "neg-vhost", null);

        // Assert -- NEGATIVE and BOTH options of getByVirtualHostname
        assertNotNull("negative vhost placeholder present",
                cache.getByVirtualHostname("neg-vhost", GetFromDomainCacheOption.NEGATIVE));
        assertNotNull("BOTH falls back to negative vhost cache",
                cache.getByVirtualHostname("neg-vhost", GetFromDomainCacheOption.BOTH));
        assertNull("positive vhost lookup misses",
                cache.getByVirtualHostname("neg-vhost", GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void putNullEntryByForeignNamePopulatesNegativeForeignNameCache() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);

        // Act -- exercises the foreignName branch of NegativeCache.put
        cache.put(DomainBy.foreignName, "neg-foreign", null);

        // Assert
        assertNotNull("negative foreign-name placeholder present",
                cache.getByForeignName("neg-foreign", GetFromDomainCacheOption.NEGATIVE));
        assertNotNull("BOTH falls back to negative foreign-name cache",
                cache.getByForeignName("neg-foreign", GetFromDomainCacheOption.BOTH));
        assertNull("positive foreign-name lookup misses",
                cache.getByForeignName("neg-foreign", GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void putNullEntryByKrb5RealmPopulatesNegativeKrb5Cache() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);

        // Act -- exercises the krb5Realm branch of NegativeCache.put
        cache.put(DomainBy.krb5Realm, "neg-realm", null);

        // Assert
        assertNotNull("negative krb5 placeholder present",
                cache.getByKrb5Realm("neg-realm", GetFromDomainCacheOption.NEGATIVE));
        assertNotNull("BOTH falls back to negative krb5 cache",
                cache.getByKrb5Realm("neg-realm", GetFromDomainCacheOption.BOTH));
        assertNull("positive krb5 lookup misses",
                cache.getByKrb5Realm("neg-realm", GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void getByNameBothFallsBackToNegativeCache() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.name, "ghost-name", null);

        // Act
        Domain found = cache.getByName("ghost-name", GetFromDomainCacheOption.BOTH);

        // Assert
        assertNotNull("BOTH falls back to negative name cache when positive misses", found);
    }

    @Test
    public void getByNameBothWithPositiveHitReturnsRealDomain() throws Exception {
        // Arrange -- a real domain cached; BOTH must return it without touching the negative cache
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.name, domain.getName(), domain);

        // Act
        Domain found = cache.getByName(domain.getName(), GetFromDomainCacheOption.BOTH);

        // Assert
        assertSame("BOTH returns the positive domain on a hit", domain, found);
    }

    @Test
    public void getByVirtualHostnameBothWithPositiveHitReturnsRealDomain() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, "bothvhost.example.com");
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(attrs);
        cache.put(DomainBy.virtualHostname, "bothvhost.example.com", domain);

        // Act
        Domain found = cache.getByVirtualHostname("BOTHVHOST.EXAMPLE.COM",
                GetFromDomainCacheOption.BOTH);

        // Assert
        assertSame("BOTH vhost lookup returns positive hit", domain, found);
    }

    @Test
    public void getByForeignNameBothMissFallsBackToNegative() throws Exception {
        // Arrange -- only a negative entry exists
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.foreignName, "ghostforeign", null);

        // Act
        Domain found = cache.getByForeignName("ghostforeign", GetFromDomainCacheOption.BOTH);

        // Assert
        assertNotNull("BOTH foreign-name lookup falls back to the negative cache", found);
    }

    @Test
    public void getByKrb5RealmBothMissFallsBackToNegative() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.krb5Realm, "ghostrealm", null);

        // Act
        Domain found = cache.getByKrb5Realm("ghostrealm", GetFromDomainCacheOption.BOTH);

        // Assert
        assertNotNull("BOTH krb5 lookup falls back to the negative cache", found);
    }

    @Test
    public void removeFromNegativeCacheByVirtualHostnameRemovesPlaceholder() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.virtualHostname, "rm-vhost", null);
        assertNotNull("present before removal",
                cache.getByVirtualHostname("rm-vhost", GetFromDomainCacheOption.NEGATIVE));

        // Act -- exercises the virtualHostname branch of NegativeCache.remove
        cache.removeFromNegativeCache(DomainBy.virtualHostname, "rm-vhost");

        // Assert
        assertNull("negative vhost placeholder removed",
                cache.getByVirtualHostname("rm-vhost", GetFromDomainCacheOption.NEGATIVE));
    }

    @Test
    public void removeFromNegativeCacheByForeignNameRemovesPlaceholder() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.foreignName, "rm-foreign", null);

        // Act
        cache.removeFromNegativeCache(DomainBy.foreignName, "rm-foreign");

        // Assert
        assertNull("negative foreign-name placeholder removed",
                cache.getByForeignName("rm-foreign", GetFromDomainCacheOption.NEGATIVE));
    }

    @Test
    public void removeFromNegativeCacheByKrb5RealmRemovesPlaceholder() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.krb5Realm, "rm-realm", null);

        // Act
        cache.removeFromNegativeCache(DomainBy.krb5Realm, "rm-realm");

        // Assert
        assertNull("negative krb5 placeholder removed",
                cache.getByKrb5Realm("rm-realm", GetFromDomainCacheOption.NEGATIVE));
    }

    @Test
    public void removeFromNegativeCacheByNameRemovesPlaceholder() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.name, "rm-name", null);

        // Act
        cache.removeFromNegativeCache(DomainBy.name, "rm-name");

        // Assert
        assertNull("negative name placeholder removed",
                cache.getByName("rm-name", GetFromDomainCacheOption.NEGATIVE));
    }

    @Test
    public void removeDomainWithForeignNameClearsForeignNameIndex() throws Exception {
        // Arrange -- a domain with a foreign name so remove() iterates the foreign-name loop
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraForeignName, "webmail:rmfn.example.com");
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(attrs);
        cache.put(DomainBy.foreignName, "webmail:rmfn.example.com", domain);
        assertSame("present before removal", domain,
                cache.getByForeignName("webmail:rmfn.example.com", GetFromDomainCacheOption.POSITIVE));

        // Act
        cache.remove(domain);

        // Assert
        assertNull("foreign-name index cleared on remove",
                cache.getByForeignName("webmail:rmfn.example.com", GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void getByNameNegativeOptionReturnsPlaceholderOnly() throws Exception {
        // Arrange -- a positive entry exists; NEGATIVE option must not return it
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.name, domain.getName(), domain);

        // Act + Assert
        assertNull("NEGATIVE option ignores the positive cache",
                cache.getByName(domain.getName(), GetFromDomainCacheOption.NEGATIVE));
    }

    /* Reflectively read the private mHitRate Counter so we can assert its raw count/total —
     *  the public getHitRate() collapses pure-miss windows to 0.0 and would hide a dropped
     *  increment in the stale-eviction path. */
    private static com.zimbra.common.stats.Counter hitRateOf(DomainCache cache) throws Exception {
        java.lang.reflect.Field f = DomainCache.class.getDeclaredField("mHitRate");
        f.setAccessible(true);
        return (com.zimbra.common.stats.Counter) f.get(cache);
    }

    @Test
    public void getByIdStaleEvictionRecordsHitRateMiss() throws Exception {
        // Arrange — negative TTL forces immediate staleness so the read takes the stale branch.
        DomainCache cache = new DomainCache(100, -1000, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.id, domain.getId(), domain);
        com.zimbra.common.stats.Counter hr = hitRateOf(cache);
        long baselineCount = hr.getCount();

        // Act — one stale read evicts the entry and must record a miss (increment(0))
        Domain found = cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE);

        // Assert — L290 VoidMethodCall drops mHitRate.increment(0); the raw count would not advance.
        assertNull("stale read returns null", found);
        assertEquals("stale-eviction read records exactly one miss sample",
                baselineCount + 1, hr.getCount());
    }

    @Test
    public void cacheEntryFreshEntryNotStaleReturnsCachedDomain() throws Exception {
        // Arrange — a generous positive TTL so the entry is fresh; the read path actually invokes
        // CacheEntry.isStale() (skipped entirely when TTL == 0).
        DomainCache cache = new DomainCache(100, 600000L, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.id, domain.getId(), domain);

        // Act
        Domain found = cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE);

        // Assert — L73 BooleanTrueReturnVals would force isStale()==true, evicting the fresh entry
        // and returning null. A fresh entry must survive and be returned.
        assertSame("fresh (non-stale) entry is returned, not evicted", domain, found);
        assertEquals("fresh entry stays cached", 1, cache.getSize());
    }

    @Test
    public void cacheEntryIsStalePastAndFutureLifetimes() throws Exception {
        // Direct unit test of CacheEntry.isStale() (same package access). A clearly-past lifetime is
        // stale; a clearly-future lifetime is fresh. Plus a best-effort boundary probe (mLifetime ==
        // now) that distinguishes '<' from '<=' (L73 ConditionalsBoundary) when caught in one ms.
        Domain domain = newDomain(new HashMap<String, Object>());

        DomainCache.CacheEntry past = new DomainCache.CacheEntry(domain, -100000L);
        assertTrue("lifetime well in the past => stale", past.isStale());

        DomainCache.CacheEntry future = new DomainCache.CacheEntry(domain, 100000L);
        assertFalse("lifetime well in the future => not stale", future.isStale());

        // Boundary probe: set mLifetime exactly to 'now' and, only if the clock has not advanced,
        // assert isStale()==false. Original '<' => now<now is false (fresh); the '<=' mutant would
        // report stale. Guarded by the same-millisecond check so it never flakily FAILS.
        java.lang.reflect.Field lf = DomainCache.CacheEntry.class.getDeclaredField("mLifetime");
        lf.setAccessible(true);
        for (int i = 0; i < 2000000; i++) {
            long now = System.currentTimeMillis();
            lf.setLong(future, now);
            if (System.currentTimeMillis() == now) {
                // still inside the same millisecond — isStale's internal now should equal mLifetime
                assertFalse("at the exact boundary (mLifetime == now) the entry is NOT yet stale",
                        future.isStale());
                break;
            }
        }
    }

    @Test
    public void putRealDomainWithKrb5CleansMatchingNegativeKrb5Entry() throws Exception {
        // Arrange — a negative krb5 entry, then put a real domain carrying that (lower-case) realm so
        // NegativeCache.clean() removes the matching negative placeholder.
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.krb5Realm, "cleanrealm", null);
        assertNotNull("negative krb5 entry present before real put",
                cache.getByKrb5Realm("cleanrealm", GetFromDomainCacheOption.NEGATIVE));

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, "cleanrealm");
        Domain domain = newDomain(attrs);

        // Act
        cache.put(DomainBy.krb5Realm, "cleanrealm", domain);

        // Assert — L188 NegateConditionals (krb5Realm != null) would skip removing the negative entry.
        assertNull("real put with a krb5 realm cleans the matching negative krb5 entry",
                cache.getByKrb5Realm("cleanrealm", GetFromDomainCacheOption.NEGATIVE));
    }

    @Test
    public void removeDomainWithKrb5ClearsKrb5IndexWhenKeysMatch() throws Exception {
        // Arrange — store with a LOWER-CASE realm so the positive lookup (which folds to lower) matches
        // both before and after, isolating the effect of remove()'s krb5 branch.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, "rmrealmlc");
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(attrs);
        cache.put(DomainBy.krb5Realm, "rmrealmlc", domain);
        assertSame("krb5 entry present before removal", domain,
                cache.getByKrb5Realm("rmrealmlc", GetFromDomainCacheOption.POSITIVE));

        // Act
        cache.remove(domain);

        // Assert — L243 NegateConditionals (krb5Realm != null) would skip the krb5 removal, leaving
        // the entry cached.
        assertNull("remove clears the krb5 index for a domain with a realm",
                cache.getByKrb5Realm("rmrealmlc", GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void clearAllPositiveIndexesIndividuallyEmptied() throws Exception {
        // Arrange — populate every positive index (name, id, vhost, foreign-name, krb5) with matching
        // lower-case keys so each lookup can confirm its own clear() call ran.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, "clrvhost.example.com");
        attrs.put(Provisioning.A_zimbraForeignName, "webmail:clrfn.example.com");
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, "clrrealm");
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(attrs);
        cache.put(DomainBy.id, domain.getId(), domain);

        // Preconditions — each index holds the domain
        assertSame(domain, cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE));
        assertSame(domain, cache.getByName(domain.getName(), GetFromDomainCacheOption.POSITIVE));
        assertSame(domain, cache.getByVirtualHostname("clrvhost.example.com", GetFromDomainCacheOption.POSITIVE));
        assertSame(domain, cache.getByForeignName("webmail:clrfn.example.com", GetFromDomainCacheOption.POSITIVE));
        assertSame(domain, cache.getByKrb5Realm("clrrealm", GetFromDomainCacheOption.POSITIVE));

        // Act
        cache.clear();

        // Assert — each positive clear() call (L219 name, L220 id, L221 vhost, L222 foreign, L223 krb5)
        // must have run, so every index misses.
        assertNull("name index cleared", cache.getByName(domain.getName(), GetFromDomainCacheOption.POSITIVE));
        assertEquals("id index cleared", 0, cache.getSize());
        assertNull("vhost index cleared",
                cache.getByVirtualHostname("clrvhost.example.com", GetFromDomainCacheOption.POSITIVE));
        assertNull("foreign-name index cleared",
                cache.getByForeignName("webmail:clrfn.example.com", GetFromDomainCacheOption.POSITIVE));
        assertNull("krb5 index cleared",
                cache.getByKrb5Realm("clrrealm", GetFromDomainCacheOption.POSITIVE));
    }

    @Test
    public void clearAllNegativeIndexesIndividuallyEmptied() throws Exception {
        // Arrange — populate every negative index so each NegativeCache.clear() call can be observed.
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        cache.put(DomainBy.name, "negclr-name", null);
        cache.put(DomainBy.id, "negclr-id", null);
        cache.put(DomainBy.virtualHostname, "negclr-vhost", null);
        cache.put(DomainBy.foreignName, "negclr-foreign", null);
        cache.put(DomainBy.krb5Realm, "negclr-realm", null);

        // Preconditions — every negative index holds a placeholder
        assertNotNull(cache.getByName("negclr-name", GetFromDomainCacheOption.NEGATIVE));
        assertNotNull(cache.getById("negclr-id", GetFromDomainCacheOption.NEGATIVE));
        assertNotNull(cache.getByVirtualHostname("negclr-vhost", GetFromDomainCacheOption.NEGATIVE));
        assertNotNull(cache.getByForeignName("negclr-foreign", GetFromDomainCacheOption.NEGATIVE));
        assertNotNull(cache.getByKrb5Realm("negclr-realm", GetFromDomainCacheOption.NEGATIVE));

        // Act
        cache.clear();

        // Assert — each negative clear() (L193 name, L194 id, L195 vhost, L196 foreign, L197 krb5) ran.
        assertNull("negative name cleared", cache.getByName("negclr-name", GetFromDomainCacheOption.NEGATIVE));
        assertNull("negative id cleared", cache.getById("negclr-id", GetFromDomainCacheOption.NEGATIVE));
        assertNull("negative vhost cleared",
                cache.getByVirtualHostname("negclr-vhost", GetFromDomainCacheOption.NEGATIVE));
        assertNull("negative foreign cleared",
                cache.getByForeignName("negclr-foreign", GetFromDomainCacheOption.NEGATIVE));
        assertNull("negative krb5 cleared",
                cache.getByKrb5Realm("negclr-realm", GetFromDomainCacheOption.NEGATIVE));
    }

    @Test
    public void getHitRateHitsAndMissesReflectsAccess() throws Exception {
        // Arrange
        DomainCache cache = new DomainCache(100, 0, 100, 0);
        Domain domain = newDomain(new HashMap<String, Object>());
        cache.put(DomainBy.id, domain.getId(), domain);

        // Act
        cache.getById(domain.getId(), GetFromDomainCacheOption.POSITIVE);   // hit
        cache.getById("missing", GetFromDomainCacheOption.POSITIVE);        // miss

        // Assert
        double rate = cache.getHitRate();
        assertTrue("hit rate averaged between 0 and 100, got " + rate,
                rate > 0.0 && rate < 100.0);
    }
}
