/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.calendar;

import java.util.List;
import java.util.ListIterator;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;

/**
 * iCalendar GEO property
 */
public class Geo {

    private String mLatitude;
    private String mLongitude;

    public String getLatitude()  { return mLatitude; }
    public String getLongitude() { return mLongitude; }

    public Geo(String lat, String lon) {
        mLatitude = lat;
        mLongitude = lon;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Geo))
            return false;
        Geo other = (Geo) o;
        return
            mLatitude != null && mLatitude.equals(other.mLatitude) &&
            mLongitude != null && mLongitude.equals(other.mLongitude);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mLatitude).append(";").append(mLongitude);
        return sb.toString();
    }

    public Element toXml(Element parent) {
        Element geo = parent.addElement(MailConstants.E_CAL_GEO);
        geo.addAttribute(MailConstants.A_CAL_GEO_LATITUDE, mLatitude);
        geo.addAttribute(MailConstants.A_CAL_GEO_LONGITUDE, mLongitude);
        return geo;
    }

    public static Geo parse(Element geoElem) throws ServiceException {
        String latitude = geoElem.getAttribute(MailConstants.A_CAL_GEO_LATITUDE, "0");
        String longitude = geoElem.getAttribute(MailConstants.A_CAL_GEO_LONGITUDE, "0");
        return new Geo(latitude, longitude);
    }

    public ZProperty toZProperty() throws ServiceException {
        ZProperty prop = new ZProperty(ICalTok.GEO);
        prop.setValue(mLatitude + ";" + mLongitude);
        return prop;
    }

    public static Geo parse(ZProperty prop) {
        String val = prop.getValue();
        String[] latlon = val.split(";");
        if (latlon != null && latlon.length == 2)
            return new Geo(latlon[0], latlon[1]);
        else
            return new Geo("0", "0");    
    }

    private static final String FN_LATITUDE = "lat";
    private static final String FN_LONGITUDE = "lon";

    public Metadata encodeMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_LATITUDE, mLatitude);
        meta.put(FN_LONGITUDE, mLongitude);
        return meta;
    }

    public static Geo decodeMetadata(Metadata meta) throws ServiceException {
        String lat = meta.get(FN_LATITUDE, "0");
        String lon = meta.get(FN_LONGITUDE, "0");
        return new Geo(lat, lon);
    }
}
