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
