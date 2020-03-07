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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ABQDevice;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ABQ_DEVICES_LIST_RESPONSE /* GetAbqDevicesListResponse */)
public class GetABQDevicesListResponse {

    /**
     * @zm-api-field-description list of ABQ devices
     */
    @XmlElement(name=AdminConstants.E_ABQ_DEVICE /* abqDevice */, required = false)
    private List<ABQDevice> abqDevices;

    public GetABQDevicesListResponse() {
        this.abqDevices = null;
    }

    public GetABQDevicesListResponse(List<ABQDevice> abqDevices) {
        this.abqDevices = abqDevices;
    }

    public List<ABQDevice> getAbqDevices() {
        return abqDevices;
    }

    public void setAbqDevices(List<ABQDevice> abqDevices) {
        this.abqDevices = new ArrayList<ABQDevice>();
        this.abqDevices.addAll(abqDevices);
    }

    public void addAbqDevice(ABQDevice device) {
        if (this.abqDevices == null) {
            this.abqDevices = new ArrayList<ABQDevice>();
        }
        this.abqDevices.add(device);
    }
}
