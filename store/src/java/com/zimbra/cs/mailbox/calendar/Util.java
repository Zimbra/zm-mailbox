/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.calendar.Attach;
import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Metadata;

public class Util {

    private static final String FN_NAME     = "n";
    private static final String FN_NUM_XPROPS_OR_XPARAMS = "numX";
    private static final String FN_VALUE    = "v";
    private static final String FN_XPROP_OR_XPARAM   = "x";

    private static final String FN_TZID = "tzid";
    private static final String FN_STANDARD_OFFSET  = "so";
    private static final String FN_DAYLIGHT_OFFSET  = "do";
    private static final String FN_DAYTOSTD_DTSTART = "d2ss";
    private static final String FN_STDTODAY_DTSTART = "s2ds";
    private static final String FN_DAYTOSTD_RULE    = "d2sr";
    private static final String FN_STDTODAY_RULE    = "s2dr";
    private static final String FN_STANDARD_TZNAME  = "sn";
    private static final String FN_DAYLIGHT_TZNAME  = "dn";

    private static final String FN_CONTENT_TYPE = "ct";
    private static final String FN_URI = "uri";
    private static final String FN_BINARY = "bin";

    private static final String FN_LATITUDE = "lat";
    private static final String FN_LONGITUDE = "lon";

    public static void encodeXParamsAsMetadata(Metadata meta, Iterator<ZParameter> xparamsIter) {
        int xparamCount = 0;
        for (; xparamsIter.hasNext(); ) {
            ZParameter xparam = xparamsIter.next();
            String paramName = xparam.getName();
            if (paramName == null) continue;
            Metadata paramMeta = new Metadata();
            paramMeta.put(FN_NAME, paramName);
            String paramValue = xparam.getValue();
            if (paramValue != null)
                paramMeta.put(FN_VALUE, paramValue);
            meta.put(FN_XPROP_OR_XPARAM + xparamCount, paramMeta);
            xparamCount++;
        }
        if (xparamCount > 0)
            meta.put(FN_NUM_XPROPS_OR_XPARAMS, xparamCount);
    }

    public static void encodeXPropsAsMetadata(Metadata meta, Iterator<ZProperty> xpropsIter) {
        int xpropCount = 0;
        for (; xpropsIter.hasNext(); ) {
            ZProperty xprop = xpropsIter.next();
            String propName = xprop.getName();
            if (propName == null) continue;
            // Never persist the transport-only special x-prop X-ZIMBRA-CHANGES.
            if (propName.equalsIgnoreCase(ICalTok.X_ZIMBRA_CHANGES.toString())) continue;
            Metadata propMeta = new Metadata();
            propMeta.put(FN_NAME, propName);
            String propValue = xprop.getValue();
            if (propValue != null)
                propMeta.put(FN_VALUE, propValue);

            encodeXParamsAsMetadata(propMeta, xprop.parameterIterator());

            meta.put(FN_XPROP_OR_XPARAM + xpropCount, propMeta);
            xpropCount++;
        }
        if (xpropCount > 0)
            meta.put(FN_NUM_XPROPS_OR_XPARAMS, xpropCount);
    }

    public static List<ZParameter> decodeXParamsFromMetadata(Metadata meta) throws ServiceException {
        int xparamCount = (int) meta.getLong(FN_NUM_XPROPS_OR_XPARAMS, 0);
        if (xparamCount > 0) {
            List<ZParameter> list = new ArrayList<ZParameter>(xparamCount);
            for (int paramNum = 0; paramNum < xparamCount; paramNum++) {
                Metadata paramMeta = meta.getMap(FN_XPROP_OR_XPARAM + paramNum, true);
                if (paramMeta == null) continue;
                String paramName = paramMeta.get(FN_NAME, null);
                if (paramName == null) continue;
                String paramValue = paramMeta.get(FN_VALUE, null);
                ZParameter xparam = new ZParameter(paramName, paramValue);
                list.add(xparam);
            }
            return list;
        }
        return null;
    }

    public static List<ZProperty> decodeXPropsFromMetadata(Metadata meta) throws ServiceException {
        int xpropCount = (int) meta.getLong(FN_NUM_XPROPS_OR_XPARAMS, 0);
        if (xpropCount > 0) {
            List<ZProperty> list = new ArrayList<ZProperty>(xpropCount);
            for (int propNum = 0; propNum < xpropCount; propNum++) {
                Metadata propMeta = meta.getMap(FN_XPROP_OR_XPARAM + propNum, true);
                if (propMeta == null) continue;
                String propName = propMeta.get(FN_NAME, null);
                if (propName == null) continue;
                // Never persist the transport-only special x-prop X-ZIMBRA-CHANGES.
                if (propName.equalsIgnoreCase(ICalTok.X_ZIMBRA_CHANGES.toString())) continue;
                ZProperty xprop = new ZProperty(propName);
                String propValue = propMeta.get(FN_VALUE, null);
                if (propValue != null)
                    xprop.setValue(propValue);
                List<ZParameter> xparams = decodeXParamsFromMetadata(propMeta);
                if (xparams != null) {
                    for (ZParameter xparam : xparams) {
                        xprop.addParameter(xparam);
                    }
                }
                list.add(xprop);
            }
            return list;
        }
        return null;
    }

