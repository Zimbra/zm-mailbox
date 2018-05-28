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

package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.AuthToken;
import com.zimbra.soap.account.type.PreAuth;
import com.zimbra.soap.account.type.Pref;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.ZmBoolean;


/**
 <AuthRequest xmlns="urn:zimbraAccount">
   [<account by="name|id|foreignPrincipal">...</account>]
   [<password>...</password>]
   [<recoveryCode>...</recoveryCode>]
   [<preauth timestamp="{timestamp}" expires="{expires}">{computed-preauth-value}</preauth>]
   [<authToken>...</authToken>]
   [<virtualHost>{virtual-host}</virtualHost>]
   [<prefs>[<pref name="..."/>...]</prefs>]
   [<attrs>[<attr name="..."/>...]</attrs>]
   [<requestedSkin>{skin}</requestedSkin>]
 </AuthRequest>
 * @zm-api-command-auth-required false - can't require auth on auth request
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Authenticate for an account
 * @zm-api-request-description when specifying an account, one of <b>&lt;password></b> or <b>&lt;preauth></b> or
 * <b>&lt;recoveryCode></b> must be specified.
 * See preauth.txt for a discussion of preauth.<br />
 * An authToken can be passed instead of account/password/preauth to validate an existing auth token.
 * If <b>{verifyAccount}="1"</b>, <b>&lt;account></b> is required and the account in the auth token is compared to the
 * named account.<br />
 * Mismatch results in auth failure.  An external app that relies on ZCS for user identification can use this to test
 * if the auth token provided by the user belongs to that user. If <b>{verifyAccount}="0"</b> (default), only the
 * auth token is verified and any <b>&lt;account></b> element specified is ignored.
 */
@XmlRootElement(name=AccountConstants.E_AUTH_REQUEST)
@XmlType(propOrder = {})
public class AuthRequest {

    /**
     * @zm-api-field-description controls whether the auth token cookie in the response should
     * be persisted when the browser exits.<br />
     * 0: (default)<br />
     *    the cookie will be deleted when the Web browser exits.<br />
     * 1: The "Expires" attribute of the cookie will be set per rfc6265.<br />
     */
    @XmlAttribute(name=AccountConstants.A_PERSIST_AUTH_TOKEN_COOKIE /* persistAuthTokenCookie */, required=false)
    private ZmBoolean persistAuthTokenCookie;


    /**
     * @zm-api-field-description controls whether the client supports CSRF token <br/>
     * 0: (default)<br />
     *    Client does not support CSRF token<br />
     * 1: The client supports CSRF token. <br />
     */
    @XmlAttribute(name=AccountConstants.A_CSRF_SUPPORT /* support CSRF Token */, required=false)
    private ZmBoolean csrfSupported;

    /**
     * @zm-api-field-description Specifies the account to authenticate against
     */
    @XmlElement(name=AccountConstants.E_ACCOUNT, required=false)
    private AccountSelector account;

    /**
     * @zm-api-field-description Password to use in conjunction with an account
     */
    @XmlElement(name=AccountConstants.E_PASSWORD, required=false)
    private String password;

    /**
     * @zm-api-field-description RecoveryCode to use in conjunction with an account in case of forgot password flow.
     */
    @XmlElement(name=AccountConstants.E_RECOVERY_CODE, required=false)
    private String recoveryCode;

    /**
     * @zm-api-field-description &lt;preauth> is an alternative to &lt;account>.  See preauth.txt
     */
    @XmlElement(name=AccountConstants.E_PREAUTH /* preauth */, required=false)
    private PreAuth preauth;

    /**
     * @zm-api-field-description An authToken can be passed instead of account/password/preauth to validate an
     * existing auth token. If <b>{verifyAccount}="1"</b>, <b>&lt;account></b> is required and the account in the
     * auth token is compared to the named account. Mismatch results in auth failure.  An external app that relies
     * on ZCS for user identification can use this to test if the auth token provided by the user belongs to that user.
     * If <b>{verifyAccount}="0"</b> (default), only the auth token is verified and any <b>&lt;account></b> element
     * specified is ignored.
     */
    @XmlElement(name=AccountConstants.E_AUTH_TOKEN /* authToken */, required=false)
    private AuthToken authToken;

    /**
     * @zm-api-field-description JWT auth token
     */
    @XmlElement(name=AccountConstants.E_JWT_TOKEN /* jwtToken */, required=false)
    private String jwtToken;

    /**
     * @zm-api-field-tag virtual-host
     * @zm-api-field-description if specified (in conjunction with by="name"), virtual-host is used to determine
     * the domain of the account name, if it does not include a domain component. For example, if the domain
     * foo.com has a zimbraVirtualHostname of "mail.foo.com", and an auth request comes in for "joe" with a
     * virtualHost of "mail.foo.com", then the request will be equivalent to logging in with "joe@foo.com".
     */
    @XmlElement(name=AccountConstants.E_VIRTUAL_HOST /* virtualHost */, required=false)
    private String virtualHost;

    /**
     * @zm-api-field-description Requested preference settings.
     */
    @XmlElementWrapper(name=AccountConstants.E_PREFS /* prefs */, required=false)
    @XmlElement(name=AccountConstants.E_PREF /* pref */, required=false)
    private final List<Pref> prefs = new ArrayList<Pref>();

    /**
     * @zm-api-field-description Requested attribute settings.  Only attributes that are allowed to be returned by
     * GetInfo will be returned by this call
     */
    @XmlElementWrapper(name=AccountConstants.E_ATTRS /* attrs */, required=false)
    @XmlElement(name=AccountConstants.E_ATTR /* attr */, required=false)
    private final List<Attr> attrs = new ArrayList<Attr>();

