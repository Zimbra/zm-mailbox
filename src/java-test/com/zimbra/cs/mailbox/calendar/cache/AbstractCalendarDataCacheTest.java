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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.AbstractCacheTest;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.cache.CalendarDataCache.Key;

/**
 * Shared unit tests for {@link CalendarDataCache} adapters.
 */
public abstract class AbstractCalendarDataCacheTest extends AbstractCacheTest {
    protected CalendarDataCache cache;

    protected abstract CalendarDataCache constructCache() throws ServiceException;

    @Test
    public void crud() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // Negative test
        Key key = new Key(mbox.getAccountId(), Mailbox.ID_FOLDER_CALENDAR);
        Assert.assertEquals(null, cache.get(key));

        // Cache something
        CalendarData calendarData = new CalendarData(Mailbox.ID_FOLDER_CALENDAR, new Random().nextInt(), 2L, 4L);
        cache.put(key, calendarData);

        // Positive test
        CalendarData calendarData_ = cache.get(key);
        Assert.assertNotNull(calendarData_);

        // Integrity test
        Assert.assertEquals(calendarData.encodeMetadata().toString(), calendarData_.encodeMetadata().toString());
    }

    @Test
    public void removeOneMailbox() throws Exception {
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
        Key key1 = new Key(mbox1.getAccountId(), Mailbox.ID_FOLDER_CALENDAR);
        Key key2 = new Key(mbox2.getAccountId(), Mailbox.ID_FOLDER_CALENDAR);
        CalendarData calendarData1 = new CalendarData(Mailbox.ID_FOLDER_CALENDAR, new Random().nextInt(), 2L, 4L);
        CalendarData calendarData2 = new CalendarData(Mailbox.ID_FOLDER_CALENDAR, new Random().nextInt(), 3L, 5L);
        cache.put(key1, calendarData1);
        cache.put(key2, calendarData2);

        // Sanity check - expect both account's cached ctag-info-for-inbox to be cached
        Assert.assertNotNull(cache.get(key1));
        Assert.assertNotNull(cache.get(key2));

        // Remove all cached items for the 1st mailbox
        try {
            cache.remove(mbox1);
        } catch (UnsupportedOperationException e) {
            Assume.assumeTrue(false); // ignore the rest of the asserts in this method
        }

        // Expect the first account's cached ctag-info-for-inbox data to be gone
        Assert.assertEquals(null, cache.get(key1));

        // Expect the 2nd account's cached ctag-info-for-inbox to remain cached
        Assert.assertNotNull(cache.get(key2));
    }
}
