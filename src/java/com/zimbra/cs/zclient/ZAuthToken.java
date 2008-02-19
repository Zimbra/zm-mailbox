/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.cs.servlet.ZimbraServlet;

public class ZAuthToken {
    
    private String mType;
    private String mValue;
    private Map<String, String> mAttrs;
    
    private void init(String type, String value, Map<String,String> attrs) {
        mType = type;
        mValue = value;
        mAttrs = attrs;
    }
    
    /**
     * Construct a ZAuthToken from <authToken> in SOAP response
     * 
     * Note: The returning ZAuthToken could be "empty" if it cannot find the expected auth data,
     *       this method does not throw exception .
     *       Callsites should call the isEmpty method and react(returns null to callers, throw 
     *       exception, etc) if it does not deal with "empty" ZAuthToken.

     * @param eAuthToken
     * @param isAdmin
     * @throws ServiceException
     */
    public ZAuthToken(com.zimbra.common.soap.Element eAuthToken, boolean isAdmin) throws ServiceException {
        mType = eAuthToken.getAttribute(isAdmin?AdminConstants.A_TYPE:AccountConstants.A_TYPE, null);
        
        mValue = eAuthToken.getText();
        if (mValue.length() == 0)
            mValue = null;
        
        String eName = isAdmin?AdminConstants.E_A:AccountConstants.E_A;
        String aName = isAdmin?AdminConstants.A_N:AccountConstants.A_N;
        for (Element a : eAuthToken.listElements(eName)) {
            String name = a.getAttribute(aName);
            String value = a.getText();
            if (mAttrs == null)
                mAttrs = new HashMap<String, String>();
            mAttrs.put(name, value);
        }
    }
    
