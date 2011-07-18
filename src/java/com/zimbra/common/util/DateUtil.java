/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;

public class DateUtil {
    
    public static final String ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT = "yyyyMMddHHmmss'Z'";

    /**
     * to LDAP generalized time string
     */
    public static String toGeneralizedTime(Date date) {
        SimpleDateFormat fmt = new SimpleDateFormat(ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT);
        TimeZone tz = fmt.getCalendar().getTimeZone();
        Date gmtDate;
        if (tz.inDaylightTime(date))
            gmtDate = new Date(date.getTime() - (tz.getRawOffset() + tz.getDSTSavings()));
        else
            gmtDate = new Date(date.getTime() - tz.getRawOffset());
        return fmt.format(gmtDate);        
    }

    public static Date parseGeneralizedTime(String time) {
        return parseGeneralizedTime(time, true);
    }

    /**
     * from LDAP generalized time string
     */
    public static Date parseGeneralizedTime(String time, boolean strict) {
        int maxLen = strict ? 15 : 17;
        if (time.length() < 14 || time.length() > maxLen)
            return null;

        TimeZone tz;
        if (time.endsWith("Z"))
            tz = TimeZone.getTimeZone("GMT");
        else
            tz = TimeZone.getDefault();
        int year = Integer.parseInt(time.substring(0, 4));
        int month = Integer.parseInt(time.substring(4, 6)) - 1;  // months are 0 base
        int date = Integer.parseInt(time.substring(6, 8));
        int hour = Integer.parseInt(time.substring(8, 10));
        int min = Integer.parseInt(time.substring(10, 12));
        int sec = Integer.parseInt(time.substring(12, 14));

        Calendar calendar = new GregorianCalendar(tz);
        calendar.clear();
        calendar.set(year, month, date, hour, min, sec);
        return calendar.getTime();
    }

    /** Serializes a date in full ISO8601 date/time format.
     *    <pre>yyyy-MM-dd'T'HH:mm:ssZ</pre> */
    public static String toISO8601(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String result = format.format(date);
        // convert YYYYMMDDTHH:mm:ss+HH00 into YYYYMMDDTHH:mm:ss+HH:00 
        //  - note the added colon for the time zone
        result = result.substring(0, result.length()-2) + ":" + result.substring(result.length()-2);
        return result;
    }


    private static final String[] DAY_NAME = new String[] { "", "Sun, ", "Mon, ", "Tue, ", "Wed, ", "Thu, ", "Fri, ", "Sat, " };
    private static final String[] MONTH_NAME = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    public static String toImapDateTime(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        StringBuilder sb = new StringBuilder(40);
        append2DigitNumber(sb, cal.get(Calendar.DAY_OF_MONTH)).append('-');
        sb.append(MONTH_NAME[cal.get(Calendar.MONTH)]).append('-');
        sb.append(cal.get(Calendar.YEAR)).append(' ');

        append2DigitNumber(sb, cal.get(Calendar.HOUR_OF_DAY)).append(':');
        append2DigitNumber(sb, cal.get(Calendar.MINUTE)).append(':');
        append2DigitNumber(sb, cal.get(Calendar.SECOND)).append(' ');

        sb.append(getTimezoneString(cal));
        return sb.toString();
    }

    public static String getTimezoneString(Calendar cal) {
        int tzoffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
        char tzsign = tzoffset > 0 ? '+' : '-';
        tzoffset = Math.abs(tzoffset);

        StringBuilder sb = new StringBuilder(5);
        sb.append(tzsign);
        append2DigitNumber(sb, tzoffset / 60);
        append2DigitNumber(sb, tzoffset % 60);
        return sb.toString();
    }

