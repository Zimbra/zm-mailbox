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
package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

/**
 * 
 * @author jyotiranjan.jena
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class ABQDevice {

    /**
     * @zm-api-field-tag deviceId
     * @zm-api-field-description device id
     */
    @XmlAttribute(name = AdminConstants.A_DEVICE_ID /* devid */)
    private String device_id;
    /**
     * @zm-api-field-tag accountId
     * @zm-api-field-description account id
     */
    @XmlAttribute(name = AdminConstants.A_ACCOUNT_ID /* acid */)
    private String account_id;
    /**
     * @zm-api-field-tag deviceStatus
     * @zm-api-field-description device status
     */
    @XmlAttribute(name = AdminConstants.A_DEVICE_STATUS /* ds */)
    private String status;

    @XmlAttribute(name = AdminConstants.A_CREATED_ON /* cd */)
    private String created_on;

    @XmlAttribute(name = AdminConstants.A_CREATED_BY /* cb */)
    private String created_by;

    @XmlAttribute(name = AdminConstants.A_MODIFIED_ON /* md */)
    private String modified_on;

    @XmlAttribute(name = AdminConstants.A_MODIFIED_BY /* mb */)
    private String modified_by;

    public ABQDevice() {
        
    }

    public ABQDevice(String deviceId, String accountId, String status, String created, String createdBy, String modified, String modifiedBy) {
        this.account_id = accountId;
        this.device_id = deviceId;
        this.status = status;
        this.created_on = created;
        this.created_by = createdBy;
        this.modified_on = modified;
        this.modified_by = modifiedBy;
    }

    /**
     * @return deviceId
     */
    public String getDevice_id() {
        return device_id;
    }

    /**
     * @param device_id the device id to set.
     */
    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    /**
     * @return accountId for the device
     */
    public String getAccount_id() {
        return account_id;
    }

    /**
     * @param account_id the account id to be set for the device.
     */
    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }

    /**
     * @return device status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status device status to be set.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return created date
     */
    public String getCreated_on() {
        return created_on;
    }

    /**
     * @param created_on the created date to set
     */
    public void setCreated_on(String created_on) {
        this.created_on = created_on;
    }

    /**
     * @return created_by
     */
    public String getCreated_by() {
        return created_by;
    }

    /**
     * @param created_by the created_by to set
     */
    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    /**
     * @return modified date
     */
    public String getModified_on() {
        return modified_on;
    }

    /**
     * @param modified_on the modified date to set
     */
    public void setModified_on(String modified_on) {
        this.modified_on = modified_on;
    }

    /**
     * @return modified by
     */
    public String getModified_by() {
        return modified_by;
    }

    /**
     * @param modified_by the modified by to set
     */
    public void setModified_by(String modified_by) {
        this.modified_by = modified_by;
    }
}
