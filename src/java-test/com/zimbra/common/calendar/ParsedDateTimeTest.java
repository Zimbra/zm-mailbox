/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.common.calendar;

import junit.framework.Assert;

import org.junit.Test;


public class ParsedDateTimeTest {
    
    /*
     * Tests the date conversion during the day light cross over dates for allday events.
     */
    
    @Test
    public void testCrossoverAllday() throws Exception {
        ICalTimeZone localTZ = new ICalTimeZone(
                "Custom TZ",
                -10800000, "16010101T000000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=2;BYDAY=-1SU", "FOO",
                -7200000, "16010101T000000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=10;BYDAY=3SU", "BAR");
        TimeZoneMap tzmap = new TimeZoneMap(localTZ);
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
