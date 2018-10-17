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
import com.zimbra.soap.admin.type.DomainSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description get address list info request
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ADDRESS_LIST_INFO_REQUEST)
public class GetAddressListInfoRequest {

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description zimbra id of the address list to get
     */
    @XmlAttribute(name=AdminConstants.E_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description name of the address list to get
     */
    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag domain
     * @zm-api-field-description domain of the address list to get (needed with name)
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, /* domain */ required=false)
    private final DomainSelector domain;

    /**
     * default private constructor to block the usage
     */
    @SuppressWarnings("unused")
    private GetAddressListInfoRequest() {
        this(null, null, null);
    }

    /**
     * @param id
     * @param name
     * @param domain
     */
    public GetAddressListInfoRequest(String id, String name, DomainSelector domain) {
        this.id = id;
        this.name = name;
        this.domain = domain;
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
     * @return The domain selector
     */
    public DomainSelector getDomain() {
        return domain;
    }

    public void validateGetAddressListInfoRequest() throws ServiceException {
        if (StringUtil.isNullOrEmpty(id) && (StringUtil.isNullOrEmpty(name) || domain == null)) {
            throw ServiceException
                .INVALID_REQUEST("Get address list info requires either an id, or a name/domain pair", null);
        }
    }

    @Override
    public String toString() {
        return "GetAddressListInfoRequest [id=" + id + ", name=" + name + ", domain=" + domain + "]";
    }
}
