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

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.BigByteArrayMemcachedMap;
import com.zimbra.common.util.memcached.ByteArraySerializer;
import com.zimbra.common.util.memcached.StringBasedMemcachedKey;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedCalendarDataCache implements CalendarDataCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected BigByteArrayMemcachedMap<Key_, CalendarData> memcachedLookup;


    public MemcachedCalendarDataCache() {
    }

    @PostConstruct
    public void init() {
        memcachedLookup = new BigByteArrayMemcachedMap<>(memcachedClient, new Serializer());
    }

    protected Key_ key(Key key) {
        return new Key_(key.getAccountId(), key.getFolderId());
    }

    protected Set<Key_> keys(Set<Key> keys) {
        Set<Key_> set = new HashSet<>();
        for (Key key: keys) {
            set.add(key(key));
        }
        return set;
    }

    @Override
    public CalendarData get(Key key) throws ServiceException {
        return memcachedLookup.get(key(key));
    }

    @Override
    public void put(Key key, CalendarData value) throws ServiceException {
        memcachedLookup.put(key(key), value);
    }

    @Override
    public void remove(Key key) throws ServiceException {
        memcachedLookup.remove(key(key));
    }

    @Override
    public void remove(Set<Key> keys) throws ServiceException {
        memcachedLookup.removeMulti(keys(keys));
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        String accountId = mbox.getAccountId();
        List<Folder> folders = mbox.getCalendarFolders(null, SortBy.NONE);
        Set<Key> keys = new HashSet<>(folders.size());
        for (Folder folder : folders) {
            keys.add(new Key(accountId, folder.getId()));
        }
        remove(keys);
    }


    private static class Serializer implements ByteArraySerializer<CalendarData> {
        Serializer() { }

        @Override
        public byte[] serialize(CalendarData value) {
            try {
                return value.encodeMetadata().toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                ZimbraLog.calendar.warn("Unable to serialize CalendarData for cache", e);
                return null;
            }
        }

        @Override
        public CalendarData deserialize(byte[] bytes) throws ServiceException {
            if (bytes != null) {
                String encoded;
                try {
                    encoded = new String(bytes, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    ZimbraLog.calendar.warn("Unable to deserialize CalendarData for cache", e);
                    return null;
                }
                Metadata meta = new Metadata(encoded);
                return new CalendarData(meta);
            } else {
                return null;
            }
        }
    }


    private static class Key_ extends StringBasedMemcachedKey {
        Key_(String accountId, int folderId) {
            super(MemcachedKeyPrefix.CAL_SUMMARY, accountId + ":" + folderId);
        }
    }
}
