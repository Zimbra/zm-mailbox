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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.admin.type.EntrySearchFilterInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_CALENDAR_RESOURCES_REQUEST)
public class SearchCalendarResourcesRequest extends AttributeSelectorImpl {

    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;
    @XmlAttribute(name=AdminConstants.A_OFFSET /* offset */, required=false)
    private Integer offset;
    @XmlAttribute(name=AdminConstants.A_DOMAIN /* domain */, required=false)
    private String domain;
    @XmlAttribute(name=AdminConstants.A_APPLY_COS /* applyCos */, required=false)
    private ZmBoolean applyCos;
    @XmlAttribute(name=AdminConstants.A_SORT_BY /* sortBy */, required=false)
    private String sortBy;
    @XmlAttribute(name=AdminConstants.A_SORT_ASCENDING /* sortAscending */, required=false)
    private ZmBoolean sortAscending;
    @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER /* searchFilter */, required=false)
    private EntrySearchFilterInfo searchFilter;

    private SearchCalendarResourcesRequest() {
    }

    public void setLimit(Integer limit) { this.limit = limit; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setApplyCos(Boolean applyCos) { this.applyCos = ZmBoolean.fromBool(applyCos); }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setSortAscending(Boolean sortAscending) { this.sortAscending = ZmBoolean.fromBool(sortAscending); }
    public void setSearchFilter(EntrySearchFilterInfo searchFilter) { this.searchFilter = searchFilter; }

    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public String getDomain() { return domain; }
    public Boolean getApplyCos() { return ZmBoolean.toBool(applyCos); }
    public String getSortBy() { return sortBy; }
    public Boolean getSortAscending() { return ZmBoolean.toBool(sortAscending); }
    public EntrySearchFilterInfo getSearchFilter() { return searchFilter; }
}
