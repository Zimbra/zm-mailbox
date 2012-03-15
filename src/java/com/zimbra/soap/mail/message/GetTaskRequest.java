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
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Get Task
 * <br />
 * Similar to GetAppointmentRequest/GetAppointmentResponse
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_TASK_REQUEST)
public class GetTaskRequest {

    /**
     * @zm-api-field-tag return-mod-date
     * @zm-api-field-description Set this to return the modified date (md) on the appointment.
     */
    @XmlAttribute(name=MailConstants.A_SYNC /* sync */, required=false)
    private ZmBoolean sync;

    /**
     * @zm-api-field-tag include-mime-body-parts
     * @zm-api-field-description If set, MIME parts for body content are returned; <b>default unset</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_INCLUDE_CONTENT /* includeContent */, required=false)
    private ZmBoolean includeContent;

    /**
     * @zm-api-field-tag icalendar-uid
     * @zm-api-field-description iCalendar UID
     * Either id or uid should be specified, but not both
     */
    @XmlAttribute(name=MailConstants.A_UID /* uid */, required=false)
    private String uid;

    /**
     * @zm-api-field-tag appointment-id
     * @zm-api-field-description Appointment ID.
     * Either id or uid should be specified, but not both
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    public GetTaskRequest() {
    }

    public void setSync(Boolean sync) { this.sync = ZmBoolean.fromBool(sync); }
    public void setIncludeContent(Boolean includeContent) { this.includeContent = ZmBoolean.fromBool(includeContent); }
    public void setUid(String uid) { this.uid = uid; }
    public void setId(String id) { this.id = id; }
    public Boolean getSync() { return ZmBoolean.toBool(sync); }
    public Boolean getIncludeContent() { return ZmBoolean.toBool(includeContent); }
    public String getUid() { return uid; }
    public String getId() { return id; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("sync", sync)
            .add("includeContent", includeContent)
            .add("uid", uid)
            .add("id", id);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
