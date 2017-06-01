/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;

public class ParsedDateTimeTest {
    
    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("zimbra.config", "../store/src/java-test/localconfig-test.xml");
    }
    /*
     * Tests the date conversion during the day light cross over dates for allday events.
     */
    
    @Test
    public void testCrossoverAllday() throws Exception {
        ICalTimeZone localTZ = new ICalTimeZone(
                "Custom TZ",
                -10800000, "16010101T000000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=2;BYDAY=-1SU", "FOO",
                -7200000, "16010101T000000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=10;BYDAY=3SU", "BAR");
        
        // Configure test timezones.ics file.
        File tzFile = File.createTempFile("timezones-", ".ics");
        BufferedWriter writer= new BufferedWriter(new FileWriter(tzFile));
        writer.write("BEGIN:VCALENDAR\r\nEND:VCALENDAR");
        writer.close();
        TimeZoneMap tzmap = new TimeZoneMap(localTZ);
        LC.timezone_file.setDefault(tzFile.getAbsolutePath());
        
        ParsedDateTime pd;
        // Cross-over dates in October
        pd = ParsedDateTime.parse("20121020", tzmap, null, localTZ);
        Assert.assertEquals("20121020", pd.getDateTimePartString());
        pd = pd.add(ParsedDuration.ONE_DAY);
        Assert.assertEquals("20121021", pd.getDateTimePartString());
        pd = ParsedDateTime.parse("20121021", tzmap, null, localTZ);
        Assert.assertEquals("20121021", pd.getDateTimePartString());
        pd = ParsedDateTime.parse("20121022", tzmap, null, localTZ);
        Assert.assertEquals("20121022", pd.getDateTimePartString());

        // Cross-over dates in February.
        pd = ParsedDateTime.parse("20120225", tzmap, null, localTZ);
        Assert.assertEquals("20120225", pd.getDateTimePartString());
        pd = ParsedDateTime.parse("20120226", tzmap, null, localTZ);
        Assert.assertEquals("20120226", pd.getDateTimePartString());
        pd = ParsedDateTime.parse("20120227", tzmap, null, localTZ);
        Assert.assertEquals("20120227", pd.getDateTimePartString());
    }
}
