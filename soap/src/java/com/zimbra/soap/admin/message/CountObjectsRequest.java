/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CountObjectsType;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.UCServiceSelector;
import com.zimbra.soap.type.ZmBoolean;

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
 * For domain, if onlyRelated attribute is true and the request is sent by a delegate or
 * domain admin, counts only domain on which has rights, without requiring countDomain right.
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
     * @zm-api-field-description Get only related if delegated/domain admin
     */
    @XmlAttribute(name=AdminConstants.A_ONLY_RELATED /* onlyrelated */, required=false)
    private ZmBoolean onlyRelated;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=false)
    private final List <DomainSelector> domains = Lists.newArrayList();

    /**
     * @zm-api-field-description UCService
     */
    @XmlElement(name=AdminConstants.E_UC_SERVICE /* ucservice */, required=false)
    private UCServiceSelector ucService;

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

    public CountObjectsRequest(CountObjectsType type, DomainSelector domain) {
        this(type, domain, (UCServiceSelector) null);
    }

    public CountObjectsRequest(CountObjectsType type, DomainSelector domain, UCServiceSelector ucService) {
        setType(type);
        addDomain(domain);
        setUcService(ucService);
    }

    public void setType(CountObjectsType type) {
        this.type = type;
    }

    public CountObjectsType getType() {
        return type;
    }

    public CountObjectsRequest setDomains(Collection<DomainSelector> domains) {
        this.domains.clear();
        if (domains != null) {
            this.domains.addAll(domains);
        }
        return this;
    }

    public CountObjectsRequest addDomain(DomainSelector domain) {
        if (domain != null) {
            domains.add(domain);
        }
        return this;
    }

    public List<DomainSelector> getDomains() {
        return Collections.unmodifiableList(domains);
    }

    public UCServiceSelector getUcService() {
        return ucService;
    }

    public void setUcService(UCServiceSelector ucService) {
        this.ucService = ucService;
    }

    public void setOnlyRelated(Boolean onlyRelated) {
        this.onlyRelated = ZmBoolean.fromBool(onlyRelated);
    }

    public Boolean getOnlyRelated() {
        return ZmBoolean.toBool(onlyRelated, false);
    }
}
