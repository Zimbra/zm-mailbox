/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.jsp;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.ZMailbox;

public class ZJspSession {
 
    private static final String ATTR_SESSION = ZJspSession.class.getCanonicalName()+".session";
    private static final String ATTR_TEMP_AUTHTOKEN = ZJspSession.class.getCanonicalName()+".authToken";    
 
    public static final String COOKIE_NAME = "ZM_AUTH_TOKEN";
    
    //TODO: get from config
    public static final String SOAP_URL = "http://localhost:7070/service/soap";
    
    private ZMailbox mMbox;
    private String mAuthToken;
 
    public ZJspSession(String authToken, ZMailbox mbox) {
        mAuthToken = authToken;
        mMbox = mbox;
    }
    
    public ZMailbox getMailbox() { return mMbox; }
    public String getAuthToken() { return mAuthToken; }
    
    public static String getAuthToken(JspContext context) {
        // check here first, in case we are logging in and cookie isn't set yet.
        String authToken = (String) context.getAttribute(ATTR_TEMP_AUTHTOKEN, PageContext.REQUEST_SCOPE);
        if (authToken != null) return authToken;
        
        PageContext pageContext = (PageContext) context;
        HttpServletRequest request= (HttpServletRequest) pageContext.getRequest();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies){
            if (c.getName().equals(COOKIE_NAME)) {
                return c.getValue();
            }
        }
        return null;
    }
    
    public static ZJspSession getSession(JspContext context) throws ServiceException {
        ZJspSession sess = (ZJspSession) context.getAttribute(ATTR_SESSION, PageContext.SESSION_SCOPE);
        String authToken = getAuthToken(context);
        
        // see if we have a session that matches auth token
        if (sess != null && sess.getAuthToken().equals(authToken)) {
            return sess;
        }

        if (authToken == null || authToken.length() == 0) {
            return null;    
        } else {
            // see if we can get a mailbox from the auth token
            ZMailbox mbox = ZMailbox.getMailbox(authToken, SOAP_URL, null);
            return setSession(context, authToken, mbox);
        }
    }
    
    public static ZJspSession setSession(JspContext context, String authToken, ZMailbox mbox) {
        ZJspSession sess = new ZJspSession(authToken, mbox);
        // save auth token for duration of request (chicken/egg in getSession)
        context.setAttribute(ATTR_TEMP_AUTHTOKEN, authToken, PageContext.REQUEST_SCOPE);
        context.setAttribute(ATTR_SESSION, sess, PageContext.SESSION_SCOPE);
        return sess;
    }
    
}
