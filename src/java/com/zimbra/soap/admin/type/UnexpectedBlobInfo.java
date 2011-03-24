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
public class UnexpectedBlobInfo {

    @XmlAttribute(name=AdminConstants.A_VOLUME_ID, required=true)
    private final short volumeId;

    @XmlAttribute(name=AdminConstants.A_PATH, required=true)
    private final String path;

    @XmlAttribute(name=AdminConstants.A_FILE_SIZE, required=true)
    private final long fileSize;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private UnexpectedBlobInfo() {
        this((short)-1, (String) null, -1L);
    }

    public UnexpectedBlobInfo(short volumeId, String path, long fileSize) {
        this.volumeId = volumeId;
        this.path = path;
        this.fileSize = fileSize;
    }

    public short getVolumeId() { return volumeId; }
    public String getPath() { return path; }
    public long getFileSize() { return fileSize; }
}
