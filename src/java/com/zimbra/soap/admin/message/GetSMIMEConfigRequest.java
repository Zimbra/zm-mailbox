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
import com.zimbra.soap.type.NamedElement;

/**
 * @zm-api-command-description Get a configuration for SMIME public key lookup via external LDAP on a domain or
 * globalconfig
 * <br />
 * Notes: if <b>&lt;domain></b> is present, get the config on the domain, otherwise get the config on globalconfig.
 * @zm-api-command-network-edition
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_SMIME_CONFIG_REQUEST)
public class GetSMIMEConfigRequest {

    /**
     * @zm-api-field-description Config
     */
    @XmlElement(name=AdminConstants.E_CONFIG /* config */, required=false)
    private final NamedElement config;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=false)
    private final DomainSelector domain;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetSMIMEConfigRequest() {
        this((NamedElement) null, (DomainSelector) null);
    }

    public GetSMIMEConfigRequest(NamedElement config, DomainSelector domain) {
        this.config = config;
        this.domain = domain;
    }

    public NamedElement getConfig() { return config; }
    public DomainSelector getDomain() { return domain; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("config", config)
            .add("domain", domain);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
