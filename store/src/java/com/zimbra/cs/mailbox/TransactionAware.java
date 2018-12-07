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

package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public abstract class TransactionAware<V, C extends TransactionAware.Change> {

    private String name;
    private TransactionCacheTracker tracker;
    private final ThreadLocal<Changes> threadChanges;
    private final Getter<V,?> transactionAccessor;

    public TransactionAware(String name, TransactionCacheTracker cacheTracker, Getter<V,?> getter) {
        this.tracker = cacheTracker;
        this.name = name;
        this.threadChanges = new ThreadLocal<>();
        this.transactionAccessor = getter;
    }

    public String getName() {
        return name;
    }

    public boolean isInTransaction() {
        return tracker.isInTransaction();
    }

    protected V data() {
        return transactionAccessor.getObject(this);
    }

    public void clearLocalCache() {
        transactionAccessor.clearCache(isInTransaction());
    }

    public boolean hasChanges() {
        return getChanges().hasChanges();
    }

    protected void addChange(C change) {
        getChanges().addChange(change);
    }

    public Changes getChanges() {
        Changes changes = threadChanges.get();
        if (changes != null) {
            return changes;
        }
        threadChanges.set(new Changes());
        return threadChanges.get();
    }

    public List<C> getChangeList() {
        return getChanges().getChanges();
    }

    public void resetChanges() {
        Changes changes = getChanges();
        if (changes.hasChanges()) {
            ZimbraLog.cache.warn("clearing %d uncommitted changes for %s", changes.size(), getName());
        }
        changes.reset();
    }


    public static enum ChangeType {
        CLEAR, REMOVE, //used by both set and map
        MAP_PUT, MAP_PUT_ALL,
        SET_ADD, SET_ADD_ALL, SET_REMOVE_ALL, SET_RETAIN_ALL;
    }

    public class Changes {
        private List<C> changes = new ArrayList<>();

        public Changes() {
            reset();
        }

        public void reset() {
            if (ZimbraLog.cache.isTraceEnabled()) {
                ZimbraLog.cache.trace("resetting changes for %s", getName());
            }
            changes.clear();
        }

        public void addChange(C change) {
            changes.add(change);
        }

        public boolean hasChanges() {
            return !changes.isEmpty();
        }

        public List<C> getChanges() {
            return changes;
        }

        public String getName() {
            return TransactionAware.this.getName();
        }

        public int size() {
            return changes.size();
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this).add("name", getName());
            if (changes.size() > 20) {
                helper.add("changes.size()", changes.size());
            } else {
                helper.add("changes", changes);
            }
            return helper.toString();
        }
    }

    public static abstract class Change {

        private ChangeType changeType;

        public Change(ChangeType changeType) {
            this.changeType = changeType;
        }

        public ChangeType getChangeType() {
            return changeType;
        }

        protected ToStringHelper toStringHelper() {
            return MoreObjects.toStringHelper(this);
        }
    }

    /**
     * Alternative to ThreadLocal to avoid memory leaks
     */
    private static class LocalCache<V, G extends CachedObject<V>> {
        private Cache<Thread, G> transactionCache;
        private Cache<Thread, G> nonTransactionCache;
        private Getter<V, G> getter;

        private LocalCache(Getter<V, G> getter) {
            this.getter = getter;
            transactionCache = buildTransactionCache();
            nonTransactionCache = buildNonTransactionCache();
        }

        private Cache<Thread, G> buildTransactionCache() {
            CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
            builder.weakKeys();
            builder.removalListener(new RemovalListener<Thread, G>() {

                @Override
                public void onRemoval(RemovalNotification<Thread, G> notification) {
                    Thread key = notification.getKey();
                    if (ZimbraLog.cache.isTraceEnabled()) {
                        ZimbraLog.cache.trace("removing %s from transactionCache for %s, cause=%s", key, getter, notification.getCause());
                    }
                }

            });
            return builder.build();
        }


        private Cache<Thread, G> buildNonTransactionCache() {
            CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
            builder.weakKeys();
            builder.maximumSize(LC.outside_transaction_threadlocal_cache_max_size.intValue());
            builder.expireAfterAccess(LC.outside_transaction_threadlocal_cache_expiry_seconds.intValue(), TimeUnit.SECONDS);
            builder.removalListener(new RemovalListener<Thread, G>() {

                @Override
                public void onRemoval(RemovalNotification<Thread, G> notification) {
                    if (ZimbraLog.cache.isTraceEnabled()) {
                        Thread key = notification.getKey();
                        ZimbraLog.cache.trace("removing %s from nonTransactionCache for %s, cause=%s", key, getter.getObjectName(), notification.getCause());
                    }
                }

            });
            return builder.build();
        }

        private Cache<Thread, G> getCache(boolean inTransaction) {
            return inTransaction ? transactionCache : nonTransactionCache;
        }

        private G get(boolean inTransaction, Callable<G> callable) {
            Thread thread = Thread.currentThread();
            try {
                return getCache(inTransaction).get(thread, callable);
            } catch (ExecutionException e) {
                ZimbraLog.cache.warn("error getting value from %s for %s, returning null!", this, thread);
                return null;
            }
        }

        private void remove(boolean inTransaction) {
            Thread thread = Thread.currentThread();
            Cache<Thread, G> cache = getCache(inTransaction);
            cache.invalidate(thread);

        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("obj", getter.getObjectName())
                    .add("transactionCacheSize", transactionCache.size())
                    .add("nonTransactionCacheSize", nonTransactionCache.size())
                    .toString();
        }
    }

    protected static abstract class CachedObject<V> {

        protected String objectName;

        public CachedObject(String objectName) {
            this.objectName = objectName;
        }

        public String getObjectName() {
            return objectName;
        }

        public abstract V getObject();

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("obj", objectName).toString();
        }
    }

    protected static class GreedyCachedObject<V> extends CachedObject<V> {

        private V object;

        public GreedyCachedObject(String objectName, V object) {
            super(objectName);
            this.object = object;
        }

        @Override
        public V getObject() {
            return object;
        }

    }

    protected static abstract class Getter<V, G extends CachedObject<V>> {

        protected String objectName;
        protected LocalCache<V, G> localCache;

        public Getter(String objectName) {
            this.objectName = objectName;
            localCache = new LocalCache<V, G>(this);
        }

        public String getObjectName() {
            return objectName;
        }

        protected abstract G loadCacheValue();

        public V getObject(TransactionAware<?,?> obj) {
            Callable<G> callable = new Callable<G>() {
                @Override
                public G call() throws Exception {
                    G val = loadCacheValue();
                    if (ZimbraLog.cache.isTraceEnabled()) {
                        ZimbraLog.cache.trace("adding threadlocal value for %s for thread %s, cache=%s", this, Thread.currentThread(), localCache);
                    }
                    obj.tracker.addToTracker(obj);
                    return val;
                }
            };
            return localCache.get(obj.isInTransaction(), callable).getObject();
        }

        public void clearCache(boolean inTransaction) {
            localCache.remove(inTransaction);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("obj", objectName).toString();
        }
    }

    protected static abstract class GreedyGetter<V> extends Getter<V, GreedyCachedObject<V>> {

        public GreedyGetter(String objectName) {
            super(objectName);
        }

        protected abstract V loadObject();

        @Override
        protected GreedyCachedObject<V> loadCacheValue() {
            return new GreedyCachedObject<>(objectName, loadObject());
        }
    }
}
