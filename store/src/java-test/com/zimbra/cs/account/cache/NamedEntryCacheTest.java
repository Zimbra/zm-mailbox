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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link NamedEntryCache} using real {@link Account}
 * NamedEntry objects created through the in-memory MockProvisioning harness.
 * Covers put/get/remove/replace workflows, TTL staleness eviction, case
 * folding of name lookups, bulk put, and hit-rate accounting.
 */
public class NamedEntryCacheTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account newAccount(String email) throws Exception {
        // MockProvisioning assigns the well-known DEFAULT_ACCOUNT_ID to any account that
        // does not supply zimbraId, so two accounts would collide on the cache's id index.
        // Give each account a distinct zimbraId so the cache holds them as separate entries.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount(email, "secret", attrs);
        return prov.get(AccountBy.name, email);
    }

    @Test
    public void putThenGetByIdAndNameReturnsSameEntry() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        Account acct = newAccount("ne-basic@example.com");

        // Act
        cache.put(acct);

        // Assert
        assertSame("getById must return cached instance", acct, cache.getById(acct.getId()));
        assertSame("getByName must return cached instance", acct, cache.getByName(acct.getName()));
        assertEquals("size reflects single id entry", 1, cache.getSize());
    }

    @Test
    public void getByNameUpperCaseKeyIsCaseInsensitive() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        Account acct = newAccount("ne-case@example.com");
        cache.put(acct);

        // Act
        Account found = cache.getByName("NE-CASE@EXAMPLE.COM");

        // Assert
        assertSame("name lookup must fold case to lower", acct, found);
    }

    @Test
    public void getByIdMissingKeyReturnsNull() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);

        // Act
        Account found = cache.getById("no-such-id");

        // Assert
        assertNull("absent id yields null", found);
        assertEquals("nothing cached", 0, cache.getSize());
    }

    @Test
    public void removeByEntryRemovesFromBothIndexes() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        Account acct = newAccount("ne-rm@example.com");
        cache.put(acct);

        // Act
        cache.remove(acct);

        // Assert
        assertNull("id index cleared", cache.getById(acct.getId()));
        assertNull("name index cleared", cache.getByName(acct.getName()));
        assertEquals("size back to zero", 0, cache.getSize());
    }

    @Test
    public void removeByNameAndIdRemovesEntry() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        Account acct = newAccount("ne-rm2@example.com");
        cache.put(acct);

        // Act
        cache.remove(acct.getName(), acct.getId());

        // Assert
        assertNull("removed by id", cache.getById(acct.getId()));
        assertNull("removed by name", cache.getByName(acct.getName()));
    }

    @Test
    public void removeNullEntryIsNoOp() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        Account acct = newAccount("ne-rmnull@example.com");
        cache.put(acct);

        // Act
        cache.remove((Account) null);

        // Assert
        assertSame("existing entry untouched by null remove", acct, cache.getById(acct.getId()));
        assertEquals(1, cache.getSize());
    }

    @Test
    public void replaceExistingEntryKeepsSingleEntry() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        Account acct = newAccount("ne-replace@example.com");
        cache.put(acct);

        // Act
        cache.replace(acct);

        // Assert
        assertSame("replace re-caches the entry", acct, cache.getById(acct.getId()));
        assertEquals("replace does not duplicate id entries", 1, cache.getSize());
    }

    @Test
    public void clearPopulatedCacheEmptiesEverything() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        cache.put(newAccount("ne-c1@example.com"));
        cache.put(newAccount("ne-c2@example.com"));
        assertEquals(2, cache.getSize());

        // Act
        cache.clear();

        // Assert
        assertEquals("clear empties id cache", 0, cache.getSize());
    }

    @Test
    public void putListWithClearReplacesAllEntries() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        cache.put(newAccount("ne-stale@example.com"));
        List<Account> entries = new ArrayList<Account>();
        entries.add(newAccount("ne-l1@example.com"));
        entries.add(newAccount("ne-l2@example.com"));

        // Act
        cache.put(entries, true);

        // Assert
        assertEquals("clear=true drops prior entries, keeps only list", 2, cache.getSize());
        assertNotNull("listed entry present", cache.getByName("ne-l1@example.com"));
    }

    @Test
    public void putListNullIsNoOp() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        cache.put(newAccount("ne-keep@example.com"));

        // Act
        cache.put((List<Account>) null, true);

        // Assert
        assertEquals("null list leaves cache untouched", 1, cache.getSize());
    }

    @Test
    public void getByIdStaleEntryEvictsAndReturnsNull() throws Exception {
        // Arrange: negative TTL forces immediate staleness (lifetime in the past).
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, -1000);
        Account acct = newAccount("ne-ttl@example.com");
        cache.put(acct);

        // Act
        Account found = cache.getById(acct.getId());

        // Assert
        assertNull("stale entry evicted on read", found);
        assertEquals("eviction removed it from the cache", 0, cache.getSize());
    }

    @Test
    public void getHitRateHitsAndMissesReflectsAccess() throws Exception {
        // Arrange
        NamedEntryCache<Account> cache = new NamedEntryCache<Account>(100, 0);
        Account acct = newAccount("ne-hr@example.com");
        cache.put(acct);

        // Act
        cache.getById(acct.getId());   // hit -> 100
        cache.getById("missing");      // miss -> 0

        // Assert
        double rate = cache.getHitRate();
        assertTrue("hit rate is averaged between 0 and 100, got " + rate,
                rate > 0.0 && rate < 100.0);
    }
}
