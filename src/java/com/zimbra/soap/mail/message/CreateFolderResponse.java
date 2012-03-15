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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.Folder;
import com.zimbra.soap.mail.type.Mountpoint;
import com.zimbra.soap.mail.type.SearchFolder;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_FOLDER_RESPONSE)
public class CreateFolderResponse {

    /**
     * @zm-api-field-description Information about created folder
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_FOLDER, type=Folder.class),
        @XmlElement(name=MailConstants.E_MOUNT, type=Mountpoint.class),
        @XmlElement(name=MailConstants.E_SEARCH, type=SearchFolder.class)
    })
    private Folder folder;

    public CreateFolderResponse() {
    }

    public void setFolder(Folder folder) { this.folder = folder; }
    public Folder getFolder() { return folder; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("folder", folder)
            .toString();
    }
}
