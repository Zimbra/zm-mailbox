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

package com.zimbra.soap.sync.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.sync.type.DeviceStatusInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SyncConstants.E_ALLOW_DEVICE_RESPONSE)
public class AllowDeviceResponse {

    /**
     * @zm-api-field-description Device status information
     */
    @XmlElement(name=SyncConstants.E_DEVICE /* device */, required=false)
    private DeviceStatusInfo device;

    public AllowDeviceResponse() {}

    public void setDevice(DeviceStatusInfo device) {
        this.device = device;
    }

    public DeviceStatusInfo getDevice() {
        return device;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("device", device);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
