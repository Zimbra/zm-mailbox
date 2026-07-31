/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Begin a SAML configuration test. Validates the test origin, generates
 * a nonce (stored in <b>zimbraSamlTestNonce</b>), and returns the pop-up URL (with RelayState) that
 * drives the test SSO flow through the SAML extension.
 * <br />
 * If a test is already in progress (<b>zimbraSamlTestNonce</b> is set and unexpired) the request
 * fails unless <b>force</b> is set, in which case the existing nonce is overwritten.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GENERATE_SAML_TEST_REQUEST)
@XmlType(propOrder = {})
public class GenerateSamlTestRequest {

    /**
     * @zm-api-field-description Domain to test. If omitted, the global SAML configuration is tested.
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private DomainSelector domain;

    /**
     * @zm-api-field-tag force
     * @zm-api-field-description If set, overwrite an existing in-progress test nonce instead of failing.
     */
    @XmlAttribute(name=AdminConstants.A_FORCE, required=false)
    private ZmBoolean force;

    public GenerateSamlTestRequest() {
    }

    public GenerateSamlTestRequest(DomainSelector domain, Boolean force) {
        this.domain = domain;
        this.force = ZmBoolean.fromBool(force);
    }

    public void setDomain(DomainSelector domain) { this.domain = domain; }
    public void setForce(Boolean force) { this.force = ZmBoolean.fromBool(force); }

    public DomainSelector getDomain() { return domain; }
    public Boolean getForce() { return ZmBoolean.toBool(force); }
}
