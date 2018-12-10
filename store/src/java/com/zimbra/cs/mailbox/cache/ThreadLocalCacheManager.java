/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.cs.mailbox.cache;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;

public class ThreadLocalCacheManager {

    public static ThreadLocalCacheManager instance = new ThreadLocalCacheManager();

    private Set<ThreadLocalCache<?,?>> caches;

    private ThreadLocalCacheManager() {
        caches = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
        startSweeperThread();
    }

    private void startSweeperThread() {
        long intervalSecs = LC.threadlocal_cache_cleanup_interval_seconds.longValue();
        ZimbraLog.cache.info("starting CacheSweeper thread with interval of %s seconds", intervalSecs);
        long intervalMillis = intervalSecs * 1000;
        Zimbra.sTimer.schedule(new CacheSweeper(), intervalMillis, intervalMillis);
    }

    public static ThreadLocalCacheManager getInstance() {
        return instance;
    }
    public <V, G extends ThreadLocalCache.CachedObject<V>> ThreadLocalCache<V, G> newThreadLocalCache(String objectName, String cacheType) {
        Cache<Thread, G> transactionCache = buildCache(objectName, LC.transaction_threadlocal_cache_expiry_seconds.intValue(), 0, cacheType, true);
        Cache<Thread, G> nonTransactionCache = buildCache(objectName,
                LC.outside_transaction_threadlocal_cache_expiry_seconds.intValue(),
                LC.outside_transaction_threadlocal_cache_max_size.intValue(),
                cacheType, false);
        ThreadLocalCache<V, G> cache = new ThreadLocalCache<>(objectName, transactionCache, nonTransactionCache);
        caches.add(cache);
        return cache;
    }

    private <G> Cache<Thread, G> buildCache(String objectName, long expireAfterAccess, long maxSize, String cacheType, boolean transaction) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder.weakKeys();
        if (maxSize > 0) {
            builder.maximumSize(maxSize);
        }
        if (expireAfterAccess > 0) {
            builder.expireAfterAccess(expireAfterAccess, TimeUnit.SECONDS);
        }
        builder.removalListener(getRemovalListener(objectName, cacheType, transaction));
        return builder.build();
    }

    private <G> RemovalListener<Thread, G> getRemovalListener(String objectName, String cacheType, boolean transaction) {
        return new RemovalListener<Thread, G>() {

            @Override
            public void onRemoval(RemovalNotification<Thread, G> notification) {
                if (ZimbraLog.cache.isTraceEnabled()) {
                    Thread key = notification.getKey();
                    String msg = "removing %s from " + (transaction ? "transaction" : "non-transaction") + " cache %s, object=%s, cause=%s";
                    ZimbraLog.cache.trace(msg, key, cacheType, objectName, notification.getCause());
                }
            }
        };
    }

    private class CacheSweeper extends TimerTask {

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            try {
                Iterator<ThreadLocalCache<?,?>> iter = caches.iterator();
                while (iter.hasNext()) {
                    iter.next().cleanUp();
                }
                ZimbraLog.cache.info("cleaned up %s caches in %s ms", caches.size(), System.currentTimeMillis() - start);
            } catch (Exception e) {
                ZimbraLog.cache.warn("caught exception in CacheSweeper", e);
            }
        }
    }
}
