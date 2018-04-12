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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

// see mail.ToXML.encodeInvite

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "timezones", "inviteComponent", "contentElems" })
public class Invitation {

    /**
     * @zm-api-field-tag type-appt|task
     * @zm-api-field-description Calendar item type - <b>appt|task</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_ITEM_TYPE /* type */, required=true)
    private final String calItemType;

    /**
     * @zm-api-field-tag sequence-number
     * @zm-api-field-description Sequence number
     */
    @XmlAttribute(name=MailConstants.A_CAL_SEQUENCE /* seq */, required=true)
    private final Integer sequence;

    /**
     * @zm-api-field-tag invite-original-mail-item-id
     * @zm-api-field-description Original mail item ID for invite
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final Integer id;

    /**
     * @zm-api-field-tag component-number
     * @zm-api-field-description Component number
     */
    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM /* compNum */, required=true)
    private final Integer componentNum;

    /**
     * @zm-api-field-tag YYMMDD[THHMMSS[Z]]
     * @zm-api-field-description Recurrence ID in format : YYMMDD[THHMMSS[Z]]
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID /* recurId */, required=false)
    private String recurrenceId;

    /**
     * @zm-api-field-description Timezones
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    /**
     * @zm-api-field-description Invite component
     */
    @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, required=false)
    private InviteComponent inviteComponent;

    /**
     * @zm-api-field-description Mime parts, share notifications and distribution list subscription notifications
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_MIMEPART /* mp */, type=PartInfo.class),
        @XmlElement(name=MailConstants.E_SHARE_NOTIFICATION /* shr */, type=ShareNotification.class),
        @XmlElement(name=MailConstants.E_DL_SUBSCRIPTION_NOTIFICATION /* dlSubs */, type=DLSubscriptionNotification.class)
    })
    private List<Object> contentElems = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private Invitation() {
        this((String) null, (Integer) null, (Integer) null, (Integer) null);
    }

    public Invitation(String calItemType, Integer sequence, Integer id, Integer componentNum) {
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
