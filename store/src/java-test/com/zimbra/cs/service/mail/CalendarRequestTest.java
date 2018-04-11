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

package com.zimbra.cs.service.mail;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.mail.CalendarRequest.MailSendQueue;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OperationContext.class, Mailbox.class, CalendarItem.class,
    MailSendQueue.class, Invite.class, CalendarRequest.class, AccountUtil.class, CalendarMailSender.class,
    MailboxLock.class})
public class CalendarRequestTest {

    private ZimbraSoapContext zsc;
    private OperationContext octxt;
    private Mailbox mbox;
    private MailboxLock mockedMailboxLock;
    private CalendarItem calItem;
    private ZAttendee addedAttendee1;
    private ZAttendee addedAttendee2;
    private MailSendQueue sendQueue;
    private Invite invite1;
    private Invite invite2;
    private Account account;
    private javax.mail.internet.MimeMessage mm;
    private javax.mail.internet.MimeMessage mm2;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        mockedMailboxLock = PowerMockito.mock(MailboxLock.class);
        mbox = PowerMockito.mock(Mailbox.class);
        octxt = PowerMockito.mock(OperationContext.class);
        invite1 = PowerMockito.mock(Invite.class);
        invite2 = PowerMockito.mock(Invite.class);
        calItem = PowerMockito.mock(CalendarItem.class);
        sendQueue = new CalendarRequest.MailSendQueue();
    }

    /**
     * Test method for {@link com.zimbra.cs.service.mail.CalendarRequest#notifyCalendarItem(com.zimbra.soap.ZimbraSoapContext, com.zimbra.cs.mailbox.OperationContext, com.zimbra.cs.account.Account, com.zimbra.cs.mailbox.Mailbox, com.zimbra.cs.mailbox.CalendarItem, boolean, java.util.List, boolean, com.zimbra.cs.service.mail.CalendarRequest.MailSendQueue)}.
     * @throws Exception
     */
    @Test
    public void testNotifyCalendarItem() throws Exception {

        JavaMailInternetAddress emailAddress = new JavaMailInternetAddress("test@zimbra.com", "test", MimeConstants.P_CHARSET_UTF8);
        PowerMockito.mockStatic(AccountUtil.class);
        PowerMockito.doReturn(emailAddress).when(AccountUtil.class, "getFriendlyEmailAddress", account);

        List<Address> addressList = new ArrayList<Address>();
        addressList.add(new InternetAddress("test1@zimbra.com", "Test 1"));
        addressList.add(new InternetAddress("test2@zimbra.com", "Test 2"));
        List<ZAttendee> attendeeList = new ArrayList<ZAttendee>();
        attendeeList.add(addedAttendee1);
        attendeeList.add(addedAttendee2);

        PowerMockito.when(mbox.getWriteLockAndLockIt()).thenReturn(mockedMailboxLock);
        PowerMockito.when(mbox.getReadLockAndLockIt()).thenReturn(mockedMailboxLock);

        PowerMockito.doReturn(System.currentTimeMillis()).when(octxt, "getTimestamp");

        PowerMockito.doReturn(1).when(invite1).getMailItemId();
        PowerMockito.doReturn(2).when(invite2).getMailItemId();
        PowerMockito.doReturn(attendeeList).when(invite1).getAttendees();
        PowerMockito.doReturn(attendeeList).when(invite2).getAttendees();
        PowerMockito.doReturn(true).when(invite2).hasRecurId();

        PowerMockito.when(calItem.isPublic()).thenReturn(true);
        PowerMockito.when(calItem.allowPrivateAccess(account, true)).thenReturn(true);
        PowerMockito.when(calItem.getId()).thenReturn(1);
        PowerMockito.when(calItem.getInvites()).thenReturn(new Invite[] {invite1, invite2});
        PowerMockito.when(calItem.getSubpartMessage(1)).thenReturn(mm);
        PowerMockito.when(calItem.getSubpartMessage(2)).thenReturn(mm2);

        PowerMockito.when(mbox.getCalendarItemById(octxt, calItem.getId())).thenReturn(calItem);
        PowerMockito.mockStatic(CalendarMailSender.class);
        PowerMockito.doReturn(addressList).when(CalendarMailSender.class, "toListFromAttendees", attendeeList);

        PowerMockito.stub(PowerMockito.method(CalendarRequest.class, "isOnBehalfOfRequest", ZimbraSoapContext.class)).toReturn(false);
        PowerMockito.stub(PowerMockito.method(CalendarRequest.class, "getAuthenticatedAccount", ZimbraSoapContext.class)).toReturn(account);

        CalendarRequest.notifyCalendarItem(zsc, octxt, account, mbox, calItem, true, attendeeList, true, sendQueue);
        assertEquals(1, sendQueue.queue.size());
    }
}
