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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class IncorrectBlobRevisionInfo {

    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final int id;

    @XmlAttribute(name=AdminConstants.A_REVISION, required=true)
    private final int revision;

    @XmlAttribute(name=AdminConstants.A_SIZE, required=true)
    private final long size;

    @XmlAttribute(name=AdminConstants.A_VOLUME_ID, required=true)
    private final short volumeId;

    @XmlElement(name=AdminConstants.E_BLOB, required=true)
    private final BlobRevisionInfo blob;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private IncorrectBlobRevisionInfo() {
        this(-1, -1, -1L, (short)-1, (BlobRevisionInfo) null);
    }

    public IncorrectBlobRevisionInfo(int id, int revision, long size,
                    short volumeId, BlobRevisionInfo blob) {
        this.id = id;
        this.revision = revision;
        this.size = size;
        this.volumeId = volumeId;
        this.blob = blob;
    }

    public int getId() { return id; }
    public int getRevision() { return revision; }
    public long getSize() { return size; }
    public short getVolumeId() { return volumeId; }
    public BlobRevisionInfo getBlob() { return blob; }
}
