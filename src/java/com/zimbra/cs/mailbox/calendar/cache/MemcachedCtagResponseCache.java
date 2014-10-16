/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class MemcachedCtagResponseCache implements CtagResponseCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected MemcachedMap<Key_, Value> memcachedLookup;

    public MemcachedCtagResponseCache() {
    }

    @PostConstruct
    public void init() {
        memcachedLookup = new MemcachedMap<>(memcachedClient, new Serializer());
    }

    @Override
    public Value get(Key key) throws ServiceException {
        return memcachedLookup.get(new Key_(key.getFirst(), key.getSecond(), key.getThird()));
    }

    @Override
    public void put(Key key, Value value) throws ServiceException {
        memcachedLookup.put(new Key_(key.getFirst(), key.getSecond(), key.getThird()), value);
    }


    private static class Serializer implements MemcachedSerializer<Value> {

        public Object serialize(Value value) throws ServiceException {
            return value.encodeMetadata().toString();
        }

        public Value deserialize(Object obj) throws ServiceException {
            Metadata meta = new Metadata((String) obj);
            return new Value(meta);
        }
    }


    // CTAG response cache key is account + client (User-Agent) + root folder
    private static class Key_ implements MemcachedKey {
        private String mAccountId;
        private String mUserAgent;
        private int mRootFolderId;
        private String mKeyVal;

        public Key_(String accountId, String userAgent, int rootFolderId) {
            mAccountId = accountId;
            mUserAgent = userAgent;
            mRootFolderId = rootFolderId;
            mKeyVal = String.format("%s:%s:%d", mAccountId, mUserAgent, mRootFolderId);
        }

        public boolean equals(Object other) {
            if (other instanceof Key_) {
                Key_ otherKey = (Key_) other;
                return mKeyVal.equals(otherKey.mKeyVal);
            }
            return false;
        }

        public int hashCode() {
            return mKeyVal.hashCode();
        }

        // MemcachedKey interface
        public String getKeyPrefix() { return MemcachedKeyPrefix.CALDAV_CTAG_RESPONSE; }
        public String getKeyValue() { return mKeyVal; }
    }
}
