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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.HsmConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=HsmConstants.E_MOVE_BLOBS_RESPONSE)
public class MoveBlobsResponse {

    /**
     * @zm-api-field-tag num-blobs-moved
     * @zm-api-field-description Number of blobs moved
     */
    @XmlAttribute(name=HsmConstants.A_NUM_BLOBS_MOVED /* numBlobsMoved */, required=false)
    private Integer numBlobsMoved;

    /**
     * @zm-api-field-tag num-bytes-moved
     * @zm-api-field-description Number of bytes moved
     */
    @XmlAttribute(name=HsmConstants.A_NUM_BYTES_MOVED /* numBytesMoved */, required=false)
    private Long numBytesMoved;

    /**
     * @zm-api-field-tag total-mailboxes
     * @zm-api-field-description Total number of mailboxes
     */
    @XmlAttribute(name=HsmConstants.A_TOTAL_MAILBOXES /* totalMailboxes */, required=false)
    private Integer totalMailboxes;

    public MoveBlobsResponse() {
    }

    public void setNumBlobsMoved(Integer numBlobsMoved) {
        this.numBlobsMoved = numBlobsMoved;
    }
    public void setNumBytesMoved(Long numBytesMoved) {
        this.numBytesMoved = numBytesMoved;
    }
    public void setTotalMailboxes(Integer totalMailboxes) {
        this.totalMailboxes = totalMailboxes;
    }

    public Integer getNumBlobsMoved() { return numBlobsMoved; }
    public Long getNumBytesMoved() { return numBytesMoved; }
    public Integer getTotalMailboxes() { return totalMailboxes; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("numBlobsMoved", numBlobsMoved)
            .add("numBytesMoved", numBytesMoved)
            .add("totalMailboxes", totalMailboxes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
