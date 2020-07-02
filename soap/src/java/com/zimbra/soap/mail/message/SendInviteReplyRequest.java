/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalTZInfo;
import com.zimbra.soap.mail.type.DtTimeInfo;
import com.zimbra.soap.mail.type.Msg;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Send a reply to an invite
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEND_INVITE_REPLY_REQUEST)
public class SendInviteReplyRequest {

    /**
     * @zm-api-field-tag mail-item-id
     * @zm-api-field-description Unique ID of the invite (and component therein) you are replying to
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag component-number
     * @zm-api-field-description component number of the invite
     */
    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM /* compNum */, required=true)
    private final int componentNum;

    // See CalendarMailSender.Verb - not an enum and used in a case insensitive way
    /**
     * @zm-api-field-tag verb
     * @zm-api-field-description Verb - <b>ACCEPT, DECLINE, TENTATIVE</b>, COMPLETED, DELEGATED  (Completed/Delegated are
     * NOT supported as of 9/12/2005)
     */
    @XmlAttribute(name=MailConstants.A_VERB /* verb */, required=true)
    private final String verb;

    /**
     * @zm-api-field-tag update-organizer
     * @zm-api-field-description Update organizer. true by default. if false then only make the update locally.
     * <br />Note that earlier documentation implied incorrectly that if this was false it would be ignored and treated
     * as being true if an <b>&lt;m></b> element is present.
     * <br />Also take a note that, if RSVP setting in original invite is not present or FALSE then <b>updateOrganizer</b> will be treated as FALSE.
     */
    @XmlAttribute(name=MailConstants.A_CAL_UPDATE_ORGANIZER /* updateOrganizer */, required=false)
    private ZmBoolean updateOrganizer;

    /**
     * @zm-api-field-tag identity-id
     * @zm-api-field-description Identity ID to use to send reply
     */
    @XmlAttribute(name=MailConstants.A_IDENTITY_ID /* idnt */, required=false)
    private String identityId;

    /**
     * @zm-api-field-tag exception-id
     * @zm-api-field-description If supplied then reply to just one instance of the specified Invite (default is all
     * instances)
     */
    @XmlElement(name=MailConstants.E_CAL_EXCEPTION_ID /* exceptId */, required=false)
    private DtTimeInfo exceptionId;

    /**
     * @zm-api-field-description Definition for TZID referenced by DATETIME in <b>&lt;exceptId></b>
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo timezone;

    // E_INVITE child is not allowed
    /**
     * @zm-api-field-description Embedded message, if the user wants to send a custom update message.
     * The client is responsible for setting the message recipient list in this case (which should include Organizer,
     * if the client wants to tell the organizer about this response)
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private Msg msg;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SendInviteReplyRequest() {
        this((String) null, -1, (String) null);
    }

    public SendInviteReplyRequest(String id, int componentNum, String verb) {
        this.id = id;
        this.componentNum = componentNum;
        this.verb = verb;
    }

    public void setUpdateOrganizer(Boolean updateOrganizer) {
        this.updateOrganizer = ZmBoolean.fromBool(updateOrganizer);
    }
    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }
    public void setExceptionId(DtTimeInfo exceptionId) {
        this.exceptionId = exceptionId;
    }
    public void setTimezone(CalTZInfo timezone) { this.timezone = timezone; }
    public void setMsg(Msg msg) { this.msg = msg; }
    public String getId() { return id; }
    public int getComponentNum() { return componentNum; }
    public String getVerb() { return verb; }
    public Boolean getUpdateOrganizer() { return ZmBoolean.toBool(updateOrganizer); }
    public String getIdentityId() { return identityId; }
    public DtTimeInfo getExceptionId() { return exceptionId; }
    public CalTZInfo getTimezone() { return timezone; }
    public Msg getMsg() { return msg; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("componentNum", componentNum)
            .add("verb", verb)
            .add("updateOrganizer", updateOrganizer)
            .add("identityId", identityId)
            .add("exceptionId", exceptionId)
            .add("timezone", timezone)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
