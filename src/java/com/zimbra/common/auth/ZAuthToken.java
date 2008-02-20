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

package com.zimbra.common.auth;

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

public class ZAuthToken {
    private static final String COOKIE_ZM_AUTH_TOKEN       = "ZM_AUTH_TOKEN";
    private static final String COOKIE_ZM_ADMIN_AUTH_TOKEN = "ZM_ADMIN_AUTH_TOKEN"; 
    
    private static final String YAHOO_AUTHTOKEN_TYPE = "YAHOO_CALENDAR_AUTH_PROVIDER";

    
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
        if (cookies == null) 
            return;
        
        initfromCookies(cookies, isAdmin);
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
        Map<String, String> cookieMap = toCookieMap(isAdminReq, addBothAdminAndUserCookiesIfAdmin);
        
        if (cookieMap != null) {
            List<org.apache.commons.httpclient.Cookie> cookies = new ArrayList<org.apache.commons.httpclient.Cookie>();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                org.apache.commons.httpclient.Cookie cookie = new org.apache.commons.httpclient.Cookie(cookieDomain, ck.getKey(), ck.getValue(), "/", -1, false);
                cookies.add(cookie);
            }
           
            HttpState state = new HttpState();
            state.addCookies(cookies.toArray(new org.apache.commons.httpclient.Cookie[cookies.size()]));
            client.setState(state);
            client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY); 
        }
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
        javax.servlet.http.Cookie authTokenCookie = new javax.servlet.http.Cookie(COOKIE_ZM_AUTH_TOKEN, "");
        authTokenCookie.setMaxAge(0);
        authTokenCookie.setPath("/");
        response.addCookie(authTokenCookie);
    }
    
    private String zimbraCookieName(boolean isAdmin) {
        return isAdmin? COOKIE_ZM_ADMIN_AUTH_TOKEN : COOKIE_ZM_AUTH_TOKEN;
    }
    
    
    /*
     * AP-TODO: retire the addBothIfAdmin param, cleanup.
     */
    private Map<String, String> toZimbraCookieMap(boolean isAdmin, boolean addBothIfAdmin) {
        Map<String, String> cookieMap = null;
        if (mValue != null) {
            String cookieName = zimbraCookieName(isAdmin);
            cookieMap = new HashMap<String, String>();
            cookieMap.put(cookieName, mValue);
            
            if (addBothIfAdmin && isAdmin)
                cookieMap.put(COOKIE_ZM_AUTH_TOKEN, mValue);
        }
        return cookieMap;  
    }
    
    private boolean fromZimbraCookies(Map<String, String> cookieMap, boolean isAdmin) {
        String cookieName = zimbraCookieName(isAdmin);
        String cookieValue = cookieMap.get(cookieName);
        if (cookieValue != null) {
            init(null, cookieValue, null);
            return true;
        }
        return false;
    }
    
    static class YahooAuthData {
        static final String YAHOO_Y_ATTR = "Y"; 
        static final String YAHOO_T_ATTR = "T";
        static final String YAHOO_Y_COOKIE = "Y"; 
        static final String YAHOO_T_COOKIE = "T";
        
        static String cookieNameToAttrName(String cookieName) {
            return cookieName;  // cookie name and attr name are identical for now
        }
        
        static String attrNameToCookieName(String attrName) {
            return attrName;   // cookie name and attr name are identical for now
        }
    }
    
    private Map<String, String> toYahooCookieMap(boolean isAdmin) {
        Map<String, String> cookieMap = null;
        if (mAttrs != null) {
            cookieMap = new HashMap<String, String>();
            
            String yCookie = mAttrs.get(YahooAuthData.YAHOO_Y_ATTR);
            if (yCookie != null)
                cookieMap.put(YahooAuthData.attrNameToCookieName(YahooAuthData.YAHOO_Y_ATTR), yCookie);
            
            String tCookie = mAttrs.get(YahooAuthData.YAHOO_T_ATTR);
            if (tCookie != null)
                cookieMap.put(YahooAuthData.attrNameToCookieName(YahooAuthData.YAHOO_T_ATTR), tCookie);
            
            if (cookieMap.size() == 0)
                cookieMap = null;
        }
        return cookieMap;  
    }
    
    private boolean fromYahooCookies(Map<String, String> cookieMap, boolean isAdmin) {
        String yCookie = cookieMap.get(YahooAuthData.YAHOO_Y_COOKIE);
        String tCookie = cookieMap.get(YahooAuthData.YAHOO_T_COOKIE);
        
        if (yCookie != null || tCookie != null) {
            Map<String, String> attrs = new HashMap<String, String>();
            if (yCookie != null)
                attrs.put(YahooAuthData.cookieNameToAttrName(YahooAuthData.YAHOO_Y_COOKIE), yCookie);
            if (tCookie != null)
                attrs.put(YahooAuthData.cookieNameToAttrName(YahooAuthData.YAHOO_T_COOKIE), tCookie);
            
            init(YAHOO_AUTHTOKEN_TYPE, null, attrs);
            return true;
        }
        return false;
    }
    
    /*
     * AP-TODO: retire the addBothIfAdmin param, cleanup.
     */
    private Map<String, String> toCookieMap(boolean isAdmin, boolean addBothIfAdmin) {
        if (mType == null)
            return toZimbraCookieMap(isAdmin, addBothIfAdmin);
        else if (mType.equals(YAHOO_AUTHTOKEN_TYPE))
            return toYahooCookieMap(isAdmin);
        else
            return null;
    }
    
    private void initfromCookies(Cookie[] cookies, boolean isAdmin) {
        if (cookies == null) return;
        Map<String, String> cookieMap = new HashMap<String, String>();
        for (Cookie ck : cookies) {
            cookieMap.put(ck.getName(), ck.getValue());
        }
        
        // look for zimbra cookie first
        if (fromZimbraCookies(cookieMap, isAdmin))
            return;
        
        // no Zimbra cookies, look for Yahoo cookies
        if (fromYahooCookies(cookieMap, isAdmin))
            return;
        
        // fall thru, leave the ZAuthToken empty
    } 
    
    public Map<String, String> toCookieMap(boolean isAdmin) {
        return toCookieMap(isAdmin, false);
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