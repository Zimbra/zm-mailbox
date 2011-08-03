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
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.DtTimeInfoInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class DtTimeInfo
implements DtTimeInfoInterface {

    @XmlAttribute(name=MailConstants.A_CAL_DATETIME, required=false)
    private final String dateTime;

    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE, required=false)
    private String timezone;

    @XmlAttribute(name=MailConstants.A_CAL_DATETIME_UTC, required=false)
    private Long utcTime;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DtTimeInfo() {
        this((String) null);
    }

    public DtTimeInfo(String dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public DtTimeInfoInterface create(String dateTime) {
        return new DtTimeInfo(dateTime);
    }

    @Override
    public void setTimezone(String timezone) { this.timezone = timezone; }
    @Override
    public void setUtcTime(Long utcTime) { this.utcTime = utcTime; }
    @Override
    public String getDateTime() { return dateTime; }
    @Override
    public String getTimezone() { return timezone; }
    @Override
    public Long getUtcTime() { return utcTime; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("dateTime", dateTime)
            .add("timezone", timezone)
            .add("utcTime", utcTime)
            .toString();
    }
}
