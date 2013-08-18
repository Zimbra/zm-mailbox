/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.GetCalendarItemRequestBase;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
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
public class GetAppointmentRequest extends GetCalendarItemRequestBase {
    public GetAppointmentRequest() {
    }

    public static GetAppointmentRequest createForUidInvitesContent(String reqUid,
            Boolean includeInvites, Boolean includeContent) {
        GetAppointmentRequest req = new GetAppointmentRequest();
        req.setUid(reqUid);
        req.setIncludeContent(includeContent);
        req.setIncludeInvites(includeInvites);
        return req;
    }
}
