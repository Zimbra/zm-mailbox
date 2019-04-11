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
import com.zimbra.soap.admin.type.EntrySearchFilterInfo;
import com.zimbra.soap.type.GalSearchType;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description modify address list request
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MODIFY_ADDRESS_LIST_REQUEST)
public class ModifyAddressListRequest {

    /**
     * @zm-api-field-tag type
     * @zm-api-field-description gal search type - defaults to `all` if unset
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=false)
    private GalSearchType type;

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description zimbra id of the address list to modify
     */
    @XmlAttribute(name=AdminConstants.E_ID /* id */, required=true)
    private String id;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description new name of the address list
     */
    @XmlElement(name=AdminConstants.E_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag desc
     * @zm-api-field-description new description of the address list
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
     * @zm-api-field-tag clearFilter
     * @zm-api-field-description remove all filter conditions if no searchFilter is specified
     */
    @XmlAttribute(name=AdminConstants.A_CLEAR_FILTER /* clearFilter */, required=false)
    private ZmBoolean clearFilter;

    /**
     * default private constructor to block the usage
     */
    @SuppressWarnings("unused")
    private ModifyAddressListRequest() {
        this(null);
    }

    /**
     * @param name
     */
    public ModifyAddressListRequest(String id) {
        this(id, null, null, null, null);
    }

    /**
     * @param id
     * @param name
     * @param desc
     * @param type
     * @param searchFilter
     */
    public ModifyAddressListRequest(String id, String name, String desc, GalSearchType type,
        EntrySearchFilterInfo searchFilter) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.type = type != null ? type : GalSearchType.all; // set gal search type to "all" if null
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
     * @return the clearFilter
     */
    public Boolean getClearFilter() {
        return ZmBoolean.toBool(clearFilter);
    }

    /**
     * @param clearFilter clearFilter to set
     */
    public void setClearFilter(Boolean clearFilter) {
        this.clearFilter = ZmBoolean.fromBool(clearFilter);
    }

    public void validateModifyAddressListRequest() throws ServiceException {
        if (StringUtil.isNullOrEmpty(id)) {
            throw ServiceException
                .INVALID_REQUEST("Modify address list requires an id", null);
        }
        if (type == null && name == null && desc == null && searchFilter == null) {
            throw ServiceException.INVALID_REQUEST("No modify params specified.", null);
        }
        if (searchFilter != null && type == null) {
            ZimbraLog.addresslist.debug("Setting gal search type to all.");
            type = GalSearchType.all;
        }
    }

    @Override
    public String toString() {
        return "ModifyAddressListRequest [type=" + type + ", id=" + id + ", name=" + name
            + ", desc=" + desc + ", searchFilter=" + searchFilter + "]";
    }
}
