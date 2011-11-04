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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.PrincipalSelector;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_AUTO_PROV_ACCOUNT_REQUEST)
public class AutoProvAccountRequest {

    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=true)
    private DomainSelector domain;

    @XmlElement(name=AdminConstants.E_PRINCIPAL /* principal */, required=true)
    private PrincipalSelector principal;
    
    @XmlElement(name=AdminConstants.E_PASSWORD /* password */, required=false)
    private String password;

    private AutoProvAccountRequest() {
    }

    private AutoProvAccountRequest(DomainSelector domain, PrincipalSelector principal) {
        setDomain(domain);
        setPrincipal(principal);
    }

    public static AutoProvAccountRequest create(DomainSelector domain, PrincipalSelector principal) {
        return new AutoProvAccountRequest(domain, principal);
    }

    public void setDomain(DomainSelector domain) { this.domain = domain; }
    public void setPrincipal(PrincipalSelector principal) { this.principal = principal; }
    public void setPassword(String password) { this.password = password; }
    public DomainSelector getDomain() { return domain; }
    public PrincipalSelector getPrincipal() { return principal; }
    public String getPassword() { return password; }
    
    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("domain", domain)
            .add("principal", principal);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
