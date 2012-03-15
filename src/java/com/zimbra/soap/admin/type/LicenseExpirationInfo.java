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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class LicenseExpirationInfo {

    /**
     * @zm-api-field-tag expiration-date-YYYYMMDD
     * @zm-api-field-description Expiration date in format : <b>YYYYMMDD</b>
     */
    @XmlAttribute(name=AdminConstants.A_LICENSE_EXPIRATION_DATE /* date */, required=false)
    private final String date;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private LicenseExpirationInfo() {
        this((String)null);
    }

    public LicenseExpirationInfo(String date) {
        this.date = date;
    }

    public String getDate() { return date; }
}
