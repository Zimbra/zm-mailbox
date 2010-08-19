/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.util.tnef;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.mapi.TimeZoneDefinition;
import java.util.Iterator;

import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.util.TimeZones;
import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFUtils;

public class IcalUtil {

    static Log sLog = ZimbraLog.tnef;
    
    public static void addProperty(ContentHandler icalOutput, Property icalProp)
            throws ParserException, URISyntaxException,IOException, ParseException {
        if (icalProp == null) {
            return;
        }
        icalOutput.startProperty(icalProp.getName());
        icalOutput.propertyValue(icalProp.getValue());
        ParameterList plist = icalProp.getParameters();
        if (plist != null) {
            for (Iterator <?> it = plist.iterator(); it.hasNext();) {
                Object currObj = it.next();
                if (currObj instanceof Parameter) {
                    Parameter parameter = (Parameter) currObj;
                    icalOutput.parameter(parameter.getName(), parameter.getValue());
                }
            }
        }
        icalOutput.endProperty(icalProp.getName());
    }

    public static void addProperty(ContentHandler icalOutput, String propName, String propValue, boolean emptyIsOk)
            throws ParserException, URISyntaxException,IOException, ParseException {
        if ( (!emptyIsOk) && ((propValue == null) || propValue.equals("")) ) {
            return;
        }
        icalOutput.startProperty(propName);
        if (propValue == null) {
            icalOutput.propertyValue("");
        } else {
            icalOutput.propertyValue(propValue);
        }
        icalOutput.endProperty(propName);
    }

    public static void addProperty(ContentHandler icalOutput, String propName, Object propValue, boolean emptyIsOk)
            throws ParserException, URISyntaxException,IOException, ParseException {
        if (propValue == null) {
            addProperty(icalOutput, propName, "", emptyIsOk);
        } else {
            addProperty(icalOutput, propName, propValue.toString(), emptyIsOk);
        }
    }

    public static void addProperty(ContentHandler icalOutput, String propName, String propValue)
            throws ParserException, URISyntaxException,IOException, ParseException {
        icalOutput.startProperty(propName);
        icalOutput.propertyValue(propValue);
        icalOutput.endProperty(propName);
    }

    public static String iCalDateTimeValue(DateTime actualTime,
            java.util.TimeZone javaTZ, boolean useDateOnly) {
        if (!useDateOnly) {
            return actualTime.toString();
        }
        String DATE_PATTERN = "yyyyMMdd";
        DateFormat dateOnlyFormat = new SimpleDateFormat(DATE_PATTERN);
        Calendar actualCalendar = new GregorianCalendar(javaTZ);
        actualCalendar.setTimeInMillis(actualTime.getTime());
        dateOnlyFormat.setCalendar(actualCalendar);
        return dateOnlyFormat.format(actualTime);
    }

    /**
     * Use when a UTC equivalent of a DateTime is required with the trailing Z indicating
     * that it is UTC
     * @param localTimeSince1601
     * @param tzDef
     * @return
     */
    public static String icalUtcTime(long localTimeSince1601, TimeZoneDefinition tzDef) {
        DateTime localTime = IcalUtil.localMinsSince1601toDate(
                                localTimeSince1601, tzDef);
        SimpleDateFormat utcTimeFmt = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        java.util.TimeZone utcTZ = TimeZone.getTimeZone(TimeZones.UTC_ID);
        utcTimeFmt.setTimeZone(utcTZ);
        return utcTimeFmt.format(localTime);
    }

    /**
     * Intended for diagnostic purposes.
     * @param localTimeSince1601 is the number of minutes since the start of 1601 in <code>tzDef</code>
     * @param tzDef
     * @return
     */
    public static String friendlyLocalTime(long localTimeSince1601, TimeZoneDefinition tzDef) {
        DateTime localTime = IcalUtil.localMinsSince1601toDate(
                                localTimeSince1601, tzDef);
        SimpleDateFormat localTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        java.util.TimeZone tz;
        if (tzDef == null) {
            tz = TimeZone.getTimeZone(TimeZones.UTC_ID);
        } else {
            tz = tzDef.getTimeZone();
        }
        localTimeFmt.setTimeZone(tz);
        return localTimeFmt.format(localTime);
    }

    /**
     * For use when the utcTime is based on a MAPI property whose value is UTC based rather than
     * localtime based.
     * @param icalOutput
     * @param propName
     * @param utcTime
     * @param tzd
     * @param useDateOnly
     * @throws ParserException
     * @throws URISyntaxException
     * @throws ParseException
     * @throws IOException
     */
    public static void addPropertyFromUtcTimeAndZone(
            ContentHandler icalOutput, String propName,
            DateTime utcTime, TimeZoneDefinition tzd,
            boolean useDateOnly) throws ParserException, URISyntaxException, ParseException, IOException {
        if (utcTime == null) {
            return;
        }
        boolean useUtc = true;
        java.util.TimeZone javaTZ = null;
        DateTime localTime = utcTime;
        if (tzd != null) {
            TimeZone tz = tzd.getTimeZone();
            javaTZ = tz;
            if (!tz.getID().equals(TimeZones.UTC_ID)) {
                useUtc = false;
                Calendar actualCalendar = new GregorianCalendar(tz);
                actualCalendar.setTimeInMillis(utcTime.getTime());
                localTime = new DateTime(actualCalendar.getTime());
                localTime.setTimeZone(tz);
            }
        } else {
            javaTZ = TimeZone.getTimeZone(TimeZones.UTC_ID);
        }
        icalOutput.startProperty(propName);
        icalOutput.propertyValue(iCalDateTimeValue(localTime, javaTZ, useDateOnly));
        if (!useUtc) {
            icalOutput.parameter(Parameter.TZID, javaTZ.getID());
        }
        if (useDateOnly) {
            icalOutput.parameter(Parameter.VALUE, Value.DATE.getValue());
        }
        icalOutput.endProperty(propName);
    }

