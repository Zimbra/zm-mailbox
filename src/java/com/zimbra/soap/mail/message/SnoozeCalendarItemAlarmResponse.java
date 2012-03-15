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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.UpdatedAlarmInfo;
import com.zimbra.soap.mail.type.UpdatedAppointmentAlarmInfo;
import com.zimbra.soap.mail.type.UpdatedTaskAlarmInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="SnoozeCalendarItemAlarmResponse")
public class SnoozeCalendarItemAlarmResponse {

    /**
     * @zm-api-field-description Updated alarm information so the client knows when to trigger the next alarm
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_APPOINTMENT /* appt */, type=UpdatedAppointmentAlarmInfo.class),
        @XmlElement(name=MailConstants.E_TASK /* task */, type=UpdatedTaskAlarmInfo.class)
    })
    private List<UpdatedAlarmInfo> updatedAlarms = Lists.newArrayList();

    public SnoozeCalendarItemAlarmResponse() {
    }

    public void setUpdatedAlarms(Iterable <UpdatedAlarmInfo> updatedAlarms) {
        this.updatedAlarms.clear();
        if (updatedAlarms != null) {
            Iterables.addAll(this.updatedAlarms,updatedAlarms);
        }
    }

    public SnoozeCalendarItemAlarmResponse addUpdatedAlarm(
                    UpdatedAlarmInfo updatedAlarm) {
        this.updatedAlarms.add(updatedAlarm);
        return this;
    }

    public List<UpdatedAlarmInfo> getUpdatedAlarms() {
        return Collections.unmodifiableList(updatedAlarms);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("updatedAlarms", updatedAlarms);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