    public static String toRFC822Date(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        String tzabbr = getTimezoneAbbreviation(cal.getTimeZone().getID(), cal.get(Calendar.DST_OFFSET) != 0);
        int tzoffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
        char tzsign = tzoffset > 0 ? '+' : '-';
        tzoffset = Math.abs(tzoffset);

        StringBuilder sb = new StringBuilder(40);
        sb.append(DAY_NAME[cal.get(Calendar.DAY_OF_WEEK)]);
        append2DigitNumber(sb, cal.get(Calendar.DAY_OF_MONTH)).append(' ');
        sb.append(MONTH_NAME[cal.get(Calendar.MONTH)]).append(' ');
        sb.append(cal.get(Calendar.YEAR)).append(' ');

        append2DigitNumber(sb, cal.get(Calendar.HOUR_OF_DAY)).append(':');
        append2DigitNumber(sb, cal.get(Calendar.MINUTE)).append(':');
        append2DigitNumber(sb, cal.get(Calendar.SECOND)).append(' ');

        sb.append(tzsign);
        append2DigitNumber(sb, tzoffset / 60);
        append2DigitNumber(sb, tzoffset % 60);
        if (tzabbr != null)
            sb.append(" (").append(tzabbr).append(')');
        return sb.toString();
    }

    private static StringBuilder append2DigitNumber(StringBuilder sb, int number) {
        return sb.append((char) ('0' + number / 10)).append((char) ('0' + number % 10));
    }

    private static final String[][] ZONE_INFO = new DateFormatSymbols().getZoneStrings();

    private static String getTimezoneAbbreviation(String tzid, boolean dst) {
        if (tzid == null)
            return null;

        for (int tzindex = 0; tzindex < ZONE_INFO.length; tzindex++) {
            if (tzid.equalsIgnoreCase(ZONE_INFO[tzindex][0]))
                return dst ? ZONE_INFO[tzindex][4] : ZONE_INFO[tzindex][2];
        }
        return null;
    }

    public static Date parseRFC2822Date(String encoded, Date fallback) {
        Calendar cal = parseRFC2822DateAsCalendar(encoded);
        return cal == null ? fallback : cal.getTime();
    }

    public static Calendar parseRFC2822DateAsCalendar(String encoded) {
        if (encoded == null)
            return null;

        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        try {
            int pos = skipCFWS(encoded, 0);
            while (encoded.charAt(pos) < '0' || encoded.charAt(pos) > '9')
                pos = skipCFWS(encoded, skipText(encoded, pos, false));

            pos = readCalendarField(encoded, pos, cal, Calendar.DAY_OF_MONTH);
            pos = readCalendarMonth(encoded, skipCFWS(encoded, pos), cal);
            pos = readCalendarField(encoded, skipCFWS(encoded, pos), cal, Calendar.YEAR);

            pos = readCalendarField(encoded, skipCFWS(encoded, pos), cal, Calendar.HOUR_OF_DAY);
            pos = skipChar(encoded, skipCFWS(encoded, pos), ':');
            pos = readCalendarField(encoded, skipCFWS(encoded, pos), cal, Calendar.MINUTE);
            pos = skipCFWS(encoded, pos);
            if (encoded.charAt(pos) == ':')
                pos = readCalendarField(encoded, skipCFWS(encoded, pos + 1), cal, Calendar.SECOND);
            readCalendarTimeZone(encoded, skipCFWS(encoded, pos), cal);

            return cal;
        } catch (ParseException e) {
            return null;
        }
    }

    private static int skipCFWS(String encoded, int start) throws ParseException {
        boolean escaped = false;
        for (int i = start, len = encoded.length(), comment = 0; i < len; i++) {
            char c = encoded.charAt(i);
            if (escaped)
                escaped = false;
            else if (c == '(')
                comment++;
            else if (c == ')' && comment > 0)
                comment--;
            else if (c == '\\' && comment > 0)
                escaped = true;
            else if (c != ' ' && c != '\t' && c != '\r' && c != '\n' && comment == 0)
                return i;
        }
        throw new ParseException("CFWS extends to end of header", start);
    }

