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
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.AccountSelector;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Block a device or all devices attached to given account/device from further sync actions
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SyncAdminConstants.E_BLOCK_DEVICE_REQUEST)
public class BlockDeviceRequest {

    /**
     * @zm-api-field-description Account selector
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private AccountSelector account;

    /**
     * @zm-api-field-description Device selector
     */
    @XmlElement(name=SyncConstants.E_DEVICE, required=false)
    private DeviceId deviceId;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=false)
    private DomainSelector domain;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BlockDeviceRequest() {
    }

    public BlockDeviceRequest(DeviceId deviceId) {
        this.deviceId = deviceId;
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

    public void setAccount(AccountSelector account) {
        this.account = account;
    }

    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    public DomainSelector getDomain() { return domain; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("account", this.account).add("device", this.deviceId).add("domain", this.domain).toString();
    }
}