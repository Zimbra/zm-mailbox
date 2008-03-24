/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ZDateTime {

    private static final String FMT_DATE = "yyyyMMdd";
    private static final String FMT_DATE_TIME = "yyyyMMdd'T'HHmmss";

    private String mDateTime;
    private String mTimeZoneId;

    public ZDateTime() {

    }

    public ZDateTime(long utcMsecs, boolean dateOnly, TimeZone tz) {
        DateFormat dateFmt = new SimpleDateFormat(dateOnly ? FMT_DATE : FMT_DATE_TIME);
        dateFmt.setTimeZone(tz);
        mTimeZoneId = TZIDMapper.canonicalize(tz.getID());
        mDateTime = dateFmt.format(new Date(utcMsecs));
    }

    public ZDateTime(String dateTime) {
        mDateTime = dateTime;
    }

    public ZDateTime(String dateTime, String timeZone) {
        mDateTime = dateTime;
        mTimeZoneId = timeZone;
    }

    public ZDateTime(Element e) throws ServiceException {
        mDateTime = e.getAttribute(MailConstants.A_CAL_DATETIME);
        mTimeZoneId = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
    }

    public Element toElement(String name, Element parent) {
        Element dtEl = parent.addElement(name);
        dtEl.addAttribute(MailConstants.A_CAL_DATETIME, mDateTime);
        if (mTimeZoneId != null)
            dtEl.addAttribute(MailConstants.A_CAL_TIMEZONE, mTimeZoneId);
        return dtEl;
    }

    public boolean getHasNoTimeZone() {
        return mTimeZoneId == null || mTimeZoneId.length() == 0;
    }

    public TimeZone getTimeZone() {
        if (!getHasNoTimeZone()) {
            return TimeZone.getTimeZone(TZIDMapper.toJava(mTimeZoneId));
        } else {
            return TimeZone.getTimeZone("GMT");
        }
    }

    public Calendar getCalendar() {
        DateFormat df =  new SimpleDateFormat((mDateTime.indexOf('T') == -1) ? FMT_DATE : FMT_DATE_TIME);
        df.setLenient(false);
        TimeZone tz = null;
        if (mTimeZoneId != null && mTimeZoneId.length() > 0) {
            tz = TimeZone.getTimeZone(TZIDMapper.toJava(mTimeZoneId));
            df.setTimeZone(tz);
        } else if (mDateTime.indexOf('Z') != -1) {
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        Date date = null;

        try {
            date = df.parse(mDateTime);
        } catch (ParseException e) {
            date = new Date();
        }
        Calendar cal = tz == null ? Calendar.getInstance() : Calendar.getInstance(tz);
        cal.setTime(date);
        return cal;
    }

    public Date getDate() {
        DateFormat df =  new SimpleDateFormat((mDateTime.indexOf('T') == -1) ? FMT_DATE : FMT_DATE_TIME);
        df.setLenient(false);
        TimeZone tz = null;
        if (mTimeZoneId != null && mTimeZoneId.length() > 0) {
            tz = TimeZone.getTimeZone(TZIDMapper.toJava(mTimeZoneId));
            df.setTimeZone(tz);
        } else if (mDateTime.indexOf('Z') != -1) {
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        Date date = null;

        try {
            date = df.parse(mDateTime);
        } catch (ParseException e) {
            date = new Date();
        }
        return date;
    }

    public String getDateTime() {
        return mDateTime;
    }

    public void setDateTime(String dateTime) {
        mDateTime = dateTime;
    }

    public String getTimeZoneId() {
        return mTimeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        mTimeZoneId = timeZoneId;
    }

    ZSoapSB toString(ZSoapSB sb) {
        sb.beginStruct();
        sb.add("dateTime", mDateTime);
        sb.add("timeZoneId", mTimeZoneId);
        sb.endStruct();
        return sb;
    }

    public String toString() {
        return toString(new ZSoapSB()).toString();
    }

}
