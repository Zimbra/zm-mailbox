/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2020 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ABQ_DEVICE_REQUEST)
public class AbqDeviceRequest {
    
    /**
     * @zm-api-field-tag device-id
     * @zm-api-field-description ABQ Device device_id
     */
    @XmlAttribute(name=AdminConstants.E_DEVICE_ID /* deviceId */, required=true)
    private String deviceId = "";
    
    /**
     * @zm-api-field-tag account_id
     * @zm-api-field-description ABQ Device account_id
     */
    @XmlAttribute(name=AdminConstants.E_ACCOUNT_ID /* accountId */, required=false)
    private String accountId = null;
    
    /**
     * @zm-api-field-tag status
     * @zm-api-field-description ABQ Device status
     */
    @XmlAttribute(name=AdminConstants.E_STATUS /* status */, required=false)
    private String status;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
