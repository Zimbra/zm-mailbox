/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 VMware, Inc.
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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class BlobSizeInfo {

    /**
     * @zm-api-field-tag path
     * @zm-api-field-description Path
     */
    @XmlAttribute(name=AdminConstants.A_PATH /* path */, required=true)
    private final String path;

    /**
     * @zm-api-field-tag data-size
     * @zm-api-field-description Data size
     */
    @XmlAttribute(name=AdminConstants.A_SIZE /* s */, required=true)
    private final Long size;

    /**
     * @zm-api-field-tag file-size
     * @zm-api-field-description File size
     */
    @XmlAttribute(name=AdminConstants.A_FILE_SIZE /* fileSize */, required=true)
    private final Long fileSize;

    /**
     * @zm-api-field-tag external-flag
     * @zm-api-field-description Set if the blob is stored in an ExternalStoreManager rather than locally in FileBlobStore
     */
    @XmlAttribute(name=AdminConstants.A_EXTERNAL /* external */, required=true)
    private final ZmBoolean external;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BlobSizeInfo() {
        this((String) null, (Long) null, (Long) null, false);
    }

    public BlobSizeInfo(String path, Long size, Long fileSize, boolean external) {
        this.path = path;
        this.size = size;
        this.fileSize = fileSize;
        this.external = ZmBoolean.fromBool(external);
    }

    public String getPath() { return path; }
    public Long getSize() { return size; }
    public Long getFileSize() { return fileSize; }
    public boolean getExternal() { return ZmBoolean.toBool(external); }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("path", path)
            .add("size", size)
            .add("fileSize", fileSize)
            .add("external", external);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
