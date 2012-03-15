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
 * @zm-api-command-description Get Appointment. Returns the metadata info for each Invite that makes up this appointment.
 * <br />
 * The content (original email) for each invite is stored within the Appointment itself in a big multipart/digest
 * containing each invite in the appointment as a sub-mimepart it can be retreived from the content servlet:
 * <pre>
 *     http://servername/service/content/get?id=&lt;calItemId>
 * </pre>
 * The content for a single Invite can be requested from the content servlet (or from <b>&lt;GetMsg></b>)
 * Individual.  The client can ALSO request just the content for each individual invite using a compound item-id
 * request:
 * <pre>
 *     http://servername/service/content/get?id="calItemId-invite_mail_item_id"
 *     &lt;GetMsgRequest>&lt;m id="calItemId-invite_mail_item_id"
 * </pre>
 * <b>IMPORTANT NOTE</b>: DO NOT use the raw invite-mail-item-id to fetch the content: it might work sometimes,
 * however the invite is a standard mail-message it can be deleted by the user at any time!
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_APPOINTMENT_REQUEST)
public class GetAppointmentRequest {

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

    public GetAppointmentRequest() {
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
