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

import com.google.common.base.Objects;
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("wellKnownTz", wellKnownTz)
            .add("calTz", calTz);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
