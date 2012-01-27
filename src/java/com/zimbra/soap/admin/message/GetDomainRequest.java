/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Get information about a domain
 */
@XmlRootElement(name=AdminConstants.E_GET_DOMAIN_REQUEST)
public class GetDomainRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-tag apply-config
     * @zm-api-field-description If <b>{apply-config}</b> is <b>1 (true)</b>, then certain unset attrs on a domain
     * will get their values from the global config.
     * <br />
     * if <b>{apply-config}</b> is <b>0 (false)</b>, then only attributes directly set on the domain will be returned
     */
    @XmlAttribute(name=AdminConstants.A_APPLY_CONFIG, required=false)
    private ZmBoolean applyConfig = ZmBoolean.ONE /* true */;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN)
    private DomainSelector domain;

    public GetDomainRequest() {
        this(null, null);
    }

    public GetDomainRequest(DomainSelector domain, Boolean applyConfig) {
        setDomain(domain);
        setApplyConfig(applyConfig);
    }

    void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    public void setApplyConfig(Boolean applyConfig) {
        this.applyConfig = ZmBoolean.fromBool(applyConfig);
    }

    public DomainSelector getDomain() { return domain; }
    public Boolean isApplyConfig() { return ZmBoolean.toBool(applyConfig); }
}
