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
import com.zimbra.soap.mail.type.NewFolderSpec;

/**
 * @zm-api-command-description Create folder
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_FOLDER_REQUEST)
public class CreateFolderRequest {

    /**
     * @zm-api-field-description New folder specification
     */
    @XmlElement(name=MailConstants.E_FOLDER, required=true)
    private final NewFolderSpec folder;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateFolderRequest() {
        this((NewFolderSpec) null);
    }

    public CreateFolderRequest(NewFolderSpec folder) {
        this.folder = folder;
    }

    public NewFolderSpec getFolder() { return folder; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("folder", folder)
            .toString();
    }
}
