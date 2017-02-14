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
