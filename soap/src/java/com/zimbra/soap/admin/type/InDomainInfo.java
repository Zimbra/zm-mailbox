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

package com.zimbra.soap.admin.type;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.NamedElement;
import com.zimbra.soap.admin.type.EffectiveRightsInfo;

@XmlAccessorType(XmlAccessType.NONE)
public class InDomainInfo {

    /**
     * @zm-api-field-description Domains
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private List <NamedElement> domains = Lists.newArrayList();

    /**
     * @zm-api-field-description Rights
     */
    @XmlElement(name=AdminConstants.E_RIGHTS, required=true)
    private final EffectiveRightsInfo rights;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private InDomainInfo() {
        this(null, null);
    }

    public InDomainInfo(EffectiveRightsInfo rights) {
        this(null, rights);
    }

    public InDomainInfo(Collection <NamedElement> domains,
            EffectiveRightsInfo rights) {
        this.rights = rights;
        setDomains(domains);
    }

    public InDomainInfo setDomains(Collection <NamedElement> domains) {
        this.domains.clear();
        if (domains != null) {
            this.domains.addAll(domains);
        }
        return this;
    }

    public InDomainInfo addDomain(NamedElement domain) {
        domains.add(domain);
        return this;
    }

    public List <NamedElement> getDomains() {
        return Collections.unmodifiableList(domains);
    }

    public EffectiveRightsInfo getRights() { return rights; }
}
