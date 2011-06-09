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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"action", "trigger", "repeat", "description", "attach",
                        "summary", "attendees", "xProps"})
public class AlarmInfo {

    @XmlAttribute(name=MailConstants.A_CAL_ALARM_ACTION /* action */, required=true)
    private final String action;

    @XmlElement(name=MailConstants.E_CAL_ALARM_TRIGGER /* trigger */, required=false)
    private AlarmTriggerInfo trigger;

    @XmlElement(name=MailConstants.E_CAL_ALARM_REPEAT /* repeat */, required=false)
    private DurationInfo repeat;

    @XmlElement(name=MailConstants.E_CAL_ALARM_DESCRIPTION /* desc */, required=false)
    private String description;

    @XmlElement(name=MailConstants.E_CAL_ATTACH /* attach */, required=false)
    private CalendarAttach attach;

    @XmlElement(name=MailConstants.E_CAL_ALARM_SUMMARY /* summary */, required=false)
    private String summary;

    @XmlElement(name=MailConstants.E_CAL_ATTENDEE /* at */, required=false)
    private List<CalendarAttendee> attendees = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_XPROP /* xprop */, required=false)
    private List<XProp> xProps = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AlarmInfo() {
        this((String) null);
    }

    public AlarmInfo(String action) {
        this.action = action;
    }

    public void setTrigger(AlarmTriggerInfo trigger) {
        this.trigger = trigger;
    }

    public void setRepeat(DurationInfo repeat) { this.repeat = repeat; }
    public void setDescription(String description) {
        this.description = description;
    }

    public void setAttach(CalendarAttach attach) { this.attach = attach; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setAttendees(Iterable <CalendarAttendee> attendees) {
        this.attendees.clear();
        if (attendees != null) {
            Iterables.addAll(this.attendees,attendees);
        }
    }

    public void addAttendee(CalendarAttendee attendee) {
        this.attendees.add(attendee);
    }

    public void setXProps(Iterable <XProp> xProps) {
        this.xProps.clear();
        if (xProps != null) {
            Iterables.addAll(this.xProps,xProps);
        }
    }

    public void addXProp(XProp xProp) {
        this.xProps.add(xProp);
    }

    public String getAction() { return action; }
    public AlarmTriggerInfo getTrigger() { return trigger; }
    public DurationInfo getRepeat() { return repeat; }
    public String getDescription() { return description; }
    public CalendarAttach getAttach() { return attach; }
    public String getSummary() { return summary; }
    public List<CalendarAttendee> getAttendees() {
        return Collections.unmodifiableList(attendees);
    }
    public List<XProp> getXProps() {
        return Collections.unmodifiableList(xProps);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("action", action)
            .add("trigger", trigger)
            .add("repeat", repeat)
            .add("description", description)
            .add("attach", attach)
            .add("summary", summary)
            .add("attendees", attendees)
            .add("xProps", xProps);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
