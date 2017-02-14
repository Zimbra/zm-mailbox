/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainInfo;

@XmlRootElement(name=AdminConstants.E_GET_ALL_DOMAINS_RESPONSE)
public class GetAllDomainsResponse {

    /**
     * @zm-api-field-description Information on domains
     */
    @XmlElement(name=AdminConstants.E_DOMAIN)
    private List <DomainInfo> domainList = new ArrayList<DomainInfo>();

    public GetAllDomainsResponse() {
    }

    public void addDomain(DomainInfo domain ) {
        this.getDomainList().add(domain);
    }

    public List <DomainInfo> getDomainList() { return domainList; }
}
