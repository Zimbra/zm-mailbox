/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("numBlobsMoved", numBlobsMoved)
            .add("numBytesMoved", numBytesMoved)
            .add("totalMailboxes", totalMailboxes);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
