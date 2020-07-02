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
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=HsmConstants.E_GET_HSM_STATUS_RESPONSE)
public class GetHsmStatusResponse {

    /**
     * @zm-api-field-description <b>1 (true)</b> if an HSM session is currently running, <b>0 (false)</b>
     * if the information returned applies to the last completed HSM session
     */
    @XmlAttribute(name=HsmConstants.A_RUNNING /* running */, required=true)
    private final ZmBoolean running;

    /**
     * @zm-api-field-tag start-date-millis
     * @zm-api-field-description the start date of the HSM session in milliseconds
     */
    @XmlAttribute(name=HsmConstants.A_START_DATE /* startDate */, required=false)
    private Long startDate;

    /**
     * @zm-api-field-tag end-date-millis
     * @zm-api-field-description the end date of the HSM session in milliseconds
     */
    @XmlAttribute(name=HsmConstants.A_END_DATE /* endDate */, required=false)
    private Long endDate;

    /**
     * @zm-api-field-tag was-aborted
     * @zm-api-field-description <b>1 (true)</b> if the HSM session was aborted
     */
    @XmlAttribute(name=HsmConstants.A_WAS_ABORTED /* wasAborted */, required=false)
    private ZmBoolean wasAborted;

    /**
     * @zm-api-field-tag abort-in-progress
     * @zm-api-field-description <b>1 (true)</b> if the HSM session is in the process of aborting
     */
    @XmlAttribute(name=HsmConstants.A_ABORTING /* aborting */, required=false)
    private ZmBoolean aborting;

    /**
     * @zm-api-field-tag error-text
     * @zm-api-field-description The error message, if an error occurred while processing the last
     * <b>&lt;HsmRequest></b>
     */
    @XmlAttribute(name=HsmConstants.A_ERROR /* error */, required=false)
    private String error;

    /**
     * @zm-api-field-tag num-blobs-moved
     * @zm-api-field-description The number of blobs that were moved
     */
    @XmlAttribute(name=HsmConstants.A_NUM_BLOBS_MOVED /* numBlobsMoved */, required=false)
    private Integer numBlobsMoved;

    /**
     * @zm-api-field-tag num-bytes-moved
     * @zm-api-field-description The number of bytes that were moved
     */
    @XmlAttribute(name=HsmConstants.A_NUM_BYTES_MOVED /* numBytesMoved */, required=false)
    private Long numBytesMoved;

    /**
     * @zm-api-field-tag num-mailboxes-processed
     * @zm-api-field-description The number of mailboxes that have been processed
     */
    @XmlAttribute(name=HsmConstants.A_NUM_MAILBOXES /* numMailboxes */, required=false)
    private Integer numMailboxes;

    /**
     * @zm-api-field-tag total-mailboxes
     * @zm-api-field-description Total number of mailboxes that should be processed by the HSM session
     */
    @XmlAttribute(name=HsmConstants.A_TOTAL_MAILBOXES /* totalMailboxes */, required=false)
    private Integer totalMailboxes;

    /**
     * @zm-api-field-tag dest-volume-id
     * @zm-api-field-description The ID of the volume to which messages are being moved
     */
    @XmlAttribute(name=HsmConstants.A_DEST_VOLUME_ID /* destVolumeId */, required=false)
    private Short destVolumeId;

    /**
     * @zm-api-field-tag search-query
     * @zm-api-field-description The query that is used to find messages to move
     */
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
