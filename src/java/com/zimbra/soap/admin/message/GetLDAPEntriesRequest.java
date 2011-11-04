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
import com.zimbra.common.soap.LDAPUtilsConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=LDAPUtilsConstants.E_GET_LDAP_ENTRIES_REQUEST)
public class GetLDAPEntriesRequest {

    @XmlElement(name=LDAPUtilsConstants.E_LDAPSEARCHBASE /* ldapSearchBase */, required=true)
    private final String ldapSearchBase;

    @XmlAttribute(name=AdminConstants.A_SORT_BY /* sortBy */, required=false)
    private String sortBy;

    @XmlAttribute(name=AdminConstants.A_SORT_ASCENDING /* sortAscending */, required=false)
    private ZmBoolean sortAscending;

    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    @XmlAttribute(name=AdminConstants.A_OFFSET /* offset */, required=false)
    private Integer offset;

    @XmlAttribute(name=AdminConstants.E_QUERY /* query */, required=true)
    private String query;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetLDAPEntriesRequest() {
        this((String) null);
    }

    public GetLDAPEntriesRequest(String ldapSearchBase) {
        this.ldapSearchBase = ldapSearchBase;
    }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setSortAscending(Boolean sortAscending) {
        this.sortAscending = ZmBoolean.fromBool(sortAscending);
    }
    public void setLimit(Integer limit) { this.limit = limit; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setQuery(String query) { this.query = query; }

    public String getLdapSearchBase() { return ldapSearchBase; }
    public String getSortBy() { return sortBy; }
    public Boolean getSortAscending() { return ZmBoolean.toBool(sortAscending); }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public String getQuery() { return query; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("ldapSearchBase", ldapSearchBase)
            .add("sortBy", sortBy)
            .add("sortAscending", sortAscending)
            .add("limit", limit)
            .add("offset", offset)
            .add("query", query);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
