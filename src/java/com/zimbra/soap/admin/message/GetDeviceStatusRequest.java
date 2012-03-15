/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.admin.type.DeviceId;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-description Get the requested device's status
 */
@XmlRootElement(name = SyncAdminConstants.E_GET_DEVICE_STATUS_REQUEST)
public class GetDeviceStatusRequest {

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private final AccountSelector account;

    /**
     * @zm-api-field-description Device specification
     */
    @XmlElement(name = SyncConstants.E_DEVICE, required = false)
    private DeviceId deviceId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetDeviceStatusRequest() {
        this(null);
    }

    public GetDeviceStatusRequest(AccountSelector account) {
        this.account = account;
    }

    public DeviceId getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public AccountSelector getAccount() {
        return this.account;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("account", this.account).add("device", this.deviceId).toString();
    }
}
