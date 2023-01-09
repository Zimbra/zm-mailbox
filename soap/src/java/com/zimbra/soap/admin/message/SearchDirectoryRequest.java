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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Search directory
 * <br />
 * <b>Access</b>: domain admin sufficient (though a domain admin can't specify "domains" as a type)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_DIRECTORY_REQUEST)
public class SearchDirectoryRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-description Query string - should be an LDAP-style filter string (RFC 2254)
     */
    @XmlAttribute(name=AdminConstants.E_QUERY, required=false)
    private String query;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description Maximum results that the backend will attempt to fetch from the directory before
     * returning an account.TOO_MANY_SEARCH_RESULTS error.
     */
    @XmlAttribute(name=AdminConstants.A_MAX_RESULTS, required=false)
    private Integer maxResults;

    /**
     * @zm-api-field-description The maximum number of accounts to return (0 is default and means all)
     * and it must be greater than or equal to 0.
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private Integer limit;

    /**
     * @zm-api-field-description The starting offset (0, 25, etc)
     */
    @XmlAttribute(name=AdminConstants.A_OFFSET, required=false)
    private Integer offset;

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
     * @zm-api-field-description whether or not to apply the global config attrs to account. specify <b>0 (false)</b>
     * if only requesting attrs that aren't inherited from global config
     */
    @XmlAttribute(name=AdminConstants.A_APPLY_CONFIG, required=false)
    private ZmBoolean applyConfig;

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Name of attribute to sort on. Default is the account name.
     */
    @XmlAttribute(name=AdminConstants.A_SORT_BY, required=false)
    private String sortBy;

    /**
     * @zm-api-field-tag types
     * @zm-api-field-description Comma-separated list of types to return. Legal values are:
     * <br />
     * <b>accounts|distributionlists|aliases|resources|domains|coses</b>
     * <br />
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
     * @zm-api-field-tag isCountOnly
     * @zm-api-field-description Whether response should be count only. Default is <b>0 (false)</b> 
     */
    @XmlAttribute(name=AdminConstants.A_COUNT_ONLY, required=false)
    private ZmBoolean isCountOnly;

    public SearchDirectoryRequest() {
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setApplyCos(Boolean applyCos) {
        this.applyCos = ZmBoolean.fromBool(applyCos);
    }

    public void setApplyConfig(Boolean applyConfig) {
        this.applyConfig = ZmBoolean.fromBool(applyConfig);
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public void setSortAscending(Boolean sortAscending) {
        this.sortAscending = ZmBoolean.fromBool(sortAscending);
    }

    public void setCountOnly(Boolean countOnly) {
        this.isCountOnly = ZmBoolean.fromBool(countOnly);
    }

    public String getQuery() { return query; }
    public Integer getMaxResults() { return maxResults; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public String getDomain() { return domain; }
    public Boolean getApplyCos() { return ZmBoolean.toBool(applyCos); }
    public Boolean getApplyConfig() { return ZmBoolean.toBool(applyConfig); }
    public String getSortBy() { return sortBy; }
    public String getTypes() { return types; }
    public Boolean getSortAscending() { return ZmBoolean.toBool(sortAscending); }
    public Boolean getCountOnly() {return ZmBoolean.toBool(isCountOnly); }
}
