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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.admin.type.DeviceId;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=SyncAdminConstants.E_REMOVE_DEVICE_REQUEST)
public class RemoveDeviceRequest {

    @XmlElement(name=SyncConstants.E_DEVICE /* device */, required = false)
    private DeviceId deviceId;

    @XmlElement(name = MailConstants.E_MAILBOX /* mailboxId */, required = false)
    private int mailboxId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RemoveDeviceRequest() {
        this(null);
    }

    public RemoveDeviceRequest(DeviceId device) {
        this.deviceId = device;
    }

    public RemoveDeviceRequest(int mboxId) {
        this.mailboxId = mboxId;
    }

    public DeviceId getDeviceId() {
        return this.deviceId;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("device", this.deviceId).add("mboxId", this.mailboxId).toString();
    }
}
