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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class TZReplaceInfo {

    // TZID from /opt/zimbra/conf/timezones.ics
    /**
     * @zm-api-field-tag well-known-tzid
     * @zm-api-field-description TZID from /opt/zimbra/conf/timezones.ics
     */
    @XmlElement(name=AdminConstants.E_WELL_KNOWN_TZ /* wellKnownTz */, required=false)
    private Id wellKnownTz;

    /**
     * @zm-api-field-description Timezone
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo calTz;

    public TZReplaceInfo() {
    }

    public void setWellKnownTz(Id wellKnownTz) { this.wellKnownTz = wellKnownTz; }
    public void setCalTz(CalTZInfo calTz) { this.calTz = calTz; }
    public Id getWellKnownTz() { return wellKnownTz; }
    public CalTZInfo getCalTz() { return calTz; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("wellKnownTz", wellKnownTz)
            .add("calTz", calTz);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
