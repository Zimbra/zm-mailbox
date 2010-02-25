/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.calendar;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.common.soap.Element;

public class RdateExdate {

    private ICalTok mPropertyName;   // RDATE or EXDATE
    private ICalTok mValueType;      // DATE_TIME, DATE, or PERIOD
    private ICalTimeZone mTimeZone;  // timezone that applies to all values; may be null
    private List<Object> mValues;
    private boolean mIsRdate;       // true if RDATE, false if EXDATE

    public RdateExdate(ICalTok propName, ICalTimeZone tz)
    throws ServiceException {
        this(propName, ICalTok.DATE_TIME, tz);
    }

    RdateExdate(ICalTok propName, ICalTok valueType, ICalTimeZone tz)
    throws ServiceException {
        if (ICalTok.RDATE.equals(propName))
            mIsRdate = true;
        else if (!ICalTok.EXDATE.equals(propName))
            throw ServiceException.INVALID_REQUEST(
                    "Property " + propName.toString() + " is neither a RDATE nor an EXDATE", null);
        mPropertyName = propName;

        mTimeZone = tz;
        setValueType(valueType);
        mValues = new ArrayList<Object>();
    }

    public void setValueType(ICalTok valueType) throws ServiceException {
        switch (valueType) {
        case DATE_TIME:
        case DATE:
            break;
        case PERIOD:
            if (!mIsRdate)
                throw ServiceException.INVALID_REQUEST(
                        "PERIOD value type not allowed in EXDATE", null);
            break;
        default:
            throw ServiceException.INVALID_REQUEST(
                    "Invalid value type " + valueType.toString() + " in " +
                    mPropertyName.toString(), null);
        }
        mValueType = valueType;
    }

    public void addValue(Object value) {
        mValues.add(value);
    }

    public int numValues() { return mValues.size(); }
    public Iterator<Object> valueIterator() { return mValues.iterator(); }

    public boolean isRDATE()  { return mIsRdate; }
    public boolean isEXDATE() { return !mIsRdate; }

    public ICalTimeZone getTimeZone() { return mTimeZone; }

    public String toString() {
        StringBuilder sb = new StringBuilder(mPropertyName.toString());
        if (!ICalTok.DATE_TIME.equals(mValueType))
            sb.append(";VALUE=").append(mValueType.toString());
        if (mTimeZone != null)
            sb.append(";TZID=").append(mTimeZone.getID());
        sb.append(":");
        sb.append(getDatesCSV());
        return sb.toString();
    }

