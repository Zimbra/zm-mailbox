/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.MailDateFormat;

public class DateUtil {

    /**
     * to LDAP generalized time string
     */
    public static String toGeneralizedTime(Date date) {
        SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        Date gmtDate;
        if (fmt.getCalendar().getTimeZone().inDaylightTime(date))
            gmtDate = new Date(date.getTime() -
                               fmt.getCalendar().getTimeZone().getRawOffset() -
                               fmt.getCalendar().getTimeZone().getDSTSavings());
        else
            gmtDate =
                new Date(date.getTime() -
                         fmt.getCalendar().getTimeZone().getRawOffset());
        return (fmt.format(gmtDate));        
    }

    public static Date parseGeneralizedTime(String time) {
        return parseGeneralizedTime(time, true);
    }
    
    /**
     * from LDAP generalized time string
     */
    public static Date parseGeneralizedTime(String time, boolean strict) {
        int maxLen;
        if (strict)
            maxLen = 15;
        else
            maxLen = 17;
        
    	if (time.length() < 14 || time.length() > maxLen)
    		return null;
    	TimeZone tz;
    	if (time.endsWith("Z"))
    		tz = TimeZone.getTimeZone("GMT");
    	else
    		tz = TimeZone.getDefault();
    	int year = Integer.parseInt(time.substring(0, 4));
    	int month = Integer.parseInt(time.substring(4, 6))-1;  // months are 0 base
    	int date = Integer.parseInt(time.substring(6, 8));
    	int hour = Integer.parseInt(time.substring(8, 10));
    	int min = Integer.parseInt(time.substring(10, 12));
    	int sec = Integer.parseInt(time.substring(12, 14));
    	Calendar calendar = new GregorianCalendar(tz);
    	calendar.set(year, month, date, hour, min, sec);
    	return calendar.getTime();
    }

    /**
     * full date/time yyyy-MM-dd'T'HH:mm:ssZ
     * @param date
     * @return
     */
    public static String toISO8601(Date date)
    {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
      String result = format.format(date);
      //convert YYYYMMDDTHH:mm:ss+HH00 into YYYYMMDDTHH:mm:ss+HH:00 
      //- note the added colon for the Timezone
      result = result.substring(0, result.length()-2) 
        + ":" + result.substring(result.length()-2);
      return result;
    }

    public static String toRFC822Date(Date date) {
        return new MailDateFormat().format(date);
    }
    
    public static Date parseRFC2822Date(String encoded, Date fallback) {
        if (encoded == null)
            return fallback;
        try {
            return new MailDateFormat().parse(encoded);
        } catch (ParseException e) {
            return fallback;
        }
    }

    public static Date parseISO8601Date(String encoded, Date fallback) {
        if (encoded == null)
            return fallback;
    
        // normalize format to "2005-10-19T16:25:38-0800"
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
        else if (length >= 22 && encoded.charAt(19) == '.')
            encoded = encoded.substring(0, 19) + encoded.substring(21);
    
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
                GregorianCalendar cal = new GregorianCalendar();
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
    
    /**
     * Returns the number of seconds specified by the time interval value.
     * The format of the time interval value is one of the following, where <tt>NN</tt>
     * is a number:
     * <ul>
     *   <li>NNd - days</li>
     *   <li>NNh - hours</li>
     *   <li>NNm - minutes</li>
     *   <li>NNs - seconds</li>
     *   <li>NN - seconds</li>
     * </ul>
     * @param value the time interval value
     * @param defaultValue returned if the time interval is null or cannot be parsed
     */
    public static long getTimeIntervalSecs(String value, long defaultValue) {
        /*
         * like getTimeInterval, but return interval in seconds, saving the overhead 
         * for code that needs seconds but gets milliseconds from getTimeInterval and 
         * has to convert it back seconds.
         */
        if (value == null || value.length() == 0)
            return defaultValue;
        else {
            try {
                char units = value.charAt(value.length()-1);
                if (units >= '0' && units <= '9') {
                    return Long.parseLong(value);
                } else {
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

        /*
        System.out.println("20070318050124Z" + "-default = " +  parseGeneralizedTime("20070318050124Z"));
        System.out.println("20070318050124Z" + "-true = " +  parseGeneralizedTime("20070318050124Z", true));
        System.out.println("20070318050124Z" + "-false = " +  parseGeneralizedTime("20070318050124Z", false));

        System.out.println("20070318050124.0Z" + "-default = " +  parseGeneralizedTime("20070318050124.0Z"));
        System.out.println("20070318050124.0Z" + "-true = " +  parseGeneralizedTime("20070318050124.0Z", true));
        System.out.println("20070318050124.0Z" + "-false = " +  parseGeneralizedTime("20070318050124.0Z", false));
        */
    }
}
