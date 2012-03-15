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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.admin.type.DeviceStatusInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SyncAdminConstants.E_REMOTE_WIPE_RESPONSE)
@XmlType(propOrder = {})
public class RemoteWipeResponse {

    /**
     * @zm-api-field-description Information about device status
     */
    @XmlElement(name=SyncConstants.E_DEVICE /* device */, required=false)
    private List<DeviceStatusInfo> devices = Lists.newArrayList();

    public RemoteWipeResponse() {
    }

    public void setDevices(Iterable<DeviceStatusInfo> devices) {
        this.devices.clear();
        if (devices != null) {
            Iterables.addAll(this.devices, devices);
        }
    }

    public void addDevice(DeviceStatusInfo device) {
        this.devices.add(device);
    }

    public List<DeviceStatusInfo> getDevices() {
        return Collections.unmodifiableList(devices);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("devices", devices);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
