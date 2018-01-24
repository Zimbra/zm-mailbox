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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.Objects;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class GetFolderSpec {

    /**
     * @zm-api-field-tag base-folder-uuid
     * @zm-api-field-description Base folder UUID
     */
    @XmlAttribute(name=MailConstants.A_UUID /* uuid */, required=false)
    private String uuid;

    /**
     * @zm-api-field-tag base-folder-id
     * @zm-api-field-description Base folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag fully-qualified-path
     * @zm-api-field-description Fully qualified path
     */
    @XmlAttribute(name=MailConstants.A_PATH /* path */, required=false)
    private String path;

    public GetFolderSpec() {
    }

    public static GetFolderSpec forID(String id) {
        GetFolderSpec spec = new GetFolderSpec();
        spec.setFolderId(id);
        return spec;
    }

    public static GetFolderSpec forUUID(String uuid) {
        GetFolderSpec spec = new GetFolderSpec();
        spec.setUuid(uuid);
        return spec;
    }

    public static GetFolderSpec forPath(String path) {
        GetFolderSpec spec = new GetFolderSpec();
        spec.setPath(path);
        return spec;
    }

    public void setUuid(String uuid) { this.uuid = uuid; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setPath(String path) { this.path = path; }
    public String getUuid() { return uuid; }
    public String getFolderId() { return folderId; }
    public String getPath() { return path; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("uuid", uuid)
            .add("folderId", folderId)
            .add("path", path);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
