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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.NamedElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_SMIME_CONFIG_REQUEST)
public class GetSMIMEConfigRequest {

    @XmlElement(name=AdminConstants.E_CONFIG, required=false)
    private final NamedElement config;

    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
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
}
