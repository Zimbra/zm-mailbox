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
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ABQ_DEVICE_REQUEST)
public class ABQDeviceRequest {
    @XmlEnum
    public enum ABQDeviceOperation {
        // case must match
        add, modify, remove;

        public static ABQDeviceOperation fromString(String s) throws ServiceException {
            try {
                return ABQDeviceOperation.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag op
     * @zm-api-field-description op can be either add, modify, remove
     */
    @XmlAttribute(name=AdminConstants.A_OP /* op */, required=true)
    private ABQDeviceOperation op;
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
    private String status = AdminConstants.DEFAULT_ABQ_STATUS.toString();

    /**
     * @return the op
     */
    public ABQDeviceOperation getOp() {
        return op;
    }

    /**
     * @param op, the op to set
     */
    public void setOp(ABQDeviceOperation op) {
        this.op = op;
    }

    /**
     * @return the deviceId
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * @param deviceId, sets the deviceId
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * @return the accountId
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * @param accountId, sets the accountId
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status, sets the status
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
