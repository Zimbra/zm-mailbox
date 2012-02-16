/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.soap.mail.type.IdStatus;

/**
 * @zm-api-command-description Update device status
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusXmlConstants.E_UPDATE_DEVICE_STATUS_REQUEST)
public class UpdateDeviceStatusRequest {

    /**
     * @zm-api-field-description Information about device status.
     * Setting of "status" attribute:
     * <table>
     * <tr> <td> <b>enabled</b> </td> <td> in normal operation </td> </tr>
     * <tr> <td> <b>disabled</b> </td>
     *      <td> user or admin requested to disable this device.  the device will perform self wipe next time it
     *           contacts the server.  </td> </tr>
     * <tr> <td> <b>locked</b> </td> <td> device is temporarily locked </td> </tr>
     * <tr> <td> <b>wiped</b> </td> 
     *      <td> device has acknowledged the disable request, and wiped the the downloaded files and
     *           authentication information.
     * </td> </tr>
     * </table>
     */
    @XmlElement(name=MailConstants.E_DEVICE /* device */, required=true)
    private final IdStatus device;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private UpdateDeviceStatusRequest() {
        this((IdStatus) null);
    }

    public UpdateDeviceStatusRequest(IdStatus device) {
        this.device = device;
    }

    public IdStatus getDevice() { return device; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("device", device);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
