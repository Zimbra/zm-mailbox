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

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.zimbra.common.util.ZimbraLog;

public abstract class TransactionAware<V, C extends TransactionAware.Change> {

    private final String name;
    private final TransactionCacheTracker tracker;
    private final ThreadLocal<V> localCache;
    /**  This can be changed from read transactions, for instance from
     * loadFoldersAndTags or even just when an item is first cached, hence
     * why it is ThreadLocal */
    private final ThreadLocal<Changes> threadChanges;

    public TransactionAware(TransactionCacheTracker cacheTracker, String name) {
        this.tracker = cacheTracker;
        this.name = name;
        this.localCache = new ThreadLocal<>();
        this.threadChanges = new ThreadLocal<>();
    }

    public String getName() {
        return name;
    }

    public void clearLocalCache() {
        localCache.set(null);
    }

    protected V getLocalCache() {
        if (localCache.get() == null) {
            if (ZimbraLog.cache.isTraceEnabled()) {
                ZimbraLog.cache.trace("initializing local cache for %s %s in thread %s",
                        getName(), getClass().getSimpleName(), Thread.currentThread().getName());
            }
            localCache.set(initLocalCache());
            tracker.addToTracker(this);
        }
        return localCache.get();
    }

    protected abstract V initLocalCache();

    public Changes getChanges() {
        Changes changes = threadChanges.get();
        if (changes != null) {
            return changes;
        }
        threadChanges.set(new Changes());
        return threadChanges.get();
    }

    public boolean hasChanges() {
        return getChanges().hasChanges();
    }

    protected void addChange(C change) {
        getChanges().addChange(change);
    }

    /**
     * @return the current state of changes
     */
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
        private final List<C> changes = new ArrayList<>();

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
}
