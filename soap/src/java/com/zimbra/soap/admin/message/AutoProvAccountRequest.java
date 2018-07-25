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

package com.zimbra.soap.admin.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.PrincipalSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Auto-provision an account
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_AUTO_PROV_ACCOUNT_REQUEST)
public class AutoProvAccountRequest {

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=true)
    private DomainSelector domain;

    /**
     * @zm-api-field-description Principal
     */
    @XmlElement(name=AdminConstants.E_PRINCIPAL /* principal */, required=true)
    private PrincipalSelector principal;

    /**
     * @zm-api-field-tag password
     * @zm-api-field-description Password
     */
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("domain", domain)
            .add("principal", principal);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
