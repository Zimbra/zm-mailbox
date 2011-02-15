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

package com.zimbra.soap.admin.type;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class PublishFolderInfo {

    @XmlAttribute(name=AdminConstants.A_PATH)
    private final String path;
    @XmlAttribute(name=AdminConstants.A_FOLDER)
    private final String folderId;
    @XmlAttribute(name=AdminConstants.A_PATH_OR_ID)
    private final String pathOrId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PublishFolderInfo() {
        this((String) null, (String) null, (String) null);
    }

    public PublishFolderInfo(String path, String folderId,
            String pathOrId) {
        this.path = path;
        this.folderId = folderId;
        this.pathOrId = pathOrId;
    }

    public String getPath() { return path; }
    public String getFolderId() { return folderId; }
    public String getPathOrId() { return pathOrId; }
    
    public static PublishFolderInfo fromPath(String path) {
        return new PublishFolderInfo(path, null, null);
    }

    public static PublishFolderInfo fromFolderId(String folderId) {
        return new PublishFolderInfo(null, folderId, null);
    }

    public static PublishFolderInfo fromPathOrId(String pathOrId) {
        return new PublishFolderInfo(null, null, pathOrId);
    }
}
