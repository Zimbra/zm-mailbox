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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.Element.XMLElement;

public class ZAuthToken {
    private static final String COOKIE_ZM_AUTH_TOKEN       = "ZM_AUTH_TOKEN";
    private static final String COOKIE_ZM_ADMIN_AUTH_TOKEN = "ZM_ADMIN_AUTH_TOKEN"; 
    
    private static final String YAHOO_AUTHTOKEN_TYPE = "YAHOO_CALENDAR_AUTH_PROVIDER";
    private static final String YAHOO_Y_COOKIE = "Y"; 
    private static final String YAHOO_T_COOKIE = "T";

    private String mType;
    private String mValue;
    private Map<String, String> mAttrs;
    

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
        fromSoap(eAuthToken, isAdmin);
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
    public ZAuthToken(Cookie[] cookies, boolean isAdmin) {
        if (cookies == null) 
            return;
        
        fromCookies(cookies, isAdmin);
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
    
    public String getType() { return mType; }
    public String getValue() { return mValue; }  // AP-TODO-20: find callsites and retire
    public Map<String,String> getAttrs() { return mAttrs; }
    
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
    
    private void init(String type, String value, Map<String,String> attrs) {
        mType = type;
        mValue = value;
        mAttrs = attrs;
    }
    
    public Element encodeSoapCtxt(Element ctxt) {
        return toSoap(ctxt, HeaderConstants.E_AUTH_TOKEN, HeaderConstants.E_A, HeaderConstants.A_N);
    }
    
    public Element encodeAuthReq(Element authReq, boolean isAdmin) {
        String authTokenElem = isAdmin?AdminConstants.E_AUTH_TOKEN:AccountConstants.E_AUTH_TOKEN;
        String attrElem = isAdmin?AdminConstants.E_A:AccountConstants.E_A;
        String nameAttr = isAdmin?AdminConstants.A_N:AccountConstants.A_N;
        return toSoap(authReq, authTokenElem, attrElem, nameAttr);
    }
    
    public Map<String, String> cookieMap(boolean isAdmin) {
        if (mType == null)
            return toZimbraCookieMap(isAdmin);
        else if (mType.equals(YAHOO_AUTHTOKEN_TYPE))
            return toYahooCookieMap(isAdmin);
        else
            return null;
    }
    
    /**
     * Clean all auth cookies
     * 
     * @param response
     */
    public static void clearCookies(HttpServletResponse response) {
        clearCookie(response, COOKIE_ZM_AUTH_TOKEN);
        clearCookie(response, YAHOO_T_COOKIE);
        clearCookie(response, YAHOO_Y_COOKIE);
    }
    
    private void fromSoap(com.zimbra.common.soap.Element eAuthToken, boolean isAdmin) throws ServiceException {
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
    
    private Element toSoap(Element parent, String authTokenElem, String attrElem, String nameAttr) {
        Element eAuthToken = parent.addElement(authTokenElem);
        if (mValue != null) {
            eAuthToken.setText(mValue);
        } else if (mAttrs != null) {
            for (Map.Entry<String, String> attr : mAttrs.entrySet())
                eAuthToken.addKeyValuePair(attr.getKey(), attr.getValue(), attrElem, nameAttr);
        }
        if (mType != null)
            eAuthToken.addAttribute(AccountConstants.A_TYPE, mType);
        
        return eAuthToken;
    }
    
    private void fromCookies(Cookie[] cookies, boolean isAdmin) {
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
   
    private static void clearCookie(HttpServletResponse response, String cookieName) {
        javax.servlet.http.Cookie authTokenCookie = new javax.servlet.http.Cookie(cookieName, "");
        authTokenCookie.setMaxAge(0);
        authTokenCookie.setPath("/");
        response.addCookie(authTokenCookie);
    }
    
    private String zimbraCookieName(boolean isAdmin) {
        return isAdmin? COOKIE_ZM_ADMIN_AUTH_TOKEN : COOKIE_ZM_AUTH_TOKEN;
    }
    
    private Map<String, String> toZimbraCookieMap(boolean isAdmin) {
        Map<String, String> cookieMap = null;
        if (mValue != null) {
            String cookieName = zimbraCookieName(isAdmin);
            cookieMap = new HashMap<String, String>();
            cookieMap.put(cookieName, mValue);
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
        String yCookie = cookieMap.get(YAHOO_Y_COOKIE);
        String tCookie = cookieMap.get(YAHOO_T_COOKIE);
        
        if (yCookie != null || tCookie != null) {
            Map<String, String> attrs = new HashMap<String, String>();
            if (yCookie != null)
                attrs.put(YahooAuthData.cookieNameToAttrName(YAHOO_Y_COOKIE), yCookie);
            if (tCookie != null)
                attrs.put(YahooAuthData.cookieNameToAttrName(YAHOO_T_COOKIE), tCookie);
            
            init(YAHOO_AUTHTOKEN_TYPE, null, attrs);
            return true;
        }
        return false;
    }
    
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
        trans.setAuthToken(new ZAuthToken(null, value, attrs));
        resp = trans.invoke(req1);
        
        Element req2 = new XMLElement(AccountConstants.GET_INFO_REQUEST);
        attrs = new HashMap<String, String>();
        attrs.put("X", "x ...");
        attrs.put("Y", "y ...");
        trans.setAuthToken(new ZAuthToken("foobar", null, attrs));
        resp = trans.invoke(req2);
    }
    
} 