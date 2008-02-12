/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 30, 2004
 */
package com.zimbra.cs.account;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.service.ServiceException;


/**
 * @author schemers
 */
public abstract class AuthToken {
    
    public static String generateDigest(String a, String b) {
        if (a == null)
            return null;
        StringBuilder buf = new StringBuilder();
        buf.append(a);
        buf.append(":");
        if (b != null)
            buf.append(b);
        return ByteUtil.getDigest(buf.toString().getBytes());
    }
    
    public abstract String toString();
    
    public abstract String getAccountId() ;

    public abstract String getAdminAccountId();
    
    public abstract long getExpires();

    public abstract boolean isExpired();

    public abstract boolean isAdmin();

    public abstract boolean isDomainAdmin();
    
    public abstract boolean isCCAdmin();
    
    public abstract boolean isZimbraUser();

    public abstract String getExternalUserEmail() ;
    
    public abstract String getDigest();
    
    public abstract int getCCTier();
    
    public abstract String getCrumb() throws AuthTokenException;
    
    /**
     * Encode original auth info into an outgoing http request.
     * 
     * @param client
     * @param method
     * @param isAdminReq
     * @param cookieDomain
     * @throws ServiceException
     */
    public abstract void encode(HttpClient client, HttpMethod method, boolean isAdminReq, String cookieDomain) throws ServiceException;
    
    /**
     * AP-TODO-4:
     *     This API is called only from ZimbraServlet.proxyServletRequest when the first hop is http basic auth(REST basic auth and DAV).  
     *     For the next hop we encode the String auth token in cookie.  For all other cases, the original cookies are "carbon-copied" "as is" 
     *     to the next hop.  See ZimbraServlet.proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, HttpMethod method, HttpState state)
     *     We should clean this after AP-TODO-3 is resolved. 
     *     
     * Encode original auth info into an outgoing http request cookie.
     * 
     * @param state
     * @param isAdminReq
     * @param cookieDomain
     * @throws ServiceException
     */
    public abstract void encode(HttpState state, boolean isAdminReq, String cookieDomain) throws ServiceException;

    /**
     * Encode original auth info into an HttpServletResponse
     * 
     * @param resp
     * @param isAdminReq
     */
    public abstract void encode(HttpServletResponse resp, boolean isAdminReq) throws ServiceException;

    // public abstract void encodeAuthReq(Element authRequest)  throws ServiceException;
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public abstract String getEncoded() throws AuthTokenException;
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(String encoded) throws AuthTokenException {
        return ZimbraAuthToken.getAuthToken(encoded);
    }
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(Account acct) {
        return new ZimbraAuthToken(acct);
    }
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(Account acct, boolean isAdmin) {
        return new ZimbraAuthToken(acct, isAdmin);
    }
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(Account acct, long expires) {
        return new ZimbraAuthToken(acct, expires);
    }
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct) {
        return new ZimbraAuthToken(acct, expires, isAdmin, adminAcct);
    }
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(String acctId, String externalEmail, String pass, String digest, long expires) {
        return new ZimbraAuthToken(acctId, externalEmail, pass, digest, expires);
    }
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getZimbraAdminAuthToken() throws ServiceException {
        return ZimbraAuthToken.getZimbraAdminAuthToken();
    }

}