    /**
     * Construct a ZAuthToken from HttpServletRequest.
     * 
     * Note: The returning ZAuthToken could be "empty" if it cannot find the expected auth data,
     *       this method does not throw exception .
     *       Callsites should call the isEmpty method and react(returns null to callers, throw 
     *       exception, etc) if it does not deal with "empty" ZAuthToken.
     * 
     * @param request
     * @param isAdmin
     */
    public ZAuthToken(HttpServletRequest request, boolean isAdmin) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (Cookie c : cookies){
            if (c.getName().equals(ZimbraServlet.COOKIE_ZM_AUTH_TOKEN) && !isAdmin) {
                init(null, c.getValue(), null);
                return;
            } else if (c.getName().equals(ZimbraServlet.COOKIE_ZM_ADMIN_AUTH_TOKEN) && isAdmin) {
                init(null, c.getValue(), null);
                return;
            } else {
                // AP-TODO-30: what to do with Yahoo Y&T cookies?
                // which cookies to look for??
            }
        }
    }
    
    // AP-TODO-20: find callsites and retire
    public ZAuthToken(String type, String value, Map<String,String> attrs) {
        init(type, value, attrs);
    }
    
    // callsites of this API should be all unittest CLIs, those are OK for now
    // AP-TODO: retire them
    public ZAuthToken(String value) {
        init(null, value, null);
    }
    
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        
        if (obj instanceof ZAuthToken) {
            ZAuthToken other = (ZAuthToken)obj;
            if (mType != null && !mType.equals(other.getType()))
                return false;
            
            if (mValue != null && !mValue.equals(other.getValue()))
                return false;
                
            if (mAttrs != null && !mAttrs.equals(other.getAttrs()))
                return false;  
            return true;
        }
        return false;
    }
    
    public boolean isEmpty() {
        return (mValue == null && (mAttrs == null || mAttrs.isEmpty()));
    }
    
    public Element encodeAuthReq(Element authReq, boolean isAdmin) {
        Element eAuthToken = authReq.addElement(isAdmin?AdminConstants.E_AUTH_TOKEN:AccountConstants.E_AUTH_TOKEN);
        if (mValue != null) {
            eAuthToken.setText(mValue);
        } else if (mAttrs != null) {
            String eName = isAdmin?AdminConstants.E_A:AccountConstants.E_A;
            String aName = isAdmin?AdminConstants.A_N:AccountConstants.A_N;
            for (Map.Entry<String, String> attr : mAttrs.entrySet())
                eAuthToken.addKeyValuePair(attr.getKey(), attr.getValue(), eName, aName);
        }
        if (mType != null)
            eAuthToken.addAttribute(AccountConstants.A_TYPE, mType);
        
        return eAuthToken;
    }

    public void encode(HttpClient client, HttpMethod method, boolean isAdminReq, String cookieDomain) {
        encode(client, method, isAdminReq, cookieDomain, false);
    }
    
    /* AP-TODO
     * retire this ugly API after the callsites in ZMailBox are verified working OK without adding both cookies for isAdmin.
     * the legacy code adds both admin cookie and user cookie if isAdmin is true.   ??
     * 
     * all callsites should be calling the encode methods above
     */
    public void encode(HttpClient client, HttpMethod method, boolean isAdminReq, String cookieDomain, 
                boolean addBothAdminAndUserCookiesIfAdmin) {
        org.apache.commons.httpclient.Cookie[] cookies = toCookies(cookieDomain, isAdminReq, addBothAdminAndUserCookiesIfAdmin);
        
        HttpState state = new HttpState();
        state.addCookies(cookies);
        client.setState(state);
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }

    /*
     * AP-TODO: retire the addBothIfAdmin param after the above encode API is retired
     */
    private org.apache.commons.httpclient.Cookie[] toCookies(String cookieDomain, boolean isAdmin, boolean addBothIfAdmin) {
        List<org.apache.commons.httpclient.Cookie> cookies = new ArrayList<org.apache.commons.httpclient.Cookie>();
        if (mValue != null) {
            // Zimbra cookies
            String cookieName = isAdmin? ZimbraServlet.COOKIE_ZM_ADMIN_AUTH_TOKEN : ZimbraServlet.COOKIE_ZM_AUTH_TOKEN;
            org.apache.commons.httpclient.Cookie cookie = new org.apache.commons.httpclient.Cookie(cookieDomain, cookieName, mValue, "/", -1, false);
            cookies.add(cookie);
            
            if (addBothIfAdmin && isAdmin) {
                org.apache.commons.httpclient.Cookie ck = new org.apache.commons.httpclient.Cookie(cookieDomain, ZimbraServlet.COOKIE_ZM_AUTH_TOKEN, mValue, "/", -1, false);
                cookies.add(ck);
            }
        } else if (mAttrs != null) {
            // Custom auth provider cookies
            for (Map.Entry<String, String> attr : mAttrs.entrySet()) {
                // AP-TODO-30: should map attr names to cookie namesbase on mType?
                org.apache.commons.httpclient.Cookie cookie = new org.apache.commons.httpclient.Cookie(cookieDomain, attr.getKey(), attr.getValue(), "/", -1, false);
                cookies.add(cookie);
            }
        }
        return cookies.toArray(new org.apache.commons.httpclient.Cookie[cookies.size()]);
    }
    
    public Cookie[] toCookies(boolean isAdmin) {
        List<Cookie> cookies = new ArrayList<Cookie>();
        if (mValue != null) {
            // Zimbra cookies
            String CookieName = isAdmin? ZimbraServlet.COOKIE_ZM_ADMIN_AUTH_TOKEN : ZimbraServlet.COOKIE_ZM_AUTH_TOKEN;
            Cookie cookie = new Cookie(CookieName, mValue);
            cookies.add(cookie);
        } else if (mAttrs != null) {
            // Custom auth provider cookies
            for (Map.Entry<String, String> attr : mAttrs.entrySet()) {
                // AP-TODO-30: should map attr names to cookie namesbase on mType?
                Cookie cookie = new Cookie(attr.getKey(), attr.getValue());
                cookies.add(cookie);
            }
        }
        return cookies.toArray(new Cookie[cookies.size()]);
    }
    
    
    /**
     * 
     * AP-TODO: called from LogoutTag to clear cookies.
     *          for now only cleans Zimbra cookies.  The code is copied from  LogoutTag.doTag 
     *          "as is": hardcoded to clean only the user cookie.  Can be more parametered if needed. 
     *          
     * AP-TODO-30:         
     *          We moved the logic from LogoutTag to here so we remember to replace it with the 
     *          solution we come up for cookies on the client side: 
     *          
     *          - maybe also build in support for Zimbra and Y&T cookie support, then put 
     *            something in web.xml that says what type we want.
     *          or   
     *          - We ultimately could make that more pluggable as well, since it is basically 
     *            just logic for extracting the right cookies, then formatting them into the auth 
     *            token element.   
     * 
     * @param response
     */
    public static void clearCookies(HttpServletResponse response) {
        javax.servlet.http.Cookie authTokenCookie = new javax.servlet.http.Cookie(ZimbraServlet.COOKIE_ZM_AUTH_TOKEN, "");
        authTokenCookie.setMaxAge(0);
        authTokenCookie.setPath("/");
        response.addCookie(authTokenCookie);
    }
    
    public String getType() { return mType; }
    public String getValue() { return mValue; }  // AP-TODO-20: find callsites and retire
    public Map<String,String> getAttrs() { return mAttrs; }
    
    
    public static void main(String[] args) throws Exception {
        SoapHttpTransport trans = new SoapHttpTransport("http://localhost:7070/service/soap/");
        trans.setTimeout(30000);
        trans.setRetryCount(3);
        trans.setUserAgent("ZAuthTokenTest", null);
        
        SoapHttpTransport.DebugListener dl = new SoapHttpTransport.DebugListener() {
            public void receiveSoapMessage(Element envelope) {
                System.out.printf("======== SOAP RECEIVE =========\n");
                System.out.println(envelope.prettyPrint());
                System.out.printf("===============================\n");
                
            }

            public void sendSoapMessage(Element envelope) {
                System.out.println("========== SOAP SEND ==========");
                System.out.println(envelope.prettyPrint());
                System.out.println("===============================");
            }
        };
        
        trans.setDebugListener(dl);
        
        Element req = new XMLElement(AccountConstants.AUTH_REQUEST);
        req.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, "name").setText("user1");
        req.addElement(AccountConstants.E_PASSWORD).setText("test123");
        Element resp = trans.invoke(req);
        
        Element authToken = resp.getElement(AccountConstants.E_AUTH_TOKEN);
        
        ZAuthToken zAuthToken = new ZAuthToken(authToken, false);
        
        String type = zAuthToken.getType();
        String value = zAuthToken.getValue();
        Map<String, String> attrs = zAuthToken.getAttrs();
        
        System.out.println("type: " + type);
        System.out.println("value: " + value);
        System.out.println("attrs:");
        if (attrs != null) {
            for (Map.Entry<String, String> a : attrs.entrySet())
                System.out.println(a.getKey() + ": " + a.getValue());
        }
        
        Element req1 = new XMLElement(AccountConstants.GET_INFO_REQUEST);
        trans.setAuthToken(null, value, attrs);
        resp = trans.invoke(req1);
        
        Element req2 = new XMLElement(AccountConstants.GET_INFO_REQUEST);
        attrs = new HashMap<String, String>();
        attrs.put("X", "x ...");
        attrs.put("Y", "y ...");
        trans.setAuthToken("foobar", null, attrs);
        resp = trans.invoke(req2);
    }
    
} 