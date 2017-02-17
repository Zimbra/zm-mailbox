/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
