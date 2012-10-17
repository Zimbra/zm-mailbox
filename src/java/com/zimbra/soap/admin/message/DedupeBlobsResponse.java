/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxBlobConsistency;

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
    
    public DedupStatus getStatus() {
        return status;
    }
    
    public Integer getTotalCount() {
        return totalCount;
    }
    
    public Long getTotalSize() {
        return totalSize;
    }

}
