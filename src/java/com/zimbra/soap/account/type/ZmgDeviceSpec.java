/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.Objects;
import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ZmgDeviceSpec {

    /**
     * @zm-api-field-tag app-id
     * @zm-api-field-description App ID.
     */
    @XmlAttribute(name = AccountConstants.E_APP_ID /* app id */, required = true)
    private final String appId;

    /**
     * @zm-api-field-tag registration-id
     * @zm-api-field-description The registration id of the device for push
     *                           notifications.
     */
    @XmlAttribute(name = AccountConstants.A_REGISTRATION_ID /* registration id */, required = false)
    private String registrationId;

    /**
     * @zm-api-field-tag push-provider
     * @zm-api-field-description the provider for pushing notifications to the
     *                           device
     */
    @XmlAttribute(name = AccountConstants.A_PUSH_PROVIDER /* push provider */, required = false)
    private String pushProvider;

    /**
     * @zm-api-field-tag os-name
     * @zm-api-field-description osName is the name of the operating system
     *                           installed on the device. Example - ios, android
     */
    @XmlAttribute(name = AccountConstants.A_OS_NAME /* OS name */, required = false)
    private String osName;

    /**
     * @zm-api-field-tag os-version
     * @zm-api-field-description The osVersion should be specified in the
     *                           following formats - a)
     *                           majorVersion.minorVersion.microVersion b)
     *                           majorVersion.minorVersion Example - iOS having
     *                           versions like 7.0, 8.0.3, 8.1 etc. Android has
     *                           OS version like 2.0, 3.1, 4.4, 5.0 etc
     */
    @XmlAttribute(name = AccountConstants.A_OS_VERSION /* OS version */, required = false)
    private String osVersion;

    /**
     * @zm-api-field-tag max-payload-size
     * @zm-api-field-description maxPayloadSize is the maximum number of bytes
     *                           allowed for the push notification payload
     *                           Example - iOS 7.0 maxPayloadSize is 256 bytes
     *                           iOS 8.0 onwards maxPayloadSize is 2048 bytes
     *                           Android maxPayloadSize is 4096 bytes
     */
    @XmlAttribute(name = AccountConstants.A_MAX_PAYLOAD_SIZE /* max payload size */, required = false)
    private Integer maxPayloadSize;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZmgDeviceSpec() {
        this((String) null);
    }

    public ZmgDeviceSpec(String appId) {
        this.appId = appId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public void setPushProvider(String pushProvider) {
        this.pushProvider = pushProvider;
    }

    public void setOSName(String osName) {
        this.osName = osName;
    }

    public void setOSVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public void setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    public String getAppId() {
        return appId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getPushProvider() {
        return pushProvider;
    }

    public String getOSName() {
        return osName;
    }

    public String getOSVersion() {
        return osVersion;
    }

    public int getMaxPayloadSize() {
        return maxPayloadSize;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("appId", appId).add("registrationId", registrationId)
            .add("pushProvider", pushProvider).add("osName", osName).add("osVersion", osVersion)
            .add("maxPayloadSize", maxPayloadSize);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
