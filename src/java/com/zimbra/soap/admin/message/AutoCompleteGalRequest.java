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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.GalSearchType;

/**
 * @zm-api-command-description Perform an autocomplete for a name against the Global Address List
 * <p>
 * Notes: admin verison of mail equiv. Used for testing via zmprov.
 * </p>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_AUTO_COMPLETE_GAL_REQUEST)
public class AutoCompleteGalRequest {

    /**
     * @zm-api-field-description domain
     */
    @XmlAttribute(name=AdminConstants.A_DOMAIN /* domain */, required=true)
    private String domain;

    /**
     * @zm-api-field-description The name to test for autocompletion
     */
    @XmlAttribute(name=AccountConstants.E_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-description Type of addresses to auto-complete on
     * <ul>
     * <li>     "account" for regular user accounts, aliases and distribution lists
     * <li>     "resource" for calendar resources
     * <li>     "group" for groups
     * <li>     "all" for combination of types
     * </ul>
     * if omitted, defaults to "accounts"
     */
    @XmlAttribute(name=AccountConstants.A_TYPE /* type */, required=false)
    private GalSearchType type;

    // TODO: is this appropriate for AutoCompleteGal?
    /**
     * @zm-api-field-tag gal-account-id
     * @zm-api-field-description GAL Account ID
     */
    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID /* galAcctId */, required=false)
    private String galAccountId;

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description An integer specifying the maximum number of results to return
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * no-argument constructor wanted by JAXB
     */
    private AutoCompleteGalRequest() {
        this((String) null, (String) null);
    }

    private AutoCompleteGalRequest(String domain, String name) {
        this.setDomain(domain);
        this.name = name;
    }

    public AutoCompleteGalRequest createForDomainAndName(String domain, String name) {
        return new AutoCompleteGalRequest(domain, name);
    }

    public void setDomain(String domain) { this.domain = domain; }
    public void setName(String name) {this.name = name; }
    public void setType(GalSearchType type) { this.type = type; }
    public void setGalAccountId(String galAccountId) { this.galAccountId = galAccountId; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public String getDomain() { return domain; }
    public String getName() { return name; }
    public GalSearchType getType() { return type; }
    public String getGalAccountId() { return galAccountId; }
    public Integer getLimit() { return limit; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("domain", domain)
            .add("name", name)
            .add("type", type)
            .add("galAccountId", galAccountId)
            .add("limit", limit);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
