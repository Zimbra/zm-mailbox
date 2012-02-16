/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("calEntries", calEntries);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
