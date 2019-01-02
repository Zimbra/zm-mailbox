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

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.cache.CachedObject;
import com.zimbra.cs.mailbox.cache.GreedyCachedObject;
import com.zimbra.cs.mailbox.cache.ThreadLocalCache;
import com.zimbra.cs.mailbox.cache.ThreadLocalCacheManager;

public abstract class TransactionAware<V, C extends TransactionAware.Change> {

    private final String name;
    private final TransactionCacheTracker tracker;
    private Changes<C> changes = null;
    private ThreadLocalCache<Changes<C>, CachedChanges<C>> threadChanges = null;
    private final Getter<V,?> transactionAccessor;
    private final ReadPolicy readPolicy;
    private final WritePolicy writePolicy;

    public TransactionAware(String name, TransactionCacheTracker cacheTracker, Getter<V,?> getter,
            ReadPolicy readPolicy, WritePolicy writePolicy) {
        this(name, cacheTracker, getter, readPolicy, writePolicy, false);
    }

    public TransactionAware(String name, TransactionCacheTracker cacheTracker, Getter<V,?> getter,
            ReadPolicy readPolicy, WritePolicy writePolicy, boolean allowConcurrentWrites) {
        this.tracker = cacheTracker;
        this.name = name;
        if (allowConcurrentWrites) {
            threadChanges = ThreadLocalCacheManager.getInstance().newThreadLocalCache(name, "CHANGES");
        } else {
            changes = new Changes<>(name);
        }
        this.transactionAccessor = getter;
        this.readPolicy = readPolicy;
        this.writePolicy = writePolicy;
    }

    public String getName() {
        return name;
    }

    public boolean isInTransaction() {
        return tracker.isInTransaction();
    }

    protected V data() {
        if (!tracker.isInTransaction() && readPolicy == ReadPolicy.TRANSACTION_ONLY) {
            ZimbraLog.cache.warn("can't access %s outside of a transaction!\n%s", name, ZimbraLog.getStackTrace(20));
        }
        return transactionAccessor != null ? transactionAccessor.getObject(tracker.isInTransaction(), this) : null;
    }

    public void clearLocalCache() {
       if (transactionAccessor != null) {
              transactionAccessor.clearCache(isInTransaction());
       }
    }

    public boolean hasChanges() {
        return getChanges().hasChanges();
    }

    protected void addChange(C change) {
        if (!tracker.isInTransaction() && writePolicy == WritePolicy.TRANSACTION_ONLY) {
            ZimbraLog.cache.warn("can't write to %s outside of a transaction!\n%s", name, ZimbraLog.getStackTrace(20));
        }
        getChanges().addChange(change);
        tracker.addToTracker(TransactionAware.this);
    }

    public Changes<C> getChanges() {
        if (changes != null ) {
            return changes;
        } else {
            return threadChanges.get(isInTransaction(), new Callable<CachedChanges<C>>() {
                @Override
                public CachedChanges<C> call() throws Exception {
                    return new CachedChanges<>(new Changes<>(name));
                }
            }).getObject();
        }
    }

    public List<C> getChangeList() {
        return getChanges().getChanges();
    }

    public void resetChanges() {
        Changes<C> changes = getChanges();
        if (changes.hasChanges()) {
            ZimbraLog.cache.warn("clearing %d uncommitted changes for %s: %s", changes.size(), getName(), changes);
        }
        changes.reset();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("read", readPolicy)
                .add("write", writePolicy)
                .add("cache", transactionAccessor.cachePolicy).toString();
    }

    public static enum ChangeType {
        CLEAR, REMOVE, //used by both set and map
        MAP_PUT, MAP_PUT_ALL,
        SET_ADD, SET_ADD_ALL, SET_REMOVE_ALL, SET_RETAIN_ALL,
        LRU_MARK_ACCESSED;
    }

    public static class Changes<C extends Change> {
        private List<C> changes = new ArrayList<>();
        private String objectName;

