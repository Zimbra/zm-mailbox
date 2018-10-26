/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.EntrySearchFilterInfo;
import com.zimbra.soap.type.GalSearchType;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description get address list info response
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ADDRESS_LIST_INFO_RESPONSE)
public class GetAddressListInfoResponse {

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description zimbra id of the address list
     */
    @XmlAttribute(name=AdminConstants.E_ID /* id */, required=true)
    private String id;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description name of the address list
     */
    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag desc
     * @zm-api-field-description description of the address list
     */
    @XmlElement(name=AdminConstants.E_DESC /* desc */, required=false)
    private String desc;

    /**
     * @zm-api-field-tag domain
     * @zm-api-field-description domain of the address list
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, /* domain */ required=false)
    private DomainSelector domain;

    /**
     * @zm-api-field-tag searchFilter
     * @zm-api-field-description search filter for conditions
     */
    @XmlElement(name=AdminConstants.E_SEARCH_FILTER /* searchFilter */, required=false)
    private EntrySearchFilterInfo searchFilter;

    /**
     * @zm-api-field-tag type
     * @zm-api-field-description gal search type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private GalSearchType type;

    /**
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * @param desc the desc to set
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * @return the domain
     */
    public DomainSelector getDomain() {
        return domain;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    /**
     * @return the searchFilter
     */
    public EntrySearchFilterInfo getSearchFilter() {
        return searchFilter;
    }

    /**
     * @param searchFilter the searchFilter to set
     */
    public void setSearchFilter(EntrySearchFilterInfo searchFilter) {
        this.searchFilter = searchFilter;
    }

    /**
     * @return the type
     */
    public GalSearchType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(GalSearchType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "GetAddressListInfoResponse [type=" + type + ", id=" + id + ", name=" + name
            + ", desc=" + desc + ", searchFilter=" + searchFilter + "]";
    }

}
