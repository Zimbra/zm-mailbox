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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;

/**
 * @zm-api-command-description Get All accounts matching the selectin criteria
 * <br />
 * <b>Access</b>: domain admin sufficient
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_ACCOUNTS_REQUEST)
@XmlType(propOrder = {})
public class GetAllAccountsRequest {

    /**
     * @zm-api-field-description Server
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=false)
    private ServerSelector server;

    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private DomainSelector domain;

    public GetAllAccountsRequest() {
        this(null, null);
    }

    public GetAllAccountsRequest(ServerSelector server, DomainSelector domain) {
        setServer(server);
        setDomain(domain);
    }

    public void setServer(ServerSelector server) {
        this.server = server;
    }

    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    public ServerSelector getServer() { return server; }
    public DomainSelector getDomain() { return domain; }
}
