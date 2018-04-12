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

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("devices", devices);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
