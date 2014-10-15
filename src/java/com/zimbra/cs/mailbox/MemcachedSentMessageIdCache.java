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

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedSentMessageIdCache implements SentMessageIdCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected MemcachedMap<SentMessageIdByMsgIdHeaderKey, Integer> sentMessageIdByMsgIdLookup;

    /** Constructor */
    public MemcachedSentMessageIdCache() {
    }

    @PostConstruct
    public void init() {
        sentMessageIdByMsgIdLookup = new MemcachedMap<>(memcachedClient, new IntegerSerializer(), false);
    }

    @Override
    public Integer get(Mailbox mbox, String msgidHeader) throws ServiceException {
        return sentMessageIdByMsgIdLookup.get(new SentMessageIdByMsgIdHeaderKey(mbox, msgidHeader));
    }

    @Override
    public void put(Mailbox mbox, String msgidHeader, int id) throws ServiceException {
        sentMessageIdByMsgIdLookup.put(new SentMessageIdByMsgIdHeaderKey(mbox, msgidHeader), id);
    }


    public static class SentMessageIdByMsgIdHeaderKey implements MemcachedKey {
        private String keyStr;

        public SentMessageIdByMsgIdHeaderKey(Mailbox mbox, String msgidHeader) {
            keyStr = mbox.getAccountId() + ":" + msgidHeader;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof SentMessageIdByMsgIdHeaderKey) {
                SentMessageIdByMsgIdHeaderKey otherKey = (SentMessageIdByMsgIdHeaderKey) other;
                return keyStr.equals(otherKey.keyStr);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return keyStr.hashCode();
        }

        @Override
        public String getKeyPrefix() { return MemcachedKeyPrefix.MBOX_SENTMSGID; }
        @Override
        public String getKeyValue() { return keyStr; }
    }


    private static class IntegerSerializer implements MemcachedSerializer<Integer> {
        @Override
        public Object serialize(Integer value) throws ServiceException { return value; }
        @Override
        public Integer deserialize(Object obj) throws ServiceException { return (Integer) obj; }
    }
}
