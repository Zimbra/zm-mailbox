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
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Search Auto Prov Directory
 * <br />
 * Only one of <b>&lt;name></b> or <b>&lt;query></b> can be provided.  If neither is provided, the configured search
 * filter for auto provision will be used.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_AUTO_PROV_DIRECTORY_REQUEST)
public class SearchAutoProvDirectoryRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-tag key-attr
     * @zm-api-field-description Name of attribute for the key.  Value of the key attribute will appear in the 
     * <b>&lt;key></b> element in the response.  It is recommended to pick a key attribute that is single-valued and
     * can unique identify an entry in the external auto provision directory.  If the key attribute contains multiple
     * values then multiple <b>&lt;key></b> elements will appear in the response. 
     * <br />
     * Entries are returned in ascending key order.
     */
    @XmlAttribute(name=AdminConstants.A_KEYATTR /* keyAttr */, required=true)
    private String keyAttr;

    // Only one of query and name allowed
    /**
     * @zm-api-field-description Query string - should be an LDAP-style filter string (RFC 2254)
     */
    @XmlAttribute(name=AdminConstants.E_QUERY /* query */, required=false)
    private String query;

    /**
     * @zm-api-field-description Name to fill the auto provisioning search template configured on the domain 
     */
    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag max-results
     * @zm-api-field-description Maximum results that the backend will attempt to fetch from the directory before
     * returning an account.TOO_MANY_SEARCH_RESULTS error.
     */
    @XmlAttribute(name=AdminConstants.A_MAX_RESULTS /* maxResults */, required=false)
    private Integer maxResults;

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description The number of accounts to return per page (0 is default and means all)
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * @zm-api-field-tag offset
     * @zm-api-field-description The starting offset (0, 25, etc)
     */
    @XmlAttribute(name=AdminConstants.A_OFFSET /* offset */, required=false)
    private Integer offset;

    /**
     * @zm-api-field-description Refresh - whether to always re-search in LDAP even when 
     * cached entries are available.  <b>0 (false)</b> is the default.
     */
    @XmlAttribute(name=AdminConstants.A_REFRESH, required=false)
    private ZmBoolean refresh;
    
    /**
     * @zm-api-field-description Domain selector for the domain name to limit the search to (do not use if searching
     * for domains)
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=true)
    private DomainSelector domain;

    public SearchAutoProvDirectoryRequest() {
    }

    private SearchAutoProvDirectoryRequest(String keyAttr, DomainSelector domain) {
        setKeyAttr(keyAttr);
        setDomain(domain);
    }

    public static SearchAutoProvDirectoryRequest createForKeyAttrAndDomain(String keyAttr, DomainSelector domain) {
        return new SearchAutoProvDirectoryRequest(keyAttr, domain);
    }

    public void setKeyAttr(String keyAttr) { this.keyAttr = keyAttr; }
    public void setQuery(String query) { this.query = query; }
    public void setName(String name) { this.name = name; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setRefresh(Boolean refresh) { this.refresh = ZmBoolean.fromBool(refresh);}
    public void setDomain(DomainSelector domain) { this.domain = domain; }
    public String getKeyAttr() { return keyAttr; }
    public String getQuery() { return query; }
    public String getName() { return name; }
    public Integer getMaxResults() { return maxResults; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public Boolean isRefresh() { return ZmBoolean.toBool(refresh); }
    public DomainSelector getDomain() { return domain; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("keyAttr", keyAttr)
            .add("query", query)
            .add("name", name)
            .add("maxResults", maxResults)
            .add("limit", limit)
            .add("offset", offset)
            .add("refresh", refresh)
            .add("domain", domain);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
