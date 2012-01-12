/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2012 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.admin.type.DeviceId;
import com.zimbra.soap.type.AccountSelector;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=SyncAdminConstants.E_SUSPEND_DEVICE_REQUEST)
public class SuspendDeviceRequest {

    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private AccountSelector account;

    @XmlElement(name=SyncConstants.E_DEVICE, required=false)
    private DeviceId deviceId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SuspendDeviceRequest() {
    }

    public SuspendDeviceRequest(AccountSelector account) {
        this.account = account;
    }

    public DeviceId getDevice() {
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
