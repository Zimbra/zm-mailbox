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
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=HsmConstants.E_GET_HSM_STATUS_RESPONSE)
public class GetHsmStatusResponse {

    @XmlAttribute(name=HsmConstants.A_RUNNING /* running */, required=true)
    private final ZmBoolean running;

    @XmlAttribute(name=HsmConstants.A_START_DATE /* startDate */, required=false)
    private Long startDate;

    @XmlAttribute(name=HsmConstants.A_END_DATE /* endDate */, required=false)
    private Long endDate;

    @XmlAttribute(name=HsmConstants.A_WAS_ABORTED /* wasAborted */, required=false)
    private ZmBoolean wasAborted;

    @XmlAttribute(name=HsmConstants.A_ABORTING /* aborting */, required=false)
    private ZmBoolean aborting;

    @XmlAttribute(name=HsmConstants.A_ERROR /* error */, required=false)
    private String error;

    @XmlAttribute(name=HsmConstants.A_NUM_BLOBS_MOVED /* numBlobsMoved */, required=false)
    private Integer numBlobsMoved;

    @XmlAttribute(name=HsmConstants.A_NUM_BYTES_MOVED /* numBytesMoved */, required=false)
    private Long numBytesMoved;

    @XmlAttribute(name=HsmConstants.A_NUM_MAILBOXES /* numMailboxes */, required=false)
    private Integer numMailboxes;

    @XmlAttribute(name=HsmConstants.A_TOTAL_MAILBOXES /* totalMailboxes */, required=false)
    private Integer totalMailboxes;

    @XmlAttribute(name=HsmConstants.A_DEST_VOLUME_ID /* destVolumeId */, required=false)
    private Short destVolumeId;

    @XmlAttribute(name=HsmConstants.A_QUERY /* query */, required=false)
    private String query;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetHsmStatusResponse() {
        this(false);
    }

    public GetHsmStatusResponse(boolean running) {
        this.running = ZmBoolean.fromBool(running);
    }

    public void setStartDate(Long startDate) { this.startDate = startDate; }
    public void setEndDate(Long endDate) { this.endDate = endDate; }
    public void setWasAborted(Boolean wasAborted) { this.wasAborted = ZmBoolean.fromBool(wasAborted); }
    public void setAborting(Boolean aborting) { this.aborting = ZmBoolean.fromBool(aborting); }
    public void setError(String error) { this.error = error; }
    public void setNumBlobsMoved(Integer numBlobsMoved) {
        this.numBlobsMoved = numBlobsMoved;
    }
    public void setNumBytesMoved(Long numBytesMoved) {
        this.numBytesMoved = numBytesMoved;
    }
    public void setNumMailboxes(Integer numMailboxes) {
        this.numMailboxes = numMailboxes;
    }
    public void setTotalMailboxes(Integer totalMailboxes) {
        this.totalMailboxes = totalMailboxes;
    }
    public void setDestVolumeId(Short destVolumeId) {
        this.destVolumeId = destVolumeId;
    }
    public void setQuery(String query) { this.query = query; }

    public boolean getRunning() { return ZmBoolean.toBool(running); }
    public Long getStartDate() { return startDate; }
    public Long getEndDate() { return endDate; }
    public Boolean getWasAborted() { return ZmBoolean.toBool(wasAborted); }
    public Boolean getAborting() { return ZmBoolean.toBool(aborting); }
    public String getError() { return error; }
    public Integer getNumBlobsMoved() { return numBlobsMoved; }
    public Long getNumBytesMoved() { return numBytesMoved; }
    public Integer getNumMailboxes() { return numMailboxes; }
    public Integer getTotalMailboxes() { return totalMailboxes; }
    public Short getDestVolumeId() { return destVolumeId; }
    public String getQuery() { return query; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("running", running)
            .add("startDate", startDate)
            .add("endDate", endDate)
            .add("wasAborted", wasAborted)
            .add("aborting", aborting)
            .add("error", error)
            .add("numBlobsMoved", numBlobsMoved)
            .add("numBytesMoved", numBytesMoved)
            .add("numMailboxes", numMailboxes)
            .add("totalMailboxes", totalMailboxes)
            .add("destVolumeId", destVolumeId)
            .add("query", query);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
