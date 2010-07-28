/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on May 30, 2004
 */
package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

import javax.servlet.http.HttpServletResponse;


/**
 * @author schemers
 */
public abstract class AuthToken {
    
    public static final long DEFAULT_AUTH_LIFETIME = 60*60*12;
    
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
    
    public static boolean isAnyAdmin(AuthToken authToken) {
        return authToken.isAdmin() || authToken.isDomainAdmin() || authToken.isDelegatedAdmin(); 
    }
    
    public abstract String toString();
    
    public abstract String getAccountId() ;

    public abstract String getAdminAccountId();
    
    public abstract long getExpires();

    public int getValidityValue() {
        return -1;
    }

    public abstract boolean isExpired();

    public abstract boolean isAdmin();

    public abstract boolean isDomainAdmin();
    
    public abstract boolean isDelegatedAdmin();
    
    public abstract boolean isZimbraUser();

    public abstract String getExternalUserEmail() ;
    
    public abstract String getDigest();
    
    public abstract String getCrumb() throws AuthTokenException;
    
    public boolean isDelegatedAuth() {
        return (getAdminAccountId() != null && !getAdminAccountId().equals(""));
    }
    
    public String getAccessKey() {
        return null;
    }

    public void setProxyAuthToken(String encoded) {}
    
    public String getProxyAuthToken() { return null; }
    
    public void resetProxyAuthToken() {}
    
    public Account getAccount() throws ServiceException {
        String acctId = getAccountId();
        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.id, acctId);
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctId);
        
        return acct;
    }

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
     *     For the next hop we encode the String auth token in cookie.  For all other cases, the original cookies are copied "as is" 
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
    public abstract void encode(HttpServletResponse resp, boolean isAdminReq, boolean secureCookie) throws ServiceException;

    public abstract void encodeAuthResp(Element parent, boolean isAdmin) throws ServiceException;
    
    public abstract ZAuthToken toZAuthToken() throws ServiceException; 
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public abstract String getEncoded() throws AuthTokenException;
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(String encoded) throws AuthTokenException {
        return ZimbraAuthToken.getAuthToken(encoded);
    }
    
    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(String acctId, String externalEmail, String pass, String digest, long expires) {
        return new ZimbraAuthToken(acctId, externalEmail, pass, digest, expires);
    }
    
    public static Map getInfo(String encoded) throws AuthTokenException {
        return ZimbraAuthToken.getInfo(encoded);
    }

}
