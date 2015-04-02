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
package com.zimbra.cs.mailbox.calendar.cache;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.AbstractCacheTest;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * Shared unit tests for {@link CtagResponseCache} adapters.
 */
public abstract class AbstractCtagResponseCacheTest extends AbstractCacheTest {
    protected CtagResponseCache cache;

    protected abstract CtagResponseCache constructCache() throws ServiceException;

    @Test(timeout=3000)
    public void test() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);

        // Negative test
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        final String USERAGENT = "blah";
        CtagResponseCache.Key key = new CtagResponseCache.Key(mbox.getAccountId(), USERAGENT, Mailbox.ID_FOLDER_ROOT);
        Assert.assertEquals(null, cache.get(key));

        // Cache something
        @SuppressWarnings("unchecked")
        CtagResponseCache.Value value = new CtagResponseCache.Value("hello".getBytes(), 5, false, "2.1", Collections.EMPTY_MAP);
        cache.put(key, value);

        // Positive test
        CtagResponseCache.Value value_ = cache.get(key);
        Assert.assertNotNull(value_);

        // Integrity test
        Assert.assertEquals(value.encodeMetadata().toString(), value_.encodeMetadata().toString());
    }
}