    /**
     * Useful for handling date related properties based on MAPI properties whose value
     * is specified as the offset from the Microsoft EPOC in 1601 BUT adjusted for localtime.
     * i.e. <code>floatingDate</code> is not an accurate date but if it is treated as a UTC
     * based date it will have the correct components.
     *
     * @param icalOutput
     * @param propName
     * @param floatingDate if treated as a UTC time then day/month/year etc will be correct.
     * @throws ParserException
     * @throws URISyntaxException
     * @throws ParseException
     * @throws IOException
     */
    public static void addFloatingDateProperty(
            ContentHandler icalOutput, String propName,
            DateTime floatingDate) throws ParserException, URISyntaxException, ParseException, IOException {
        if (floatingDate == null) {
            return;
        }
        java.util.TimeZone javaTZ = null;
        javaTZ = TimeZone.getTimeZone(TimeZones.UTC_ID);
        icalOutput.startProperty(propName);
        icalOutput.propertyValue(iCalDateTimeValue(floatingDate, javaTZ, true));
        icalOutput.parameter(Parameter.VALUE, Value.DATE.getValue());
        icalOutput.endProperty(propName);
    }

    /**
     *
     * @param minsSince1601 is minutes since start of 1601 in local time (used in MAPI FILETIME)
     * @param tzd - local timezone applicable to <code>minseSince1601</code>
     * @return
     */
    public static DateTime localMinsSince1601toDate(long minsSince1601, TimeZoneDefinition tzd) {
        long millisecs = minsSince1601 * 60L * 1000L; // to milliseconds
        // subtract milliseconds between 1/1/1601 and 1/1/1970 (including 89 leap year days)
        millisecs = millisecs - 1000L*60L*60L*24L*(365L*369L+89L);
        // time is in localtime.  We need UTC
        GregorianCalendar utcCalendar =
            new GregorianCalendar(java.util.TimeZone.getTimeZone(TimeZones.UTC_ID));
        utcCalendar.setTimeInMillis(millisecs);
        if ( (tzd == null) || tzd.getTimeZone().getID().equals(TimeZones.UTC_ID) ) {
            return new DateTime(utcCalendar.getTime());
        }
        // At this point, the fields in utcCalendar represent the localtime
        // values of YEAR/MONTH/DAY/HOUR etc - so, create a new object in the correct
        // local timezone and copy the fields across.
        GregorianCalendar localCalendar = new GregorianCalendar(tzd.getTimeZone());
        localCalendar.set(utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                utcCalendar.get(Calendar.HOUR_OF_DAY),
                utcCalendar.get(Calendar.MINUTE),
                utcCalendar.get(Calendar.SECOND));
        DateTime localTime = new DateTime(localCalendar.getTime());
        return localTime;
    }

    /**
     * Missing method from RawInputStream
     * @param ris
     * @return
     * @throws IOException
     */
    public static Long readI32(RawInputStream ris) throws IOException {
        return (ris.readU8() | (ris.readU8() << 8) | (ris.readU8() << 16) | (ris.readU8() << 24)) & 0xFFFFFFFFFFFFFFFFL;
    }

    /**
     * Missing method from RawInputStream
     * @param ris
     * @param len
     * @param oemCodePage
     * @return
     * @throws IOException
     */
    public static String readString(RawInputStream ris, int len, String oemCodePage) throws IOException {
        String myString = null;
        if (oemCodePage != null) {
            if (oemCodePage.equals("Cp932")) {
                oemCodePage = new String("Cp942"); // IBM OS/2 Japanese, superset of Cp932
            }
        }
        byte[] asBytes = ris.readBytes(len);
        try {
            myString = TNEFUtils.removeTerminatingNulls(new String(asBytes, oemCodePage));
        } catch (UnsupportedEncodingException uee) {
            sLog.debug("Problem with oemCodePage=%s", oemCodePage, uee);
            myString = TNEFUtils.createString(asBytes, 0, len);
        }
        return myString;
    }
 
    public static String toHexString(byte[] bytes, int offset, int len) {
        if (bytes == null) {
            return "";
        }
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < len; i++) {
            String b = Integer.toHexString(bytes[offset + i] & 0xFF).toUpperCase();
            if (b.length() == 1) {
                s.append('0');
            }
            s.append(b);
        }
        return s.toString();
    }
}
