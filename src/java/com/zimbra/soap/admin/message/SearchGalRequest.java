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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.GalSearchType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_GAL_REQUEST)
public class SearchGalRequest {

    @XmlAttribute(name=AdminConstants.A_DOMAIN /* domain */, required=true)
    private String domain;

    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=false)
    private String name;

    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=false)
    private GalSearchType type;

    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID /* galAcctId */, required=false)
    private String galAccountId;

    @XmlAttribute(name=AdminConstants.A_TOKEN /* token */, required=false)
    private String token;

    public SearchGalRequest() {
    }

    private SearchGalRequest(String domain) {
        setDomain(domain);
    }

    public static SearchGalRequest createForDomain(String domain) {
        return new SearchGalRequest(domain);
    }

    public void setDomain(String domain) { this.domain = domain; }
    public void setName(String name) { this.name = name; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public void setType(GalSearchType type) { this.type = type; }
    public void setGalAccountId(String galAccountId) { this.galAccountId = galAccountId; }
    public void setToken(String token) { this.token = token; }
    public String getDomain() { return domain; }
    public String getName() { return name; }
    public Integer getLimit() { return limit; }
    public GalSearchType getType() { return type; }
    public String getGalAccountId() { return galAccountId; }
    public String getToken() { return token; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("domain", domain)
            .add("name", name)
            .add("limit", limit)
            .add("type", type)
            .add("galAccountId", galAccountId)
            .add("token", token);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
