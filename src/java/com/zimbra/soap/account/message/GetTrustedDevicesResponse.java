/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlRootElement(name=AccountConstants.E_GET_TRUSTED_DEVICES_RESPONSE)
public class GetTrustedDevicesResponse {

    public GetTrustedDevicesResponse() {}

    @XmlAttribute(name=AccountConstants.A_NUM_OTHER_TRUSTED_DEVICES)
    private Integer numOtherTrustedDevices;

    @XmlAttribute(name=AccountConstants.A_THIS_DEVICE_TRUSTED)
    private ZmBoolean thisDeviceTrusted;

    public void setNumOtherTrustedDevices(Integer n) { numOtherTrustedDevices = n; }
    public Integer getNumOtherTrustedDevices() { return numOtherTrustedDevices; }

    public void setThisDeviceTrusted(Boolean b) { thisDeviceTrusted = ZmBoolean.fromBool(b); }
    public Boolean getThisDeviceTrusted() { return ZmBoolean.toBool(thisDeviceTrusted); }

}
