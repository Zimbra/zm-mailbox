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

package com.zimbra.common.auth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.ZimbraCookie;

public class ZAuthToken {
    private static final String COOKIE_ZM_AUTH_TOKEN       = "ZM_AUTH_TOKEN";
    private static final String COOKIE_ZM_ADMIN_AUTH_TOKEN = "ZM_ADMIN_AUTH_TOKEN"; 
    
    private static final String YAHOO_CALENDAR_AUTHTOKEN_TYPE = "YAHOO_CALENDAR_AUTH_PROVIDER";
    private static final String YAHOO_MAIL_AUTHTOKEN_TYPE     = "YAHOO_MAIL_AUTH_PROVIDER";
    
    private static final String AUTHTOKEN_TYPE_COOKIE = "AUTH_TOKEN_TYPE";
    
    private static final String YAHOO_Y_COOKIE = "Y"; 
    private static final String YAHOO_T_COOKIE = "T";
    private static final String YAHOO_ADMIN_COOKIE = "ADMIN_AUTH_KEY";
    private static final String YAHOO_DELEGATED_COOKIE = "DELEGATED_AUTH_KEY";

    private static final String YAHOO_K_ATTR = "K";
    private static final String YAHOO_H_ATTR = "H";
    
    private static final String YAHOO_QP_ACCESSKEY = "k";
    private static final String YAHOO_QP_HOSTACCOUNTID = "h";