    private static int skipChar(String encoded, int start, char skip) throws ParseException {
        if (start >= encoded.length() || encoded.charAt(start) != skip)
            throw new ParseException("missing expected character '" + skip + "'", start);
        return start + 1;
    }

    private static int skipText(String encoded, int start, boolean canTerminate) throws ParseException {
        for (int i = start, len = encoded.length(); i < len; i++) {
            char c = encoded.charAt(i);
            if (c == '(' || c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                if (start == i)
                    throw new ParseException("unexpected zero-length text string", start);
                return i;
            }
        }
        if (!canTerminate)
            throw new ParseException("skipped text extends to end of header", start);
        return encoded.length();
    }

    private static int readCalendarField(String encoded, int start, Calendar cal, int field) {
        int value = 0, i = start;
        for (int len = encoded.length(); i < len; i++) {
            char c = encoded.charAt(i);
            if (c < '0' || c > '9')
                break;
            value = value * 10 + c - '0';
        }

        if (field == Calendar.YEAR && value < 32)
            value += 2000;
        else if (field == Calendar.YEAR && value < 1000)
            value += 1900;
        cal.set(field, value);
        return i;
    }

    private static final Map<String, Integer> MONTH_NUMBER = new HashMap<String, Integer>(16);
        static {
            for (int i = 0; i < MONTH_NAME.length; i++)
                MONTH_NUMBER.put(MONTH_NAME[i].toUpperCase(), i);
        }

    private static int readCalendarMonth(String encoded, int start, Calendar cal) throws ParseException {
        int i = skipText(encoded, start, true);
        String monthabbr = encoded.substring(start, i);

        Integer month = MONTH_NUMBER.get(monthabbr.toUpperCase());
        if (month == null) {
            try {
                month = Integer.valueOf(monthabbr) - 1;
            } catch (NumberFormatException nfe) { }
        }
        if (month == null)
            throw new ParseException("invalid month abbreviation: " + monthabbr, start);

        cal.set(Calendar.MONTH, month);
        return i;
    }

    private static final Map<String, String> KNOWN_ZONES = new HashMap<String,String>(30);
        static {
            KNOWN_ZONES.put("UT",  "+0000");  KNOWN_ZONES.put("GMT", "+0000");
            KNOWN_ZONES.put("EDT", "-0400");  KNOWN_ZONES.put("EST", "-0500");
            KNOWN_ZONES.put("CDT", "-0500");  KNOWN_ZONES.put("CST", "-0600");
            KNOWN_ZONES.put("MDT", "-0600");  KNOWN_ZONES.put("MST", "-0700");
            KNOWN_ZONES.put("PDT", "-0700");  KNOWN_ZONES.put("PST", "-0800");
            KNOWN_ZONES.put("A",   "+0100");  KNOWN_ZONES.put("B",   "+0200");
            KNOWN_ZONES.put("C",   "+0300");  KNOWN_ZONES.put("D",   "+0400");
            KNOWN_ZONES.put("E",   "+0500");  KNOWN_ZONES.put("F",   "+0600");
            KNOWN_ZONES.put("G",   "+0700");  KNOWN_ZONES.put("H",   "+0800");
            KNOWN_ZONES.put("I",   "+0900");  KNOWN_ZONES.put("K",   "+1000");
            KNOWN_ZONES.put("L",   "+1100");  KNOWN_ZONES.put("M",   "+1200");
            KNOWN_ZONES.put("N",   "-0100");  KNOWN_ZONES.put("O",   "-0200");
            KNOWN_ZONES.put("P",   "-0300");  KNOWN_ZONES.put("Q",   "-0400");
            KNOWN_ZONES.put("R",   "-0500");  KNOWN_ZONES.put("S",   "-0600");
            KNOWN_ZONES.put("T",   "-0700");  KNOWN_ZONES.put("U",   "-0800");
            KNOWN_ZONES.put("V",   "-0900");  KNOWN_ZONES.put("W",   "-1000");
            KNOWN_ZONES.put("X",   "-1100");  KNOWN_ZONES.put("Y",   "-1200");
            KNOWN_ZONES.put("Z",   "+0000");
        }

