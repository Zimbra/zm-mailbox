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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.HsmConstants;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Moves blobs between volumes.  Unlike <b>HsmRequest</b>, this request is synchronous,
 * and reads parameters from the request attributes instead of <b>zimbraHsmPolicy</b>.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=HsmConstants.E_MOVE_BLOBS_REQUEST)
public class MoveBlobsRequest {

    // all or comma separated list of MailItem.Type (gets lowercased)
    /**
     * @zm-api-field-tag types
     * @zm-api-field-description A comma-separated list of item types, or "all" for all types.  See the spec for
     * <b>&lt;SearchRequest></b> for details.
     */
    @XmlAttribute(name=AdminConstants.A_TYPES /* types */, required=true)
    private String types;

    /**
     * @zm-api-field-tag volume-ids
     * @zm-api-field-description A comma separated list of source volume IDs
     */
    @XmlAttribute(name=HsmConstants.A_SOURCE_VOLUME_IDS /* sourceVolumeIds */, required=true)
    private String sourceVolumeIds;

    /**
     * @zm-api-field-tag dest-volume-id
     * @zm-api-field-description Destination volume ID
     */
    @XmlAttribute(name=HsmConstants.A_DEST_VOLUME_ID /* destVolumeId */, required=true)
    private Short destVolumeId;

    /**
     * @zm-api-field-description Limit for the total number of bytes of data to move.  Blob move will abort if this
     * threshold is exceeded.
     */
    @XmlAttribute(name=HsmConstants.A_MAX_BYTES /* maxBytes */, required=false)
    private Long maxBytes;

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description Query - if specified, only items that match this query will be moved
     */
    @XmlElement(name=AdminConstants.E_QUERY /* query */, required=false)
    private String query;

    public MoveBlobsRequest() {
    }

    public void setTypes(String types) { this.types = types; }
    public void setSourceVolumeIds(String sourceVolumeIds) {
        this.sourceVolumeIds = sourceVolumeIds;
    }
    public void setDestVolumeId(Short destVolumeId) {
        this.destVolumeId = destVolumeId;
    }
    public void setMaxBytes(Long maxBytes) { this.maxBytes = maxBytes; }
    public void setQuery(String query) { this.query = query; }

    public String getTypes() { return types; }
    public String getSourceVolumeIds() { return sourceVolumeIds; }
    public Short getDestVolumeId() { return destVolumeId; }
    public Long getMaxBytes() { return maxBytes; }
    public String getQuery() { return query; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("types", types)
            .add("sourceVolumeIds", sourceVolumeIds)
            .add("destVolumeId", destVolumeId)
            .add("maxBytes", maxBytes)
            .add("query", query);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
