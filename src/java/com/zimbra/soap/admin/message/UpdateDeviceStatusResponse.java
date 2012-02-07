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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusAdminConstants;
import com.zimbra.soap.mail.type.DeviceInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusAdminConstants.E_UPDATE_DEVICE_STATUS_RESPONSE)
@XmlType(propOrder = {})
public class UpdateDeviceStatusResponse {

    /**
     * @zm-api-field-description Information about devices
     */
    @XmlElement(name=MailConstants.E_DEVICE /* device */, required=false)
    private List<DeviceInfo> devices = Lists.newArrayList();

    public UpdateDeviceStatusResponse() {
    }

    public void setDevices(Iterable <DeviceInfo> devices) {
        this.devices.clear();
        if (devices != null) {
            Iterables.addAll(this.devices,devices);
        }
    }

    public void addDevice(DeviceInfo device) {
        this.devices.add(device);
    }

    public List<DeviceInfo> getDevices() {
        return Collections.unmodifiableList(devices);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("devices", devices);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
