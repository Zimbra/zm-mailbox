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

package com.zimbra.soap.sync.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.sync.type.DeviceStatusInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SyncConstants.E_CANCEL_PENDING_REMOTE_WIPE_RESPONSE)
public class CancelPendingRemoteWipeResponse {

    /**
     * @zm-api-field-description Device status information
     */
    @XmlElement(name=SyncConstants.E_DEVICE /* device */, required=false)
    private DeviceStatusInfo device;

    public CancelPendingRemoteWipeResponse() {
    }

    public void setDevice(DeviceStatusInfo device) { this.device = device; }
    public DeviceStatusInfo getDevice() { return device; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("device", device);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
