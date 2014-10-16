/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedCtagInfoCache implements CtagInfoCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected MemcachedMap<CalendarKey, CtagInfo> mMemcachedLookup;

    public MemcachedCtagInfoCache() {
    }

    @PostConstruct
    public void init() {
        mMemcachedLookup = new MemcachedMap<CalendarKey, CtagInfo>(memcachedClient, new CtagInfoSerializer());
    }

    protected CalendarKey key(String accountId, int folderId) {
        return new CalendarKey(accountId, folderId);
    }

    protected Collection<CalendarKey> keys(Collection<Pair<String,Integer>> pairs) {
        Collection<CalendarKey> result = new ArrayList<>();
        for (Pair<String,Integer> pair: pairs) {
            result.add(key(pair.getFirst(), pair.getSecond()));
        }
        return result;
    }

    protected Map<Pair<String,Integer>, CtagInfo> toResult(Map<CalendarKey,CtagInfo> map) {
        Map<Pair<String,Integer>, CtagInfo> result = new HashMap<>();
        for (Map.Entry<CalendarKey, CtagInfo> entry: map.entrySet()) {
            result.put(new Pair<>(entry.getKey().mAccountId, entry.getKey().mFolderId), entry.getValue());
        }
        return result;
    }

    @Override
    public CtagInfo get(String accountId, int folderId) throws ServiceException {
        return mMemcachedLookup.get(key(accountId, folderId));
    }

    @Override
    public Map<Pair<String,Integer>, CtagInfo> get(List<Pair<String,Integer>> keys) throws ServiceException {
        Map<CalendarKey, CtagInfo> result = mMemcachedLookup.getMulti(keys(keys));
        return toResult(result);
    }

    @Override
    public void put(String accountId, int folderId, CtagInfo ctagInfo) throws ServiceException {
        mMemcachedLookup.put(key(accountId, folderId), ctagInfo);
    }

    @Override
    public void put(Map<Pair<String,Integer>, CtagInfo> pairs) throws ServiceException {
        Map<CalendarKey, CtagInfo> map = new HashMap<>();
        for (Map.Entry<Pair<String,Integer>, CtagInfo> entry: pairs.entrySet()) {
            map.put(new CalendarKey(entry.getKey().getFirst(), entry.getKey().getSecond()), entry.getValue());
        }
        mMemcachedLookup.putMulti(map);
    }

    @Override
    public void remove(String accountId, int folderId) throws ServiceException {
        List<Pair<String,Integer>> list = new ArrayList<>();
        list.add(new Pair<>(accountId, folderId));
        remove(list);
    }

    @Override
    public void remove(List<Pair<String,Integer>> keys) throws ServiceException {
        mMemcachedLookup.removeMulti(keys(keys));
    }

    /**
     * Queries the mailbox for all calendar folders, then removes them from the cache.
     */
    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        String accountId = mbox.getAccountId();
        List<Folder> folders = mbox.getCalendarFolders(null, SortBy.NONE);
        List<CalendarKey> keys = new ArrayList<CalendarKey>(folders.size());
        for (Folder folder : folders) {
            CalendarKey key = new CalendarKey(accountId, folder.getId());
            keys.add(key);
        }
        mMemcachedLookup.removeMulti(keys);
    }


    private static class CalendarKey implements MemcachedKey {
        private String mAccountId;
        private int mFolderId;
        private String mKeyVal;

        public CalendarKey(String accountId, int folderId) {
            mAccountId = accountId;
            mFolderId = folderId;
            mKeyVal = mAccountId + ":" + folderId;
        }

        public boolean equals(Object other) {
            if (other instanceof CalendarKey) {
                CalendarKey otherKey = (CalendarKey) other;
                return mKeyVal.equals(otherKey.mKeyVal);
            }
            return false;
        }

        public int hashCode()    { return mKeyVal.hashCode(); }
        public String toString() { return mKeyVal; }

        // MemcachedKey interface
        public String getKeyPrefix() { return MemcachedKeyPrefix.CTAGINFO; }
        public String getKeyValue() { return mKeyVal; }
    }


    private static class CtagInfoSerializer implements MemcachedSerializer<CtagInfo> {

        public CtagInfoSerializer() { }

        @Override
        public Object serialize(CtagInfo value) {
            return value.encodeMetadata().toString();
        }

        @Override
        public CtagInfo deserialize(Object obj) throws ServiceException {
            Metadata meta = new Metadata((String) obj);
            return new CtagInfo(meta);
        }
    }
}
