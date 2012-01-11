/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2012 Zimbra, Inc.  All Rights Reserved.
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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=SyncAdminConstants.E_RESUME_DEVICE_RESPONSE)
@XmlType(propOrder = {})
public class ResumeDeviceResponse {

    @XmlElement(name=SyncConstants.E_DEVICE /* device */, required=false)
    private List<DeviceStatusInfo> devices = Lists.newArrayList();

    public ResumeDeviceResponse() {
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
