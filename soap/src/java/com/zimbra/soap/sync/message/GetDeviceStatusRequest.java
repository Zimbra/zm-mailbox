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

package com.zimbra.soap.sync.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.SyncConstants;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get status for devices
 */
@XmlRootElement(name=SyncConstants.E_GET_DEVICE_STATUS_REQUEST)
public class GetDeviceStatusRequest {

    /**
     * @zm-api-field-tag includeRemovedDevices
     * @zm-api-field-description Whether to include devices removed by user or not. Default is FALSE, which will not include removed devices.
     */
    @XmlAttribute(name=SyncConstants.A_INCLUDE_REMOVED_DEVICES /* includeRemovedDevices */, required=false)
    private Boolean includeRemovedDevices = false;

    public GetDeviceStatusRequest() {
        this(false);
    }

    /**
     * @param includeRemovedDevices
     */
    public GetDeviceStatusRequest(Boolean includeRemovedDevices) {
        this.includeRemovedDevices = includeRemovedDevices;
    }

    /**
     * @return the includeRemovedDevices
     */
    public Boolean getIncludeRemovedDevices() {
        return includeRemovedDevices;
    }

    /**
     * @param includeRemovedDevices the includeRemovedDevices to set
     */
    public void setIncludeRemovedDevices(Boolean includeRemovedDevices) {
        this.includeRemovedDevices = includeRemovedDevices;
    }
}
