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
 * Request payload for registering a device push notification token.
 * The mailbox is determined from the authenticated request context.  It is
 * never accepted as part of this payload.
 *
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Register or update a device push notification token
 */
@XmlRootElement(name = "AddDeviceTokenRequest")
@XmlType(propOrder = {})
public class AddDeviceTokenRequest {

    /**
     * @zm-api-field-tag device-token
     * @zm-api-field-description Device registration token supplied by the push notification provider.
     */
    @XmlElement(name = "deviceToken", required = true)
    private String deviceToken;

    /**
     * @zm-api-field-tag device-id
     * @zm-api-field-description Stable client generated identifier of the device installation.
     * Together with the mailbox it uniquely identifies a registration.
     */
    @XmlElement(name = "deviceId", required = true)
    private String deviceId;

    /**
     * @zm-api-field-tag device-type
     * @zm-api-field-description Type of the device. Valid values: {@code MOBILE}, {@code DESKTOP}.
     */
    @XmlElement(name = "deviceType", required = true)
    private String deviceType;

    /**
     * @zm-api-field-tag push-provider
     * @zm-api-field-description Push notification provider that issued the token.
     * Valid values: {@code FCM}. {@code APNS}, {@code WNS} and {@code WEBPUSH} are reserved.
     */
    @XmlElement(name = "pushProvider", required = true)
    private String pushProvider;

    /**
     * @zm-api-field-tag app-id
     * @zm-api-field-description Bundle or package identifier of the application registering the token.
     */
    @XmlElement(name = "appId", required = true)
    private String appId;

    /**
     * @zm-api-field-tag app-version
     * @zm-api-field-description Version of the mobile application registering the token.
     */
    @XmlElement(name = "appVersion", required = true)
    private String appVersion;

    /**
     * @zm-api-field-tag device-os-name
     * @zm-api-field-description Operating system name of the device, such as iOS or Android.
     */
    @XmlElement(name = "deviceOSName", required = true)
    private String deviceOSName;

    /**
     * @zm-api-field-tag device-os-version
     * @zm-api-field-description Operating system version of the device.
     */
    @XmlElement(name = "deviceOSVersion", required = true)
    private String deviceOSVersion;

    public AddDeviceTokenRequest() {
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getPushProvider() {
        return pushProvider;
    }

    public void setPushProvider(String pushProvider) {
        this.pushProvider = pushProvider;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getDeviceOSName() {
        return deviceOSName;
    }

    public void setDeviceOSName(String deviceOSName) {
        this.deviceOSName = deviceOSName;
    }

    public String getDeviceOSVersion() {
        return deviceOSVersion;
    }

    public void setDeviceOSVersion(String deviceOSVersion) {
        this.deviceOSVersion = deviceOSVersion;
    }
}
