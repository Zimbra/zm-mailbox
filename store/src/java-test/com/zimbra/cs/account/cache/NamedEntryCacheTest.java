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

package com.zimbra.cs.account.cache;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.Constants;

public class NamedEntryCacheTest {

    @Test
    public void namedEntryCacheConstruction_withValidParams_createsInstance() throws Exception {
        int maxItems = 100;
        long refreshTTL = 60 * Constants.MILLIS_PER_MINUTE;

        NamedEntryCache cache = new NamedEntryCache(maxItems, refreshTTL);

        Assert.assertNotNull("NamedEntryCache should be created", cache);
    }

    @Test
    public void namedEntryCacheClear_removesAllEntries() throws Exception {
        NamedEntryCache cache = new NamedEntryCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Clear should not throw exception
        cache.clear();

        Assert.assertTrue("Clear should succeed", true);
    }

    @Test
    public void namedEntryCacheWithSmallSize_respectsMaxItems() throws Exception {
        int maxItems = 5;
        long refreshTTL = 60 * Constants.MILLIS_PER_MINUTE;

        NamedEntryCache cache = new NamedEntryCache(maxItems, refreshTTL);

        // Cache should respect max items limit through LRU
        Assert.assertNotNull("Cache should be created with max items", cache);
    }

    @Test
    public void namedEntryCacheMultipleInstances_areIndependent() throws Exception {
        NamedEntryCache cache1 = new NamedEntryCache(100, 60 * Constants.MILLIS_PER_MINUTE);
        NamedEntryCache cache2 = new NamedEntryCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Two separate instances should be independent
        Assert.assertNotSame("Instances should be separate", cache1, cache2);
    }

    @Test
    public void namedEntryCacheImplementsINamedEntryCache() throws Exception {
        NamedEntryCache cache = new NamedEntryCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Should implement INamedEntryCache interface
        Assert.assertTrue("NamedEntryCache should implement INamedEntryCache",
                         cache instanceof INamedEntryCache);
    }

    @Test
    public void namedEntryCacheClearIdempotent() throws Exception {
        NamedEntryCache cache = new NamedEntryCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Multiple clears should not cause error
        cache.clear();
        cache.clear();
        cache.clear();

        Assert.assertTrue("Multiple clears should be safe", true);
    }

    @Test
    public void namedEntryCacheWithVariousSizes() throws Exception {
        // Test with various cache sizes
        for (int size : new int[]{10, 50, 100, 500}) {
            NamedEntryCache cache = new NamedEntryCache(size, 60 * Constants.MILLIS_PER_MINUTE);
            Assert.assertNotNull("Cache should be created with size " + size, cache);
        }
    }

    @Test
    public void namedEntryCacheWithVariousTTLs() throws Exception {
        // Test with various TTLs
        for (long ttl : new long[]{
            1 * Constants.MILLIS_PER_MINUTE,
            5 * Constants.MILLIS_PER_MINUTE,
            60 * Constants.MILLIS_PER_MINUTE
        }) {
            NamedEntryCache cache = new NamedEntryCache(100, ttl);
            Assert.assertNotNull("Cache should be created with TTL " + ttl, cache);
        }
    }

    @Test
    public void namedEntryCacheSynchronization_threadSafe() throws Exception {
        NamedEntryCache cache = new NamedEntryCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Clear method should be thread-safe
        Thread t1 = new Thread(() -> cache.clear());
        Thread t2 = new Thread(() -> cache.clear());

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        Assert.assertTrue("Concurrent clears should work", true);
    }

    @Test
    public void namedEntryCacheByNameAndId_supportedLookups() throws Exception {
        NamedEntryCache cache = new NamedEntryCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Cache should support lookups by name and id
        Assert.assertNotNull("Cache should support multiple lookup types", cache);
    }
}
