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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.AbstractCacheTest;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * Shared unit tests for {@link CtagInfoCache} adapters.
 */
public abstract class AbstractCtagInfoCacheTest extends AbstractCacheTest {
    protected CtagInfoCache cache;

    protected abstract CtagInfoCache constructCache() throws ServiceException;

    @Test
    public void crud() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);

        // Negative test
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals(null, cache.get(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));

        // Cache something
        cache.put(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR, new CtagInfo(mbox.getFolderById(null, Mailbox.ID_FOLDER_CALENDAR)));

        // Positive test
        CtagInfo ctagInfo_ = cache.get(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR);
        Assert.assertNotNull(ctagInfo_);

        // Integrity test
        Assert.assertEquals(ctagInfo_.encodeMetadata().toString(), ctagInfo_.encodeMetadata().toString());

        // Remove it
        cache.remove(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR);

        // Negative test
        Assert.assertEquals(null, cache.get(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));
    }

    @Test
    public void crudUsingMulti() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // Cache 3 things via multi-put
        Map<Pair<String,Integer>, CtagInfo> putMap = new HashMap<>();
        putMap.put(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_INBOX)   , new CtagInfo(mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX)));
        putMap.put(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR), new CtagInfo(mbox.getFolderById(null, Mailbox.ID_FOLDER_CALENDAR)));
        putMap.put(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CONTACTS), new CtagInfo(mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS)));
        cache.put(putMap);

        // Multi-get test, expect to find 3 things
        List<Pair<String,Integer>> keys = new ArrayList<>();
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_INBOX));
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CONTACTS));
        Map<Pair<String,Integer>, CtagInfo> map = cache.get(keys);
        Assert.assertEquals(3, map.size());

        // Remove 2 things, and try and remove 1 thing that isn't there, then expect that to leave 1 thing
        keys = new ArrayList<>();
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_INBOX));
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_DRAFTS));
        cache.remove(keys);

        // Multi-get test, expect to find 1 thing
        keys = new ArrayList<>();
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_INBOX));
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));
        keys.add(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CONTACTS));
        map = cache.get(keys);
        Assert.assertEquals(3, map.size());
        Assert.assertNull(map.get(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_INBOX)));
        Assert.assertNull(map.get(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR)));
        Assert.assertNull(map.get(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_DRAFTS)));
        Assert.assertNotNull(map.get(new Pair<>(mbox.getAccountId(), Mailbox.ID_FOLDER_CONTACTS)));
    }

    @Test
    public void removeOneMailboxesData() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);

        Provisioning prov = Provisioning.getInstance();
        Map<String,Object> attr1 = new HashMap<>();
        Map<String,Object> attr2 = new HashMap<>();
        attr1.put(Provisioning.A_zimbraId, new UUID(1L, 0L).toString());
        attr2.put(Provisioning.A_zimbraId, new UUID(2L, 0L).toString());
        Account acct1 = prov.createAccount("test1@zimbra.com", "secret", attr1);
        Account acct2 = prov.createAccount("test2@zimbra.com", "secret", attr2);

        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

        // Put 2 account's inbox folders into the cache
        cache.put(mbox1.getAccountId(), Mailbox.ID_FOLDER_CALENDAR, new CtagInfo(mbox1.getFolderById(null, Mailbox.ID_FOLDER_CALENDAR)));
        cache.put(mbox2.getAccountId(), Mailbox.ID_FOLDER_CALENDAR, new CtagInfo(mbox2.getFolderById(null, Mailbox.ID_FOLDER_CALENDAR)));

        // Sanity check - expect both account's cached ctag-info-for-inbox to be cached
        Assert.assertNotNull(cache.get(mbox1.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));
        Assert.assertNotNull(cache.get(mbox2.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));

        // Remove all cached items for the 1st mailbox
        try {
            cache.remove(mbox1);
        } catch (UnsupportedOperationException e) {
            Assume.assumeTrue(false); // ignore the rest of the asserts in this method
        }

        // Expect the first account's cached ctag-info-for-inbox data to be gone
        Assert.assertEquals(null, cache.get(mbox1.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));

        // Expect the 2nd account's cached ctag-info-for-inbox to remain cached
        Assert.assertNotNull(cache.get(mbox2.getAccountId(), Mailbox.ID_FOLDER_CALENDAR));
    }
}
