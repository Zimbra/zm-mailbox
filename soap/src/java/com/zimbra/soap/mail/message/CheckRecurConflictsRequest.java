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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalTZInfo;
import com.zimbra.soap.mail.type.ExpandedRecurrenceCancel;
import com.zimbra.soap.mail.type.ExpandedRecurrenceComponent;
import com.zimbra.soap.mail.type.ExpandedRecurrenceException;
import com.zimbra.soap.mail.type.ExpandedRecurrenceInvite;
import com.zimbra.soap.mail.type.FreeBusyUserSpec;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Check conflicts in recurrence against list of users.
 * <br />
 * Set <b>all</b> attribute to get all instances, even those without conflicts.  By default only instances that have
 * conflicts are returned.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CHECK_RECUR_CONFLICTS_REQUEST)
@XmlType(propOrder = {"timezones", "components", "freebusyUsers"})
public class CheckRecurConflictsRequest {

    /**
     * @zm-api-field-tag start-time-millis
     * @zm-api-field-description Start time in millis.  If not specified, defaults to current time
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=false)
    private Long startTime;

    /**
     * @zm-api-field-tag end-time-millis
     * @zm-api-field-description End time in millis.  If not specified, unlimited
     */
    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=false)
    private Long endTime;

    /**
     * @zm-api-field-tag get-all-instances
     * @zm-api-field-description Set this to get all instances, even those without conflicts.  By default only
     * instances that have conflicts are returned.
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALL /* all */, required=false)
    private ZmBoolean allInstances;

    /**
     * @zm-api-field-tag exclude-uid
     * @zm-api-field-description UID of appointment to exclude from free/busy search
     */
    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY_EXCLUDE_UID /* excludeUid */, required=false)
    private String excludeUid;

    /**
     * @zm-api-field-description Timezones
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    /**
     * @zm-api-field-description Expanded recurrences
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_CAL_CANCEL /* cancel */, type=ExpandedRecurrenceCancel.class),
        @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, type=ExpandedRecurrenceInvite.class),
        @XmlElement(name=MailConstants.E_CAL_EXCEPT /* except */, type=ExpandedRecurrenceException.class)
    })
    private List<ExpandedRecurrenceComponent> components = Lists.newArrayList();

    /**
     * @zm-api-field-description Freebusy user specifications
     */
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
