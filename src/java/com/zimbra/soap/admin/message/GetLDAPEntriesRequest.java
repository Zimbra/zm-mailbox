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

/**
 * @zm-api-command-description Get LDAP entries
 * <br />
 * GetLDAPEntriesRequest fetches ldap entry (or entries) by a search-base (<b>{ldap-search-base}</b>) and a search
 * query (<b>{query}</b>).
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=LDAPUtilsConstants.E_GET_LDAP_ENTRIES_REQUEST)
public class GetLDAPEntriesRequest {

    /**
     * @zm-api-field-tag ldap-search-base
     * @zm-api-field-description LDAP search base.  An LDAP-style filter string that defines an LDAP search base
     * (RFC 2254)
     */
    @XmlElement(name=LDAPUtilsConstants.E_LDAPSEARCHBASE /* ldapSearchBase */, required=true)
    private final String ldapSearchBase;

    /**
     * @zm-api-field-tag sort-by-attrib
     * @zm-api-field-description Name of attribute to sort on. default is null
     */
    @XmlAttribute(name=AdminConstants.A_SORT_BY /* sortBy */, required=false)
    private String sortBy;

    /**
     * @zm-api-field-tag sort-ascending
     * @zm-api-field-description Flag whether to sort in ascending order <b>1 (true)</b> is default
     */
    @XmlAttribute(name=AdminConstants.A_SORT_ASCENDING /* sortAscending */, required=false)
    private ZmBoolean sortAscending;

    /**
     * @zm-api-field-description Limit - the maximum number of LDAP objects (records) to return (0 is default and
     * means all)
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * @zm-api-field-description The starting offset (0, 25, etc)
     */
    @XmlAttribute(name=AdminConstants.A_OFFSET /* offset */, required=false)
    private Integer offset;

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description Query string. Should be an LDAP-style filter string (RFC 2254)
     */
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
