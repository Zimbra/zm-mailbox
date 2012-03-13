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
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-deprecation-info Note: <b>SearchAccountsRequest</b> is deprecated. See <b>SearchDirectoryRequest</b>.
 * @zm-api-command-description Search Accounts
 * <br />
 * <b>Access</b>: domain admin sufficient (a domain admin can't specify "domains" as a type)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_ACCOUNTS_REQUEST)
public class SearchAccountsRequest {

    /**
     * @zm-api-field-description Query string - should be an LDAP-style filter string (RFC 2254)
     */
    @XmlAttribute(name=AdminConstants.E_QUERY, required=true)
    private final String query;

    /**
     * @zm-api-field-description The maximum number of accounts to return (0 is default and means all)
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private final Integer limit;

    /**
     * @zm-api-field-description The starting offset (0, 25, etc)
     */
    @XmlAttribute(name=AdminConstants.A_OFFSET, required=false)
    private final Integer offset;

    /**
     * @zm-api-field-tag domain-name
     * @zm-api-field-description The domain name to limit the search to
     */
    @XmlAttribute(name=AdminConstants.A_DOMAIN, required=false)
    private String domain;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description applyCos - Flag whether or not to apply the COS policy to account.
     * Specify <b>0 (false)</b> if only requesting attrs that aren't inherited from COS
     */
    @XmlAttribute(name=AdminConstants.A_APPLY_COS, required=false)
    private ZmBoolean applyCos;

    /**
     * @zm-api-field-tag attrs
     * @zm-api-field-description Comma-seperated list of attrs to return ("displayName", "zimbraId",
     * "zimbraAccountStatus")
     */
    @XmlAttribute(name=AdminConstants.A_ATTRS, required=false)
    private String attrs;

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Name of attribute to sort on. Default is the account name.
     */
    @XmlAttribute(name=AdminConstants.A_SORT_BY, required=false)
    private String sortBy;

    /**
     * @zm-api-field-tag types
     * @zm-api-field-description Comma-separated list of types to return. Legal values are: <b>accounts|resources</b>
     * (default is accounts)
     */
    @XmlAttribute(name=AdminConstants.A_TYPES, required=false)
    private String types;

    /**
     * @zm-api-field-tag sort-ascending
     * @zm-api-field-description Whether to sort in ascending order. Default is <b>1 (true)</b>
     */
    @XmlAttribute(name=AdminConstants.A_SORT_ASCENDING, required=false)
    private ZmBoolean sortAscending;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SearchAccountsRequest() {
        this((String) null, (Integer) null, (Integer) null);
    }

    public SearchAccountsRequest(String query, Integer limit, Integer offset) {
        this.query = query;
        this.limit = limit;
        this.offset = offset;
    }

    public void setDomain(String domain) { this.domain = domain; }
    public void setApplyCos(Boolean applyCos) { this.applyCos = ZmBoolean.fromBool(applyCos); }
    public void setAttrs(String attrs) { this.attrs = attrs; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setTypes(String types) { this.types = types; }
    public void setSortAscending(Boolean sortAscending) { this.sortAscending = ZmBoolean.fromBool(sortAscending); }
    public String getQuery() { return query; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public String getDomain() { return domain; }
    public Boolean getApplyCos() { return ZmBoolean.toBool(applyCos); }
    public String getAttrs() { return attrs; }
    public String getSortBy() { return sortBy; }
    public String getTypes() { return types; }
    public Boolean getSortAscending() { return ZmBoolean.toBool(sortAscending); }
}
