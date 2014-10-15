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

public class MemcachedConversationIdCache implements ConversationIdCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected MemcachedMap<ConversationIDBySubjectHashKey, Integer> conversationIdBySubjectHashLookup;

    /** Constructor */
    public MemcachedConversationIdCache() {
    }

    @PostConstruct
    public void init() {
        conversationIdBySubjectHashLookup = new MemcachedMap<>(memcachedClient, new IntegerSerializer(), false);
    }

    @Override
    public Integer get(Mailbox mbox, String subjectHash) throws ServiceException {
        return conversationIdBySubjectHashLookup.get(new ConversationIDBySubjectHashKey(mbox, subjectHash));
    }

    @Override
    public void put(Mailbox mbox, String subjectHash, int conversationId) throws ServiceException {
        conversationIdBySubjectHashLookup.put(new ConversationIDBySubjectHashKey(mbox, subjectHash), conversationId);
    }

    @Override
    public void remove(Mailbox mbox, String subjectHash) throws ServiceException {
        conversationIdBySubjectHashLookup.remove(new ConversationIDBySubjectHashKey(mbox, subjectHash));
    }


    public static class ConversationIDBySubjectHashKey implements MemcachedKey {
        private String keyStr;

        public ConversationIDBySubjectHashKey(Mailbox mbox, String subjectHash) {
            keyStr = mbox.getAccountId() + ":" +subjectHash;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ConversationIDBySubjectHashKey) {
                ConversationIDBySubjectHashKey otherKey = (ConversationIDBySubjectHashKey) other;
                return keyStr.equals(otherKey.keyStr);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return keyStr.hashCode();
        }

        @Override
        public String getKeyPrefix() { return MemcachedKeyPrefix.MBOX_CONVERSATION; }
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
