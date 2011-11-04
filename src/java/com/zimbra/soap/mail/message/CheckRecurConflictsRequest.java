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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalTZInfo;
import com.zimbra.soap.mail.type.ExpandedRecurrenceCancel;
import com.zimbra.soap.mail.type.ExpandedRecurrenceComponent;
import com.zimbra.soap.mail.type.ExpandedRecurrenceException;
import com.zimbra.soap.mail.type.ExpandedRecurrenceInvite;
import com.zimbra.soap.mail.type.FreeBusyUserSpec;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_CHECK_RECUR_CONFLICTS_REQUEST)
@XmlType(propOrder = {"timezones", "components", "freebusyUsers"})
public class CheckRecurConflictsRequest {

    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=false)
    private Long startTime;

    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=false)
    private Long endTime;

    @XmlAttribute(name=MailConstants.A_CAL_ALL /* all */, required=false)
    private ZmBoolean allInstances;

    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY_EXCLUDE_UID /* excludeUid */, required=false)
    private String excludeUid;

    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    @XmlElements({
        @XmlElement(name=MailConstants.E_CAL_CANCEL /* cancel */, type=ExpandedRecurrenceCancel.class),
        @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, type=ExpandedRecurrenceInvite.class),
        @XmlElement(name=MailConstants.E_CAL_EXCEPT /* except */, type=ExpandedRecurrenceException.class)
    })
    private List<ExpandedRecurrenceComponent> components = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_FREEBUSY_USER /* usr */, required=false)
    private List<FreeBusyUserSpec> freebusyUsers = Lists.newArrayList();

    public CheckRecurConflictsRequest() {
    }

    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public void setAllInstances(Boolean allInstances) {
        this.allInstances = ZmBoolean.fromBool(allInstances);
    }
    public void setExcludeUid(String excludeUid) {
        this.excludeUid = excludeUid;
    }
    public void setTimezones(Iterable <CalTZInfo> timezones) {
        this.timezones.clear();
        if (timezones != null) {
            Iterables.addAll(this.timezones,timezones);
        }
    }

    public CheckRecurConflictsRequest addTimezone(CalTZInfo timezone) {
        this.timezones.add(timezone);
        return this;
    }

    public void setComponents(
                    Iterable <ExpandedRecurrenceComponent> components) {
        this.components.clear();
        if (components != null) {
            Iterables.addAll(this.components,components);
        }
    }

    public CheckRecurConflictsRequest addComponent(
                    ExpandedRecurrenceComponent component) {
        this.components.add(component);
        return this;
    }

    public void setFreebusyUsers(Iterable <FreeBusyUserSpec> freebusyUsers) {
        this.freebusyUsers.clear();
        if (freebusyUsers != null) {
            Iterables.addAll(this.freebusyUsers,freebusyUsers);
        }
    }

    public CheckRecurConflictsRequest addFreebusyUser(
                    FreeBusyUserSpec freebusyUser) {
        this.freebusyUsers.add(freebusyUser);
        return this;
    }

    public Long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public Boolean getAllInstances() { return ZmBoolean.toBool(allInstances); }
    public String getExcludeUid() { return excludeUid; }
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    public List<ExpandedRecurrenceComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }
    public List<FreeBusyUserSpec> getFreebusyUsers() {
        return Collections.unmodifiableList(freebusyUsers);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("startTime", startTime)
            .add("endTime", endTime)
            .add("allInstances", allInstances)
            .add("excludeUid", excludeUid)
            .add("timezones", timezones)
            .add("components", components)
            .add("freebusyUsers", freebusyUsers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
