package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.zimbra.common.util.ZimbraLog;

public abstract class TransactionAware<V, C extends TransactionAware.Change> {

    private String name;
    private TransactionCacheTracker tracker;
    private ThreadLocal<V> localCache;
    private Changes changes;

    public TransactionAware(TransactionCacheTracker cacheTracker, String name) {
        this.tracker = cacheTracker;
        this.name = name;
        this.localCache = new ThreadLocal<>();
        this.changes = new Changes();
    }

    public String getName() {
        return name;
    }

    public void clearLocalCache() {
        localCache.set(null);
    }

    public boolean hasChanges() {
        return changes.hasChanges();
    }

    protected V getLocalCache() {
        if (localCache.get() == null) {
            ZimbraLog.cache.trace("initializing local cache for %s %s in thread %s", getName(), getClass().getSimpleName(), Thread.currentThread().getName());
            localCache.set(initLocalCache());
            tracker.addToTracker(this);
        }
        return localCache.get();
    }

    protected abstract V initLocalCache();

    protected void addChange(C change) {
        changes.addChange(change);
    }

    public Changes getChanges() {
        return changes;
    }

    public List<C> getChangeList() {
        return changes.getChanges();
    }

    public void resetChanges() {
        if (changes.hasChanges()) {
            ZimbraLog.cache.warn("clearing uncommitted changes: %s", changes.toString());
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
            ZimbraLog.cache.trace("resetting changes for %s", getName());
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
            return MoreObjects.toStringHelper(this)
                    .add("name", getName())
                    .add("changes", Joiner.on(",").join(changes)).toString();
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
}
