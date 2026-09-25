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
 * Response payload returned after removing a device push notification token.
 *
 * @zm-api-response-description Result of removing a device push notification token.
 */
@XmlRootElement(name = "RemoveDeviceTokenResponse")
@XmlType(propOrder = {})
public class RemoveDeviceTokenResponse {

    /**
     * @zm-api-field-tag removal-status
     * @zm-api-field-description Removal result. The successful value is {@code success}.
     */
    @XmlElement(name = "status", required = true)
    private String status;

    public RemoveDeviceTokenResponse() {
    }

    public RemoveDeviceTokenResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

