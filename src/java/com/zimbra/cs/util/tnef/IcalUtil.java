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
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

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

public class IcalUtil {

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
     *
     * @param minsSince1601 is minutes since start of 1601 (used in MAPI FILETIME)
     * @param tzd
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
                return new DateTime(localCalendar.getTime());
    }

    /**
    *
    * @param minsSince1601 is minutes since start of 1601 (used in MAPI FILETIME)
    * @param tz
    * @return
    */
   public static DateTime localMinsSince1601toUtcDate(long minsSince1601, TimeZoneDefinition tzd) {
               DateTime localDate = IcalUtil.localMinsSince1601toDate(minsSince1601, tzd);
               if ( (tzd == null) || tzd.getTimeZone().getID().equals(TimeZones.UTC_ID) ) {
                   return localDate;
               }
               GregorianCalendar utcCalendar =
                   new GregorianCalendar(java.util.TimeZone.getTimeZone(TimeZones.UTC_ID));

               utcCalendar.setTimeInMillis(localDate.getTime());
               return new DateTime(utcCalendar.getTime());
   }
}
