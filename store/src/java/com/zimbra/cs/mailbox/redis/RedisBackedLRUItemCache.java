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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RScript.Mode;
import org.redisson.api.RScript.ReturnType;

import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.TransactionAwareLRUItemCache;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

public class RedisBackedLRUItemCache extends TransactionAwareLRUItemCache {

    public RedisBackedLRUItemCache(RScoredSortedSet<Integer> lruItemSet, RMap<Integer, String> itemCacheMap, TransactionCacheTracker cacheTracker) {
        super(lruItemSet.getName(), cacheTracker,
                new LRUCacheGetter(lruItemSet.getName(),
                        new LRUCacheTrimScript(lruItemSet.getName(), itemCacheMap.getName())));
    }

    public static class LRUCacheTrimScript implements LRUCacheTrimAction {

        private String lruItemSetName;
        private String itemMapName;

        public LRUCacheTrimScript(String lruItemSetName, String itemMapName) {
            this.lruItemSetName = lruItemSetName;
            this.itemMapName = itemMapName;
        }

        @Override
        public Collection<Integer> trimCache(int numItemsToKeep) {
            RScript script = RedissonClientHolder.getInstance().getRedissonClient().getScript();
            String luaScript =
                    "local trimmedIds = redis.call('zrange', KEYS[1], 0, -1 * (ARGV[1]+1)); "
                    + "for _,id in ipairs(trimmedIds) do "
                    +   "redis.call('hdel', KEYS[2], id); "
                    +   "redis.call('zrem', KEYS[1], id) "
                    + "end; "
                    + "return trimmedIds;";

            List<Object> keys = Arrays.<Object>asList(lruItemSetName, itemMapName);
            return script.eval(Mode.READ_WRITE, luaScript, ReturnType.MULTI, keys, numItemsToKeep);
        }

    }
}
