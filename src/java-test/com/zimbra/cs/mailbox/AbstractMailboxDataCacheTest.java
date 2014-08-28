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
package com.zimbra.cs.mailbox;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;

/**
 * Shared unit tests for {@link MailboxDataCache} adapters.
 */
public abstract class AbstractMailboxDataCacheTest extends AbstractCacheTest {
    protected MailboxDataCache cache;

    protected abstract MailboxDataCache constructCache() throws ServiceException;

    @Test
    public void test() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);

        // Negative test
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals(null, cache.get(mbox));

        // Cache something
        MailboxData mailboxData = EasyMock.createNiceMock(MailboxData.class);
        cache.put(mbox, mailboxData);

        // Positive test
        MailboxData mailboxData_ = cache.get(mbox);
        Assert.assertNotNull(mailboxData_);

        // Integrity test
        Assert.assertEquals(mailboxData.id, mailboxData_.id);

        // Remove and negative test
        cache.remove(mbox);
        Assert.assertEquals(null, cache.get(mbox));
    }
}