    private String mType;
    private String mValue;
    private String mProxyAuthToken;
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
     * Construct a ZAuthToken from a HttpServletRequest
     * 
     * Note: The returning ZAuthToken could be "empty" if it cannot find the expected auth data,
     *       this method does not throw exception.
     *       Callsites should call the isEmpty method and react(returns null to callers, throw 
     *       exception, etc) if it does not deal with "empty" ZAuthToken.
     * 
     * @param request
     * @param isAdmin
     */
    public ZAuthToken(HttpServletRequest req, boolean isAdmin) {
        fromHttpReq(req, isAdmin);
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
    
    public ZAuthToken(String value, String proxyAuthToken) {
        this(value);
        mProxyAuthToken = proxyAuthToken;
    }
    
    /**
     * Parse a JSON string to ZAuthToken, use by CLI tools to pass an auth token string from command line
     * 
     * e.g.
     * {"type": "foo", "value": "bar", "attrs": { "k1" : "v1", "k2" : "v2"}}
     * 
     * or should we use keys like they appear in our AuthResponse? probably not.
     * e.g.
     * {"_content": "bar", "type": "foo", "_attrs": { "k1": "v1", "k2": "v2"}}
     */
    public static ZAuthToken fromJSONString(String jsonString) throws ServiceException {
        String type = null;
        String value = null;
        Map<String, String> attrs = null;
        
        try {
            JSONObject json = new JSONObject(jsonString);
            type = json.optString("type", null);
            value = json.optString("value", null);
            
            attrs = null;
            JSONObject jAttrs = json.optJSONObject("attrs");
            if (jAttrs != null) {
                attrs = new HashMap<String, String>();
                for (Iterator iter = jAttrs.keys(); iter.hasNext(); ) {
                    String k = (String)iter.next();
                    String v = jAttrs.getString(k);
                    attrs.put(k, v);
                }
            }
        } catch (JSONException e) {
            throw ServiceException.PARSE_ERROR("cannot parse JSON auth token: " + jsonString, e);
        }
        
        return new ZAuthToken(type, value, attrs);
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
        else if (mType.equals(YAHOO_CALENDAR_AUTHTOKEN_TYPE) ||
                 mType.equals(YAHOO_MAIL_AUTHTOKEN_TYPE))
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
        clearCookie(response, AUTHTOKEN_TYPE_COOKIE);
        clearCookie(response, YAHOO_T_COOKIE);
        clearCookie(response, YAHOO_Y_COOKIE);
        clearCookie(response, YAHOO_ADMIN_COOKIE);
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
        if (mProxyAuthToken != null) {
            eAuthToken.setText(mProxyAuthToken);
        } else if (mValue != null) {
            eAuthToken.setText(mValue);
        } else if (mAttrs != null) {
            for (Map.Entry<String, String> attr : mAttrs.entrySet())
                eAuthToken.addKeyValuePair(attr.getKey(), attr.getValue(), attrElem, nameAttr);
        }
        if (mType != null)
            eAuthToken.addAttribute(AccountConstants.A_TYPE, mType);
        
        return eAuthToken;
    }
    
    private void fromHttpReq(HttpServletRequest request, boolean isAdmin) {
        Cookie[] cookies = request.getCookies();
        
        Map<String, String> cookieMap = new HashMap<String, String>();
        if (cookies != null) {
            for (Cookie ck : cookies) {
                cookieMap.put(ck.getName(), ck.getValue());
            }
        }
        
        // look for zimbra cookie first
        if (fromZimbraCookies(cookieMap, isAdmin))
            return;
        
        // no Zimbra cookies, look for Yahoo cookies
        if (fromYahooCookies(request, cookieMap, isAdmin))
            return;
        
        // fall thru, leave the ZAuthToken empty
    } 
   
    private static void clearCookie(HttpServletResponse response, String cookieName) {
        javax.servlet.http.Cookie authTokenCookie = new javax.servlet.http.Cookie(cookieName, "");
        authTokenCookie.setMaxAge(0);
        ZimbraCookie.setAuthTokenCookieDomainPath(authTokenCookie, ZimbraCookie.PATH_ROOT);
        response.addCookie(authTokenCookie);
    }
    
    private String zimbraCookieName(boolean isAdmin) {
        return isAdmin? COOKIE_ZM_ADMIN_AUTH_TOKEN : COOKIE_ZM_AUTH_TOKEN;
    }
    
    private Map<String, String> toZimbraCookieMap(boolean isAdmin) {
        Map<String, String> cookieMap = null;
        if (mValue != null || mProxyAuthToken != null) {
            String cookieName = zimbraCookieName(isAdmin);
            cookieMap = new HashMap<String, String>();
            cookieMap.put(cookieName, mProxyAuthToken !=null ? mProxyAuthToken : mValue);
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
    
    public void resetProxyAuthToken() {
        mProxyAuthToken = null;
    }
    
    static class YahooAuthData {
        static final String YAHOO_Y_ATTR = "Y"; 
        static final String YAHOO_T_ATTR = "T";
        static final String YAHOO_DELEGATED_ATTR = "DELEGATED_AUTH_KEY";
        
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
            
            // auth token type
            cookieMap.put(AUTHTOKEN_TYPE_COOKIE, mType);
            
            String yCookie = mAttrs.get(YahooAuthData.YAHOO_Y_ATTR);
            if (yCookie != null)
                cookieMap.put(YahooAuthData.attrNameToCookieName(YahooAuthData.YAHOO_Y_ATTR), yCookie);
            
            String tCookie = mAttrs.get(YahooAuthData.YAHOO_T_ATTR);
            if (tCookie != null)
                cookieMap.put(YahooAuthData.attrNameToCookieName(YahooAuthData.YAHOO_T_ATTR), tCookie);
            
            String dCookie = mAttrs.get(YahooAuthData.YAHOO_DELEGATED_ATTR);
            if (dCookie != null)
            	cookieMap.put(YahooAuthData.attrNameToCookieName(YahooAuthData.YAHOO_DELEGATED_ATTR), dCookie);
            
            if (cookieMap.size() == 0)
                cookieMap = null;
        }
        return cookieMap;  
    }
    
    private boolean fromYahooCookies(HttpServletRequest request, Map<String, String> cookieMap, boolean isAdmin) {
        String authTokenTypeCookie = cookieMap.get(AUTHTOKEN_TYPE_COOKIE);
        
        String yCookie = cookieMap.get(YAHOO_Y_COOKIE);
        String tCookie = cookieMap.get(YAHOO_T_COOKIE);
        String aCookie = cookieMap.get(YAHOO_ADMIN_COOKIE);
        String dCookie = cookieMap.get(YAHOO_DELEGATED_COOKIE);
        
        String accessKey = getYahooAccessKey(request);
        String hostAccountId = getYahooHostAccountId(request);
        
        if (yCookie != null || tCookie != null || aCookie != null || dCookie != null || 
            accessKey != null || hostAccountId != null) {
            Map<String, String> attrs = new HashMap<String, String>();
            
            if (yCookie != null)
                attrs.put(YahooAuthData.cookieNameToAttrName(YAHOO_Y_COOKIE), yCookie);
            if (tCookie != null)
                attrs.put(YahooAuthData.cookieNameToAttrName(YAHOO_T_COOKIE), tCookie);
            if (aCookie != null)
                attrs.put(YahooAuthData.cookieNameToAttrName(YAHOO_ADMIN_COOKIE), aCookie);
            if (dCookie != null)
                attrs.put(YahooAuthData.cookieNameToAttrName(YAHOO_DELEGATED_COOKIE), dCookie);
            
            if (accessKey != null) 
                attrs.put(YAHOO_K_ATTR, accessKey);
            if (hostAccountId != null) 
                attrs.put(YAHOO_H_ATTR, hostAccountId);
            
            // if there is no auth token type cookie from the cookie map, default to
            // YAHOO_MAIL_AUTHTOKEN_TYPE
            if (authTokenTypeCookie == null)
                authTokenTypeCookie = YAHOO_MAIL_AUTHTOKEN_TYPE;
            init(authTokenTypeCookie, null, attrs);
            return true;
        }
        return false;
    }
    
    public static String getYahooAccessKey(HttpServletRequest request) {
        return request.getParameter(YAHOO_QP_ACCESSKEY);
    }
    
    public static String getYahooHostAccountId(HttpServletRequest request) {
        return request.getParameter(YAHOO_QP_HOSTACCOUNTID);
    }
    
    public static void main(String[] args) throws Exception {
        SoapHttpTransport trans = new SoapHttpTransport("http://localhost:7070/service/soap/");
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