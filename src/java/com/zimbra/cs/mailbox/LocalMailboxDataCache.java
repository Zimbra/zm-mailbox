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

/** An in-process {@link MailboxDataCache}, can be used by unit tests. */
public class LocalMailboxDataCache implements MailboxDataCache {
    protected Map<String, Mailbox.MailboxData> dataByAccountId = new ConcurrentHashMap<String, Mailbox.MailboxData>();

    public LocalMailboxDataCache() {
    }

    @VisibleForTesting
    void flush() {
        dataByAccountId.clear();
    }

    @Override
    public Mailbox.MailboxData get(Mailbox mbox) throws ServiceException {
        return dataByAccountId.get(mbox.getAccountId());
    }

    @Override
    public void put(Mailbox mbox, Mailbox.MailboxData mailboxData) throws ServiceException {
        dataByAccountId.put(mbox.getAccountId(), mailboxData);
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        dataByAccountId.remove(mbox.getAccountId());
    }
}
