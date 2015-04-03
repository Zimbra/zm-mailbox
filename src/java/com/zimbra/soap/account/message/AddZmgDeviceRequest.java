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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.ZmgDeviceSpec;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Add Zmg Device
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AccountConstants.E_ADD_ZMG_DEVICE_REQUEST)
public class AddZmgDeviceRequest {

    /**
     * @zm-api-field-description Zmg Device specification
     */
    @ZimbraUniqueElement
    @XmlElement(name = AccountConstants.E_ZMG_DEVICE /* m */, required = true)
    private final ZmgDeviceSpec zmgDevice;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AddZmgDeviceRequest() {
        this((ZmgDeviceSpec) null);
    }

    public AddZmgDeviceRequest(ZmgDeviceSpec zmgDevice) {
        this.zmgDevice = zmgDevice;
    }

    public ZmgDeviceSpec getZmgDevice() {
        return zmgDevice;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("zmgDevice", zmgDevice);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
