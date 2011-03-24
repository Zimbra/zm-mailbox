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
public class BlobRevisionInfo {

    @XmlAttribute(name=AdminConstants.A_PATH, required=true)
    private final String path;

    @XmlAttribute(name=AdminConstants.A_FILE_SIZE, required=true)
    private final long fileSize;

    @XmlAttribute(name=AdminConstants.A_REVISION, required=true)
    private final int revision;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BlobRevisionInfo() {
        this((String) null, -1L, -1);
    }

    public BlobRevisionInfo(String path, long fileSize, int revision) {
        this.path = path;
        this.fileSize = fileSize;
        this.revision = revision;
    }

    public String getPath() { return path; }
    public long getFileSize() { return fileSize; }
    public int getRevision() { return revision; }
}
