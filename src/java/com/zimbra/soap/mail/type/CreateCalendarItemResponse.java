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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class CreateCalendarItemResponse {

    @XmlAttribute(name=MailConstants.A_CAL_ID /* calItemId */, required=false)
    private String calItemId;

    // For backwards compat
    @XmlAttribute(name=MailConstants.A_APPT_ID_DEPRECATE_ME /* apptId */, required=false)
    private String deprecatedApptId;

    @XmlAttribute(name=MailConstants.A_CAL_INV_ID /* invId */, required=false)
    private String calInvId;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private Id msg;

    @XmlElement(name=MailConstants.A_CAL_ECHO /* echo */, required=false)
    private CalEcho echo;

    public CreateCalendarItemResponse() {
    }

    public void setCalItemId(String calItemId) { this.calItemId = calItemId; }
    public void setDeprecatedApptId(String deprecatedApptId) {
        this.deprecatedApptId = deprecatedApptId;
    }
    public void setCalInvId(String calInvId) { this.calInvId = calInvId; }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setMsg(Id msg) { this.msg = msg; }
    public void setEcho(CalEcho echo) { this.echo = echo; }
    public String getCalItemId() { return calItemId; }
    public String getDeprecatedApptId() { return deprecatedApptId; }
    public String getCalInvId() { return calInvId; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public Integer getRevision() { return revision; }
    public Id getMsg() { return msg; }
    public CalEcho getEcho() { return echo; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("calItemId", calItemId)
            .add("deprecatedApptId", deprecatedApptId)
            .add("calInvId", calInvId)
            .add("modifiedSequence", modifiedSequence)
            .add("revision", revision)
            .add("msg", msg)
            .add("echo", echo);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
