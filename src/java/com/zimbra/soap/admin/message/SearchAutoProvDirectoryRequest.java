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
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_AUTO_PROV_DIRECTORY_REQUEST)
public class SearchAutoProvDirectoryRequest extends AttributeSelectorImpl {

    @XmlAttribute(name=AdminConstants.A_KEYATTR /* keyAttr */, required=true)
    private String keyAttr;

    // Only one of query and name allowed
    @XmlAttribute(name=AdminConstants.E_QUERY /* query */, required=false)
    private String query;

    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=false)
    private String name;

    @XmlAttribute(name=AdminConstants.A_MAX_RESULTS /* maxResults */, required=false)
    private Integer maxResults;

    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    @XmlAttribute(name=AdminConstants.A_OFFSET /* offset */, required=false)
    private Integer offset;

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
    public void setDomain(DomainSelector domain) { this.domain = domain; }
    public String getKeyAttr() { return keyAttr; }
    public String getQuery() { return query; }
    public String getName() { return name; }
    public Integer getMaxResults() { return maxResults; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public DomainSelector getDomain() { return domain; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("keyAttr", keyAttr)
            .add("query", query)
            .add("name", name)
            .add("maxResults", maxResults)
            .add("limit", limit)
            .add("offset", offset)
            .add("domain", domain);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
