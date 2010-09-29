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

package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.soap.account.type.Account;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.PreAuth;
import com.zimbra.soap.account.type.Pref;


/*
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
 */
@XmlRootElement(name="AuthRequest")
@XmlType(propOrder = {})
public class AuthRequest {

    @XmlElement private Account account;
    @XmlElement private String password;
    @XmlElement private PreAuth preauth;
    @XmlElement private String authToken;
    @XmlElement private String virtualHost;
    
    @XmlElementWrapper(name="prefs")
    @XmlElement(name="pref")
    private List<Pref> prefs = new ArrayList<Pref>();
    
    @XmlElementWrapper(name="attrs")
    @XmlElement(name="attr")
    private List<Attr> attrs = new ArrayList<Attr>();
    
    @XmlElement private String requestedSkin;
    
    public AuthRequest() {
    }
    
    public AuthRequest(Account account) {
        setAccount(account);
    }
    
    public AuthRequest(Account account, String password) {
        setAccount(account);
        setPassword(password);
    }
    
    public Account getAccount() { return account; }
    public AuthRequest setAccount(Account account) { this.account = account; return this; }
    
    public String getPassword() { return password; }
    public AuthRequest setPassword(String password) { this.password = password; return this; }
    
    public PreAuth getPreauth() { return preauth; }
    public AuthRequest setPreauth(PreAuth preauth) { this.preauth = preauth; return this; }
    
    public String getAuthToken() { return authToken; }
    public AuthRequest setAuthToken(String authToken) { this.authToken = authToken; return this; }
    
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
