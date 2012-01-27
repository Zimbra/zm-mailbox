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

import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Get Domain information
 * <br />
 * This call does <b>not</b> require an auth token.  It returuns attributes that are pertinent to domain settings
 * for cases when the user is not authenticated.  For example, URL to direct the user to upon logging out or
 * when auth token is expired.
 * <br />
 * <br />
 * If the domain doesn't exist, this call returns an empty body:
 */
@XmlRootElement(name=AdminConstants.E_GET_DOMAIN_INFO_REQUEST)
public class GetDomainInfoRequest {

    /**
     * @zm-api-field-tag apply-config
     * @zm-api-field-description If <b>{apply-config}</b> is <b>1 (true)</b>, then certain unset attrs on a domain
     * will get their values from the global config.
     * <br />
     * if <b>{apply-config}</b> is <b>0 (false)</b>, then only attributes directly set on the domain will be returned
     */
    @XmlAttribute(name=AdminConstants.A_APPLY_CONFIG, required=false)
    private ZmBoolean applyConfig;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN)
    private DomainSelector domain;

    public GetDomainInfoRequest() {
        this(null, null);
    }

    public GetDomainInfoRequest(DomainSelector domain, Boolean applyConfig) {
        setDomain(domain);
        setApplyConfig(applyConfig);
    }

    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    public void setApplyConfig(Boolean applyConfig) {
        this.applyConfig = ZmBoolean.fromBool(applyConfig);
    }

    public DomainSelector getDomain() { return domain; }
    public Boolean isApplyConfig() { return ZmBoolean.toBool(applyConfig); }
}
