/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.admin.type.DeviceId;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get the requested device's status
 */
@XmlRootElement(name = SyncAdminConstants.E_GET_DEVICE_STATUS_REQUEST)
public class GetDeviceStatusRequest {

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required = false)
    private AccountSelector account;

    /**
     * @zm-api-field-description Device id
     */
    @XmlElement(name = SyncConstants.E_DEVICE, required = false)
    private DeviceId deviceId;

    /**
     * @zm-api-field-tag device-status
     * @zm-api-field-description Device status
     */
    @XmlElement(name=SyncConstants.E_STATUS /* status */, required = false)
    private Byte status;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetDeviceStatusRequest() {
        this(null);
    }

    public GetDeviceStatusRequest(AccountSelector account) {
        this(account, null, null);
    }

    public GetDeviceStatusRequest(AccountSelector account, DeviceId deviceId, Byte status) {
        this.account = account;
        this.deviceId = deviceId;
        this.status = status;
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

    public void setAccount(AccountSelector account) {
        this.account = account;
    }

    public Byte getStatus() {
        return this.status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("account", this.account).add("device", this.deviceId).add("status", this.status).toString();
    }
}
