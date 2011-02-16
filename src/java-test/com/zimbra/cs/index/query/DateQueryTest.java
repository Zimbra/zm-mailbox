/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.index.query;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.document.DateTools;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

/**
 * Unit test for {@link DateQuery}.
 *
 * @author ysasaki
 */
public class DateQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();
        prov.createAccount("zero@zimbra.com", "secret",
                Collections.<String, Object> singletonMap(Provisioning.A_zimbraId, "0-0-0"));
        Provisioning.setInstance(prov);
    }

    @Test
    public void parseAbsoluteDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String expected = "Q(DATE,DATE,201001230000-201001240000)";

        query.parseDate("1/23/2010", tz, Locale.ENGLISH);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("23/1/2010", tz, Locale.FRENCH);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("23.1.2010", tz, Locale.GERMAN);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("23/1/2010", tz, Locale.ITALIAN);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("2010/1/23", tz, Locale.JAPANESE);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("2010. 1. 23", tz, Locale.KOREAN);
        Assert.assertEquals(expected, query.toString());
    }

    @Test
    public void parseRelativeDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        TimeZone tz = TimeZone.getTimeZone("UTC");

        query.parseDate("+2mi", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getMinute(2) + "-" + getMinute(3) + ")", query.toString());

        query.parseDate("+2minute", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getMinute(2) + "-" + getMinute(3) + ")", query.toString());

        query.parseDate("+2minutes", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getMinute(2) + "-" + getMinute(3) + ")", query.toString());

        query.parseDate("+2h", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getHour(2) + "-" + getHour(3) + ")", query.toString());

        query.parseDate("+2hour", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getHour(2) + "-" + getHour(3) + ")", query.toString());

        query.parseDate("+2hours", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getHour(2) + "-" + getHour(3) + ")", query.toString());

        query.parseDate("+2d", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getDate(2) + "-" + getDate(3) + ")", query.toString());

        query.parseDate("+2day", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getDate(2) + "-" + getDate(3) + ")", query.toString());

        query.parseDate("+2days", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getDate(2) + "-" + getDate(3) + ")", query.toString());

        query.parseDate("+2w", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getWeek(2) + "-" + getWeek(3) + ")", query.toString());

        query.parseDate("+2week", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getWeek(2) + "-" + getWeek(3) + ")", query.toString());

        query.parseDate("+2weeks", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getWeek(2) + "-" + getWeek(3) + ")", query.toString());

        query.parseDate("+2m", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getMonth(2) + "-" + getMonth(3) + ")", query.toString());

        query.parseDate("+2month", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getMonth(2) + "-" + getMonth(3) + ")", query.toString());

        query.parseDate("+2months", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getMonth(2) + "-" + getMonth(3) + ")", query.toString());

        query.parseDate("+2y", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getYear(2) + "-" + getYear(3) + ")", query.toString());

        query.parseDate("+2year", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getYear(2) + "-" + getYear(3) + ")", query.toString());

        query.parseDate("+2years", tz, Locale.ENGLISH);
        Assert.assertEquals("Q(DATE,DATE," + getYear(2) + "-" + getYear(3) + ")", query.toString());
    }

    private String getMinute(int minute) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.MINUTE, minute);
        return DateTools.dateToString(cal.getTime(), DateTools.Resolution.MINUTE);
    }

    private String getHour(int hour) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.HOUR, hour);
        cal.set(Calendar.MINUTE, 0);
        return DateTools.dateToString(cal.getTime(), DateTools.Resolution.MINUTE);
    }

    private String getDate(int day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.DATE, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        return DateTools.dateToString(cal.getTime(), DateTools.Resolution.MINUTE);
    }

    private String getWeek(int week) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.WEEK_OF_YEAR, week);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        return DateTools.dateToString(cal.getTime(), DateTools.Resolution.MINUTE);
    }

    private String getMonth(int month) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        return DateTools.dateToString(cal.getTime(), DateTools.Resolution.MINUTE);
    }

    private String getYear(int year) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        return DateTools.dateToString(cal.getTime(), DateTools.Resolution.MINUTE);
    }

    @Test
    public void parseInvalidDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        TimeZone tz = TimeZone.getTimeZone("UTC");

        try {
            query.parseDate("-1/1/2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }

        try {
            query.parseDate("1/-1/2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }

        try {
            query.parseDate("1/1/-2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }

        try {
            query.parseDate("111/1/2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }

        try {
            query.parseDate("1/111/2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }
    }

    @Test
    public void parseDateFallback() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        query.parseDate("1/23/2010", TimeZone.getTimeZone("UTC"), Locale.GERMAN);
        Assert.assertEquals("Q(DATE,DATE,201001230000-201001240000)", query.toString());
    }

}
