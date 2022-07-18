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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.admin.type.DeviceId;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.CosSelector;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get the requested device's status
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_SEND_EMAIL_REQUEST)
public class SendEmailRequest {

    /**
     * @zm-api-field-tag to
     * @zm-api-field-description the email address of the user to which the email is to be send.
     */
    @XmlElement(name = SyncAdminConstants.E_TO /* to */, required = true)
    private String to;

    /**
     * @zm-api-field-tag subject
     * @zm-api-field-description subject of the email that will be sent to above user.
     */
    @XmlElement(name = SyncAdminConstants.E_SUBJECT /* subject */, required = true)
    private String subject;

    /**
     * @zm-api-field-tag message
     * @zm-api-field-description email content of the mail that will be sent to user.
     */
    @XmlElement(name = SyncAdminConstants.E_MESSAGE /* message */, required = true)
    private String message;

    @SuppressWarnings("unused")
    public SendEmailRequest() {
    }

    public SendEmailRequest(String to, String subject, String message) {
        this.to = to;
        this.subject = subject;
        this.message = message;
    }

    public SendEmailRequest getTo() {
        return this;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public SendEmailRequest getSubject() {
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public SendEmailRequest getMessage() {
        return this;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
