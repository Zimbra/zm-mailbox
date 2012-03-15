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

import com.google.common.collect.Multimap;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.Pref;
import com.zimbra.soap.account.type.Session;


/*
<AuthResponse">
   <authToken>...</authToken>
   <lifetime>...</lifetime>
   <session .../>
   <refer>{mail-host}</refer>  
   [<prefs><pref name="{name}" modified="{modified-time}">{value}</pref>...</prefs>]
   [<attrs><attr name="{name}">{value}</a>...</attrs>]
   [<skin>{skin-name}</skin>]
 </AuthResponse>
/**
 * @zm-api-response-description Response to account authentication request.
 */
@XmlRootElement(name=AccountConstants.E_AUTH_RESPONSE)
@XmlType(propOrder = {})
public class AuthResponse {

    /**
     * @zm-api-field-description The authorization token
     */
    @XmlElement(name=AccountConstants.E_AUTH_TOKEN, required=true)
    private String authToken;
    /**
     * @zm-api-field-description Life time for the authorization
     */
    @XmlElement(name=AccountConstants.E_LIFETIME, required=true)
    private long lifetime;
    /**
     * @zm-api-field-description Session information
     */
    @XmlElement(name=HeaderConstants.E_SESSION)
    private Session session;
    /**
     * @zm-api-field-description host additional SOAP requests should be directed to.
     * Always returned, might be same as original host request was sent to.
     */
    @XmlElement(name=AccountConstants.E_REFERRAL)
    private String refer;
    /**
     * @zm-api-field-description if requestedSkin specified, the name of the skin to use
     * Always returned, might be same as original host request was sent to.
     */
    @XmlElement(name=AccountConstants.E_SKIN)
    private String skin;
    
    /**
     * @zm-api-field-description Requested preference settings.
     */
    @XmlElementWrapper(name=AccountConstants.E_PREFS)
    @XmlElement(name=AccountConstants.E_PREF)
    private List<Pref> prefs = new ArrayList<Pref>();
    
    /**
     * @zm-api-field-description Requested attribute settings.  Only attributes that are allowed to be returned by
     * GetInfo will be returned by this call
     */
    @XmlElementWrapper(name=AccountConstants.E_ATTRS)
    @XmlElement(name=AccountConstants.E_ATTR)
    private List<Attr> attrs = new ArrayList<Attr>();
    
    public AuthResponse() {
    }
    
    public AuthResponse(String authToken, long lifetime) {
        setAuthToken(authToken);
        setLifetime(lifetime);
    }
    
    public String getAuthToken() { return authToken; }
    public AuthResponse setAuthToken(String authToken) { this.authToken = authToken; return this; }
    
    public long getLifetime() { return lifetime; }
    public AuthResponse setLifetime(long lifetime) { this.lifetime = lifetime; return this; }
    
    public Session getSession() { return session; }
    public AuthResponse setSession(Session session) { this.session = session; return this; }
    
    public String getRefer() { return refer; }
    public AuthResponse setRefer(String refer) { this.refer = refer; return this; }
    
    public String getSkin() { return skin; }
    public AuthResponse setSkin(String skin) { this.skin = skin; return this; }
    
    public List<Pref> getPrefs() { return Collections.unmodifiableList(prefs); }
    public AuthResponse setPrefs(Collection<Pref> prefs) {
        this.prefs.clear();
        if (prefs != null) {
            this.prefs.addAll(prefs);
        }
        return this;
    }
    
    public List<Attr> getAttrs() { return Collections.unmodifiableList(attrs); }
    public AuthResponse setAttrs(Collection<Attr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            this.attrs.addAll(attrs);
        }
        return this;
    }

    public Multimap<String, String> getAttrsMultimap() {
        return Attr.toMultimap(attrs);
    }
    
    public Multimap<String, String> getPrefsMultimap() {
        return Pref.toMultimap(prefs);
    }
}
