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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.GetSessionsSortBy;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_SESSIONS_REQUEST)
public class GetSessionsRequest {

    // Valid values are Case insensitive Session.Type
    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final String type;

    @XmlAttribute(name=AdminConstants.A_SORT_BY, required=false)
    private final GetSessionsSortBy sortBy;

    @XmlAttribute(name=AdminConstants.A_OFFSET, required=false)
    private final Long offset;

    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private final Long limit;

    @XmlAttribute(name=AdminConstants.A_REFRESH, required=false)
    private final ZmBoolean refresh;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetSessionsRequest() {
        this((String) null, (GetSessionsSortBy) null,
                (Long) null, (Long) null, (Boolean) null);
    }

    public GetSessionsRequest(String type, GetSessionsSortBy sortBy,
                    Long offset, Long limit, Boolean refresh) {
        this.type = type;
        this.sortBy = sortBy;
        this.offset = offset;
        this.limit = limit;
        this.refresh = ZmBoolean.fromBool(refresh);
    }

    public String getType() { return type; }
    public GetSessionsSortBy getSortBy() { return sortBy; }
    public Long getOffset() { return offset; }
    public Long getLimit() { return limit; }
    public Boolean getRefresh() { return ZmBoolean.toBool(refresh); }
}
