/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/** Explicitly sets ETag header in response; bypassing Jetty ETag generation */
public class ETagHeaderFilter implements Filter {

    public static String ZIMBRA_ETAG_HEADER = "X-Zimbra-ETag";

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ETagResponseWrapper wrapper = new ETagResponseWrapper((HttpServletResponse) response);
        chain.doFilter(request, wrapper);
    }

    private class ETagResponseWrapper extends HttpServletResponseWrapper {

        public ETagResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            if (ZIMBRA_ETAG_HEADER.equalsIgnoreCase(name)) {
                super.addHeader("ETag", value);
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if (ZIMBRA_ETAG_HEADER.equalsIgnoreCase(name)) {
                super.setHeader("ETag", value);
            }
            super.setHeader(name, value);
        }
    }
}
