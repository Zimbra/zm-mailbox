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

package com.zimbra.common.calendar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.service.ServiceException;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.util.Uris;

/**
 * Intended to test features that Zimbra has chosen to add to ical4j in the past, to ensure that future
 * integrations still function correctly.
 *
 */
public class Ical4JTest {

    public static final String emptyCN =
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:Microsoft Exchange Server 2007\r\n" +
            "BEGIN:VEVENT\r\n" +
            "ORGANIZER;CN=:MAILTO:test1@invalid.dom\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=:MAILTO:te\r\n" +
            " st2@invalid.dom\r\n" +
            "SUMMARY;LANGUAGE=en-US:Testing Empty CNs\r\n" +
            "UID:U4\r\n" +
            "DTSTAMP:20070228T183803Z\r\n" +
            "DTSTART;VALUE=DATE:20051018\r\n" +
            "DTEND;VALUE=DATE:20051019\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n";

    public static final String multiVcalendar =
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:Oracle/Oracle Calendar Server 9.0.4.2.8\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:U1\r\n" +
            "DTSTAMP:20070228T183803Z\r\n" +
            "DTSTART;VALUE=DATE:20051018\r\n" +
            "DTEND;VALUE=DATE:20051019\r\n" +
            "SUMMARY:event in cal1\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n" +
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:Oracle/Oracle Calendar Server 9.0.4.2.8\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:U2\r\n" +
            "DTSTAMP:20070228T183800Z\r\n" +
            "DTSTART;VALUE=DATE:20051019\r\n" +
            "DTEND;VALUE=DATE:20051020\r\n" +
            "SUMMARY:event in cal2\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n";

    public static final String wrappedWithTab =
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Microsoft Corporation//Outlook 12.0 MIMEDIR//EN\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DESCRIPTION:When: Friday\\, March 30\\, 2007 8:00 AM-8:30 AM (GMT-07:00) Moun\r\n" +
            "\ttain Time (US & Canada).\\nWhere: Certified Service\\, Inc. \\n\\n*~*~*~*~*~*~\r\n" +
            "\t*~*~*~*\\n\\nGood morning Ray\\, \\n\\nFriday works for me.  Just let me know w\r\n" +
            "\that time.  \\n\\nThanks\\, \\n\\r\\n\r\n" +
            "UID:U1\r\n" +
            "DTSTAMP:20070228T183803Z\r\n" +
            "DTSTART;VALUE=DATE:20051018\r\n" +
            "DTEND;VALUE=DATE:20051019\r\n" +
            "SUMMARY:mySumm\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n";

    /**
     * Uri-encoding was causing problems with java.net.URL building (specifically we would end up
     * with URL's where the SchemeSpecificPart was "MAILTO%3Afoo%40bar.com")
     */
    @Test
    public void testUrisEncodeDecode() {
        String mailtourl = "mailto:foobar@example.net";
        String decoded = Uris.decode(mailtourl);
        Assert.assertEquals("Result of Decode", mailtourl, decoded);
        String encoded = Uris.encode(mailtourl);
        /**
         * This was failing on baseline ical4j-0.9.16 with:
         * junit.framework.ComparisonFailure: Result of Encode expected:<mailto[:foobar@]example.net>
         *                                                      but was:<mailto[%3Afoobar%40]example.net>
         */
        Assert.assertEquals("Result of Encode", mailtourl, encoded);
        mailtourl = "rubbishfoobar@example.net";
        decoded = Uris.decode(mailtourl);
        Assert.assertEquals("Result of Decode", mailtourl, decoded);
        encoded = Uris.encode(mailtourl);
        Assert.assertEquals("Result of Encode", mailtourl, encoded);
    }

    @Test
    public void testMultiVCALENDAR() throws IOException, ParserException, ServiceException {
        List<ZVCalendar> zvcals = doParse(multiVcalendar);
        Assert.assertNotNull("List of ZVCalendar", zvcals);
        Assert.assertEquals("Number of cals", 2,  zvcals.size());
    }

    @Test
    public void testTabWrappedLine() throws IOException, ParserException, ServiceException {
        List<ZVCalendar> zvcals = doParse(wrappedWithTab);
        Assert.assertNotNull("List of ZVCalendar", zvcals);
        Assert.assertEquals("Number of cals", 1,  zvcals.size());
    }

    /**
     * Bug 50398 - handling of empty CN parameter
     * An empty CN parameter should not affect the value of a property. With unpatched ical4j-0.9.16 it did.
     * It is acceptable to either drop the CN parameter or leave it as the empty string
     */
    @Test
    public void testEmptyCN() throws IOException, ParserException, ServiceException {
        List<ZVCalendar> zvcals = doParse(emptyCN);
        Assert.assertNotNull("List of ZVCalendar", zvcals);
        Assert.assertEquals("Number of cals", 1,  zvcals.size());
        ZVCalendar zvcal = zvcals.get(0);
        ZComponent vevent = zvcal.getComponent(ICalTok.VEVENT);
        Assert.assertNotNull("VEVENT", vevent);
        ZProperty orgProp = vevent.getProperty(ICalTok.ORGANIZER);
        // With unpatched ical4j-0.9.16 value is ":test1@invalid.dom"
        Assert.assertEquals("ORGANIZER value", "MAILTO:test1@invalid.dom", orgProp.getValue());
        ZParameter orgCNparam = orgProp.getParameter(ICalTok.CN);
        Assert.assertNotNull("ORGANIZER CN parameter", orgCNparam);
        String cnValue = orgCNparam.getValue();
        if (cnValue != null) {
            // Note that with ical4j-0.9.16-patched, the value is null
            Assert.assertEquals("ORGANIZER CN param value", "", cnValue);
        }
    }

    public static List<ZVCalendar> doParse(String ical)
            throws IOException, ParserException, ServiceException {
        ByteArrayInputStream bais = new ByteArrayInputStream (ical.getBytes(Charsets.UTF_8));
        List<ZVCalendar> zvcals = ZCalendar.ZCalendarBuilder.buildMulti(bais, Charsets.UTF_8.name());
        return zvcals;
    }
}
