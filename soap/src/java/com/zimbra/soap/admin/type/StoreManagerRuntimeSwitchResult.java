/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C)2022 Synacor, Inc.
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

import com.zimbra.common.soap.AdminConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;

@XmlAccessorType(XmlAccessType.NONE)
public class StoreManagerRuntimeSwitchResult {

    @XmlEnum
    public enum RuntimeSwitchStatus {
        SUCCESS,
        FAIL,
        NO_OPERATION;
    }

    @XmlAttribute(name= AdminConstants.A_SM_RUNTIME_SWITCH_STATUS /* status */, required=true)
    private RuntimeSwitchStatus runtimeSwitchStatus;

    /**
     * @zm-api-field-tag volume-root-path
     * @zm-api-field-description Absolute path to root of volume, e.g. /opt/zimbra/store
     */
    @XmlAttribute(name=AdminConstants.A_SM_RUNTIME_SWITCH_MESSAGE /* message */, required=true)
    private String message;

    public StoreManagerRuntimeSwitchResult() {
    }

    public StoreManagerRuntimeSwitchResult(RuntimeSwitchStatus status, String message) {
        this.runtimeSwitchStatus = status;
        this.message = message;
    }

    public RuntimeSwitchStatus getStatus() {
        return runtimeSwitchStatus;
    }

    public void setStatus(RuntimeSwitchStatus status) {
        this.runtimeSwitchStatus = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}