    private String getDatesCSV() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object value : mValues) {
            if (first)
                first = false;
            else
                sb.append(",");
            if (value instanceof ParsedDateTime) {
                ParsedDateTime t = (ParsedDateTime) value;
                sb.append(t.getDateTimePartString(false));
            } else if (value instanceof Period)
                sb.append(value.toString());
        }
        return sb.toString();
    }

    public Element toXml(Element parent) {
        Element dateElem = parent.addElement(MailConstants.E_CAL_DATES);
        if (mTimeZone != null)
            dateElem.addAttribute(MailConstants.A_CAL_TIMEZONE, mTimeZone.getID());
        for (Object val : mValues) {
            Element dtvalElem = dateElem.addElement(MailConstants.E_CAL_DATE_VAL);
            if (val instanceof ParsedDateTime) {
                ParsedDateTime dt = (ParsedDateTime) val;
                Element startElem = dtvalElem.addElement(MailConstants.E_CAL_START_TIME);
                startElem.addAttribute(MailConstants.A_CAL_DATETIME,
                                       dt.getDateTimePartString(false));
            } else if (val instanceof Period) {
                Period p = (Period) val;
                Element startElem = dtvalElem.addElement(MailConstants.E_CAL_START_TIME);
                startElem.addAttribute(MailConstants.A_CAL_DATETIME,
                                       p.getStart().getDateTimePartString(false));
                if (p.hasEnd()) {
                    Element endElem = dtvalElem.addElement(MailConstants.E_CAL_END_TIME);
                    endElem.addAttribute(MailConstants.A_CAL_DATETIME,
                                         p.getEnd().getDateTimePartString(false));
                } else {
                    p.getDuration().toXml(dtvalElem);
                }
            }
        }
        return dateElem;
    }

    public ZProperty toZProperty() {
        ZProperty prop = new ZProperty(mPropertyName);
        if (mTimeZone != null)
            prop.addParameter(new ZParameter(ICalTok.TZID, mTimeZone.getID()));
        if (!ICalTok.DATE_TIME.equals(mValueType))
            prop.addParameter(new ZParameter(ICalTok.VALUE, mValueType.toString()));
        prop.setValue(getDatesCSV());
        return prop;
    }

    public static RdateExdate parse(ZProperty prop, TimeZoneMap tzmap)
    throws ServiceException {
        ICalTok propName = prop.getToken();

        ZParameter valueParam = prop.getParameter(ICalTok.VALUE);
        ICalTok valueType = ICalTok.DATE_TIME;
        if (valueParam != null) {
            String typeStr = valueParam.getValue();
            if (typeStr != null) {
                valueType = ICalTok.lookup(typeStr);
                if (valueType == null)
                    throw ServiceException.INVALID_REQUEST(
                            "Invalid " + propName.toString() +
                            " value type " + typeStr, null);
            }
        }

        String tzid = prop.getParameterVal(ICalTok.TZID, null);
        ICalTimeZone tz = null;
        if (tzid != null)
            tz = tzmap.lookupAndAdd(tzid);

        RdateExdate rexdate = new RdateExdate(propName, valueType, tz);

        String csv = prop.getValue();
        if (csv == null || csv.length() == 0)
            throw ServiceException.INVALID_REQUEST(
                    "Empty value not allowed for " + propName.toString() +
                    " property", null);
        for (String value : csv.split(",")) {
            try {
                switch (valueType) {
                case DATE_TIME:
                case DATE:
                    ParsedDateTime dt = ParsedDateTime.parse(value, tzmap, tz, tzmap.getLocalTimeZone());
                    rexdate.addValue(dt);
                    break;
                case PERIOD:
                    Period p = Period.parse(value, tz, tzmap);
                    rexdate.addValue(p);
                    break;
                }
            } catch (ParseException e) {
                throw ServiceException.INVALID_REQUEST(
                        "Unable to parse " + propName.toString() + " value \"" +
                        value + "\"", e);
            }
        }

        return rexdate;
    }

    private static final String FN_IS_RDATE = "isrd";  // 1 (RDATE) or 0 (EXDATE)
    private static final String FN_TZID = "tzid";
    private static final String FN_VALUE_TYPE = "vt";  // "DATE-TIME", "DATE" or "PERIOD"
    private static final String FN_NUM_VALUES ="numV";
    private static final String FN_VALUE = "v";

    private static final String VT_DATE_TIME = "dt";
    private static final String VT_DATE = "d";
    private static final String VT_PERIOD = "p";

    public Metadata encodeMetadata() {
        Metadata meta = new Metadata();

        meta.put(FN_IS_RDATE, isRDATE());

        if (mTimeZone != null)
            meta.put(FN_TZID, mTimeZone.getID());

        String vt = VT_DATE_TIME;
        if (!ICalTok.DATE_TIME.equals(mValueType)) {
            vt = ICalTok.DATE.equals(mValueType) ? VT_DATE : VT_PERIOD;
        }
        meta.put(FN_VALUE_TYPE, vt);

        meta.put(FN_NUM_VALUES, mValues.size());
        int i = 0;
        for (Object val : mValues) {
            if (val instanceof ParsedDateTime) {
                ParsedDateTime dt = (ParsedDateTime) val;
                meta.put(FN_VALUE + i, dt.getDateTimePartString(false));
            } else if (val instanceof Period) {
                Period p = (Period) val;
                meta.put(FN_VALUE + i, p.encodeMetadata());
            }
            i++;
        }

        return meta;
    }

    public static RdateExdate decodeMetadata(Metadata meta, TimeZoneMap tzmap)
    throws ServiceException {
        boolean isRdate = meta.getBool(FN_IS_RDATE, true);
        ICalTok propName = isRdate ? ICalTok.RDATE : ICalTok.EXDATE;

        ICalTimeZone tz = null;
        String tzid = meta.get(FN_TZID, null);
        if (tzid != null)
            tz = tzmap.lookupAndAdd(tzid);

        String vt = meta.get(FN_VALUE_TYPE, VT_DATE_TIME);
        ICalTok valueType;
        if (vt.equals(VT_DATE_TIME))
            valueType = ICalTok.DATE_TIME;
        else if (vt.equals(VT_DATE))
            valueType = ICalTok.DATE;
        else
            valueType = ICalTok.PERIOD;

        RdateExdate rexdate = new RdateExdate(propName, valueType, tz);

        int numValues = (int) meta.getLong(FN_NUM_VALUES, 0);
        for (int i = 0; i < numValues; i++) {
            String key = FN_VALUE + i;
            if (valueType.equals(ICalTok.DATE_TIME) || valueType.equals(ICalTok.DATE)) {
                String dtStr = meta.get(key);
                ParsedDateTime dt;
                try {
                    dt = ParsedDateTime.parse(dtStr, tzmap, tz, tzmap.getLocalTimeZone());
                } catch (ParseException e) {
                    throw ServiceException.INVALID_REQUEST(
                            "Invalid " + propName.toString() +
                            " date/time in metadata: " + meta.toString(), e);
                }
                rexdate.addValue(dt);
            } else if (valueType.equals(ICalTok.PERIOD)) {
                Period p = Period.decodeMetadata(meta.getMap(key), tz, tzmap);
                rexdate.addValue(p);
            }
        }

        return rexdate;
    }
}
