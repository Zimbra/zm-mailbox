/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ZDateTime implements ToZJSONObject {

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
            return TimeZone.getTimeZone(TZIDMapper.canonicalize(mTimeZoneId));
        } else {
            return TimeZone.getTimeZone("GMT");
        }
    }

    public Calendar getCalendar() {
        DateFormat df =  new SimpleDateFormat((mDateTime.indexOf('T') == -1) ? FMT_DATE : FMT_DATE_TIME);
        df.setLenient(false);
        TimeZone tz = null;
        if (mTimeZoneId != null && mTimeZoneId.length() > 0) {
            tz = TimeZone.getTimeZone(TZIDMapper.canonicalize(mTimeZoneId));
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
            tz = TimeZone.getTimeZone(TZIDMapper.canonicalize(mTimeZoneId));
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

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("time", getDate().getTime());
        zjo.put("dateTime", mDateTime);
        zjo.put("timeZoneId", mTimeZoneId);
        return zjo;
    }

    public String toString() {
        return String.format("[ZDateTime %s]", getDate().toString());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

}
