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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalTZInfo;
import com.zimbra.soap.mail.type.ExpandedRecurrenceCancel;
import com.zimbra.soap.mail.type.ExpandedRecurrenceComponent;
import com.zimbra.soap.mail.type.ExpandedRecurrenceException;
import com.zimbra.soap.mail.type.ExpandedRecurrenceInvite;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Expand recurrences
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_EXPAND_RECUR_REQUEST)
public class ExpandRecurRequest {

    /**
     * @zm-api-field-tag start-time-millis
     * @zm-api-field-description Start time in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=true)
    private final long startTime;

    /**
     * @zm-api-field-tag end-time-millis
     * @zm-api-field-description End time in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=true)
    private final long endTime;

    /**
     * @zm-api-field-description Timezone definitions
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    /**
     * @zm-api-field-description Specifications for series, modified instances and canceled instances
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, type=ExpandedRecurrenceInvite.class),
        @XmlElement(name=MailConstants.E_CAL_EXCEPT /* except */, type=ExpandedRecurrenceException.class),
        @XmlElement(name=MailConstants.E_CAL_CANCEL /* cancel */, type=ExpandedRecurrenceCancel.class)
    })
    private List<ExpandedRecurrenceComponent> components = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExpandRecurRequest() {
        this(-1L, -1L);
    }

    public ExpandRecurRequest(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setTimezones(Iterable <CalTZInfo> timezones) {
        this.timezones.clear();
        if (timezones != null) {
            Iterables.addAll(this.timezones,timezones);
        }
    }

    public ExpandRecurRequest addTimezone(CalTZInfo timezone) {
        this.timezones.add(timezone);
        return this;
    }

    public void setComponents(Iterable <ExpandedRecurrenceComponent> components) {
        this.components.clear();
        if (components != null) {
            Iterables.addAll(this.components,components);
        }
    }

    public ExpandRecurRequest addComponent(ExpandedRecurrenceComponent component) {
        this.components.add(component);
        return this;
    }

    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    public List<ExpandedRecurrenceComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("startTime", startTime)
            .add("endTime", endTime)
            .add("timezones", timezones)
            .add("components", components);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
