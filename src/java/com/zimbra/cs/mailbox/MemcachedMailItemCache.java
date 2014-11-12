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

import java.util.Date;

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
    protected MemcachedMap<IdKey, Metadata> mailItemByIdLookup;
    protected MemcachedMap<UuidKey, Integer> mailItemByUuidLookup;

    /** Constructor */
    public MemcachedMailItemCache() {
    }

    @PostConstruct
    public void init() {
        mailItemByIdLookup = new MemcachedMap<>(memcachedClient, new MailItemSerializer(), false);
        mailItemByUuidLookup = new MemcachedMap<>(memcachedClient, new IntegerSerializer(), false);
    }

    protected static String keyForCheckpoint(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_MAILITEM + mbox.getAccountId() + "-checkpoint";
    }

    protected long getCheckpoint(Mailbox mbox) throws ServiceException {
        // Get checkpoint
        String key = keyForCheckpoint(mbox);
        Object value = memcachedClient.get(key);
        long checkpoint = -1;
        if (value != null) {
            try {
                checkpoint = Long.parseLong(value.toString());
            } catch (NumberFormatException e) {}
        }
        if (checkpoint != -1) {
            return checkpoint;
        }

        // Generate & put new checkpoint
        checkpoint = new Date().getTime();
        memcachedClient.put(key, Long.toString(checkpoint), true);
        return checkpoint;
    }

    protected long incrementCheckpoint(Mailbox mbox) throws ServiceException {
        // Increment existing key
        String key = keyForCheckpoint(mbox);
        Long checkpoint = memcachedClient.incr(key);
        if (checkpoint == null) {
            throw ServiceException.TEMPORARILY_UNAVAILABLE();
        }
        if (checkpoint != -1) {
            return checkpoint;
        }

        // If increment failed or key didn't exist, generate & put a new checkpoint.
        checkpoint = new Date().getTime();
        memcachedClient.put(key, Long.toString(checkpoint), true);
        return checkpoint;
    }

    @Override
    public MailItem get(Mailbox mbox, int itemId) throws ServiceException {
        IdKey key = new IdKey(mbox, itemId);
        Metadata meta = mailItemByIdLookup.get(key);
        if (meta != null) {
            MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
            ud.deserialize(meta);
            return MailItem.constructItem(mbox, ud, true);
        } else {
            return null;
        }
    }

    @Override
    public MailItem get(Mailbox mbox, String uuid) throws ServiceException {
        UuidKey key = new UuidKey(mbox, uuid);
        Integer itemId = mailItemByUuidLookup.get(key);
        if (itemId != null) {
            return get(mbox, itemId);
        } else {
            return null;
        }
    }

    @Override
    public void put(Mailbox mbox, MailItem item) throws ServiceException {
        IdKey key = new IdKey(mbox, item.getId());
        mailItemByIdLookup.put(key, item.serializeUnderlyingData());
        UuidKey uuidKey = new UuidKey(mbox, item.getUuid());
        mailItemByUuidLookup.put(uuidKey, item.getId());
    }

    @Override
    public MailItem remove(Mailbox mbox, int itemId) throws ServiceException {
        MailItem item = get(mbox, itemId);
        if (item != null) {
            IdKey key = new IdKey(mbox, itemId);
            mailItemByIdLookup.remove(key);
            UuidKey uuidKey = new UuidKey(mbox, item.getUuid());
            mailItemByUuidLookup.remove(uuidKey);
            return item;
        } else {
            return null;
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        incrementCheckpoint(mbox);
    }


    private class IdKey extends StringBasedMemcachedKey {
        public IdKey(Mailbox mbox, int itemId) throws ServiceException {
            super(MemcachedKeyPrefix.MBOX_MAILITEM, mbox.getAccountId() + ":" + getCheckpoint(mbox) + ":" + itemId);
        }
    }

    private class UuidKey extends StringBasedMemcachedKey {
        public UuidKey(Mailbox mbox, String uuid) throws ServiceException {
            super(MemcachedKeyPrefix.MBOX_MAILITEM, mbox.getAccountId() + ":" + getCheckpoint(mbox) + ":" + uuid);
        }
    }


    private static class IntegerSerializer implements MemcachedSerializer<Integer> {
        @Override
        public Object serialize(Integer value) throws ServiceException { return value; }
        @Override
        public Integer deserialize(Object obj) throws ServiceException { return (Integer) obj; }
    }


    private static class MailItemSerializer implements MemcachedSerializer<Metadata> {
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
