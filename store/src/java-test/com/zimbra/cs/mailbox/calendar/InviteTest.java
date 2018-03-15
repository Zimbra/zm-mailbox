/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.AddInviteData;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;

public class InviteTest {
    private Account account;
    private Mailbox mbox;
    private OperationContext octxt;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        octxt = PowerMockito.mock(OperationContext.class);
        account = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        mbox = MailboxManager.getInstance().getMailboxByAccount(account);
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInviteWithoutColor() throws Exception {
        Color color = null;
        Invite invite = createInvite(color);
        Metadata metadata = Invite.encodeMetadata(invite);
        Assert.assertNull(invite.getRgbColor());
        Assert.assertEquals(0, metadata.getLong("c", 0));

        int mailboxId = mbox.getId();
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        ParsedMessage pm = new ParsedMessage(newTestMessage(new Random().nextInt()), System.currentTimeMillis(), false);
        AddInviteData aid = mbox.addInvite(octxt, invite, Mailbox.ID_FOLDER_CALENDAR, pm, false);
        CalendarItem calItem = mbox.getCalendarItemById(octxt, aid.calItemId);
        Invite newInvite = Invite.decodeMetadata(mailboxId, metadata, calItem, tzMap.getLocalTimeZone());
        Assert.assertEquals(new Color(MailItem.DEFAULT_COLOR), newInvite.getRgbColor());
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInviteWithMappedColor() throws Exception {
        Color color = new Color((byte)1); // #5b9bf2
        Invite invite = createInvite(color);
        Metadata metadata = Invite.encodeMetadata(invite);
        Assert.assertEquals(new Color("#5b9bf2").toString(), invite.getRgbColor().toString());
        Assert.assertEquals(color.getValue(), metadata.getLong("c", 0));

        int mailboxId = mbox.getId();
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        ParsedMessage pm = new ParsedMessage(newTestMessage(new Random().nextInt()), System.currentTimeMillis(), false);
        AddInviteData aid = mbox.addInvite(octxt, invite, Mailbox.ID_FOLDER_CALENDAR, pm, false);
        CalendarItem calItem = mbox.getCalendarItemById(octxt, aid.calItemId);
        Invite newInvite = Invite.decodeMetadata(mailboxId, metadata, calItem, tzMap.getLocalTimeZone());
        Assert.assertEquals(color, newInvite.getRgbColor());
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInviteWithCustomColor() throws Exception {
        Color color = new Color("#7ab81a");
        Invite invite = createInvite(color);
        Metadata metadata = Invite.encodeMetadata(invite);
        Assert.assertEquals(color, invite.getRgbColor());
        Assert.assertEquals(color.getValue(), metadata.getLong("c", 0));

        int mailboxId = mbox.getId();
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        ParsedMessage pm = new ParsedMessage(newTestMessage(new Random().nextInt()), System.currentTimeMillis(), false);
        AddInviteData aid = mbox.addInvite(octxt, invite, Mailbox.ID_FOLDER_CALENDAR, pm, false);
        CalendarItem calItem = mbox.getCalendarItemById(octxt, aid.calItemId);
        Invite newInvite = Invite.decodeMetadata(mailboxId, metadata, calItem, tzMap.getLocalTimeZone());
        Assert.assertEquals(color, newInvite.getRgbColor());
    }

    private Invite createInvite(Color color) throws Exception {
        Account account = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        int mailboxId = mbox.getId();
        MailItem.Type type = MailItem.Type.APPOINTMENT;
        String method = ICalTok.PUBLISH.toString();
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        String uidOrNull = "11111111-1111-1111-1111-111111111111";
        String status = IcalXmlStrMap.STATUS_CONFIRMED;
        String priority = null;
        String pctComplete = null;
        long completed = 0;
        String freeBusy = null;
        String transparency = IcalXmlStrMap.TRANSP_OPAQUE;
        String classProp = IcalXmlStrMap.CLASS_PUBLIC;
        boolean allDayEvent = false;
        ParsedDateTime dtStart = ParsedDateTime.parse(LocalDateTime.now().toString(), tzMap, null, tzMap.getLocalTimeZone());
        ParsedDateTime dtEndOrNull = ParsedDateTime.parse(LocalDateTime.now().plusMinutes(30).toString(), tzMap, null, tzMap.getLocalTimeZone());
        ParsedDuration durationOrNull = null;
        RecurId recurId = null;
        Recurrence.IRecurrence recurrenceOrNull = null;
        boolean isOrganizer = true;
        ZOrganizer organizer = new ZOrganizer(account.getMail(), null, null, null, null, null);
        ArrayList<ZAttendee> attendees = new ArrayList<ZAttendee>(0);
        String name = "appointment-1";
        String location = null;
        String description = "sample";
        String descHtml = null;
        List<String> comments = null;
        List<String> categories = null;
        List<String> contacts = null;
        Geo geo = null;
        String url = null;
        long dtStamp = 0;
        long lastModified = 0;
        int seqNo = 0;
        int lastFullSeqNo = 0;
        String partStat = IcalXmlStrMap.PARTSTAT_ACCEPTED;
        boolean rsvp = true;
        boolean sentByMe = false;
        Invite invite;
        if (color != null) {
            invite = Invite.createInvite(mailboxId, type, method, tzMap, uidOrNull, status, priority, pctComplete, completed, freeBusy,
                transparency, classProp, allDayEvent, dtStart, dtEndOrNull, durationOrNull, recurId, recurrenceOrNull, isOrganizer,
                organizer, attendees, name, location, description, descHtml, comments, categories, contacts, geo, url, dtStamp,
                lastModified, seqNo, lastFullSeqNo, partStat, rsvp, sentByMe, color);
        } else {
            invite = Invite.createInvite(mailboxId, type, method, tzMap, uidOrNull, status, priority, pctComplete, completed, freeBusy,
                transparency, classProp, allDayEvent, dtStart, dtEndOrNull, durationOrNull, recurId, recurrenceOrNull, isOrganizer,
                organizer, attendees, name, location, description, descHtml, comments, categories, contacts, geo, url, dtStamp,
                lastModified, seqNo, lastFullSeqNo, partStat, rsvp, sentByMe);
        }
        return invite;
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
