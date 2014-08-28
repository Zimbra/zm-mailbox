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

/** An in-process ItemCache implementation, can be used by unit tests. */
public class LocalSentMessageIdCache implements SentMessageIdCache {
    protected Map<String, Integer> sentMsgIdsByMsgIdHeader = new ConcurrentHashMap<>();

    @VisibleForTesting
    void flush() {
        sentMsgIdsByMsgIdHeader.clear();
    }

    @Override
    public Integer get(Mailbox mbox, String msgidHeader) throws ServiceException {
        return sentMsgIdsByMsgIdHeader.get(key(mbox, msgidHeader));
    }

    @Override
    public void put(Mailbox mbox, String msgidHeader, int id) throws ServiceException {
        sentMsgIdsByMsgIdHeader.put(key(mbox, msgidHeader), id);
    }

    protected String key(Mailbox mbox, int itemId) {
        return "id:" + mbox.getAccountId() + ':' + itemId;
    }

    protected String key(Mailbox mbox, String uuid) {
        return "uuid:" + mbox.getAccountId() + ':' + uuid;
    }
}
