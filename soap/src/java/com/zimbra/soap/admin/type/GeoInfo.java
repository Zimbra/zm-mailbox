/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.calendar.Geo;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.GeoInfoInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class GeoInfo implements GeoInfoInterface {

    /**
     * @zm-api-field-tag longitude
     * @zm-api-field-description Longitude (float value)
     */
    @XmlAttribute(name=MailConstants.A_CAL_GEO_LATITUDE /* lat */, required=false)
    private final String latitude;

    /**
     * @zm-api-field-tag longitude
     * @zm-api-field-description Longitude (float value)
     */
    @XmlAttribute(name=MailConstants.A_CAL_GEO_LONGITUDE /* lon */, required=false)
    private final String longitude;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GeoInfo() {
        this((String) null, (String) null);
    }

    public GeoInfo(String latitude, String longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GeoInfo(Geo geo) {
        this.latitude = geo.getLatitude();
        this.longitude = geo.getLongitude();
    }

    @Override
    public GeoInfoInterface create(String latitude, String longitude) {
        return new GeoInfo(latitude, longitude);
    }

    @Override
    public GeoInfoInterface create(Geo geo) {
        return new GeoInfo(geo);
    }
    @Override
    public String getLatitude() { return latitude; }
    @Override
    public String getLongitude() { return longitude; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("latitude", latitude)
            .add("longitude", longitude)
            .toString();
    }
}
