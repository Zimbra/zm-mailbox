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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.EntrySearchFilterInfo;
import com.zimbra.soap.type.GalSearchType;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description crteate address list request
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_ADDRESS_LIST_REQUEST)
public class CreateAddressListRequest {

    /**
     * @zm-api-field-tag type
     * @zm-api-field-description gal search type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=false)
    private GalSearchType type;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description name of the address list
     */
    @XmlElement(name=AdminConstants.E_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag desc
     * @zm-api-field-description description of the address list
     */
    @XmlElement(name=AdminConstants.E_DESC /* desc */, required=false)
    private String desc;

    /**
     * @zm-api-field-tag searchFilter
     * @zm-api-field-description search filter for conditions
     */
    @XmlElement(name=AdminConstants.E_SEARCH_FILTER /* searchFilter */, required=false)
    private EntrySearchFilterInfo searchFilter;

    /**
     * @zm-api-field-tag domain
     * @zm-api-field-description Domain selector
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private final DomainSelector domain;

    /**
     * default private constructor to block the usage
     */
    @SuppressWarnings("unused")
    private CreateAddressListRequest() {
        this(null);
    }

    /**
     * @param name
     */
    public CreateAddressListRequest(String name) {
        this(name, null, null, null, null);
    }

    /**
     * @param name
     * @param desc
     * @param type
     * @param searchFilter
     * @param domain
     */
    public CreateAddressListRequest(String name, String desc, GalSearchType type, EntrySearchFilterInfo searchFilter, DomainSelector domain) {
        this.name = name;
        this.desc = desc;
        this.type = type != null ? type : GalSearchType.all; // set gal search type to "all" if null
        this.searchFilter = searchFilter;
        this.domain = domain;
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
     * @return the domain
     */
    public DomainSelector getDomain() {
        return domain;
    }

    public void validateCreateAddressListRequest() throws ServiceException {
        if (type == null) {
            ZimbraLog.addresslist.debug("Setting gal search type to all.");
            type = GalSearchType.all;
        }
        if (StringUtil.isNullOrEmpty(name)) {
            ZimbraLog.addresslist.debug("Missing name input.");
            throw ServiceException.INVALID_REQUEST("Missing name input", null);
        }
        if (StringUtil.isNullOrEmpty(desc)) {
            desc = "";
        }
        if (searchFilter == null) {
            ZimbraLog.addresslist.debug("searchFilter is empty, so search all the contacts in GAL.");
        }
        if (domain == null) {
            ZimbraLog.addresslist.debug("Missing domain selector, auth account's domain will be used.");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CreateAddressListRequest [type=" + type + ", name=" + name + ", desc=" + desc + ", searchFilter="
                + searchFilter + ", domain=" + domain + "]";
    }
}
