/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CountObjectsType;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.UCServiceSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Count number of objects.
 * <br />
 * Returns number of objects of requested type.
 * <br />
 * <br />
 * Note: For account/alias/dl, if a domain is specified, only entries on the specified 
 * domain are counted.  If no domain is specified, entries on all domains are counted.
 * 
 * For accountOnUCService/cosOnUCService/domainOnUCService, UCService is required, 
 * and domain cannot be specified.
 *
 */
@XmlRootElement(name=AdminConstants.E_COUNT_OBJECTS_REQUEST)
public class CountObjectsRequest {

    /**
     * @zm-api-field-description Object type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private CountObjectsType type;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=false)
    private DomainSelector domain;
    
    /**
     * @zm-api-field-description UCService
     */
    @XmlElement(name=AdminConstants.E_UC_SERVICE /* ucservice */, required=false)
    private UCServiceSelector usService;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public CountObjectsRequest() {
        this((CountObjectsType) null, (DomainSelector) null, (UCServiceSelector) null);
    }

    public CountObjectsRequest(CountObjectsType type) {
        this(type, (DomainSelector) null, (UCServiceSelector) null);
    }

    public CountObjectsRequest(CountObjectsType type, DomainSelector domain, UCServiceSelector ucService) {
        setType(type);
        setDomain(domain);
        setUCService(ucService);
    }

    public void setType(CountObjectsType type) {
        this.type = type;
    }

    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }
    
    public void setUCService(UCServiceSelector usService) {
        this.usService = usService;
    }

    public CountObjectsType getType() {
        return type;
    }

    public DomainSelector getDomain() {
        return domain;
    }

    public UCServiceSelector getUsService() {
        return usService;
    }

    public void setUsService(UCServiceSelector usService) {
        this.usService = usService;
    }
}
