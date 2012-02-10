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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalTZInfoInterface;
import com.zimbra.soap.base.CalendarReplyInterface;
import com.zimbra.soap.base.InviteComponentInterface;
import com.zimbra.soap.base.InviteInfoInterface;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"timezones", "inviteComponent", "calendarReplies"})
public class InviteInfo
implements InviteInfoInterface {

    // Valid values - "appt" and "task"
    /**
     * @zm-api-field-tag appt-or-task
     * @zm-api-field-description Invite type - <b>appt|task</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_ITEM_TYPE /* type */, required=true)
    private final String calItemType;

    /**
     * @zm-api-field-description Timezones
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    /**
     * @zm-api-field-description Invite components
     */
    @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, required=false)
    private InviteComponent inviteComponent;

    /**
     * @zm-api-field-description List of replies received from attendees.
     */
    @XmlElementWrapper(name=MailConstants.E_CAL_REPLIES /* replies */, required=false)
    @XmlElement(name=MailConstants.E_CAL_REPLY /* reply */, required=false)
    private List<CalendarReply> calendarReplies = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private InviteInfo() {
        this((String) null);
    }

    public InviteInfo(String calItemType) {
        this.calItemType = calItemType;
    }

    @Override
    public InviteInfoInterface create(String calItemType) {
        return new InviteInfo(calItemType);
    }

    public void setTimezones(Iterable <CalTZInfo> timezones) {
        this.timezones.clear();
        if (timezones != null) {
            Iterables.addAll(this.timezones,timezones);
        }
    }

    public InviteInfo addTimezone(CalTZInfo timezone) {
        this.timezones.add(timezone);
        return this;
    }

    public void setInviteComponent(InviteComponent inviteComponent) {
        this.inviteComponent = inviteComponent;
    }
    public void setCalendarReplies(Iterable <CalendarReply> calendarReplies) {
        this.calendarReplies.clear();
        if (calendarReplies != null) {
            Iterables.addAll(this.calendarReplies,calendarReplies);
        }
    }

    public InviteInfo addCalendarReply(CalendarReply calendarReply) {
        this.calendarReplies.add(calendarReply);
        return this;
    }

    @Override
    public String getCalItemType() { return calItemType; }
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    public InviteComponent getInviteComponent() { return inviteComponent; }
    public List<CalendarReply> getCalendarReplies() {
        return Collections.unmodifiableList(calendarReplies);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("calItemType", calItemType)
            .add("timezones", timezones)
            .add("inviteComponent", inviteComponent)
            .add("calendarReplies", calendarReplies)
            .toString();
    }

    @Override
    public void setTimezoneInterfaces(Iterable<CalTZInfoInterface> timezones) {
        setTimezones(CalTZInfo.fromInterfaces(timezones));
    }

    @Override
    public void addTimezoneInterface(CalTZInfoInterface timezone) {
        addTimezone((CalTZInfo) timezone);
    }

    @Override
    public void setInviteComponentInterface(
            InviteComponentInterface inviteComponent) {
        setInviteComponent((InviteComponent) inviteComponent);

    }

    @Override
    public void setCalendarReplyInterfaces(
            Iterable<CalendarReplyInterface> calendarReplies) {
        setCalendarReplies(CalendarReply.fromInterfaces(calendarReplies));
    }

    @Override
    public void addCalendarReplyInterface(CalendarReplyInterface calendarReply) {
        addCalendarReply((CalendarReply) calendarReply);
    }

    @Override
    public List<CalTZInfoInterface> getTimezoneInterfaces() {
        return CalTZInfo.toInterfaces(timezones);
    }

    @Override
    public InviteComponentInterface getInviteComponentInterface() {
        return inviteComponent;
    }

    @Override
    public List<CalendarReplyInterface> getCalendarReplyInterfaces() {
        return CalendarReply.toInterfaces(calendarReplies);
    }
}
