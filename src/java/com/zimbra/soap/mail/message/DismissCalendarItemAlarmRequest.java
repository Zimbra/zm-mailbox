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
import com.zimbra.soap.mail.type.DismissAlarm;
import com.zimbra.soap.mail.type.DismissAppointmentAlarm;
import com.zimbra.soap.mail.type.DismissTaskAlarm;

/**
 * @zm-api-command-description Dismiss calendar item alarm
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_DISMISS_CALITEM_ALARM_REQUEST)
public class DismissCalendarItemAlarmRequest {

    /**
     * @zm-api-field-description Details of alarms to dismiss
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_APPOINTMENT /* appt */, type=DismissAppointmentAlarm.class),
        @XmlElement(name=MailConstants.E_TASK /* task */, type=DismissTaskAlarm.class)
    })
    private List<DismissAlarm> alarms = Lists.newArrayList();

    public DismissCalendarItemAlarmRequest() {
    }

    public void setAlarms(Iterable <DismissAlarm> alarms) {
        this.alarms.clear();
        if (alarms != null) {
            Iterables.addAll(this.alarms,alarms);
        }
    }

    public DismissCalendarItemAlarmRequest addAlarm(DismissAlarm alarm) {
        this.alarms.add(alarm);
        return this;
    }

    public List<DismissAlarm> getAlarms() {
        return Collections.unmodifiableList(alarms);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("alarms", alarms);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
