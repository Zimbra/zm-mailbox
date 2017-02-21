/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
