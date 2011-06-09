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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "timezones", "inviteComponent", "contentElems" })
public class Invitation {

    // Valid values - "appt" and "task"
    @XmlAttribute(name=MailConstants.A_CAL_ITEM_TYPE /* type */, required=true)
    private final String calItemType;

    @XmlAttribute(name=MailConstants.A_CAL_SEQUENCE /* seq */, required=true)
    private final Integer sequence;

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final Integer id;

    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM /* compNum */,
                                            required=true)
    private final Integer componentNum;

    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID /* recurId */,
                                            required=false)
    private String recurrenceId;

    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */,
                                            required=false)
    private InviteComponent inviteComponent;

    @XmlElements({
        @XmlElement(name=MailConstants.E_MIMEPART /* mp */,
            type=PartInfo.class),
        @XmlElement(name=MailConstants.E_SHARE_NOTIFICATION /* shr */,
            type=ShareNotification.class)
    })
    private List<Object> contentElems = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private Invitation() {
        this((String) null, (Integer) null, (Integer) null, (Integer) null);
    }

    public Invitation(String calItemType, Integer sequence, Integer id,
                        Integer componentNum) {
        this.calItemType = calItemType;
        this.sequence = sequence;
        this.id = id;
        this.componentNum = componentNum;
    }

    public void setRecurrenceId(String recurrenceId) {
        this.recurrenceId = recurrenceId;
    }
    public void setTimezones(Iterable <CalTZInfo> timezones) {
        this.timezones.clear();
        if (timezones != null) {
            Iterables.addAll(this.timezones,timezones);
        }
    }

    public Invitation addTimezone(CalTZInfo timezone) {
        this.timezones.add(timezone);
        return this;
    }

    public void setInviteComponent(InviteComponent inviteComponent) {
        this.inviteComponent = inviteComponent;
    }
    public void setContentElems(Iterable <Object> contentElems) {
        this.contentElems.clear();
        if (contentElems != null) {
            Iterables.addAll(this.contentElems,contentElems);
        }
    }

    public Invitation addContentElem(Object contentElem) {
        this.contentElems.add(contentElem);
        return this;
    }

    public String getCalItemType() { return calItemType; }
    public Integer getSequence() { return sequence; }
    public Integer getId() { return id; }
    public Integer getComponentNum() { return componentNum; }
    public String getRecurrenceId() { return recurrenceId; }
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    public InviteComponent getInviteComponent() { return inviteComponent; }
    public List<Object> getContentElems() {
        return Collections.unmodifiableList(contentElems);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("calItemType", calItemType)
            .add("sequence", sequence)
            .add("id", id)
            .add("componentNum", componentNum)
            .add("recurrenceId", recurrenceId)
            .add("timezones", timezones)
            .add("inviteComponent", inviteComponent)
            .add("contentElems", contentElems);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
