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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
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
        return MoreObjects.toStringHelper(this)
            .add("folder", folder)
            .toString();
    }
}
