/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.VolumeIdAndProgress;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DEDUPE_BLOBS_RESPONSE)
public class DedupeBlobsResponse {

    @XmlEnum
    public static enum DedupStatus {
        running,
        stopped
    }

    /**
     * @zm-api-field-description Status - one of <b>started|running|idle|stopped</b>
     */
    @XmlAttribute(name=AdminConstants.A_STATUS, required=false)
    private DedupStatus status;
    
    @XmlAttribute(name=AdminConstants.A_TOTAL_SIZE, required=false)
    private Long totalSize;
   
    @XmlAttribute(name=AdminConstants.A_TOTAL_COUNT, required=false)
    private Integer totalCount;

    @XmlElement(name=AdminConstants.E_VOLUME_BLOBS_PROGRESS, required=false)
    private VolumeIdAndProgress[] volumeBlobsProgress;
    
    @XmlElement(name=AdminConstants.E_BLOB_DIGESTS_PROGRESS , required=false)
    private VolumeIdAndProgress[] blobDigestsProgress;

    public DedupeBlobsResponse() {
    }
    
    public void setStatus(DedupStatus status) {
        this.status = status;
    }
   
    public void setTotalCount(int count) {
        this.totalCount = count;
    }
    
    public void setTotalSize(long size) {
        this.totalSize = size;
    }
    
    public void setVolumeBlobsProgress(VolumeIdAndProgress[] progress) {
        this.volumeBlobsProgress = progress;
    }

    public void setBlobDigestsProgress(VolumeIdAndProgress[] progress) {
        this.blobDigestsProgress = progress;
    }
    
    public DedupStatus getStatus() {
        return status;
    }
    
    public Integer getTotalCount() {
        return totalCount;
    }
    
    public Long getTotalSize() {
        return totalSize;
    }
    
    public VolumeIdAndProgress[] getVolumeBlobsProgress() {
        return volumeBlobsProgress;
    }

    public VolumeIdAndProgress[] getBlobDigestsProgress() {
        return blobDigestsProgress;
    }
}
