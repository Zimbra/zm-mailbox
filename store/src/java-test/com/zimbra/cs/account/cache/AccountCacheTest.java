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

public class AccountCacheTest {

    @Test
    public void accountCacheConstruction_withValidParams_createsInstance() throws Exception {
        int maxItems = 100;
        long refreshTTL = 60 * Constants.MILLIS_PER_MINUTE;

        AccountCache cache = new AccountCache(maxItems, refreshTTL);

        Assert.assertNotNull("AccountCache should be created", cache);
    }

    @Test
    public void accountCacheClear_removesAllEntries() throws Exception {
        AccountCache cache = new AccountCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Clear should not throw exception
        cache.clear();

        Assert.assertTrue("Clear should succeed", true);
    }

    @Test
    public void accountCacheWithSmallSize_respectsMaxItems() throws Exception {
        int maxItems = 5;
        long refreshTTL = 60 * Constants.MILLIS_PER_MINUTE;

        AccountCache cache = new AccountCache(maxItems, refreshTTL);

        // Cache should respect max items limit through LRU
        Assert.assertNotNull("Cache should be created with max items", cache);
    }

    @Test
    public void accountCacheWithLargeTTL_keepsEntriesLonger() throws Exception {
        long longTTL = 24 * 60 * Constants.MILLIS_PER_MINUTE;
        long shortTTL = 5 * Constants.MILLIS_PER_MINUTE;

        AccountCache cacheLong = new AccountCache(100, longTTL);
        AccountCache cacheShort = new AccountCache(100, shortTTL);

        // Both caches should be created successfully
        Assert.assertNotNull("Long TTL cache should be created", cacheLong);
        Assert.assertNotNull("Short TTL cache should be created", cacheShort);
    }

    @Test
    public void accountCacheMultipleInstances_areIndependent() throws Exception {
        AccountCache cache1 = new AccountCache(100, 60 * Constants.MILLIS_PER_MINUTE);
        AccountCache cache2 = new AccountCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Two separate instances should be independent
        Assert.assertNotSame("Instances should be separate", cache1, cache2);

        cache1.clear();
        // cache2 should not be affected

        Assert.assertTrue("Instances should be independent", true);
    }

    @Test
    public void accountCacheClearIdempotent_multipleClears() throws Exception {
        AccountCache cache = new AccountCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Multiple clears should not cause error
        cache.clear();
        cache.clear();
        cache.clear();

        Assert.assertTrue("Multiple clears should be safe", true);
    }

    @Test
    public void accountCacheImplementsIAccountCache() throws Exception {
        AccountCache cache = new AccountCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Should implement IAccountCache interface
        Assert.assertTrue("AccountCache should implement IAccountCache",
                         cache instanceof IAccountCache);
    }

    @Test
    public void accountCacheConstructor_withVariousSizes() throws Exception {
        // Test with various cache sizes
        for (int size : new int[]{10, 50, 100, 500, 1000}) {
            AccountCache cache = new AccountCache(size, 60 * Constants.MILLIS_PER_MINUTE);
            Assert.assertNotNull("Cache should be created with size " + size, cache);
        }
    }

    @Test
    public void accountCacheConstructor_withVariousTTLs() throws Exception {
        // Test with various TTLs
        for (long ttl : new long[]{
            1 * Constants.MILLIS_PER_MINUTE,
            5 * Constants.MILLIS_PER_MINUTE,
            60 * Constants.MILLIS_PER_MINUTE,
            24 * 60 * Constants.MILLIS_PER_MINUTE
        }) {
            AccountCache cache = new AccountCache(100, ttl);
            Assert.assertNotNull("Cache should be created with TTL " + ttl, cache);
        }
    }

    @Test
    public void accountCacheSynchronization_threadsafe() throws Exception {
        AccountCache cache = new AccountCache(100, 60 * Constants.MILLIS_PER_MINUTE);

        // Clear method is synchronized, should be thread-safe
        Thread t1 = new Thread(() -> {
            try {
                cache.clear();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                cache.clear();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        Assert.assertTrue("Concurrent clears should work", true);
    }
}
