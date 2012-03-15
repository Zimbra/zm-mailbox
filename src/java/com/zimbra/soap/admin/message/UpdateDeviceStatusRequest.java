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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusAdminConstants;
import com.zimbra.soap.mail.type.IdStatus;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-description Update device status
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusAdminConstants.E_UPDATE_DEVICE_STATUS_REQUEST)
public class UpdateDeviceStatusRequest {

    /**
     * @zm-api-field-description Account selector
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=true)
    private final AccountSelector account;

    /**
     * @zm-api-field-description Information on new device status
     */
    @XmlElement(name=MailConstants.E_DEVICE /* device */, required=true)
    private final IdStatus device;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private UpdateDeviceStatusRequest() {
        this((AccountSelector) null, (IdStatus) null);
    }

    public UpdateDeviceStatusRequest(AccountSelector account, IdStatus device) {
        this.account = account;
        this.device = device;
    }

    public AccountSelector getAccount() { return account; }
    public IdStatus getDevice() { return device; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("account", account)
            .add("device", device);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
