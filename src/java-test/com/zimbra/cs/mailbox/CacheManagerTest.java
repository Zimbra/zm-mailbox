/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashMap;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;

/**
 * Unit test for {@link CacheManager}.
 */
public final class CacheManagerTest {
    static Mailbox mbox;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testCachesClearedAfterDeleteMailbox() throws Exception {
        // Setup for test
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        CacheManager cacheManager = EasyMock.createNiceMock(CacheManager.class);
        CacheManager.setInstance(cacheManager);
        cacheManager.calendarCacheManager = EasyMock.createMock(CalendarCacheManager.class);
        cacheManager.effectiveACLCache = EasyMock.createMock(EffectiveACLCache.class);
        cacheManager.foldersAndTagsCache = EasyMock.createMock(FoldersAndTagsCache.class);

        cacheManager.calendarCacheManager.purgeMailbox(mbox);
        EasyMock.expectLastCall();

        cacheManager.effectiveACLCache.remove(mbox);
        EasyMock.expectLastCall();

        cacheManager.foldersAndTagsCache.remove(mbox);
        EasyMock.expectLastCall();

        // Perform test
        mbox.deleteMailbox();
        EasyMock.replay(cacheManager, cacheManager.calendarCacheManager, cacheManager.effectiveACLCache, cacheManager.foldersAndTagsCache);
    }
}
