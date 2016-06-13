package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedItemCache {
    
    public static class ItemCacheKey implements MemcachedKey {
        private String keyStr;

        public ItemCacheKey(Mailbox mbox, int itemId) {
            keyStr = mbox.getAccountId() + ":" + mbox.getItemcacheCheckpoint() + ":" + itemId;
        }

        public boolean equals(Object other) {
            if (other instanceof ItemCacheKey) {
                ItemCacheKey otherKey = (ItemCacheKey) other;
                return keyStr.equals(otherKey.keyStr);
            }
            return false;
        }

        public int hashCode() {
            return keyStr.hashCode();
        }

        // MemcachedKey interface
        public String getKeyPrefix() { return MemcachedKeyPrefix.MBOX_MAILITEM; }
        public String getKeyValue() { return keyStr; }
    }

    public static class ItemCacheUuidKey implements MemcachedKey {
        private String keyStr;

        public ItemCacheUuidKey(Mailbox mbox, String uuid) {
            keyStr = mbox.getAccountId() + ":" + mbox.getItemcacheCheckpoint() + ":" + uuid;
        }

        public boolean equals(Object other) {
            if (other instanceof ItemCacheUuidKey) {
                ItemCacheUuidKey otherKey = (ItemCacheUuidKey) other;
                return keyStr.equals(otherKey.keyStr);
            }
            return false;
        }

        public int hashCode() {
            return keyStr.hashCode();
        }

        // MemcachedKey interface
        public String getKeyPrefix() { return MemcachedKeyPrefix.MBOX_MAILITEM; }
        public String getKeyValue() { return keyStr; }
    }
    
    private static MemcachedItemCache sTheInstance = new MemcachedItemCache();

    private MemcachedMap<ItemCacheKey, Metadata> memcachedLookup;
    private MemcachedMap<ItemCacheUuidKey, Integer> memcachedUuidLookup;

    public static MemcachedItemCache getInstance() { return sTheInstance; }

    MemcachedItemCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        memcachedLookup = new MemcachedMap<ItemCacheKey, Metadata>(memcachedClient, new MailItemSerializer(), false);
        memcachedUuidLookup = new MemcachedMap<ItemCacheUuidKey, Integer>(memcachedClient, new IntegerSerializer(), false);
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

    /**
     * Retrieves the item from memcached cache
     * @param mbox
     * @param itemId
     * @return item if present, else null
     * @throws ServiceException
     */
    public MailItem get(Mailbox mbox, int itemId) throws ServiceException {
        ItemCacheKey key = new ItemCacheKey(mbox, itemId);
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
        ItemCacheUuidKey key = new ItemCacheUuidKey(mbox, uuid);
        Integer itemId = memcachedUuidLookup.get(key);
        if (itemId != null) {
            return get(mbox, itemId);
        } else {
            return null;
        }
    }

    public void put(Mailbox mbox, MailItem item) throws ServiceException {
        ItemCacheKey key = new ItemCacheKey(mbox, item.getId());
        memcachedLookup.put(key, item.serializeUnderlyingData());
        ItemCacheUuidKey uuidKey = new ItemCacheUuidKey(mbox, item.getUuid());
        memcachedUuidLookup.put(uuidKey, item.getId());
    }
    
    public MailItem remove(Mailbox mbox, int itemId) throws ServiceException {
        MailItem item = get(mbox, itemId);
        if (item != null) {
            ItemCacheKey key = new ItemCacheKey(mbox, itemId);
            memcachedLookup.remove(key);
            ItemCacheUuidKey uuidKey = new ItemCacheUuidKey(mbox, item.getUuid());
            memcachedUuidLookup.remove(uuidKey);
            return item;
        } else {
            return null;
        }
    }

    public void purgeMailbox(Mailbox mbox) {
        // nothing to do
    }
}

