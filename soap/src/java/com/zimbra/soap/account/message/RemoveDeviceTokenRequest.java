/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Request payload for removing a device push notification token.
 *
 * The mailbox is determined from the authenticated request context. It is never accepted as part of this payload,
 * so a caller can only remove tokens that belong to its own mailbox.
 *
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Remove a device push notification token
 */
@XmlRootElement(name = "RemoveDeviceTokenRequest")
@XmlType(propOrder = {})
public class RemoveDeviceTokenRequest {

    /**
     * @zm-api-field-tag device-token
     * @zm-api-field-description Device registration token supplied by the push notification provider.
     */
    @XmlElement(name = "deviceToken", required = true)
    private String deviceToken;

    public RemoveDeviceTokenRequest() {
    }

    public RemoveDeviceTokenRequest(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}

