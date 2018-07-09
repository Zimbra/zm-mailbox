/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

import java.net.URI;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;

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
    public void receiveSoapMessage(HttpPost postMethod, Element envelope) {
        if (level == Level.OFF) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Response ===");
        
        if (Level.needsHeader(level)) {
            Header[] headers = postMethod.getAllHeaders();
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
    public void sendSoapMessage(HttpPost postMethod, Element envelope, BasicCookieStore httpState) {
        if (level == Level.OFF) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Request ===");
        
        if (Level.needsHeader(level)) {
            
            URI uri = postMethod.getURI();
            System.out.println(uri.toString());
            
            
            // headers
            Header[] headers = postMethod.getAllHeaders();
            for (Header header : headers) {
                System.out.println(header.toString().trim()); // trim the ending crlf
            }
            System.out.println();
            
            //cookies
            if (httpState != null) {
                List<Cookie> cookies = httpState.getCookies();
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
