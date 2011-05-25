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
import com.zimbra.soap.mail.type.InstanceRecurIdInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_CANCEL_APPOINTMENT_REQUEST)
public class CancelAppointmentRequest {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.E_INVITE_COMPONENT /* comp */,
                    required=false)
    private Integer numComponents;

    // Ignored on server
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */,
                    required=false)
    private Integer modifiedSequence;

    // Ignored on server
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    @XmlElement(name=MailConstants.E_INSTANCE /* inst */, required=false)
    private InstanceRecurIdInfo instance;

    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo timezone;

    // E_INVITE child is not allowed
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private CalendarItemMsg msg;

    public CancelAppointmentRequest() {
    }

    public void setId(String id) { this.id = id; }
    public void setNumComponents(Integer numComponents) {
        this.numComponents = numComponents;
    }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setInstance(InstanceRecurIdInfo instance) {
        this.instance = instance;
    }
    public void setTimezone(CalTZInfo timezone) { this.timezone = timezone; }
    public void setMsg(CalendarItemMsg msg) { this.msg = msg; }
    public String getId() { return id; }
    public Integer getNumComponents() { return numComponents; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public Integer getRevision() { return revision; }
    public InstanceRecurIdInfo getInstance() { return instance; }
    public CalTZInfo getTimezone() { return timezone; }
    public CalendarItemMsg getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("numComponents", numComponents)
            .add("modifiedSequence", modifiedSequence)
            .add("revision", revision)
            .add("instance", instance)
            .add("timezone", timezone)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
