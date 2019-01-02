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

import java.util.Collection;

import org.redisson.api.RSet;

import com.zimbra.cs.mailbox.TransactionAwareSet;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

public class RedisBackedSet<E> extends TransactionAwareSet<E> {

    public RedisBackedSet(RSet<E> redisSet, TransactionCacheTracker cacheTracker,
            ReadPolicy readPolicy, WritePolicy writePolicy, CachePolicy cachePolicy) {
        super(redisSet.getName(), cacheTracker,
                new GreedySetGetter<>(redisSet.getName(), cachePolicy, () -> redisSet.readAll()),
                readPolicy, writePolicy);
    }

    public Collection<E> values() {
        return data();
    }
}
