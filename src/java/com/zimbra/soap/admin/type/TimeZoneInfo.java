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
public class TimeZoneInfo {

    /**
     * @zm-api-field-tag timezone-id
     * @zm-api-field-description timezone ID. e.g "America/Los_Angeles"
     */
    @XmlAttribute(name=AdminConstants.A_TIMEZONE_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag timezone-display-name
     * @zm-api-field-description Timezone display anme, e.g. "Pacific Standard Time"
     */
    @XmlAttribute(name=AdminConstants.A_TIMEZONE_DISPLAYNAME /* displayName */, required=true)
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

    public static TimeZoneInfo fromIdAndDisplayName(String id, String displayName) {
        return new TimeZoneInfo(id, displayName);
    }
    public String getDisplayName() { return displayName; }
    public String getId() { return id; }
}
