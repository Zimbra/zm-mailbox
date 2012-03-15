/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

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
