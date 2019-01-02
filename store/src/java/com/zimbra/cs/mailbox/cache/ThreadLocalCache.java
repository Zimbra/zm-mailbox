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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;
import com.zimbra.common.util.ZimbraLog;

public class ThreadLocalCache<V, G extends CachedObject<V>> {

    protected String objectName;
    private Cache<Thread, G> transactionCache;
    private Cache<Thread, G> nonTransactionCache;

    public ThreadLocalCache(String objectName, Cache<Thread, G> transactionCache, Cache<Thread, G> nonTransactionCache) {
        this.objectName = objectName;
        this.transactionCache = transactionCache;
        this.nonTransactionCache = nonTransactionCache;
    }

    protected Cache<Thread, G> getCache(boolean inTransaction) {
        return inTransaction ? transactionCache : nonTransactionCache;
    }

    public G get(boolean inTransaction, Callable<G> callable) {
        Thread thread = Thread.currentThread();
        try {
            return getCache(inTransaction).get(thread, callable);
        } catch (ExecutionException e) {
            ZimbraLog.cache.warn("error getting value from %s for %s, returning null!", this, thread);
            return null;
        }
    }

    public void remove(boolean inTransaction) {
        Thread thread = Thread.currentThread();
        Cache<Thread, G> cache = getCache(inTransaction);
        cache.invalidate(thread);
    }

    public void cleanUp() {
        if (ZimbraLog.cache.isTraceEnabled()) {
            ZimbraLog.cache.trace("cleaning up caches for %s", this);
        }
        transactionCache.cleanUp();
        nonTransactionCache.cleanUp();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("obj", objectName)
            .add("transactionCacheSize", transactionCache.size())
            .add("nonTransactionCacheSize", nonTransactionCache.size()).toString();
    }
}