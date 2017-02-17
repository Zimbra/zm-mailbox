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
package com.zimbra.common.calendar;

import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

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

    public static Geo parse(Element geoElem) {
        String latitude = geoElem.getAttribute(MailConstants.A_CAL_GEO_LATITUDE, "0");
        String longitude = geoElem.getAttribute(MailConstants.A_CAL_GEO_LONGITUDE, "0");
        return new Geo(latitude, longitude);
    }

    public ZProperty toZProperty() {
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
}
