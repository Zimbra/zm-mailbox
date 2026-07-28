package com.zimbra.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class DateParserTest {

    @Test
    public void formatUsesDefaultTimezoneWhenTimezoneIsNull() {
        DateParser parser = new DateParser("yyyyMMdd");
        Date date = new Date(0L);

        SimpleDateFormat expected = new SimpleDateFormat("yyyyMMdd");
        expected.setTimeZone(TimeZone.getDefault());

        assertEquals(expected.format(date), parser.format(date, null));
    }

    @Test
    public void formatUsesExplicitTimezoneWhenProvided() {
        DateParser parser = new DateParser("yyyyMMdd");
        Date date = new Date(0L);

        SimpleDateFormat expected = new SimpleDateFormat("yyyyMMdd");
        expected.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        assertEquals(expected.format(date), parser.format(date, "America/New_York"));
    }

    @Test
    public void parseUsesExplicitTimezoneWhenProvided() {
        DateParser parser = new DateParser("yyyyMMdd");
        Date parsed = parser.parse("20200101", "America/New_York");

        assertNotNull(parsed);
    }
}