    /**
     * Returns the time zone for the given account.
     */
    public static ICalTimeZone getAccountTimeZone(Account account) {
        String tzid = account.getAttr(Provisioning.A_zimbraPrefTimeZoneId);
        tzid = TZIDMapper.canonicalize(tzid);
        ICalTimeZone timeZone = WellKnownTimeZones.getTimeZoneById(tzid);
        if (timeZone == null) {
            return ICalTimeZone.getUTC();
        }
        return timeZone;
    }

    public static Metadata encodeAsMetadata(ICalTimeZone tz) {
        Metadata meta = new Metadata();
        String tzid = tz.getID();
        meta.put(FN_TZID, tzid);
        // For well-known time zone we only need the TZID.
        if (ICalTimeZone.lookupByTZID(tzid) != null)
            return meta;

        meta.put(FN_STANDARD_OFFSET, tz.getStandardOffset());
        meta.put(FN_DAYTOSTD_DTSTART, tz.getStandardDtStart());
        meta.put(FN_DAYTOSTD_RULE, tz.getStandardRule());
        meta.put(FN_STANDARD_TZNAME, tz.getStandardTzname());

        meta.put(FN_DAYLIGHT_OFFSET, tz.getDaylightOffset());
        meta.put(FN_STDTODAY_DTSTART, tz.getDaylightDtStart());
        meta.put(FN_STDTODAY_RULE, tz.getDaylightRule());
        meta.put(FN_DAYLIGHT_TZNAME, tz.getDaylightTzname());
        return meta;
    }

    public static ICalTimeZone decodeTimeZoneFromMetadata(Metadata m) throws ServiceException {
        String tzid;
        if (m.containsKey(FN_TZID)) {
            tzid = m.get(FN_TZID);
            boolean hasDef = m.containsKey(FN_STANDARD_OFFSET);
            if (!DebugConfig.disableCalendarTZMatchByID || !hasDef) {
                ICalTimeZone tz = WellKnownTimeZones.getTimeZoneById(tzid);
                if (tz != null) {
                    return tz;
                } else if (!hasDef) {
                    ZimbraLog.calendar.debug("Unknown time zone \"" + tzid + "\" in metadata; using UTC instead");
                    return ICalTimeZone.getUTC().cloneWithNewTZID(tzid);
                }
            }
        } else
            tzid = "unknown time zone";
        ICalTimeZone newTz = newICalTimeZone(tzid, m);
        ICalTimeZone tz = ICalTimeZone.lookupByRule(newTz, false);
        return tz;
    }

    private static ICalTimeZone newICalTimeZone(String tzId, Metadata meta) throws ServiceException {
        int standardOffset = (int) meta.getLong(FN_STANDARD_OFFSET, 0);
        String dayToStdDtStart = meta.get(FN_DAYTOSTD_DTSTART, null);
        String dayToStdRule = meta.get(FN_DAYTOSTD_RULE, null);
        String standardTzname = meta.get(FN_STANDARD_TZNAME, null);

        int daylightOffset = (int) meta.getLong(FN_DAYLIGHT_OFFSET, 0);
        String stdToDayDtStart = meta.get(FN_STDTODAY_DTSTART, ICalTimeZone.DEFAULT_DTSTART);
        String stdToDayRule = meta.get(FN_STDTODAY_RULE, null);
        String daylightTzname = meta.get(FN_DAYLIGHT_TZNAME, null);

        ICalTimeZone tz = new ICalTimeZone(tzId, standardOffset, dayToStdDtStart, dayToStdRule, standardTzname,
            daylightOffset, stdToDayDtStart, stdToDayRule, daylightTzname);
        tz.initFromICalData(true);
        return tz;
    }

    public static Metadata encodeAsMetadata(TimeZoneMap tzmap) {
        Metadata meta = new Metadata();
        Map<String /* real TZID */, Integer /* index */> tzIndex = new HashMap<String, Integer>();
        int nextIndex = 0;
        for (Iterator<Entry<String, ICalTimeZone>> iter = tzmap.getMap().entrySet().iterator(); iter.hasNext(); ) {
            Entry<String, ICalTimeZone> entry = iter.next();
            String tzid = entry.getKey();
            if (tzid == null || tzid.length() < 1)    // ignore null/empty TZIDs (bug 25183)
                continue;
            ICalTimeZone zone = entry.getValue();
            String realTzid = zone.getID();
            if (!tzIndex.containsKey(realTzid)) {
                meta.put("#" + nextIndex, encodeAsMetadata(zone));
                tzIndex.put(realTzid, nextIndex);
                ++nextIndex;
            }
        }
        for (Iterator<Entry<String, String>> iter = tzmap.getAliasMap().entrySet().iterator(); iter.hasNext(); ) {
            Entry<String, String> entry = iter.next();
            String alias = entry.getKey();
            String realTzid = entry.getValue();
            if (tzIndex.containsKey(realTzid)) {
                int index = tzIndex.get(realTzid);
                meta.put(alias, index);
            }
        }
        return meta;
    }

