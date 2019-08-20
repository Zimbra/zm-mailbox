/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required false - can't require auth on auth request
 * @zm-api-command-admin-auth-required false - can't require auth on auth request
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

    /**
     * @zm-api-field-description controls whether the client supports CSRF token <br/>
     *                           0: (default)<br />
     *                           Client does not support CSRF token<br />
     *                           1: The client supports CSRF token. <br />
     */
    @XmlAttribute(name = AccountConstants.A_CSRF_SUPPORT /* support CSRF Token */, required = false)
    private ZmBoolean csrfSupported;

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

    /**
     *@zm-api-field-description the TOTP code used for two-factor authentication
     *
     */
    @XmlElement(name=AccountConstants.E_TWO_FACTOR_CODE /* twoFactorCode */, required=false)
    private String twoFactorCode;

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

    public String getTwoFactorCode() {
        return twoFactorCode;
    }

    public void setTwoFactorCode(String twoFactorCode) {
        this.twoFactorCode = twoFactorCode;
    }

    public void setPersistAuthTokenCookie(ZmBoolean persistAuthTokenCookie) {
        this.persistAuthTokenCookie = persistAuthTokenCookie;
    }

    public void setCsrfSupported(ZmBoolean csrfSupported) {
        this.csrfSupported = csrfSupported;
    }



    /**
     * @return the csrfSupported
     */
    public boolean getCsrfSupported() {
        return ZmBoolean.toBool(csrfSupported);
    }

    /**
     * @param csrfSupported
     *            the csrfSupported to set
     */
    public void setCsrfSupported(Boolean csrfSupported) {
        this.csrfSupported = ZmBoolean.fromBool(csrfSupported);
    }
}
