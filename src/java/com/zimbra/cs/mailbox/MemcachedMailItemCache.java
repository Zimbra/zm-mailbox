package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014 Zimbra, Inc.
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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.StringBasedMemcachedKey;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class MemcachedMailItemCache implements MailItemCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
//    protected static MemcachedMailItemCache sTheInstance = new MemcachedMailItemCache();
    protected MemcachedMap<IdKey, Metadata> memcachedLookup;
    protected MemcachedMap<UuidKey, Integer> memcachedUuidLookup;
//    public static MemcachedMailItemCache getInstance() { return sTheInstance; }

    MemcachedMailItemCache() {
    }

    @PostConstruct
    public void init() {
        memcachedLookup = new MemcachedMap<IdKey, Metadata>(memcachedClient, new MailItemSerializer(), false);
        memcachedUuidLookup = new MemcachedMap<UuidKey, Integer>(memcachedClient, new IntegerSerializer(), false);
    }

    /**
     * Retrieves the item from memcached cache
     * @param mbox
     * @param itemId
     * @return item if present, else null
     * @throws ServiceException
     */
    public MailItem get(Mailbox mbox, int itemId) throws ServiceException {
        IdKey key = new IdKey(mbox, itemId);
        Metadata meta = memcachedLookup.get(key);
        if (meta != null) {
            MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
            ud.deserialize(meta);
            return MailItem.constructItem(mbox, ud, true);
        } else {
            return null;
        }
    }

    public MailItem get(Mailbox mbox, String uuid) throws ServiceException {
        UuidKey key = new UuidKey(mbox, uuid);
        Integer itemId = memcachedUuidLookup.get(key);
        if (itemId != null) {
            return get(mbox, itemId);
        } else {
            return null;
        }
    }

    public void put(Mailbox mbox, MailItem item) throws ServiceException {
        IdKey key = new IdKey(mbox, item.getId());
        memcachedLookup.put(key, item.serializeUnderlyingData());
        UuidKey uuidKey = new UuidKey(mbox, item.getUuid());
        memcachedUuidLookup.put(uuidKey, item.getId());
    }

    public MailItem remove(Mailbox mbox, int itemId) throws ServiceException {
        MailItem item = get(mbox, itemId);
        if (item != null) {
            IdKey key = new IdKey(mbox, itemId);
            memcachedLookup.remove(key);
            UuidKey uuidKey = new UuidKey(mbox, item.getUuid());
            memcachedUuidLookup.remove(uuidKey);
            return item;
        } else {
            return null;
        }
    }

    /** @throws UnsupportedOperationException always */
    public void remove(Mailbox mbox) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }


    private static class IdKey extends StringBasedMemcachedKey {
        public IdKey(Mailbox mbox, int itemId) {
            super(MemcachedKeyPrefix.MBOX_MAILITEM, mbox.getAccountId() + ":" + mbox.getItemcacheCheckpoint() + ":" + itemId);
        }
    }

    private static class UuidKey extends StringBasedMemcachedKey {
        public UuidKey(Mailbox mbox, String uuid) {
            super(MemcachedKeyPrefix.MBOX_MAILITEM, mbox.getAccountId() + ":" + mbox.getItemcacheCheckpoint() + ":" + uuid);
        }
    }


    private static class IntegerSerializer implements MemcachedSerializer<Integer> {
        public Object serialize(Integer value) throws ServiceException { return value; }
        public Integer deserialize(Object obj) throws ServiceException { return (Integer) obj; }
    }


    private static class MailItemSerializer implements MemcachedSerializer<Metadata> {
        MailItemSerializer() {
        }

        @Override
        public Object serialize(Metadata value) {
            return value.toString();
        }

        @Override
        public Metadata deserialize(Object obj) throws ServiceException {
            return new Metadata((String) obj);
        }
    }
}
