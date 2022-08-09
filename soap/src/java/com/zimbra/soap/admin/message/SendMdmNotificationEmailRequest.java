/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.common.soap.Element;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_SEND_MDM_NOTIFICATION_EMAIL_REQUEST)
public class SendMdmNotificationEmailRequest {
    /**
     * @zm-api-field-tag message
     * @zm-api-field-description email content of the mail that will be sent to
     *                           user.
     */
    @XmlElement(name = SyncAdminConstants.E_MESSAGE /* message */, required = true)
    private String message;

    /**
     * @zm-api-field-tag status
     * @zm-api-field-description for which status email has to be sent like
     *                           quarantined/blocked.
     */
    @XmlElement(name = SyncAdminConstants.E_STATUS /* status */, required = true)
    private String status;

    @SuppressWarnings("unused")
    public SendMdmNotificationEmailRequest() {
    }

    public SendMdmNotificationEmailRequest(String message, String status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
