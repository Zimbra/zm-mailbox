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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Authenticate for administration
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_AUTH_REQUEST)
@XmlType(propOrder = {})
public class AuthRequest {
    
    /**
     * @zm-api-field-description controls whether the auth token cookie in the response should 
     * be persisted when the browser exits.<br />
     * 0: (default)<br />
     *    the cookie will be deleted when the Web browser exits.<br />
     * 1: The "Expires" attribute of the cookie will be set per rfc6265.<br />
     */
    @XmlAttribute(name=AdminConstants.A_PERSIST_AUTH_TOKEN_COOKIE /* persistAuthTokenCookie */, required=false)
    private ZmBoolean persistAuthTokenCookie;

    // TODO: authToken can be more complex than this and needs to be extendable.
    /**
     * @zm-api-field-description An authToken can be passed instead of account/password/name to validate an
     * existing auth token.
     */
    @XmlElement(name=AccountConstants.E_AUTH_TOKEN /* authToken */, required=false)
    private String authToken;

    /**
     * @zm-api-field-tag auth-name
     * @zm-api-field-description Name.  Only one of <b>{auth-name}</b> or <b>&lt;account></b> can be specified
     */
    @XmlAttribute(name=AdminConstants.E_NAME, required=false)
    private String name;

    /**
     * @zm-api-field-description Password - must be present if not using AuthToken
     */
    @XmlAttribute(name=AdminConstants.E_PASSWORD, required=false)
    private String password;

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AccountConstants.E_ACCOUNT, required=false)
    private AccountSelector account;

    /**
     * @zm-api-field-description Virtual host
     */
    @XmlElement(name=AccountConstants.E_VIRTUAL_HOST /* virtualHost */, required=false)
    private String virtualHost;

    public AuthRequest() {
        this((String)null, (String)null);
    }

    public AuthRequest(String name, String password) {
        this.authToken = null;
        this.name = name;
        this.password = password;
        this.account = null;
        this.virtualHost = null;
    }

    public Boolean getPersistAuthTokenCookie() { 
        return ZmBoolean.toBool(persistAuthTokenCookie); 
    }
    
    public void setPersistAuthTokenCookie(Boolean persistAuthTokenCookie) { 
        this.persistAuthTokenCookie = ZmBoolean.fromBool(persistAuthTokenCookie); 
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
    public void setAccount(AccountSelector account) {
        this.account = account;
    }
    public AccountSelector getAccount() {
        return account;
    }
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }
    public String getVirtualHost() {
        return virtualHost;
    }
}