        public Changes(String objectName) {
            this.objectName = objectName;
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
            return objectName;
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

    public static abstract class Getter<V, G extends CachedObject<V>> {

        protected String objectName;
        protected GetterValueCache<V, G> cache;
        private CachePolicy cachePolicy;

        public Getter(String objectName, CachePolicy cachePolicy) {
            this.objectName = objectName;
            this.cachePolicy = cachePolicy;
            this.cache = cachePolicy == CachePolicy.SINGLE_VALUE ? new SimpleValueCache<>(objectName) : new ThreadLocalValueCache<>(objectName);
        }

        public String getObjectName() {
            return objectName;
        }

        protected abstract G loadCacheValue();

        public V getObject(boolean inTransaction, TransactionAware<?,?> obj) {
            Callable<G> loaderCallback = new Callable<G>() {

                @Override
                public G call() throws Exception {
                    G val = loadCacheValue();
                    if (ZimbraLog.cache.isTraceEnabled()) {
                        ZimbraLog.cache.trace("caching value for %s", this);
                    }
                    if (cachePolicy == CachePolicy.THREAD_LOCAL) {
                        obj.tracker.addToTracker(obj);
                    }
                    return val;
                }
            };
            return cache.get(inTransaction, loaderCallback).getObject();
        }

        public void clearCache(boolean inTransaction) {
            cache.clear(inTransaction);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("obj", objectName).toString();
        }
    }

    public static abstract class GreedyCachingGetter<V> extends Getter<V, GreedyCachedObject<V>> {

        public GreedyCachingGetter(String objectName, CachePolicy cachePolicy) {
            super(objectName, cachePolicy);
        }

        protected abstract V loadObject();

        @Override
        protected GreedyCachedObject<V> loadCacheValue() {
            return new GreedyCachedObject<>(objectName, loadObject());
        }
    }

    public static enum ReadPolicy {
        ANYTIME, TRANSACTION_ONLY;
    }

    public static enum WritePolicy {
        ANYTIME, TRANSACTION_ONLY;
    }

    public static enum CachePolicy {
        THREAD_LOCAL, SINGLE_VALUE;
    }

    protected static abstract class GetterValueCache<V, G extends CachedObject<V>> {

        protected String objectName;

        public GetterValueCache(String objectName) {
            this.objectName = objectName;
        }

        public abstract G get(boolean inTransaction, Callable<G> loaderCallback);

        public abstract void clear(boolean inTransaction);
    }

    protected static class SimpleValueCache<V, G extends CachedObject<V>> extends GetterValueCache<V, G> {

        public SimpleValueCache(String objectName) {
            super(objectName);
        }

        private G value;

        @Override
        public G get(boolean inTransaction, Callable<G> loaderCallback) {
            if (value == null) {
                try {
                    value = loaderCallback.call();
                } catch (Exception e) {
                    ZimbraLog.cache.error("unable to invoke cache loading callback for %s!", objectName, e);
                    return null;
                }
            }
            return value;
        }

        @Override
        public void clear(boolean inTransaction) {
            value = null;
        }
    }

    protected static class ThreadLocalValueCache<V, G extends CachedObject<V>> extends GetterValueCache<V, G> {

        private ThreadLocalCache<V, G> threadLocalCache;

        public ThreadLocalValueCache(String objectName) {
            super(objectName);
            threadLocalCache = ThreadLocalCacheManager.getInstance().newThreadLocalCache(objectName, "CACHED_VALUES");
        }

        @Override
        public G get(boolean inTransaction, Callable<G> loaderCallback) {
            return threadLocalCache.get(inTransaction, loaderCallback);
        }

        @Override
        public void clear(boolean inTransaction) {
            threadLocalCache.remove(inTransaction);
        }
    }

    private static class CachedChanges<C extends Change> extends CachedObject<Changes<C>> {

        private Changes<C> changes;

        public CachedChanges(Changes<C> changes) {
            super(changes.getName());
            this.changes = changes;
        }

        @Override
        public Changes<C> getObject() {
            return changes;
        }
    }
}
