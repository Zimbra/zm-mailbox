/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Account;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_AUTH_REQUEST)
@XmlType(propOrder = {})
public class AuthRequest {

    @XmlElement(name=AccountConstants.E_AUTH_TOKEN) private String authToken;
    @XmlAttribute(name=AdminConstants.E_NAME, required=false) private String name;
    // password must be present if not using AuthToken
    @XmlAttribute(name=AdminConstants.E_PASSWORD, required=false) private String password;
    @XmlElement(name=AccountConstants.E_ACCOUNT, required=false) private Account account;
    @XmlElement(name=AccountConstants.E_VIRTUAL_HOST, required=false) private String virtualHost;
    public AuthRequest() {
        this.authToken = null;
        this.name = null;
        this.password = null;
        this.account = null;
        this.virtualHost = null;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    public String getAuthToken() {
        return authToken;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getPassword() {
        return password;
    }
    public void setAccount(Account account) {
        this.account = account;
    }
    public Account getAccount() {
        return account;
    }
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }
    public String getVirtualHost() {
        return virtualHost;
    }
}
