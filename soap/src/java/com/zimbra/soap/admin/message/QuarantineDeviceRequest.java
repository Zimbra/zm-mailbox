/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.admin.type.DeviceId;
import com.zimbra.soap.type.AccountSelector;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Quarantine a device or all devices attached to an account from further sync actions
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SyncAdminConstants.E_QUARANTINE_DEVICE_REQUEST)
public class QuarantineDeviceRequest {

    /**
     * @zm-api-field-description Account selector
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private AccountSelector account;

    /**
     * @zm-api-field-description Device selector
     */
    @XmlElement(name=SyncConstants.E_DEVICE, required=true)
    private DeviceId deviceId;

    /**
     * @zm-api-field-description Boolean byDeviceIdOnly
     */
    @XmlElement(name=AdminConstants.E_BY_DEVICEID_ONLY /* byDeviceIdOnly */, required=false)
    private Boolean byDeviceIdOnly;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private QuarantineDeviceRequest() {
    }

    public QuarantineDeviceRequest(AccountSelector account) {
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

    public Boolean getByDeviceIdOnly() { return byDeviceIdOnly; }

    public void setByDeviceIdOnly(Boolean byDeviceIdOnly) { this.byDeviceIdOnly = byDeviceIdOnly; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("account", this.account).add("device", this.deviceId).add("byDeviceIdOnly", this.byDeviceIdOnly).toString();
    }
}
