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
package com.zimbra.cs.mailbox.redis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RMapAsync;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RSetAsync;
import org.redisson.api.RedissonClient;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.TransactionAware;
import com.zimbra.cs.mailbox.TransactionAware.Change;
import com.zimbra.cs.mailbox.TransactionAware.Changes;
import com.zimbra.cs.mailbox.TransactionAwareLRUItemCache;
import com.zimbra.cs.mailbox.TransactionAwareLRUItemCache.LRUCacheChange;
import com.zimbra.cs.mailbox.TransactionAwareLRUItemCache.LRUClearOp;
import com.zimbra.cs.mailbox.TransactionAwareLRUItemCache.LRUMarkAccessedOp;
import com.zimbra.cs.mailbox.TransactionAwareLRUItemCache.LRURemoveOp;
import com.zimbra.cs.mailbox.TransactionAwareMap;
import com.zimbra.cs.mailbox.TransactionAwareMap.MapChange;
import com.zimbra.cs.mailbox.TransactionAwareMap.MapPutAllOp;
import com.zimbra.cs.mailbox.TransactionAwareMap.MapPutOp;
import com.zimbra.cs.mailbox.TransactionAwareMap.MapRemoveOp;
import com.zimbra.cs.mailbox.TransactionAwareSet;
import com.zimbra.cs.mailbox.TransactionAwareSet.SetAddAllOp;
import com.zimbra.cs.mailbox.TransactionAwareSet.SetAddOp;
import com.zimbra.cs.mailbox.TransactionAwareSet.SetChange;
import com.zimbra.cs.mailbox.TransactionAwareSet.SetRemoveAllOp;
import com.zimbra.cs.mailbox.TransactionAwareSet.SetRemoveOp;
import com.zimbra.cs.mailbox.TransactionAwareSet.SetRetainAllOp;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

public class RedisCacheTracker extends TransactionCacheTracker {

    public RedisCacheTracker(Mailbox mbox) {
        super(mbox);
    }

    @Override
    public void addToTracker(TransactionAware<?,?> item) {
        if (ZimbraLog.cache.isTraceEnabled()) {
            ZimbraLog.cache.trace("adding %s to RedisCacheTracker from thread %s",
                    item.getName(), Thread.currentThread().getName());
        }
        super.addToTracker(item);
    }

    @Override
    public void transactionEnd(boolean success, boolean endChange) {
        ZimbraLog.cache.trace("RedisCacheTracker.transactionEnd(): success=%s, endChange=%s", success, endChange);
        super.transactionEnd(success, endChange);
    }

    @Override
    public void transactionBegin(boolean startChange) {
        ZimbraLog.cache.trace("RedisCacheTracker.transactionBegin(): startChange=%s", startChange);
        super.transactionBegin(startChange);
    }

    @Override
    protected void processItems(Set<TransactionAware<?, ?>> items) {
        BatchedChanges batchedChanges = new BatchedChanges();
        items.stream().forEachOrdered(item -> batchedChanges.addItem(item));
        if (!batchedChanges.hasChanges()) {
            return;
        }
        if (ZimbraLog.cache.isDebugEnabled()) {
            ZimbraLog.cache.debug("batched redis update for account %s: %s", mbox.getAccountId(), batchedChanges);
        }
        batchedChanges.execute();
    }

    @Override
    protected void clearTouchedItems(Set<TransactionAware<?,?>> items) {
        if (ZimbraLog.cache.isTraceEnabled()) {
            List<String> touched = items.stream().map(s -> s.getName()).sorted().collect(Collectors.toList());
            ZimbraLog.cache.trace("RedisCacheTracker.clearTouchedItems(): clearing touched items: %s", Joiner.on(", ").join(touched));
        }
        super.clearTouchedItems(items);
    }

    private static class BatchedChanges {

        private final RBatch batch;
        private final Map<String, List<? extends Change>> changeMap;

        public BatchedChanges() {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            batch = client.createBatch(BatchOptions.defaults());
            changeMap = new HashMap<>();
        }

        private void putBatchedMapUpdates(RMapAsync<Object, Object> batchMap, Map<Object, Object> batchedValueUpdates) {
            if (!batchedValueUpdates.isEmpty()) {
                batchMap.putAllAsync(batchedValueUpdates);
                batchedValueUpdates.clear();
            }
        }

        private void putBatchedSetUpdates(RSetAsync<Object> batchSet, Set<Object> batchedValueUpdates) {
            if (!batchedValueUpdates.isEmpty()) {
                batchSet.addAllAsync(batchedValueUpdates);
                batchedValueUpdates.clear();
            }
        }

