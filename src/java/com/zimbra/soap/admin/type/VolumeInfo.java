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
public class VolumeInfo {

    @XmlAttribute(name=AdminConstants.A_ID, required=false)
    Short id;
    @XmlAttribute(name=AdminConstants.A_VOLUME_NAME, required=false)
    String name;
    @XmlAttribute(name=AdminConstants.A_VOLUME_ROOTPATH, required=false)
    String rootPath;
    @XmlAttribute(name=AdminConstants.A_VOLUME_TYPE, required=false)
    Short volType;
    @XmlAttribute(name=AdminConstants.A_VOLUME_COMPRESS_BLOBS, required=false)
    Boolean compressBlobs;
    @XmlAttribute(name=AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD, required=false)
    Long compressionThreshold;
    @XmlAttribute(name=AdminConstants.A_VOLUME_MGBITS, required=false)
    Short mgbits;
    @XmlAttribute(name=AdminConstants.A_VOLUME_MBITS, required=false)
    Short mbits;
    @XmlAttribute(name=AdminConstants.A_VOLUME_FGBITS, required=false)
    Short fgbits;
    @XmlAttribute(name=AdminConstants.A_VOLUME_FBITS, required=false)
    Short fbits;
    @XmlAttribute(name=AdminConstants.A_VOLUME_IS_CURRENT, required=false)
    Boolean volumeIsCurrent;

    public VolumeInfo() {
        this((String)null, (String)null, (Short)null, (Boolean)null, (Long)null);
    }

    public VolumeInfo(String name, String rootPath, Short volType,
            Boolean compressBlobs, Long compressionThreshold) {
        this((Short)null, name, rootPath, volType, compressBlobs,
                compressionThreshold, (Short)null, (Short)null,
                (Short)null, (Short)null, (Boolean)null);
    }
    public VolumeInfo(Short id, String name, String rootPath, Short volType,
            Boolean compressBlobs, Long compressionThreshold,
            Short mgbits, Short mbits, Short fgbits, Short fbits,
            Boolean volumeIsCurrent) {
        this.id = id;
        this.name = name;
        this.rootPath = rootPath;
        this.volType = volType;
        this.compressBlobs = compressBlobs;
        this.compressionThreshold = compressionThreshold;
        this.mgbits = mgbits;
        this.mbits = mbits;
        this.fgbits = fgbits;
        this.fbits = fbits;
        this.volumeIsCurrent = volumeIsCurrent;
    }

    public void setId(Short id) { this.id = id; }
    public short getId() { return id; }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setRootPath(String rootPath) { this.rootPath = rootPath; }
    public String getRootPath() { return rootPath; }

    public void setVolType(short volType) { this.volType = volType; }
    public short getVolType() { return volType; }

    public void setCompressBlobs(Boolean compressBlobs) {
        this.compressBlobs = compressBlobs;
    }
    public Boolean getCompressBlobs() { return compressBlobs; }

    public void setCompressionThreshold(Long compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }
    public Long getCompressionThreshold() { return compressionThreshold; }

    public void setMgbits(Short mgbits) { this.mgbits = mgbits; }
    public Short getMgbits() { return mgbits; }

    public void setMbits(Short mbits) { this.mbits = mbits; }
    public Short getMbits() { return mbits; }

    public void setFgbits(Short fgbits) { this.fgbits = fgbits; }
    public Short getFgbits() { return fgbits; }

    public void setFbits(Short fbits) { this.fbits = fbits; }
    public Short getFbits() { return fbits; }

    public void setVolumeIsCurrent(Boolean volumeIsCurrent) {
        this.volumeIsCurrent = volumeIsCurrent;
    }
    public Boolean getVolumeIsCurrent() { return volumeIsCurrent; }
}