    /**
     * @zm-api-field-tag requested-skin
     * @zm-api-field-description if specified the name of the skin requested by the client
     */
    @XmlElement(name=AccountConstants.E_REQUESTED_SKIN /* requestedSkin */, required=false)
    private String requestedSkin;

    /**
     *@zm-api-field-description the TOTP code used for two-factor authentication
     *
     */
    @XmlElement(name=AccountConstants.E_TWO_FACTOR_CODE /* twoFactorCode */, required=false)
    private String twoFactorCode;

    /**
     *@zm-api-field-description whether the client represents a trusted device
     *
     */
    @XmlAttribute(name=AccountConstants.A_TRUSTED_DEVICE /* deviceTrusted */, required=false)
    private ZmBoolean deviceTrusted;

    /**
     *@zm-api-field-description whether the client represents a trusted device
     *
     */
    @XmlElement(name=AccountConstants.E_TRUSTED_TOKEN /* trustedToken */, required=false)
    private String trustedDeviceToken;

    /**
     *@zm-api-field-description unique device identifier; used to verify trusted mobile devices
     *
     */
    @XmlElement(name=AccountConstants.E_DEVICE_ID /* deviceId */, required=false)
    private String deviceId;

    @XmlAttribute(name=AccountConstants.A_GENERATE_DEVICE_ID /* generateDeviceId */, required=false)
    private ZmBoolean generateDeviceId;

    /**
     * @zm-api-field-description type of token to be returned, it can be auth or jwt
     * 
     */
    @XmlAttribute(name = AccountConstants.A_TOKEN_TYPE /* token type to be returned */, required = false)
    private String tokenType;

    public AuthRequest() {
    }

    public AuthRequest(AccountSelector account) {
        setAccount(account);
    }

    public AuthRequest(AccountSelector account, String password) {
        setAccount(account);
        setPassword(password);
    }

    public Boolean getPersistAuthTokenCookie() { return ZmBoolean.toBool(persistAuthTokenCookie); }
    public void setPersistAuthTokenCookie(Boolean persistAuthTokenCookie) {
        this.persistAuthTokenCookie = ZmBoolean.fromBool(persistAuthTokenCookie);
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public AccountSelector getAccount() { return account; }
    public AuthRequest setAccount(AccountSelector account) { this.account = account; return this; }

    public String getPassword() { return password; }
    public AuthRequest setPassword(String password) { this.password = password; return this; }

    public String getRecoveryCode() { return recoveryCode; }
    public AuthRequest setRecoveryCode(String recoveryCode) { this.recoveryCode = recoveryCode; return this; }

    public PreAuth getPreauth() { return preauth; }
    public AuthRequest setPreauth(PreAuth preauth) { this.preauth = preauth; return this; }

    public AuthToken getAuthToken() { return authToken; }
    public AuthRequest setAuthToken(AuthToken authToken) { this.authToken = authToken; return this; }

    public String getJwtToken() { return jwtToken; }
    public AuthRequest setJwtToken(String jwtToken) { this.jwtToken = jwtToken; return this; }

    public String getVirtualHost() { return virtualHost; }
    public AuthRequest setVirtualHost(String host) { this.virtualHost = host; return this; }

    public List<Pref> getPrefs() { return Collections.unmodifiableList(prefs); }

    public AuthRequest setPrefs(Collection<Pref> prefs) {
        this.prefs.clear();
        if (prefs != null) {
            this.prefs.addAll(prefs);
        }
        return this;
    }

    public AuthRequest addPref(Pref pref) {
        prefs.add(pref);
        return this;
    }

    public AuthRequest addPref(String prefName) {
        prefs.add(new Pref(prefName));
        return this;
    }

    public List<Attr> getAttrs() { return Collections.unmodifiableList(attrs); }

    public AuthRequest setAttrs(Collection<Attr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            this.attrs.addAll(attrs);
        }
        return this;
    }

    public AuthRequest addAttr(Attr attr) {
        attrs.add(attr);
        return this;
    }

    public AuthRequest addAttr(String attrName) {
        attrs.add(new Attr(attrName));
        return this;
    }

    public String getRequestedSkin() { return requestedSkin; }
    public AuthRequest setRequestedSkin(String skin) { this.requestedSkin = skin; return this; }


    /**
     * @return the csrfSupported
     */
    public ZmBoolean getCsrfSupported() {
        return csrfSupported;
    }


    /**
     * @param csrfSupported the csrfSupported to set
     */
    public void setCsrfSupported(Boolean csrfSupported) {
        this.csrfSupported = ZmBoolean.fromBool(csrfSupported);
    }

    public AuthRequest setTwoFactorCode(String totp) {
        this.twoFactorCode = totp;
        return this;
    }

    public String getTwoFactorCode() {
        return twoFactorCode;
    }

    public AuthRequest setDeviceTrusted(Boolean deviceTrusted) {
        this.deviceTrusted = ZmBoolean.fromBool(deviceTrusted);
        return this;
    }

    public ZmBoolean getDeviceTrusted() {
        return deviceTrusted;
    }

    public AuthRequest setTrustedDeviceToken(String token) {
        this.trustedDeviceToken = token;
        return this;
    }

    public String getTrustedDeviceToken() {
        return trustedDeviceToken;
    }

    public AuthRequest setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public AuthRequest setGenerateDeviceId(Boolean generateId) {
        this.generateDeviceId = ZmBoolean.fromBool(generateId);
        return this;
    }

    public ZmBoolean getGenerateDeviceId() {
        return generateDeviceId;
    }
}
