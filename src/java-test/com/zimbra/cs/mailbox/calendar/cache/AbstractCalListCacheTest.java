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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.AbstractCacheTest;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * Shared unit tests for {@link CalListCache} adapters.
 */
public abstract class AbstractCalListCacheTest extends AbstractCacheTest {
    protected CalListCache cache;

    protected abstract CalListCache constructCache() throws ServiceException;

    @Test
    public void test() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);

        // Negative test
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals(null, cache.get(mbox.getAccountId()));

        // Cache something
        List<Folder> calFolders = mbox.getCalendarFolders(null, SortBy.NONE);
        Set<Integer> idset = new HashSet<Integer>(calFolders.size());
        idset.add(Mailbox.ID_FOLDER_INBOX);
        for (Folder calFolder : calFolders) {
            idset.add(calFolder.getId());
        }
        CalList calList = new CalList(idset);
        cache.put(mbox.getAccountId(), calList);

        // Positive test
        CalList calList_ = cache.get(mbox.getAccountId());
        Assert.assertNotNull(calList_);

        // Integrity test
        Assert.assertEquals(calList.encodeMetadata().toString(), calList_.encodeMetadata().toString());
    }
}
