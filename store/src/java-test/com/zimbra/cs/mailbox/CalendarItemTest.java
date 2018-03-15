/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox.AddInviteData;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OperationContext.class, Mailbox.class, CalendarItem.class})
public class CalendarItemTest {
    private OperationContext octxt;
    private CalendarItem calItem;
    private final Integer SEQ = 123;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        octxt = PowerMockito.mock(OperationContext.class);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        calItem = PowerMockito.mock(CalendarItem.class);
        calItem.mData = new UnderlyingData();
        calItem.mMailbox = mbox;
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testPerformSetPrevFoldersOperation() throws Exception {
        PowerMockito.when(calItem.getModifiedSequence()).thenReturn(SEQ);
        PowerMockito.when(calItem, "performSetPrevFoldersOperation", octxt).thenCallRealMethod();
        PowerMockito.when(calItem, "getPrevFolders").thenCallRealMethod();

        calItem.performSetPrevFoldersOperation(octxt);
        // above method adds 2 in mod sequence, so verify SEQ+2 and Mailbox.ID_FOLDER_TRASH

        Assert.assertEquals(calItem.mData.getPrevFolders(), (SEQ+2)+":" + Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(calItem.getPrevFolders(), (SEQ+2)+":" + Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(calItem.mData.getPrevFolders(), calItem.getPrevFolders());
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testColor() throws Exception {
        Account account = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        // Default color
        Invite invite = getNewInvite(account);
        invite.setUid("11111111-1111-1111-1111-111111111111");
        ParsedMessage pm = new ParsedMessage(newTestMessage(new Random().nextInt()), System.currentTimeMillis(), false);
        AddInviteData aid = mbox.addInvite(octxt, invite, Mailbox.ID_FOLDER_CALENDAR, pm, false);
        CalendarItem item = mbox.getCalendarItemById(octxt, aid.calItemId);
        Assert.assertEquals(new Color(MailItem.DEFAULT_COLOR), item.getInvite(0).getRgbColor());
        // Custom color
        invite = getNewInvite(account);
        invite.setUid("22222222-2222-2222-2222-222222222222");
        invite.setColor(new Color("#e3a1b8"));
        pm = new ParsedMessage(newTestMessage(new Random().nextInt()), System.currentTimeMillis(), false);
        aid = mbox.addInvite(octxt, invite, Mailbox.ID_FOLDER_CALENDAR, pm, false);
        item = mbox.getCalendarItemById(octxt, aid.calItemId);
        Assert.assertEquals(new Color("#e3a1b8"), item.getInvite(0).getRgbColor());
        // Mapped color
        invite = getNewInvite(account);
        invite.setUid("33333333-3333-3333-3333-333333333333");
        invite.setColor(new Color((byte)5));
        pm = new ParsedMessage(newTestMessage(new Random().nextInt()), System.currentTimeMillis(), false);
        aid = mbox.addInvite(octxt, invite, Mailbox.ID_FOLDER_CALENDAR, pm, false);
        item = mbox.getCalendarItemById(octxt, aid.calItemId);
        Assert.assertEquals(new Color("#f66666").toString(), item.getInvite(0).getRgbColor().toString());
    }

    private Invite getNewInvite(Account account) {
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        return (new Invite(ICalTok.PUBLISH.toString(), tzMap, false));
    }
    private MimeMessage newTestMessage(int num) throws MessagingException {
        MimeMessage mm = new ZMimeMessage(JMSession.getSession());
        mm.setHeader("Message-ID", "<test-" + num + "@foo.com>");
        mm.setHeader("To", "nobody@foo.com");
        mm.setHeader("From", "nobody@bar.com");
        mm.setSubject("Test message " + num);
        mm.setSentDate(new Date(System.currentTimeMillis()));
        mm.setContent("This is test message " + num, "text/plain");
        return mm;
    }
}
