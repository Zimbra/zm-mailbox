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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
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
@XmlRootElement(name="DismissCalendarItemAlarmResponse")
public class DismissCalendarItemAlarmResponse {

    /**
     * @zm-api-field-description Updated alarm information
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_APPOINTMENT /* appt */, type=UpdatedAppointmentAlarmInfo.class),
        @XmlElement(name=MailConstants.E_TASK /* task */, type=UpdatedTaskAlarmInfo.class)
    })
    private List<UpdatedAlarmInfo> updatedAlarms = Lists.newArrayList();

    public DismissCalendarItemAlarmResponse() {
    }

    public void setUpdatedAlarm(Iterable <UpdatedAlarmInfo> updatedAlarms) {
        this.updatedAlarms.clear();
        if (updatedAlarms != null) {
            Iterables.addAll(this.updatedAlarms,updatedAlarms);
        }
    }

    public DismissCalendarItemAlarmResponse addUpdatedAlarm(
                        UpdatedAlarmInfo updatedAlarm) {
        this.updatedAlarms.add(updatedAlarm);
        return this;
    }

    public List<UpdatedAlarmInfo> getUpdatedAlarms() {
        return Collections.unmodifiableList(updatedAlarms);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("updatedAlarms", updatedAlarms);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