    /**
     *
     * @param meta
     * @param localTZ local time zone of user account
     * @return
     * @throws ServiceException
     */
    public static TimeZoneMap decodeFromMetadata(Metadata meta, ICalTimeZone localTZ) throws ServiceException {
        Map<String, ?> map = meta.asMap();
        Map<String, String> aliasMap = new HashMap<String, String>();
        ICalTimeZone[] tzlist = new ICalTimeZone[map.size()];
        // first time, find the tz's
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.length() > 0) {  // ignore null/empty TZIDs (bug 25183)
                if (key.charAt(0) == '#') {
                    int idx = Integer.parseInt(key.substring(1));
                    Metadata tzMeta = (Metadata) entry.getValue();
                    String tzidMeta = tzMeta.get(FN_TZID, null);
                    if (tzidMeta != null) {
                        ICalTimeZone tz = decodeTimeZoneFromMetadata(tzMeta);
                        if (tz != null) {
                            String tzid = tz.getID();
                            if (!DebugConfig.disableCalendarTZMatchByID)
                                tzid = TZIDMapper.canonicalize(tzid);
                            if (!tzidMeta.equals(tzid)) {
                                aliasMap.put(tzidMeta, tzid);
                                tz = WellKnownTimeZones.getTimeZoneById(tzid);
                            }
                            tzlist[idx] = tz;
                        }
                    }
                }
            }
        }

        Map<String, ICalTimeZone> tzmap = new HashMap<String, ICalTimeZone>();
        for (ICalTimeZone tz : tzlist) {
            if (tz != null)
                tzmap.put(tz.getID(), tz);
        }
        // second time, build the real map
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String tzid = entry.getKey();
            if (tzid != null && tzid.length() > 0) {  // ignore null/empty TZIDs (bug 25183)
                if (tzid.charAt(0) != '#') {
                    int idx = -1;
                    try {
                        idx = Integer.parseInt(entry.getValue().toString());
                    } catch (NumberFormatException e) {}
                    if (idx >= 0 && idx < tzlist.length) {
                        ICalTimeZone tz = tzlist[idx];
                        if (tz != null) {
                            String realId = tz.getID();
                            if (!realId.equals(tzid))
                                aliasMap.put(tzid, realId);
                        }
                    }
                }
            }
        }

        return new TimeZoneMap(tzmap, aliasMap, localTZ);
    }

    public static Metadata encodeMetadata(Attach att) {
        Metadata meta = new Metadata();
        if (att.getUri() != null) {
            meta.put(FN_URI, att.getUri());
            meta.put(FN_CONTENT_TYPE, att.getContentType());
        } else {
            meta.put(FN_BINARY, att.getBinaryB64Data());
        }
        return meta;
    }

    public static Attach decodeAttachFromMetadata(Metadata meta) {
        String uri = meta.get(FN_URI, null);
        String ct = meta.get(FN_CONTENT_TYPE, null);
        if (uri != null) {
            return Attach.fromUriAndContentType(uri, ct);
        } else {
            String binary = meta.get(FN_BINARY, null);
            return Attach.fromEncodedAndContentType(binary, ct);
        }
    }

    public static Geo decodeGeoFromMetadata(Metadata meta) {
        String lat = meta.get(FN_LATITUDE, "0");
        String lon = meta.get(FN_LONGITUDE, "0");
        return new Geo(lat, lon);
    }


    public static Metadata encodeMetadata(Geo geo) {
        Metadata meta = new Metadata();
        meta.put(Util.FN_LATITUDE, geo.getLatitude());
        meta.put(Util.FN_LONGITUDE, geo.getLongitude());
        return meta;
    }

    /**
     * Converts text to html.
     *
     * @param String containing plain text
     * @return String Html converted string
     */
    public static String textToHtml(String s) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasASpace = false;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                if (previousWasASpace) {
                    builder.append("&nbsp;");
                    previousWasASpace = false;
                    continue;
                }
                previousWasASpace = true;
            } else {
                previousWasASpace = false;
            }
            switch (c) {
                case '&':
                    builder.append("&amp;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\n':
                    builder.append("<br>");
                    break;
                case '\t':
                    builder.append("&nbsp; &nbsp; &nbsp;");
                    break;
                default:
                    builder.append(c);

            }
        }
        String converted = builder.toString();
        String str = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\'\".,<>?«»“”‘’]))";
        Pattern patt = Pattern.compile(str);
        Matcher matcher = patt.matcher(converted);
        converted = matcher.replaceAll("<a href=\"$1\">$1</a>");
        return converted;
    }
}