        private void addMapChanges(String mapName, List<MapChange> changes) {
            RMapAsync<Object, Object> batchMap = batch.getMap(mapName);
            Map<Object, Object> batchedValueUpdates = new HashMap<>();
            for (MapChange op: changes) {
                switch (op.getChangeType()) {
                case CLEAR:
                    putBatchedMapUpdates(batchMap, batchedValueUpdates);
                    batchMap.deleteAsync();
                    break;
                case REMOVE:
                    putBatchedMapUpdates(batchMap, batchedValueUpdates);
                    MapRemoveOp removeOp = (MapRemoveOp) op;
                    batchMap.fastRemoveAsync(removeOp.getKey());
                    break;
                case MAP_PUT:
                    //collapse consecutive HSET operations into a single HMSET
                    MapPutOp putOp = (MapPutOp) op;
                    batchedValueUpdates.put(putOp.getKey(), putOp.getValue());
                    break;
                case MAP_PUT_ALL:
                    MapPutAllOp putAllOp = (MapPutAllOp) op;
                    batchedValueUpdates.putAll(putAllOp.getChanges());
                    break;
                default:
                    break;
                }
            }
            putBatchedMapUpdates(batchMap, batchedValueUpdates);
        }

        private void addSetChanges(String setName, List<SetChange> changes) {
            RSetAsync<Object> batchSet = batch.getSet(setName);
            Set<Object> batchedValueUpdates = new HashSet<>();
            for (SetChange op: changes) {
                switch (op.getChangeType()) {
                case CLEAR:
                    putBatchedSetUpdates(batchSet, batchedValueUpdates);
                    batchSet.deleteAsync();
                    break;
                case REMOVE:
                    putBatchedSetUpdates(batchSet, batchedValueUpdates);
                    //TODO: batch removals too
                    SetRemoveOp removeOp = (SetRemoveOp) op;
                    batchSet.removeAsync(removeOp.getValue());
                    break;
                case SET_ADD:
                    SetAddOp addOp = (SetAddOp) op;
                    batchedValueUpdates.add(addOp.getValue());
                    break;
                case SET_ADD_ALL:
                    SetAddAllOp addAllOp = (SetAddAllOp) op;
                    batchedValueUpdates.addAll(addAllOp.getValues());
                    break;
                case SET_REMOVE_ALL:
                    putBatchedSetUpdates(batchSet, batchedValueUpdates);
                    SetRemoveAllOp removeAllOp = (SetRemoveAllOp) op;
                    batchSet.removeAsync(removeAllOp.getValues());
                    break;
                case SET_RETAIN_ALL:
                    putBatchedSetUpdates(batchSet, batchedValueUpdates);
                    SetRetainAllOp retainOp = (SetRetainAllOp) op;
                    batchSet.retainAllAsync(retainOp.getValues());
                    break;
                default:
                    break;
                }
            }
            putBatchedSetUpdates(batchSet, batchedValueUpdates);
        }

        private void addLruCacheChanges(String lruCacheName, List<LRUCacheChange> changes) {
            RScoredSortedSetAsync<Object> batchSet = batch.getScoredSortedSet(lruCacheName);
            for (LRUCacheChange op: changes) {
                switch (op.getChangeType()) {
                case LRU_MARK_ACCESSED:
                    LRUMarkAccessedOp accessedOp = (LRUMarkAccessedOp) op;
                    batchSet.addAsync(System.currentTimeMillis(), accessedOp.getItemId());
                    break;
                case REMOVE:
                    LRURemoveOp removeOp = (LRURemoveOp) op;
                    batchSet.removeAsync(removeOp.getItemId());
                    break;
                case CLEAR:
                    LRUClearOp clearOp = (LRUClearOp) op;
                    batchSet.deleteAsync();
                    break;
                default:
                    break;
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void addItem(TransactionAware<?,?> item) {
            if (!item.hasChanges()) {
                return;
            }
            Changes changes = item.getChanges();
            if (item instanceof TransactionAwareMap) {
                TransactionAwareMap<?, ?> map = (TransactionAwareMap<?, ?>) item;
                List<MapChange> mapChanges = map.getChangeList();
                changeMap.put(changes.getName(), mapChanges);
                addMapChanges(map.getName(), mapChanges);
            } else if (item instanceof TransactionAwareSet) {
                TransactionAwareSet<?> set = (TransactionAwareSet<?>) item;
                List<SetChange> setChanges = set.getChangeList();
                changeMap.put(changes.getName(), setChanges);
                addSetChanges(set.getName(), setChanges);
            } else if (item instanceof TransactionAwareLRUItemCache) {
                TransactionAwareLRUItemCache lruCache = (TransactionAwareLRUItemCache) item;
                List<LRUCacheChange> setChanges = lruCache.getChangeList();
                changeMap.put(changes.getName(), setChanges);
                addLruCacheChanges(lruCache.getName(), setChanges);
            }
            changes.reset();
        }

        public void execute() {
            batch.execute();
        }

        public boolean hasChanges() {
            return !changeMap.isEmpty();
        }

        @Override
        public String toString() {
            ToStringHelper helper = MoreObjects.toStringHelper(this);
            for (Map.Entry<String, List<? extends Change>> entry: changeMap.entrySet()) {
                helper.add(entry.getKey(), Joiner.on(",").join(entry.getValue()));
            }
            return helper.toString();
        }
    }
}
