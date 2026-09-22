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
 * Response payload carrying the Firebase Cloud Messaging client configuration.
 *
 * @zm-api-response-description FCM client configuration for mobile applications.
 */
@XmlRootElement(name = "GetFCMDetailsResponse")
@XmlType(propOrder = {})
public class GetFCMDetailsResponse {

    /**
     * @zm-api-field-tag fcm-device-config
     * @zm-api-field-description FCM client configuration document, as configured on the global config attribute
     * {@code zimbraFCMDeviceJSON}. It never contains server side service account credentials.
     */
    @XmlElement(name = "deviceConfig", required = true)
    private String deviceConfig;

    public GetFCMDetailsResponse() {
    }

    public GetFCMDetailsResponse(String deviceConfig) {
        this.deviceConfig = deviceConfig;
    }

    public String getDeviceConfig() {
        return deviceConfig;
    }

    public void setDeviceConfig(String deviceConfig) {
        this.deviceConfig = deviceConfig;
    }
}

