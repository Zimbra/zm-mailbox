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

/**
 * Shared unit tests for {@link FoldersAndTagsCache} adapters.
 */
public abstract class AbstractFoldersAndTagsCacheTest extends AbstractCacheTest {
    protected FoldersAndTagsCache cache;

    protected abstract FoldersAndTagsCache constructCache() throws ServiceException;

    @Test
    public void test() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);

        // Negative test
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals(null, cache.get(mbox));

        // Cache something
        FoldersAndTags foldersAndTags = mbox.getFoldersAndTags();
        cache.put(mbox, foldersAndTags);

        // Positive test
        FoldersAndTags foldersAndTags_ = cache.get(mbox);
        Assert.assertNotNull(foldersAndTags_);

        // Integrity test
        Assert.assertEquals(foldersAndTags.getFolderMetadata().size(), foldersAndTags_.getFolderMetadata().size());
        Assert.assertEquals(foldersAndTags.getTagMetadata().size(), foldersAndTags_.getTagMetadata().size());

        // Remove and negative test
        cache.remove(mbox);
        Assert.assertEquals(null, cache.get(mbox));
    }
}
