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
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

import org.codehaus.jackson.map.ObjectMapper;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedMailboxDataCache implements MailboxDataCache {
    MemcachedMap<Key, Mailbox.MailboxData> memcachedLookup;

    public MemcachedMailboxDataCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        memcachedLookup = new MemcachedMap<Key, Mailbox.MailboxData>(memcachedClient, new MailboxDataSerializer(), true);
    }

    @Override
    public Mailbox.MailboxData get(Mailbox mbox) throws ServiceException {
        Key key = new Key(mbox);
        return memcachedLookup.get(key);
    }

    @Override
    public void put(Mailbox mbox, Mailbox.MailboxData mailboxData) throws ServiceException {
        Key key = new Key(mbox);
        memcachedLookup.put(key, mailboxData);
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        Key key = new Key(mbox);
        memcachedLookup.remove(key);
    }


    private static class Key implements MemcachedKey {
        private String keyStr;

        public Key(Mailbox mbox) {
            keyStr = mbox.getAccountId();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Key) {
                Key otherKey = (Key) other;
                return keyStr.equals(otherKey.keyStr);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return keyStr.hashCode();
        }

        @Override
        public String getKeyPrefix() { return MemcachedKeyPrefix.MBOX_DATA; }
        @Override
        public String getKeyValue() { return keyStr; }
    }


    static class MailboxDataSerializer implements MemcachedSerializer<Mailbox.MailboxData> {
        private ObjectMapper mapper = new ObjectMapper();

        MailboxDataSerializer() {
        }

        @Override
        public Object serialize(Mailbox.MailboxData value) throws ServiceException {
            try {
                return mapper.writer().writeValueAsString(value);
            } catch (Exception e) {
                throw ServiceException.PARSE_ERROR("failed serializing MailboxData for cache", e);
            }
        }

        @Override
        public Mailbox.MailboxData deserialize(Object obj) throws ServiceException {
            try {
                return mapper.readValue(obj.toString(), Mailbox.MailboxData.class);
            } catch (Exception e) {
                throw ServiceException.PARSE_ERROR("failed serializing MailboxData for cache", e);
            }
        }
    }
}
