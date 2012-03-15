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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class UpdatedAlarmInfo {

    /**
     * @zm-api-field-tag cal-item-id
     * @zm-api-field-description Calendar item ID
     */
    @XmlAttribute(name=MailConstants.A_CAL_ID /* calItemId */, required=true)
    private final String calItemId;

    /**
     * @zm-api-field-description Updated alarm information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_DATA /* alarmData */, required=false)
    private AlarmDataInfo alarmData;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    protected UpdatedAlarmInfo() {
        this((String) null);
    }

    public UpdatedAlarmInfo(String calItemId) {
        this.calItemId = calItemId;
    }

    public void setAlarmData(AlarmDataInfo alarmData) {
        this.alarmData = alarmData;
    }
    public String getCalItemId() { return calItemId; }
    public AlarmDataInfo getAlarmData() { return alarmData; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("calItemId", calItemId)
            .add("alarmData", alarmData);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