    private static int readCalendarTimeZone(String encoded, int start, Calendar cal) throws ParseException {
        int i = skipText(encoded, start, true);
        // a la fin tu es las de ce monde ancien
        String zone = encoded.substring(start, i);

        int offset = 0;
        if (zone.charAt(0) != '-' && zone.charAt(0) != '+')
            zone = KNOWN_ZONES.get(zone.toUpperCase());
        if (zone != null) {
            try {
                int parsed = Integer.parseInt(zone.substring(1));
                offset = (zone.charAt(0) == '-' ? -1 : 1) * (parsed / 100 * 60 + parsed % 100) * 60000;
            } catch (NumberFormatException nfe) { }
        }

        cal.set(Calendar.ZONE_OFFSET, offset);
        cal.set(Calendar.DST_OFFSET, 0);
        return i;
    }


    public static Date parseISO8601Date(String encoded, Date fallback) {
        if (encoded == null)
            return fallback;
    
        // normalize format to "2005-10-19T16:25:38-0800"
        encoded = encoded.toUpperCase();
        int length = encoded.length();
        if (length == 4)
            encoded += "-01-01T00:00:00-0000";
        else if (length == 7)
            encoded += "-01T00:00:00-0000";
        else if (length == 10)
            encoded += "T00:00:00-0000";
        else if (length < 17)
            return fallback;
        else if (encoded.charAt(16) != ':')
            encoded = encoded.substring(0, 16) + ":00" + encoded.substring(16);
        else if (length >= 21 && encoded.charAt(19) == '.') {
            int pos = 20;
            while (pos < length && Character.isDigit(encoded.charAt(pos)))
                pos++;
            encoded = encoded.substring(0, 19) + encoded.substring(pos);
        }
    
        // timezone cleanup: this format understands '-0800', not '-08:00'
        int colon = encoded.lastIndexOf(':');
        if (colon > 19)
            encoded = encoded.substring(0, colon) + encoded.substring(colon + 1);
        // timezone cleanup: this format doesn't understand 'Z' or default timezones
        if (encoded.length() == 19)
            encoded += "-0000";
        else if (encoded.endsWith("Z"))
            encoded = encoded.substring(0, encoded.length() - 1) + "-0000";
    
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(encoded);
        } catch (ParseException e) {
            return fallback;
        }
    }
 
    private static final String ABSDATE_YFIRST_PATTERN = "(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})";

    private static final String ABSDATE_YLAST_PATTERN  = "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})";
    
    private static final String RELDATE_PATTERN        = "([mp+-]?)([0-9]+)([mhdwy][a-z]*)?";

    private static final String ABS_MILLISECS_PATTERN = "\\d+";
    
    private static final Pattern sAbsYFirstPattern = Pattern.compile(ABSDATE_YFIRST_PATTERN);
    private static final Pattern sAbsYLastPattern = Pattern.compile(ABSDATE_YLAST_PATTERN);
    private static final Pattern sRelDatePattern = Pattern.compile(RELDATE_PATTERN);
    private static final Pattern sAbsMillisecsDatePattern = Pattern.compile(ABS_MILLISECS_PATTERN);
    
    /**
     * parse a date specifier string. Examples are:
     * <pre> 
     * absolute dates:
     * 
     *  mm/dd/yyyy (i.e., 12/25/1998)
     *  yyyy/dd/mm (i.e., 1989/12/25)
     *  \\d+       (num milliseconds, i.e., 1132276598000)
     *  
     *  relative dates:
     *
     *  [mp+-]?([0-9]+)([mhdwy][a-z]*)?g
     * 
     *   p/+/{not-specified}   current time plus an offset (p and '' are supported for use in query params)
     *   m/-                   current time minus an offset
     *   
     *   (0-9)+    value
     *   
     *   ([mhdwy][a-z]*)  units, everything after the first character is ignored (except for "mi" case):
     *   m(onths)
     *   mi(nutes)
     *   d(ays)
     *   w(eeks)
     *   h(ours)
     *   y(ears)
     *   
     *  examples:
     *     1day     1 day from now
     *    +1day     1 day from now 
     *    p1day     1 day from now
     *    +60mi     60 minutes from now
     *    +1week    1 week from now
     *    +6mon     6 months from now 
     *    1year     1 year from now
     *
     * </pre>
     * 
     * @param dateStr
     * @param defaultValue
     * @return
     */
    public static long parseDateSpecifier(String dateStr, long defaultValue) {
        Date date = parseDateSpecifier(dateStr);
        return date == null ? defaultValue : date.getTime();
    }

    public static Date parseDateSpecifier(String dateStr) {
        try {
            Matcher m = sAbsMillisecsDatePattern.matcher(dateStr);
            String yearStr, monthStr, dayStr;

            if (m.matches()) {
                return new Date(Long.parseLong(dateStr));
            }
            m = sAbsYFirstPattern.matcher(dateStr);
            if (m.matches()) {
                yearStr = m.group(1);
                monthStr = m.group(2);                
                dayStr = m.group(3);                
                return new SimpleDateFormat("MM/dd/yyyy").parse(monthStr+"/"+dayStr+"/"+yearStr);
            }
            m = sAbsYLastPattern.matcher(dateStr);
            if (m.matches()) {
                monthStr = m.group(1);
                dayStr = m.group(2);                
                yearStr = m.group(3);
                return new SimpleDateFormat("MM/dd/yyyy").parse(monthStr+"/"+dayStr+"/"+yearStr);
            }
            m = sRelDatePattern.matcher(dateStr);
            if (m.matches()) {
                String ss = m.group(1);
                int sign = (ss == null || ss.equals("") || ss.equals("+") || ss.equals("p")) ? 1 : -1;
                int value = Integer.parseInt(m.group(2)) * sign;                
                String unitsStr = m.group(3);
                int field = Calendar.DATE;
                if (unitsStr != null && unitsStr.length() > 0) {
                    switch (unitsStr.charAt(0)) {
                    case 'm':
                        field = (unitsStr.length() > 1 && unitsStr.charAt(1) == 'i') ? Calendar.MINUTE : Calendar.MONTH;
                        break;
                    case 'h':
                        field = Calendar.HOUR;
                        break;
                    case 'd':
                        field = Calendar.DATE;
                        break;
                    case 'w':
                        field = Calendar.WEEK_OF_YEAR;
                        break;
                    case 'y':
                        field = Calendar.YEAR;
                        break;                        
                    }
                }
                Calendar cal = new GregorianCalendar();
                cal.setTime(new Date());
                cal.add(field, value);
                return cal.getTime();
            }
        } catch (Exception e) {
            //
        }
        return null;
    }
    
    /**
     * Returns the number of milliseconds specified by the time interval value.
     * The format of the time interval value is one of the following, where <tt>NN</tt>
     * is a number:
     * <ul>
     *   <li>NNd - days</li>
     *   <li>NNh - hours</li>
     *   <li>NNm - minutes</li>
     *   <li>NNs - seconds</li>
     *   <li>NNms - milli seconds</li>
     *   <li>NN - seconds</li>
     * </ul>
     * @param value the time interval value
     * @param defaultValue returned if the time interval is null or cannot be parsed
     */
    public static long getTimeInterval(String value, long defaultValue) {
        if (value == null || value.length() == 0)
            return defaultValue;
        else {
            try {
                char units = value.charAt(value.length()-1);
                if (units >= '0' && units <= '9') {
                    return Long.parseLong(value)*1000;
                } else {
                    if (value.endsWith("ms")) {
                        return Long.parseLong(value.substring(0, value.length()-2));
                    }
                    
                    long n = Long.parseLong(value.substring(0, value.length()-1));
                    
                    switch (units) {
                    case 'd':
                        n = n * Constants.MILLIS_PER_DAY;
                        break;
                    case 'h':
                        n = n * Constants.MILLIS_PER_HOUR;
                        break;
                    case 'm':
                        n = n * Constants.MILLIS_PER_MINUTE;
                        break;
                    case 's':
                        n = n * Constants.MILLIS_PER_SECOND;
                        break;
                    default:
                        return defaultValue;
                    }
                    return n;
                }
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }
    
    public static long getTimeInterval(String value) throws ServiceException {
        if (value == null || value.length() == 0)
            throw ServiceException.FAILURE("no value", null);
        else {
            try {
                char units = value.charAt(value.length()-1);
                if (units >= '0' && units <= '9') {
                    return Long.parseLong(value)*1000;
                } else {
                    if (value.endsWith("ms")) {
                        return Long.parseLong(value.substring(0, value.length()-2));
                    }
                    
                    long n = Long.parseLong(value.substring(0, value.length()-1));
                    switch (units) {
                    case 'd':
                        n = n * Constants.MILLIS_PER_DAY;
                        break;
                    case 'h':
                        n = n * Constants.MILLIS_PER_HOUR;
                        break;
                    case 'm':
                        n = n * Constants.MILLIS_PER_MINUTE;
                        break;
                    case 's':
                        n = n * Constants.MILLIS_PER_SECOND;
                        break;
                    default:
                        throw ServiceException.FAILURE("unknown unit", null);
                    }
                    return n;
                }
            } catch (NumberFormatException e) {
                throw ServiceException.FAILURE("invalid format", e);
            }
        }
    }
    
    /**
     * Returns the number of seconds specified by the time interval value.
     * The format of the time interval value is one of the following, where <tt>NN</tt>
     * is a number:
     * <ul>
     *   <li>NNd - days</li>
     *   <li>NNh - hours</li>
     *   <li>NNm - minutes</li>
     *   <li>NNs - seconds</li>
     *   <li>NNms - milli seconds</li>
     *   <li>NN - seconds</li>
     * </ul>
     * 
     * If the value is in ms (milli seconds), round to the nearest second.
     * 
     * @param value the time interval value
     * @param defaultValue returned if the time interval is null or cannot be parsed
     */
    public static long getTimeIntervalSecs(String value, long defaultValue) {
        /*
         * like getTimeInterval, but return interval in seconds, saving the overhead 
         * for code that needs seconds but gets milliseconds from getTimeInterval and 
         * has to convert it back seconds.
         * 
         * If the value is in ms, round to the nearest second.
         */
        if (value == null || value.length() == 0)
            return defaultValue;
        else {
            try {
                char units = value.charAt(value.length()-1);
                if (units >= '0' && units <= '9') {
                    return Long.parseLong(value);
                } else {
                    if (value.endsWith("ms")) {
                        // round up to the next second
                        long ms = Long.parseLong(value.substring(0, value.length()-2));
                        return Math.round((float)ms/Constants.MILLIS_PER_SECOND);
                    }
                    
                    long n = Long.parseLong(value.substring(0, value.length()-1));
                    switch (units) {
                    case 'd':
                        n = n * Constants.SECONDS_PER_DAY;
                        break;
                    case 'h':
                        n = n * Constants.SECONDS_PER_HOUR;
                        break;
                    case 'm':
                        n = n * Constants.SECONDS_PER_MINUTE;
                        break;
                    case 's':
                        break;
                    default:
                        return defaultValue;
                    }
                    return n;
                }
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    public static void main(String args[]) {
        System.out.println("date = " + new Date(parseDateSpecifier("12/25/1998", 0)));
        System.out.println("date = " + new Date(parseDateSpecifier("1989/12/25", 0)));
        System.out.println("date = " + new Date(parseDateSpecifier("1day", 0)));        
        System.out.println("date = " + new Date(parseDateSpecifier("+1day", 0)));
        System.out.println("date = " + new Date(parseDateSpecifier("+10day", 0)));
        System.out.println("date = " + new Date(parseDateSpecifier("+60minute", 0)));                
        System.out.println("date = " + new Date(parseDateSpecifier("+1week", 0)));        
        System.out.println("date = " + new Date(parseDateSpecifier("+1month", 0)));                
        System.out.println("date = " + new Date(parseDateSpecifier("+1year", 0)));                        
        System.out.println("date = " + new Date(parseDateSpecifier("-1day", 0)));
        System.out.println("date = " + new Date(parseDateSpecifier("1132276598000", 0)));
        System.out.println("date = " + new Date(parseDateSpecifier("p10day", 0)));

        System.out.println("iso = " + parseISO8601Date("2009-11-20", new Date()));
        System.out.println("iso = " + parseISO8601Date("2009-11-20T13:55:49Z", new Date()));
        System.out.println("iso = " + parseISO8601Date("2009-11-20T13:55:49.823", new Date()));
        System.out.println("iso = " + parseISO8601Date("2009-11-20t13:55:49.724z", new Date()));

        System.out.println(parseRFC2822Date("01 May 2007 09:27 -0600", new Date()));
        System.out.println(parseRFC2822Date("01 Apr 2005 18:20:24 -0800", new Date()));
        System.out.println(parseRFC2822Date("Fri, 10 Oct 2003 12:52:52 -0700", new Date()));
        System.out.println(parseRFC2822Date("Wed, 27 Apr 2005 11:14:18 -0700 (PDT)", new Date()));
        System.out.println(parseRFC2822Date("Fri, 8 Feb 2008 00:56:02 -0500 (EST)", new Date()));
        System.out.println(parseRFC2822Date("Wed, 27 Apr 2005 15:37:37 PDT", new Date()));
        System.out.println(parseRFC2822Date("21 Nov 97 09:55:06 GMT", new Date()));
        System.out.println(parseRFC2822Date("Tue,  1 May 2007 09:41(() )():(() )()26 -0700", new Date()));

        System.out.println(parseRFC2822Date("21 Nov 97 09::06 GMT", new Date()));
        System.out.println(parseRFC2822Date("21 nOV 97 09:55:06 R", new Date()));
        System.out.println(parseRFC2822Date("21 11 06 09:55:06 GMT", new Date()));

        System.out.println(toImapDateTime(new Date()));

        /*
        System.out.println("20070318050124Z" + "-default = " +  parseGeneralizedTime("20070318050124Z"));
        System.out.println("20070318050124Z" + "-true = " +  parseGeneralizedTime("20070318050124Z", true));
        System.out.println("20070318050124Z" + "-false = " +  parseGeneralizedTime("20070318050124Z", false));

        System.out.println("20070318050124.0Z" + "-default = " +  parseGeneralizedTime("20070318050124.0Z"));
        System.out.println("20070318050124.0Z" + "-true = " +  parseGeneralizedTime("20070318050124.0Z", true));
        System.out.println("20070318050124.0Z" + "-false = " +  parseGeneralizedTime("20070318050124.0Z", false));
        */

        java.util.Calendar cal = DateUtil.parseRFC2822DateAsCalendar("21 Nov 97 09:55:06 GMT");
        if (cal != null) {
            java.text.DateFormat format = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, java.util.Locale.UK);
            System.out.println(format.format(cal.getTime()));
            format.setTimeZone(TimeZone.getTimeZone("GMT" + DateUtil.getTimezoneString(cal)));
            System.out.println(format.format(cal.getTime()));
        }
    }
}
