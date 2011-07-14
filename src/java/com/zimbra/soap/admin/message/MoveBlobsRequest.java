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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.HsmConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=HsmConstants.E_MOVE_BLOBS_REQUEST)
public class MoveBlobsRequest {

    // all or comma separated list of MailItem.Type (gets lowercased)
    @XmlAttribute(name=AdminConstants.A_TYPES /* types */, required=true)
    private String types;

    // comma separated list
    @XmlAttribute(name=HsmConstants.A_SOURCE_VOLUME_IDS
                    /* sourceVolumeIds */, required=true)
    private String sourceVolumeIds;

    @XmlAttribute(name=HsmConstants.A_DEST_VOLUME_ID
                    /* destVolumeId */, required=true)
    private Short destVolumeId;

    @XmlAttribute(name=HsmConstants.A_MAX_BYTES /* maxBytes */, required=false)
    private Long maxBytes;

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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("types", types)
            .add("sourceVolumeIds", sourceVolumeIds)
            .add("destVolumeId", destVolumeId)
            .add("maxBytes", maxBytes)
            .add("query", query);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
