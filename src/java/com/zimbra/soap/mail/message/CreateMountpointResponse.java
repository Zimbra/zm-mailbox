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
import com.zimbra.soap.mail.type.Mountpoint;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_MOUNTPOINT_RESPONSE)
public class CreateMountpointResponse {

    /**
     * @zm-api-field-description Details of the created mountpoint
     */
    @XmlElement(name=MailConstants.E_MOUNT)
    private Mountpoint mount;

    public CreateMountpointResponse() {
    }

    public void setMount(Mountpoint mount) { this.mount = mount; }
    public Mountpoint getMount() { return mount; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("mount", mount)
            .toString();
    }
}
