/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;

public class SoapDebugListener implements HttpDebugListener {
    
    enum Level {
        OFF,
        HEADER,
        BODY,
        ALL;
        
        private static boolean needsHeader(Level level) {
            return level == Level.ALL || level == Level.HEADER;
        }
        
        private static boolean needsBody(Level level) {
            return level == Level.ALL || level == Level.BODY;
        }
    }
    
    private Level level = Level.BODY;
    
    SoapDebugListener() {
    }
    
    SoapDebugListener(Level level) {
        this.level = level;
    }
    
    @Override
    public void receiveSoapMessage(PostMethod postMethod, Element envelope) {
        if (level == Level.OFF) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Response ===");
        
        if (Level.needsHeader(level)) {
            Header[] headers = postMethod.getResponseHeaders();
            for (Header header : headers) {
                System.out.println(header.toString().trim()); // trim the ending crlf
            }
            System.out.println();
        }
        
        if (Level.needsBody(level)) {
            System.out.println(envelope.prettyPrint());
        }
    }

    @Override
    public void sendSoapMessage(PostMethod postMethod, Element envelope, HttpState httpState) {
        if (level == Level.OFF) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Request ===");
        
        if (Level.needsHeader(level)) {
            try {
                URI uri = postMethod.getURI();
                System.out.println(uri.toString());
            } catch (URIException e) {
                e.printStackTrace();
            }
            
            // headers
            Header[] headers = postMethod.getRequestHeaders();
            for (Header header : headers) {
                System.out.println(header.toString().trim()); // trim the ending crlf
            }
            System.out.println();
            
            //cookies
            if (httpState != null) {
                Cookie[] cookies = httpState.getCookies();
                for (Cookie cookie : cookies) {
                    System.out.println("Cookie: " + cookie.toString());
                }
            }
            System.out.println();
        }
        
        if (Level.needsBody(level)) {
            System.out.println(envelope.prettyPrint());
        }
    }
}
