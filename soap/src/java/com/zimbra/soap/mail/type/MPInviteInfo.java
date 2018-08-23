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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
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
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"timezones", "calendarReplies", "inviteComponents"})
public class MPInviteInfo {

    /**
     * @zm-api-field-tag type-appt|task
     * @zm-api-field-description Calendar item type - <b>appt|task</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_ITEM_TYPE /* type */, required=true)
    private final String calItemType;

    /**
     * @zm-api-field-description Timezones
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    /**
     * @zm-api-field-description Replies
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=MailConstants.E_CAL_REPLIES /* replies */, required=false)
    @XmlElement(name=MailConstants.E_CAL_REPLY /* reply */, required=false)
    private List<CalendarReply> calendarReplies = Lists.newArrayList();

    /**
     * @zm-api-field-description Invite components
     */
    @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, required=false)
    private List<InviteComponent> inviteComponents = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MPInviteInfo() {
        this((String) null);
    }

    public MPInviteInfo(String calItemType) {
        this.calItemType = calItemType;
    }

    public void setTimezones(Iterable <CalTZInfo> timezones) {
        this.timezones.clear();
        if (timezones != null) {
            Iterables.addAll(this.timezones,timezones);
        }
    }

    public void addTimezone(CalTZInfo timezone) {
        this.timezones.add(timezone);
    }

    public void setCalendarReplies(Iterable <CalendarReply> calendarReplies) {
        this.calendarReplies.clear();
        if (calendarReplies != null) {
            Iterables.addAll(this.calendarReplies,calendarReplies);
        }
    }

    public void addCalendarReply(CalendarReply calendarReply) {
        this.calendarReplies.add(calendarReply);
    }

    public void setInviteComponents(
                    Iterable <InviteComponent> inviteComponents) {
        this.inviteComponents.clear();
        if (inviteComponents != null) {
            Iterables.addAll(this.inviteComponents,inviteComponents);
        }
    }

    public void addInviteComponent(InviteComponent inviteComponent) {
        this.inviteComponents.add(inviteComponent);
    }

    public String getCalItemType() { return calItemType; }
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    public List<CalendarReply> getCalendarReplies() {
        return Collections.unmodifiableList(calendarReplies);
    }
    public List<InviteComponent> getInviteComponents() {
        return Collections.unmodifiableList(inviteComponents);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("calItemType", calItemType)
            .add("timezones", timezones)
            .add("calendarReplies", calendarReplies)
            .add("inviteComponents", inviteComponents);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
