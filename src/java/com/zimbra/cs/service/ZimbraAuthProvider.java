/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.SoapServlet;

public class ZimbraAuthProvider extends AuthProvider{

    ZimbraAuthProvider() {
        super(ZIMBRA_AUTH_PROVIDER);
    }
    
    protected ZimbraAuthProvider(String name) {
        super(name);
    }

    // AP-TODO-6: dup in ZAuthToken, move to common?
    public static String cookieName(boolean isAdminReq) {
        return isAdminReq? ZimbraServlet.COOKIE_ZM_ADMIN_AUTH_TOKEN : ZimbraServlet.COOKIE_ZM_AUTH_TOKEN;
    }
    
    protected AuthToken authToken(HttpServletRequest req, boolean isAdminReq) throws AuthProviderException, AuthTokenException {
        String cookieName = cookieName(isAdminReq);
        String encodedAuthToken = null;
        javax.servlet.http.Cookie cookies[] =  req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(cookieName)) {
                    encodedAuthToken = cookies[i].getValue();
                    break;
                }
            }
        }
        
        return genAuthToken(encodedAuthToken);
    }

    protected AuthToken authToken(Element soapCtxt, Map engineCtxt) throws AuthProviderException, AuthTokenException  {
        String encodedAuthToken = (soapCtxt == null ? null : soapCtxt.getAttribute(HeaderConstants.E_AUTH_TOKEN, null));
        
        // check for auth token in engine context if not in header  
        if (encodedAuthToken == null)
            encodedAuthToken = (String) engineCtxt.get(SoapServlet.ZIMBRA_AUTH_TOKEN);
        
        return genAuthToken(encodedAuthToken);
    }
    
    protected AuthToken authToken(String encoded) throws AuthProviderException, AuthTokenException {
        return genAuthToken(encoded);
    }
    
    protected AuthToken genAuthToken(String encodedAuthToken) throws AuthProviderException, AuthTokenException {
        if (StringUtil.isNullOrEmpty(encodedAuthToken))
            throw AuthProviderException.NO_AUTH_DATA();
        
        return ZimbraAuthToken.getAuthToken(encodedAuthToken);
    }
    
    protected AuthToken authToken(Account acct) {
        return new ZimbraAuthToken(acct);
    }
    
    protected AuthToken authToken(Account acct, boolean isAdmin) {
        return new ZimbraAuthToken(acct, isAdmin);
    }
    
    protected AuthToken authToken(Account acct, long expires) {
        return new ZimbraAuthToken(acct, expires);
    }
    
    protected AuthToken authToken(Account acct, long expires, boolean isAdmin, Account adminAcct) {
        return new ZimbraAuthToken(acct, expires, isAdmin, adminAcct);
    }
    
}
