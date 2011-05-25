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

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class InstanceRecurIdInfo {

    @XmlAttribute(name=MailConstants.A_CAL_RANGE /* range */, required=false)
    private String range;

    @XmlAttribute(name=MailConstants.A_CAL_DATETIME /* d */, required=false)
    private String dateTime;

    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE /* tz */, required=false)
    private String timezone;

    public InstanceRecurIdInfo() {
    }

    public void setRange(String range) { this.range = range; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getRange() { return range; }
    public String getDateTime() { return dateTime; }
    public String getTimezone() { return timezone; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("range", range)
            .add("dateTime", dateTime)
            .add("timezone", timezone);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
