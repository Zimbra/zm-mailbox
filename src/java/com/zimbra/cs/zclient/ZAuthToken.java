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

import org.apache.commons.httpclient.Cookie;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
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
    
    public ZAuthToken(com.zimbra.common.soap.Element eAuthToken, boolean isAdmin) throws ServiceException {
        mType = eAuthToken.getAttribute(HeaderConstants.A_TYPE, null);
        
        mValue = eAuthToken.getText();
        if (mValue.length() == 0)
            mValue = null;
        
        for (Element a : eAuthToken.listElements(HeaderConstants.E_A)) {
            String name = a.getAttribute(HeaderConstants.A_N);
            String value = a.getText();
            if (mAttrs == null)
                mAttrs = new HashMap<String, String>();
            mAttrs.put(name, value);
        }
    }

    // AP-TODO-20: find callsites and retire
    public ZAuthToken(String type, String value, Map<String,String> attrs) {
        init(type, value, attrs);
    }
    
    // callsites of this API should be all unittest CLIs ,OK for now
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
    
    // AP-TODO-30: test all callsites
    public Element encodeAuthRequest(Element authReq) {
        Element eAuthToken = authReq.addElement(AccountConstants.E_AUTH_TOKEN);
        if (mValue != null) {
            eAuthToken.setText(mValue);
        } else if (mAttrs != null) {
            for (Map.Entry<String, String> attr : mAttrs.entrySet())
                eAuthToken.addKeyValuePair(attr.getKey(), attr.getValue(), AccountConstants.E_A, AccountConstants.A_N);
        }
        if (mType != null)
            eAuthToken.addAttribute(AccountConstants.A_TYPE, mType);
        
        return eAuthToken;
    }

    // AP-TODO-30: test all callsites
    // AP-TODO: hack for now, a way for client to encode auth token into Cookie
    public Cookie[] toCookies(String cookieDomain, boolean isAdmin) {
        List<Cookie> cookies = new ArrayList<Cookie>();
        if (mType == null) {
            String CookieName = isAdmin? "ZM_ADMIN_AUTH_TOKEN" : "ZM_AUTH_TOKEN";
            Cookie cookie = new Cookie(cookieDomain, CookieName, mValue, "/", -1, false);
            cookies.add(cookie);
        } else {
            if (mAttrs != null) {
                for (Map.Entry<String, String> attr : mAttrs.entrySet()) {
                    // AP-TODO: should map attr names to cookie names??
                    Cookie cookie = new Cookie(cookieDomain, attr.getKey(), attr.getValue(), "/", -1, false);
                    cookies.add(cookie);
                }
            }
        }
        return cookies.toArray(new Cookie[cookies.size()]);
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