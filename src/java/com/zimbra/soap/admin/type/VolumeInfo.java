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
public final class VolumeInfo {

    @XmlAttribute(name=AdminConstants.A_ID, required=false)
    private short id = -1;

    @XmlAttribute(name=AdminConstants.A_VOLUME_NAME, required=false)
    private String name;

    @XmlAttribute(name=AdminConstants.A_VOLUME_ROOTPATH, required=false)
    private String rootPath;

    @XmlAttribute(name=AdminConstants.A_VOLUME_TYPE, required=false)
    private short type = -1;

    @XmlAttribute(name=AdminConstants.A_VOLUME_COMPRESS_BLOBS, required=false)
    private Boolean compressBlobs;

    @XmlAttribute(name=AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD, required=false)
    private long compressionThreshold = -1;

    @XmlAttribute(name=AdminConstants.A_VOLUME_MGBITS, required=false)
    private short mgbits;

    @XmlAttribute(name=AdminConstants.A_VOLUME_MBITS, required=false)
    private short mbits;

    @XmlAttribute(name=AdminConstants.A_VOLUME_FGBITS, required=false)
    private short fgbits;

    @XmlAttribute(name=AdminConstants.A_VOLUME_FBITS, required=false)
    private short fbits;

    @XmlAttribute(name=AdminConstants.A_VOLUME_IS_CURRENT, required=false)
    private boolean current;

    public void setId(short value) {
        id = value;
    }

    public short getId() {
        return id;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public void setRootPath(String value) {
        rootPath = value;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setType(short value) {
        type = value;
    }

    public short getType() {
        return type;
    }

    public void setCompressBlobs(Boolean value) {
        compressBlobs = value;
    }

    public boolean isCompressBlobs() {
        return compressBlobs != null ? compressBlobs : false;
    }

    public Boolean getCompressBlobs() {
        return compressBlobs;
    }

    public void setCompressionThreshold(long value) {
        compressionThreshold = value;
    }

    public long getCompressionThreshold() {
        return compressionThreshold;
    }

    public void setMgbits(short value) {
        mgbits = value;
    }

    public short getMgbits() {
        return mgbits;
    }

    public void setMbits(short value) {
        mbits = value;
    }

    public short getMbits() {
        return mbits;
    }

    public void setFgbits(short value) {
        fgbits = value;
    }

    public short getFgbits() {
        return fgbits;
    }

    public void setFbits(short value) {
        fbits = value;
    }

    public short getFbits() {
        return fbits;
    }

    public void setCurrent(boolean value) {
        current = value;
    }

    public boolean isCurrent() {
        return current;
    }
}
