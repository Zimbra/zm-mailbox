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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
import com.zimbra.soap.mail.type.NewSearchFolderSpec;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Create a search folder
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_SEARCH_FOLDER_REQUEST)
public class CreateSearchFolderRequest {

    /**
     * @zm-api-field-description New Search Folder specification
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_SEARCH /* search */, required=true)
    private final NewSearchFolderSpec searchFolder;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateSearchFolderRequest() {
        this((NewSearchFolderSpec) null);
    }

    public CreateSearchFolderRequest(NewSearchFolderSpec searchFolder) {
        this.searchFolder = searchFolder;
    }

    public NewSearchFolderSpec getSearchFolder() { return searchFolder; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("searchFolder", searchFolder)
            .toString();
    }
}
