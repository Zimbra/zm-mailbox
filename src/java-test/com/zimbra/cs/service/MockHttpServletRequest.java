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
package com.zimbra.cs.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.zimbra.common.mime.shim.JavaMailInternetHeaders.IteratorEnumeration;

public class MockHttpServletRequest implements HttpServletRequest {
    final URL url;
    final byte[] body;
    final String ctype;

    public MockHttpServletRequest(byte[] body, URL url, String ctype) {
        this.url = url;
        this.body = body;
        this.ctype = ctype;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new IteratorEnumeration<String>(Collections.<String>emptyList());
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public String getContentType() {
        return ctype;
    }

    private class MockServletInputStream extends ServletInputStream {
        private ByteArrayInputStream bais = new ByteArrayInputStream(body);

        public MockServletInputStream() {
        }

        @Override
        public int read() throws IOException {
            return bais.read();
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new MockServletInputStream();
    }

    @Override
    public String getLocalAddr() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLocalName() {
        return url.getHost();
    }

    @Override
    public int getLocalPort() {
        return url.getPort();
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return new IteratorEnumeration<Locale>(Collections.singletonList(getLocale()));
    }

    @Override
    public String getParameter(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map getParameterMap() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProtocol() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public String getRealPath(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRemoteAddr() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRemoteHost() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRemotePort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getScheme() {
        return url.getProtocol();
    }

    @Override
    public String getServerName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getServerPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSecure() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeAttribute(String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAttribute(String name, Object o) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getAuthType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getContextPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getDateHeader(String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getHeader(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration getHeaderNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration getHeaders(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getIntHeader(String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getMethod() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPathInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPathTranslated() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getQueryString() {
        return url.getQuery();
    }

    @Override
    public String getRemoteUser() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRequestURI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getServletPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpSession getSession() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUserInRole(String role) {
        // TODO Auto-generated method stub
        return false;
    }
}