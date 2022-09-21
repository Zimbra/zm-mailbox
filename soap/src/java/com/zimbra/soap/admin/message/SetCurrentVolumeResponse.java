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

import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.StoreManagerRuntimeSwitchResult;

import javax.xml.bind.annotation.XmlElement;

@XmlRootElement(name=AdminConstants.E_SET_CURRENT_VOLUME_RESPONSE)
public class SetCurrentVolumeResponse {

    @XmlElement(name=AdminConstants.E_STORE_MANAGER_RUNTIME_SWITCH_RESULT, required=false)
    private StoreManagerRuntimeSwitchResult runtimeSwitchResult;

    public StoreManagerRuntimeSwitchResult getRuntimeSwitchResult() {
        return runtimeSwitchResult;
    }

    public void setRuntimeSwitchResult(StoreManagerRuntimeSwitchResult runtimeSwitchResult) {
        this.runtimeSwitchResult = runtimeSwitchResult;
    }
}
