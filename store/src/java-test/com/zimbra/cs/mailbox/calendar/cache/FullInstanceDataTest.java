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
package com.zimbra.cs.mailbox.calendar.cache;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;

import junit.framework.Assert;

public class FullInstanceDataTest {
    private Account account;
    private Mailbox mbox;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        account = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        mbox = MailboxManager.getInstance().getMailboxByAccount(account);
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInstanceWithoutColor() throws Exception {
        Color color = null;
        Invite invite = createInvite(color);
        FullInstanceData inst = createFullInstanceData(invite);
        Assert.assertNull(inst.getRgbColor());
        Metadata metadata = inst.encodeMetadata();
        inst = new FullInstanceData(metadata);
        Assert.assertEquals(new Color(MailItem.DEFAULT_COLOR), inst.getRgbColor());
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInstanceWithMappedColor() throws Exception {
        Color color = new Color((byte)3); // #6acb9e
        Invite invite = createInvite(color);
        FullInstanceData inst = createFullInstanceData(invite);
        Assert.assertEquals(color, inst.getRgbColor());
        Metadata metadata = inst.encodeMetadata();
        inst = new FullInstanceData(metadata);
        Assert.assertEquals(color, inst.getRgbColor());
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInstanceWithCustomColor() throws Exception {
        Color color = new Color("#2e8af3");
        Invite invite = createInvite(color);
        FullInstanceData inst = createFullInstanceData(invite);
        Assert.assertEquals(color, inst.getRgbColor());
        Metadata metadata = inst.encodeMetadata();
        inst = new FullInstanceData(metadata);
        Assert.assertEquals(color, inst.getRgbColor());
    }

    private Invite createInvite(Color clr) throws Exception {
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
        Color color = clr;
        Invite invite = Invite.createInvite(mailboxId, type, method, tzMap, uidOrNull, status, priority, pctComplete, completed, freeBusy,
            transparency, classProp, allDayEvent, dtStart, dtEndOrNull, durationOrNull, recurId, recurrenceOrNull, isOrganizer,
            organizer, attendees, name, location, description, descHtml, comments, categories, contacts, geo, url, dtStamp,
            lastModified, seqNo, lastFullSeqNo, partStat, rsvp, sentByMe, color);
        return invite;
    }

    private FullInstanceData createFullInstanceData(Invite invite) throws Exception {
        String recurIdZ = null;
        long dtStart = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        Long duration = new Long(10000);
        Long alarmAt = null;
        String partStat = null;
        String freeBusyActual = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
        FullInstanceData inst = new FullInstanceData(invite, recurIdZ, dtStart, duration, partStat, freeBusyActual, alarmAt);
        return inst;
    }


}
