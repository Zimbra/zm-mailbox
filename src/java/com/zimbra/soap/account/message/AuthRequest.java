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

package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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


/**
 <AuthRequest xmlns="urn:zimbraAccount">
   [<account by="name|id|foreignPrincipal">...</account>]
   [<password>...</password>]
   [<preauth timestamp="{timestamp}" expires="{expires}">{computed-preauth-value}</preauth>]
   [<authToken>...</authToken>]
   [<virtualHost>{virtual-host}</virtualHost>]
   [<prefs>[<pref name="..."/>...]</prefs>]
   [<attrs>[<attr name="..."/>...]</attrs>]
   [<requestedSkin>{skin}</requestedSkin>]
 </AuthRequest>
 * @zm-api-command-description Authenticate for an account
 * @zm-api-request-description when specifying an account, one of <b>&lt;password></b> or <b>&lt;preauth></b> must be
 * specified.  See preauth.txt for a discussion of preauth.<br />
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
    private List<Pref> prefs = new ArrayList<Pref>();

    /**
     * @zm-api-field-description Requested attribute settings.  Only attributes that are allowed to be returned by
     * GetInfo will be returned by this call
     */
    @XmlElementWrapper(name=AccountConstants.E_ATTRS /* attrs */, required=false)
    @XmlElement(name=AccountConstants.E_ATTR /* attr */, required=false)
    private List<Attr> attrs = new ArrayList<Attr>();

    /**
     * @zm-api-field-tag requested-skin
     * @zm-api-field-description if specified the name of the skin requested by the client
     */
    @XmlElement(name=AccountConstants.E_REQUESTED_SKIN /* requestedSkin */, required=false)
    private String requestedSkin;

    public AuthRequest() {
    }

    public AuthRequest(AccountSelector account) {
        setAccount(account);
    }

    public AuthRequest(AccountSelector account, String password) {
        setAccount(account);
        setPassword(password);
    }

    public AccountSelector getAccount() { return account; }
    public AuthRequest setAccount(AccountSelector account) { this.account = account; return this; }

    public String getPassword() { return password; }
    public AuthRequest setPassword(String password) { this.password = password; return this; }

    public PreAuth getPreauth() { return preauth; }
    public AuthRequest setPreauth(PreAuth preauth) { this.preauth = preauth; return this; }

    public AuthToken getAuthToken() { return authToken; }
    public AuthRequest setAuthToken(AuthToken authToken) { this.authToken = authToken; return this; }

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
}
