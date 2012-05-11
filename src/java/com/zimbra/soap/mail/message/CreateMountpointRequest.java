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
import com.zimbra.soap.mail.type.NewMountpointSpec;

/**
 * @zm-api-command-description Create mountpoint
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_MOUNTPOINT_REQUEST)
public class CreateMountpointRequest {

    /**
     * @zm-api-field-description New mountpoint specification
     */
    @XmlElement(name=MailConstants.E_FOLDER, required=true)
    private final NewMountpointSpec folder;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateMountpointRequest() {
        this((NewMountpointSpec) null);
    }

    public CreateMountpointRequest(NewMountpointSpec folder) {
        this.folder = folder;
    }

    public NewMountpointSpec getFolder() { return folder; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("folder", folder)
            .toString();
    }
}
