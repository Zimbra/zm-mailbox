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
import com.zimbra.soap.mail.type.LegacyAppointmentData;
import com.zimbra.soap.mail.type.LegacyCalendaringData;
import com.zimbra.soap.mail.type.LegacyTaskData;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="GetCalendarItemSummariesResponse")
public class GetCalendarItemSummariesResponse {

    /**
     * @zm-api-field-description Appointment summaries
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_APPOINTMENT /* appt */, type=LegacyAppointmentData.class),
        @XmlElement(name=MailConstants.E_TASK /* task */, type=LegacyTaskData.class)
    })
    private List<LegacyCalendaringData> calEntries = Lists.newArrayList();

    public GetCalendarItemSummariesResponse() {
    }

    public void setCalEntries(Iterable <LegacyCalendaringData> calEntries) {
        this.calEntries.clear();
        if (calEntries != null) {
            Iterables.addAll(this.calEntries,calEntries);
        }
    }

    public void addCalEntry(LegacyCalendaringData calEntry) {
        this.calEntries.add(calEntry);
    }

    public List<LegacyCalendaringData> getCalEntries() {
        return calEntries;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("calEntries", calEntries);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
