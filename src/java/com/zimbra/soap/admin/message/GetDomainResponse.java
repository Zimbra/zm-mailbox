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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainInfo;

@XmlRootElement(name=AdminConstants.E_GET_DOMAIN_RESPONSE)
public class GetDomainResponse {

    /**
     * @zm-api-field-description Information about domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN)
    private final DomainInfo domain;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetDomainResponse() {
        this(null);
    }

    public GetDomainResponse(DomainInfo domain) {
        this.domain = domain;
    }

    public DomainInfo getDomain() { return domain; }
}
