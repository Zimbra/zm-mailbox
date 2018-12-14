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

import java.util.Collection;

import com.zimbra.cs.mailbox.cache.LRUItemCache;
import com.zimbra.cs.mailbox.cache.ThreadLocalCache.CachedObject;

public abstract class TransactionAwareLRUItemCache extends TransactionAware<LRUItemCache, TransactionAwareLRUItemCache.LRUCacheChange> implements LRUItemCache {

    public TransactionAwareLRUItemCache(String name, TransactionCacheTracker cacheTracker, Getter<LRUItemCache, ?> getter) {
        super(name, cacheTracker, getter);
    }

    @Override
    public void markAccessed(int itemId) {
        addChange(new LRUMarkAccessedOp(itemId));
    }

    @Override
    public void remove(int itemId) {
        addChange(new LRURemoveOp(itemId));
    }

    @Override
    public Collection<Integer> trimCache(int numItemsToKeep) {
        return data().trimCache(numItemsToKeep);
    }

    public static abstract class LRUCacheChange extends TransactionAware.Change {

        private int itemId;

        public LRUCacheChange(int itemId, ChangeType changeType) {
            super(changeType);
            this.itemId = itemId;
        }

        public int getItemId() {
            return itemId;
        }

        @Override
        public String toString() {
            return toStringHelper().add("itemId", itemId).toString();
        }
    }

    public class LRUMarkAccessedOp extends LRUCacheChange {

        public LRUMarkAccessedOp(int itemId) {
            super(itemId, ChangeType.LRU_MARK_ACCESSED);
        }

    }

    public class LRURemoveOp extends LRUCacheChange {

        public LRURemoveOp(int itemId) {
            super(itemId, ChangeType.REMOVE);
        }
    }

    @FunctionalInterface
    protected static interface LRUCacheTrimAction {
        public Collection<Integer> trimCache(int numItemsToKeep);
    }

    protected static class LRUCacheCachedObject extends CachedObject<LRUItemCache> implements LRUItemCache {

        private LRUCacheTrimAction action;

        public LRUCacheCachedObject(String objectName, LRUCacheTrimAction action) {
            super(objectName);
            this.action = action;
        }

        @Override
        public void markAccessed(int itemId) {}

        @Override
        public LRUItemCache getObject() {
            return this;
        }

        @Override
        public Collection<Integer> trimCache(int numItemsToKeep) {
            return action.trimCache(numItemsToKeep);
        }

        @Override
        public void remove(int itemId) {
            //nothing to do here, since we are never loading the values into memory
        }

    }
    protected static class LRUCacheGetter extends Getter<LRUItemCache, LRUCacheCachedObject> {

        LRUCacheTrimAction action;

        public LRUCacheGetter(String objectName, LRUCacheTrimAction action) {
            super(objectName);
            this.action = action;
        }

        @Override
        protected LRUCacheCachedObject loadCacheValue() {
            return new LRUCacheCachedObject(getObjectName(), action);
        }
    }
}
