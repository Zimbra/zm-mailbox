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
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.mailbox.Color;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class InviteChangesTest {
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        startTime = LocalDateTime.now();
        endTime = startTime.plusMinutes(30);
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testDiffInvitesColor() throws Exception {
        InviteChanges changes;
        Color red = new Color((byte)5);  // #f66666
        Color cyan = new Color((byte)2); // #43eded
        Color custom1 = new Color("#f66666");
        Color custom2 = new Color("#5f3ac7");
        Invite invite_red = createInvite(red);
        Invite invite_red2 = createInvite(red);
        Invite invite_cyan = createInvite(cyan);
        Invite invite_custom1_1 = createInvite(custom1);
        Invite invite_custom1_2 = createInvite(custom1);
        Invite invite_custom2 = createInvite(custom2);

        // same mapped color
        changes = new InviteChanges(invite_red, invite_red2);
        Assert.assertTrue(changes.noChange());
        Assert.assertFalse(changes.changedTime());
        Assert.assertFalse(changes.changedRecurrence());
        Assert.assertFalse(changes.changedSubject());
        Assert.assertFalse(changes.changedColor());

        // different mapped color
        changes = new InviteChanges(invite_red, invite_cyan);
        Assert.assertFalse(changes.noChange());
        Assert.assertFalse(changes.changedTime());
        Assert.assertFalse(changes.changedRecurrence());
        Assert.assertFalse(changes.changedSubject());
        Assert.assertTrue(changes.changedColor());

        // mapped color red (#f66666) and custom color #f66666 are treated as different color
        changes = new InviteChanges(invite_red, invite_custom1_1);
        Assert.assertFalse(changes.noChange());
        Assert.assertFalse(changes.changedTime());
        Assert.assertFalse(changes.changedRecurrence());
        Assert.assertFalse(changes.changedSubject());
        Assert.assertTrue(changes.changedColor());

        // same custom color
        changes = new InviteChanges(invite_custom1_1, invite_custom1_2);
        Assert.assertTrue(changes.noChange());
        Assert.assertFalse(changes.changedTime());
        Assert.assertFalse(changes.changedRecurrence());
        Assert.assertFalse(changes.changedSubject());
        Assert.assertFalse(changes.changedColor());

        // different custom color
        changes = new InviteChanges(invite_custom1_1, invite_custom2);
        Assert.assertFalse(changes.noChange());
        Assert.assertFalse(changes.changedTime());
        Assert.assertFalse(changes.changedRecurrence());
        Assert.assertFalse(changes.changedSubject());
        Assert.assertTrue(changes.changedColor());
    }

    private Invite createInvite(Color clr) throws Exception {
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
        ParsedDateTime dtStart = ParsedDateTime.parse(startTime.toString(), tzMap, null, tzMap.getLocalTimeZone());
        ParsedDateTime dtEndOrNull = ParsedDateTime.parse(endTime.toString(), tzMap, null, tzMap.getLocalTimeZone());
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
        Color color = clr;
        Invite invite = Invite.createInvite(mailboxId, type, method, tzMap, uidOrNull, status, priority, pctComplete, completed, freeBusy,
            transparency, classProp, allDayEvent, dtStart, dtEndOrNull, durationOrNull, recurId, recurrenceOrNull, isOrganizer,
            organizer, attendees, name, location, description, descHtml, comments, categories, contacts, geo, url, dtStamp,
            lastModified, seqNo, lastFullSeqNo, partStat, rsvp, sentByMe, color);
        return invite;
    }
}
