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
import com.zimbra.soap.mail.type.Msg;
import com.zimbra.soap.mail.type.CalTZInfo;
import com.zimbra.soap.mail.type.InstanceRecurIdInfo;

/**
 * @zm-api-command-description Cancel appointment
 * <br />
 * NOTE: If canceling an exception, the original instance (ie the one the exception was "excepting") WILL NOT be
 * restored when you cancel this exception.
 * <br />
 * <br />
 * if <b>&lt;inst></b> is set, then this cancels just the specified instance or range of instances, otherwise it
 * cancels the entire appointment.  If <b>&lt;inst></b> is not set, then id MUST refer to the default invite for the
 * appointment.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CANCEL_APPOINTMENT_REQUEST)
public class CancelAppointmentRequest {

    /**
     * @zm-api-field-tag default-invite-id
     * @zm-api-field-description ID of default invite
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag default-invite-component-num
     * @zm-api-field-description Component number of default invite
     */
    @XmlAttribute(name=MailConstants.E_INVITE_COMPONENT /* comp */, required=false)
    private Integer componentNum;

    // Ignored on server
    /**
     * @zm-api-field-tag modified-sequence
     * @zm-api-field-description Modified sequence
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    // Ignored on server
    /**
     * @zm-api-field-tag revision
     * @zm-api-field-description Revision
     */
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    /**
     * @zm-api-field-description Instance recurrence ID information
     */
    @XmlElement(name=MailConstants.E_INSTANCE /* inst */, required=false)
    private InstanceRecurIdInfo instance;

    /**
     * @zm-api-field-description Definition for TZID referenced by DATETIME in <b>&lt;inst></b>
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo timezone;

    // E_INVITE child is not allowed
    /**
     * @zm-api-field-description Message
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private Msg msg;

    public CancelAppointmentRequest() {
    }

    public void setId(String id) { this.id = id; }
    public void setComponentNum(Integer componentNum) {
        this.componentNum = componentNum;
    }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setInstance(InstanceRecurIdInfo instance) {
        this.instance = instance;
    }
    public void setTimezone(CalTZInfo timezone) { this.timezone = timezone; }
    public void setMsg(Msg msg) { this.msg = msg; }
    public String getId() { return id; }
    public Integer getComponentNum() { return componentNum; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public Integer getRevision() { return revision; }
    public InstanceRecurIdInfo getInstance() { return instance; }
    public CalTZInfo getTimezone() { return timezone; }
    public Msg getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("componentNum", componentNum)
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
