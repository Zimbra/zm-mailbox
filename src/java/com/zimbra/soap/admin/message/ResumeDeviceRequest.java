/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2012 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.sync.type.DeviceId;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=SyncAdminConstants.E_RESUME_DEVICE_REQUEST)
public class ResumeDeviceRequest {

    @XmlElement(name=SyncConstants.E_DEVICE /* device */, required=true)
    private final DeviceId device;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ResumeDeviceRequest() {
        this((DeviceId) null);
    }

    public ResumeDeviceRequest(DeviceId device) {
        this.device = device;
    }

    public DeviceId getDevice() { return device; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("device", device);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
