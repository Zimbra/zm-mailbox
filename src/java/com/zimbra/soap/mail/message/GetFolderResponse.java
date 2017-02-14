/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.Folder;
import com.zimbra.soap.mail.type.Mountpoint;
import com.zimbra.soap.mail.type.SearchFolder;

/*
<GetFolderResponse>
  <folder ...>
    <folder .../>
    <folder ...>
      <folder .../>
    </folder>
    <folder .../>
    [<link .../>]
    [<search .../>]
  </folder>
</GetFolderResponse>
 */
@XmlRootElement(name=MailConstants.E_GET_FOLDER_RESPONSE)
@XmlType(propOrder = {MailConstants.E_FOLDER})
public class GetFolderResponse {

    /**
     * @zm-api-field-description Folder information
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_FOLDER /* folder */, type=Folder.class),
        @XmlElement(name=MailConstants.E_MOUNT /* link */, type=Mountpoint.class),
        @XmlElement(name=MailConstants.E_SEARCH /* search */, type=SearchFolder.class)
    })
    private Folder folder;

    public GetFolderResponse() {
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }
}
