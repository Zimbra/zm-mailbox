package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisFoldersAndTagsCache implements FoldersAndTagsCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected Pool<Jedis> jedisPool;

    /** Constructor */
    public RedisFoldersAndTagsCache() {
    }

    protected static String key(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_FOLDERS_TAGS + mbox.getAccountId();
    }

    @Override
    public FoldersAndTags get(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(mbox));
            if (value == null) {
                return null;
            }

            Metadata meta = new Metadata(value);
            return FoldersAndTags.decode(meta);
        }
    }

    @Override
    public void put(Mailbox mbox, FoldersAndTags foldersAndTags) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = key(mbox);
            if (expirySecs > -1) {
                jedis.setex(key, expirySecs, foldersAndTags.encode().toString());
            } else {
                jedis.set(key, foldersAndTags.encode().toString());
            }
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key(mbox));
        }
    }
}
