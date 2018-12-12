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

import org.redisson.api.RMap;

import com.zimbra.cs.mailbox.TransactionAwareMap;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

/**
 * This class tracks changes to an underlying redis map that get batched over the course of a transaction
 */
public class RedisBackedMap<K, V> extends TransactionAwareMap<K, V> {

    protected RMap<K, V> redisMap;

    public RedisBackedMap(RMap<K, V> redisMap, TransactionCacheTracker tracker) {
        this(redisMap, tracker, true);
    }

    public RedisBackedMap(RMap<K, V> redisMap, TransactionCacheTracker tracker, boolean greedyLoad) {
        super(redisMap.getName(), tracker, greedyLoad ?
            new GreedyMapGetter<>(redisMap.getName(), () -> redisMap.readAllMap()) :
            new LazyMapGetter<>(redisMap.getName(), (key) -> redisMap.get(key)));
        this.redisMap = redisMap;
    }

    public RMap<K, V> getMap() {
        return redisMap;
    }
}
