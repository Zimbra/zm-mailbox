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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;


/** An in-process {@link ConversationIdCache} implementation, can be used by unit tests. */
public class LocalConversationIdCache implements ConversationIdCache {
    protected Map<String, Integer> conversationIdsBySubjectHash = new ConcurrentHashMap<>();

    @VisibleForTesting
    void flush() {
        conversationIdsBySubjectHash.clear();
    }

    @Override
    public Integer get(Mailbox mbox, String subjectHash) throws ServiceException {
        return conversationIdsBySubjectHash.get(key(mbox, subjectHash));
    }

    @Override
    public void put(Mailbox mbox, String subjectHash, int conversationId) throws ServiceException {
        conversationIdsBySubjectHash.put(key(mbox, subjectHash),  conversationId);
    }

    @Override
    public void remove(Mailbox mbox, String subjectHash) throws ServiceException {
        conversationIdsBySubjectHash.remove(key(mbox, subjectHash));
    }

    protected String key(Mailbox mbox, int itemId) {
        return "id:" + mbox.getAccountId() + ':' + itemId;
    }

    protected String key(Mailbox mbox, String uuid) {
        return "uuid:" + mbox.getAccountId() + ':' + uuid;
    }
}
