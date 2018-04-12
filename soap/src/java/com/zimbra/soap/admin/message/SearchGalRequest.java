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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.GalSearchType;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Search Global Address Book (GAL)
 * <br />
 * Notes: admin verison of mail equiv. Used for testing via zmprov.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_GAL_REQUEST)
public class SearchGalRequest {

    /**
     * @zm-api-field-description Domain name
     */
    @XmlAttribute(name=AdminConstants.A_DOMAIN /* domain */, required=true)
    private String domain;

    /**
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-description The maximum number of entries to return (0 is default and means all)
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * @zm-api-field-tag type-of-addresses-to-search
     * @zm-api-field-description Type of addresses to search.
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=false)
    private GalSearchType type;

    /**
     * @zm-api-field-tag gal-account-id
     * @zm-api-field-description GAL account ID
     */
    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID /* galAcctId */, required=false)
    private String galAccountId;

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
    public String getDomain() { return domain; }
    public String getName() { return name; }
    public Integer getLimit() { return limit; }
    public GalSearchType getType() { return type; }
    public String getGalAccountId() { return galAccountId; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("domain", domain)
            .add("name", name)
            .add("limit", limit)
            .add("type", type)
            .add("galAccountId", galAccountId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
