/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.common.util;


import java.net.URI;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;

public class SoapDebugListener implements HttpDebugListener {
    
    public interface Printer {
        // print an empty line
        public void println();
        
        // print str on a line
        public void println(String str);
    }
    
    private static class StdoutPrinter implements Printer {
        @Override
        public void println() {
            System.out.println();
        }
        
        @Override
        public void println(String str) {
            System.out.println(str);
        }
    }
    
    public enum Level {
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
    
    private final Level level;
    private final Printer printer;
    
    public SoapDebugListener() {
        this(null, null);
    }
    
    public SoapDebugListener(Printer printer, Level level) {
        
        if (printer == null) {
            printer = new StdoutPrinter();
        }
        
        if (level == null) {
            level = Level.BODY;
        }
        
        this.printer = printer;
        this.level = level;
    }
    
    @Override
    public void receiveSoapMessage(HttpPost postMethod, Element envelope) {
        if (level == Level.OFF) {
            return;
        }
        
        printer.println();
        printer.println("=== Response ===");
        
        if (Level.needsHeader(level)) {
            Header[] headers = postMethod.getAllHeaders();
            for (Header header : headers) {
                printer.println(header.toString().trim()); // trim the ending crlf
            }
            printer.println();
        }
        
        if (Level.needsBody(level)) {
            printer.println(envelope.prettyPrint());
        }
    }

    @Override
    public void sendSoapMessage(HttpPost postMethod, Element envelope, BasicCookieStore cookieStore) {
        if (level == Level.OFF) {
            return;
        }
        
        printer.println();
        printer.println("=== Request ===");
        
        if (Level.needsHeader(level)) {
            URI uri = postMethod.getURI();
            printer.println(uri.toString());
            
            // headers
            Header[] headers = postMethod.getAllHeaders();
            for (Header header : headers) {
                printer.println(header.toString().trim()); // trim the ending crlf
            }
            printer.println();
            
            //cookies
            if (cookieStore != null) {
                List<Cookie> cookies = cookieStore.getCookies();
                for (Cookie cookie : cookies) {
                    printer.println("Cookie: " + cookie.toString());
                }
            }
            printer.println();
        }
        
        if (Level.needsBody(level)) {
            printer.println(envelope.prettyPrint());
        }
    }
}
