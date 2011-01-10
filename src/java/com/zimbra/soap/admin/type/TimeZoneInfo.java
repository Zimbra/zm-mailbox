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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class TimeZoneInfo {

    @XmlAttribute(name=AdminConstants.A_TIMEZONE_ID, required=true)
    private final String id;
    @XmlAttribute(name=AdminConstants.A_TIMEZONE_DISPLAYNAME, required=true)
    private final String displayName;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private TimeZoneInfo() {
        this(null, null);
    }

    public TimeZoneInfo(String id, String displayName) {
        this.displayName = displayName;
        this.id = id;
    }

    public String getDisplayName() { return displayName; }
    public String getId() { return id; }
}
