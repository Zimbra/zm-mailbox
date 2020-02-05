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

    public static GetAppointmentRequest createForIdInvitesContent(String reqId,
            Boolean includeInvites, Boolean includeContent, Boolean sync) {
        GetAppointmentRequest req = new GetAppointmentRequest();
        req.setId(reqId);
        req.setIncludeContent(includeContent);
        req.setIncludeInvites(includeInvites);
        req.setSync(sync);
        return req;
    }
}
