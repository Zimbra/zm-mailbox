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

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;

/**
 * Shared unit tests for {@link EffectiveACLCache} adapters.
 */
public abstract class AbstractEffectiveACLCacheTest extends AbstractCacheTest {
    protected EffectiveACLCache cache;

    protected abstract EffectiveACLCache constructCache() throws ServiceException;

    @Test
    public void test() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // Negative test
        EffectiveACLCache.Key key = new EffectiveACLCache.Key(mbox.getAccountId(), Mailbox.ID_FOLDER_INBOX);
        Assert.assertEquals(null, cache.get(key));

        // Cache something
        ACL acl = new ACL();
        acl.grantAccess(mbox.getAccountId(), ACL.GRANTEE_USER, ACL.RIGHT_DELETE, "secret", 0L);
        cache.put(key, acl);

        // Positive test
        ACL acl_ = cache.get(key);
        Assert.assertNotNull(acl_);

        // Integrity test
        Assert.assertEquals(acl.toString(), acl_.toString());

        // Remove and negative test
        cache.remove(mbox);
        Assert.assertEquals(null, cache.get(key));
    }
}
