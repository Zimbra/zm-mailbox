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

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;

/**
 * Shared unit tests for {@link ConversationIdCache} adapters.
 */
public abstract class AbstractConversationIdCacheTest extends AbstractCacheTest {
    protected ConversationIdCache cache;

    protected abstract ConversationIdCache constructCache() throws ServiceException;

    @Test
    public void test() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);

        // Negative test
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String subjectHash = "" + new Random().nextInt();
        Assert.assertEquals(null, cache.get(mbox, subjectHash));

        // Cache something
        int conversationId = new Random().nextInt();
        cache.put(mbox, subjectHash, conversationId);

        // Positive test
        Integer conversationId_ = cache.get(mbox, subjectHash);
        Assert.assertNotNull(conversationId_);

        // Integrity test
        Assert.assertEquals(conversationId, conversationId_.intValue());
    }
}
