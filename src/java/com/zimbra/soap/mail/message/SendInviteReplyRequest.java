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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalendarItemMsg;
import com.zimbra.soap.mail.type.CalTZInfo;
import com.zimbra.soap.mail.type.DtTimeInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SEND_INVITE_REPLY_REQUEST)
public class SendInviteReplyRequest {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM /* compNum */, required=true)
    private final int numComponents;

    @XmlAttribute(name=MailConstants.A_VERB /* verb */, required=true)
    private final String verb;

    @XmlAttribute(name=MailConstants.A_CAL_UPDATE_ORGANIZER /* updateOrganizer */, required=false)
    private ZmBoolean updateOrganizer;

    @XmlAttribute(name=MailConstants.A_IDENTITY_ID /* idnt */, required=false)
    private String identityId;

    @XmlElement(name=MailConstants.E_CAL_EXCEPTION_ID /* exceptId */, required=false)
    private DtTimeInfo exceptionId;

    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo timezone;

    // E_INVITE child is not allowed
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private CalendarItemMsg msg;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SendInviteReplyRequest() {
        this((String) null, -1, (String) null);
    }

    public SendInviteReplyRequest(String id, int numComponents, String verb) {
        this.id = id;
        this.numComponents = numComponents;
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
    public void setMsg(CalendarItemMsg msg) { this.msg = msg; }
    public String getId() { return id; }
    public int getNumComponents() { return numComponents; }
    public String getVerb() { return verb; }
    public Boolean getUpdateOrganizer() { return ZmBoolean.toBool(updateOrganizer); }
    public String getIdentityId() { return identityId; }
    public DtTimeInfo getExceptionId() { return exceptionId; }
    public CalTZInfo getTimezone() { return timezone; }
    public CalendarItemMsg getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("numComponents", numComponents)
            .add("verb", verb)
            .add("updateOrganizer", updateOrganizer)
            .add("identityId", identityId)
            .add("exceptionId", exceptionId)
            .add("timezone", timezone)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
