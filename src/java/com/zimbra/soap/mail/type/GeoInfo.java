/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
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
        return Objects.toStringHelper(this)
            .add("latitude", latitude)
            .add("longitude", longitude)
            .toString();
    }
}
