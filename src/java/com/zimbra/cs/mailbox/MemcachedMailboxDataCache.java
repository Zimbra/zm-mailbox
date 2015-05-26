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

import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.StringBasedMemcachedKey;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedMailboxDataCache implements MailboxDataCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected MemcachedMap<MemcachedKey, Mailbox.MailboxData> memcachedLookup;

    public MemcachedMailboxDataCache() {
    }

    @PostConstruct
    public void init() {
        memcachedLookup = new MemcachedMap<>(memcachedClient, new MailboxDataSerializer(), true);
    }

    protected MemcachedKey key(Mailbox mbox) {
        return new StringBasedMemcachedKey(MemcachedKeyPrefix.MBOX_DATA, mbox.getAccountId());
    }

    @Override
    public Mailbox.MailboxData get(Mailbox mbox) throws ServiceException {
        return memcachedLookup.get(key(mbox));
    }

    @Override
    public void put(Mailbox mbox, Mailbox.MailboxData mailboxData) throws ServiceException {
        memcachedLookup.put(key(mbox), mailboxData);
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        memcachedLookup.remove(key(mbox));
    }


    static class MailboxDataSerializer implements MemcachedSerializer<Mailbox.MailboxData> {
        private ObjectMapper mapper = new ObjectMapper();

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
