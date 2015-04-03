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
     * @zm-api-field-tag device-id
     * @zm-api-field-description Device ID.
     */
    @XmlAttribute(name = AccountConstants.A_DEVICE_ID /* device id */, required = true)
    private final String deviceId;

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
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZmgDeviceSpec() {
        this((String) null);
    }

    public ZmgDeviceSpec(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public void setPushProvider(String pushProvider) {
        this.pushProvider = pushProvider;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getPushProvider() {
        return pushProvider;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("deviceId", deviceId).add("registrationId", registrationId)
            .add("pushProvider", pushProvider);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
