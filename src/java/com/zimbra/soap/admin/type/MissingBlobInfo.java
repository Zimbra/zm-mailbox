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

@XmlAccessorType(XmlAccessType.NONE)
public class MissingBlobInfo {

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=true)
    private final int id;

    /**
     * @zm-api-field-tag revision
     * @zm-api-field-description revision
     */
    @XmlAttribute(name=AdminConstants.A_REVISION /* rev */, required=true)
    private final int revision;

    /**
     * @zm-api-field-tag data-size
     * @zm-api-field-description Data size
     */
    @XmlAttribute(name=AdminConstants.A_SIZE /* s */, required=true)
    private final long size;

    /**
     * @zm-api-field-tag volume-id
     * @zm-api-field-description Volume ID
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_ID /* volumeId */, required=true)
    private final short volumeId;

    /**
     * @zm-api-field-tag blob-path
     * @zm-api-field-description Blob path
     */
    @XmlAttribute(name=AdminConstants.A_BLOB_PATH /* blobPath */, required=true)
    private final String blobPath;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MissingBlobInfo() {
        this(-1, -1, -1L, (short)-1, (String) null);
    }

    public MissingBlobInfo(int id, int revision, long size,
                        short volumeId, String blobPath) {
        this.id = id;
        this.revision = revision;
        this.size = size;
        this.volumeId = volumeId;
        this.blobPath = blobPath;
    }

    public int getId() { return id; }
    public int getRevision() { return revision; }
    public long getSize() { return size; }
    public short getVolumeId() { return volumeId; }
    public String getBlobPath() { return blobPath; }
}
