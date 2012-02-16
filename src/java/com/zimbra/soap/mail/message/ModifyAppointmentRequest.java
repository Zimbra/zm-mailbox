/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalItemRequestBase;

/**
 * @zm-api-command-description Modify an appointment, or if the appointment is a recurrence then modify the "default"
 * invites. That is, all instances that do not have exceptions.
 * <br />
 * <br />
 * If the appointment has a <b>&lt;recur></b>, then the following caveats are worth mentioning:
 * <br />
 * <b>If any of: START, DURATION, END or RECUR change, then all exceptions are implicitly canceled!</b>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_MODIFY_APPOINTMENT_REQUEST)
public class ModifyAppointmentRequest extends CalItemRequestBase {

    /**
     * @zm-api-field-tag invite-id-of-default-invite
     * @zm-api-field-description Invite ID of default invite
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag default-invite-component-num
     * @zm-api-field-description Component number of default component
     */
    @XmlAttribute(name=MailConstants.A_CAL_COMP /* comp */, required=false)
    private Integer componentNum;

    /**
     * @zm-api-field-tag changed-seq-of-fetched-version
     * @zm-api-field-description Changed sequence of fetched version.
     * <br />
     * Used for conflict detection.  By setting this, the request indicates which version of the appointment it is
     * attempting to modify.  If the appointment was updated on the server between the fetch and modify, an
     * <b>INVITE_OUT_OF_DATE exception</b> will be thrown.
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description
     */
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    public ModifyAppointmentRequest() {
    }

    public void setId(String id) { this.id = id; }
    public void setComponentNum(Integer componentNum) { this.componentNum = componentNum; }
    public void setModifiedSequence(Integer modifiedSequence) { this.modifiedSequence = modifiedSequence; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public String getId() { return id; }
    public Integer getComponentNum() { return componentNum; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public Integer getRevision() { return revision; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("componentNum", componentNum)
            .add("modifiedSequence", modifiedSequence)
            .add("revision", revision);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
