/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessage.CalendarPartInfo;
import com.zimbra.cs.util.ZTestWatchman;

public class ZAttendeeTest {

    @Rule public TestName testName = new TestName();
    @Rule public MethodRule watchman = new ZTestWatchman();

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        System.out.println(testName.getMethodName());
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@testdomain.com", "secret", Maps.<String, Object>newHashMap());
    }


    @Test
    public void resourceRsvpTest() throws ServiceException {
        ZAttendee attendee = new ZAttendee ("test-resource@testdomain.com", "testResource", null, null, null, "RES", "NON", "AC", Boolean.TRUE,
                                            null, null, null, null);
        ZProperty prop = new ZProperty("ATTENDEE");
        attendee.setProperty(prop);
        ZParameter rsvpParam = prop.getParameter(ICalTok.RSVP);
        Assert.assertTrue(Boolean.parseBoolean(rsvpParam.getValue()));
    }

    @Test
    public void resourceRsvpTest2() throws ServiceException, IOException {
        InputStream is = getClass().getResourceAsStream("Calendar_NewMeetingRequest_RsvpTrue.txt");
        ParsedMessage pm = new ParsedMessage(ByteUtil.getContent(is, -1), false);
        CalendarPartInfo cpi = pm.getCalendarPartInfo();
        Iterator<ZComponent> compIter = cpi.cal.getComponentIterator();
        while(compIter.hasNext()) {
            ZComponent comp = compIter.next();
            List<ZProperty> properties = Lists.newArrayList(comp.getPropertyIterator());
            for (ZProperty prop : properties) {
                if (prop.getName().equalsIgnoreCase("ATTENDEE")) {
                    ZAttendee attendee = new ZAttendee (prop);
                    Assert.assertTrue(attendee.getRsvp());
                }
            }
        }
    }

    @Test
    public void resourceRsvpTest3() throws Exception {
        InputStream is = getClass().getResourceAsStream("Calendar_NewMeetingRequest_RsvpFalse.txt");
        ParsedMessage pm = new ParsedMessage(ByteUtil.getContent(is, -1), false);
        CalendarPartInfo cpi = pm.getCalendarPartInfo();
        Iterator<ZComponent> compIter = cpi.cal.getComponentIterator();
        while(compIter.hasNext()) {
            ZComponent comp = compIter.next();
            List<ZProperty> properties = Lists.newArrayList(comp.getPropertyIterator());
            for (ZProperty prop : properties) {
                if (prop.getName().equalsIgnoreCase("ATTENDEE")) {
                    ZAttendee attendee = new ZAttendee (prop);
                    Assert.assertFalse(attendee.getRsvp());
                }
            }
        }
    }

    @Test
    public void resourceRsvpTest4() throws Exception {
        InputStream is = getClass().getResourceAsStream("Calendar_ChangeMeetingRequest_WO_Rsvp.txt");
        ParsedMessage pm = new ParsedMessage(ByteUtil.getContent(is, -1), false);
        CalendarPartInfo cpi = pm.getCalendarPartInfo();
        Iterator<ZComponent> compIter = cpi.cal.getComponentIterator();
        while(compIter.hasNext()) {
            ZComponent comp = compIter.next();
            List<ZProperty> properties = Lists.newArrayList(comp.getPropertyIterator());
            for (ZProperty prop : properties) {
                if (prop.getName().equalsIgnoreCase("ATTENDEE")) {
                    ZAttendee attendee = new ZAttendee (prop);
                    Assert.assertNull(attendee.getRsvp());
                }
            }
        }
    }

    @Test
    public void nullRsvp() throws Exception {
        TimeZoneMap tzMap = new TimeZoneMap(WellKnownTimeZones.getTimeZoneById("JST"));
        Invite inv = new Invite(MailItem.Type.APPOINTMENT, ICalTok.ACCEPTED.toString(),
                tzMap, false);
        Assert.assertThat(inv.toString(), StringContains.containsString("rsvp: (not specified)"));
    }
}
