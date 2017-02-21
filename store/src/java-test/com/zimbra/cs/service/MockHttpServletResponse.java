/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class MockHttpServletResponse implements HttpServletResponse {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    private List<Cookie> cookies = new ArrayList<Cookie>();

    @Override
    public void flushBuffer() throws IOException {
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    class MockServletOutputStream extends ServletOutputStream {
        @Override
        public void write(int b) throws IOException {
            output.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            //stubbed for now...should implement if we want to support async IO
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new MockServletOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(output);
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetBuffer() {
    }

    @Override
    public void setBufferSize(int arg0) {
    }

    @Override
    public void setCharacterEncoding(String arg0) {
    }

    @Override
    public void setContentLength(int arg0) {
    }

    @Override
    public void setContentType(String arg0) {
    }

    @Override
    public void setLocale(Locale arg0) {
    }

    @Override
    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    @Override
    public void addDateHeader(String arg0, long arg1) {
    }

    @Override
    public void addHeader(String arg0, String arg1) {
    }

    @Override
    public void addIntHeader(String arg0, int arg1) {
    }

    @Override
    public boolean containsHeader(String arg0) {
        return false;
    }

    @Override
    public String encodeRedirectURL(String arg0) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String arg0) {
        return null;
    }

    @Override
    public String encodeURL(String arg0) {
        return null;
    }

    @Override
    public String encodeUrl(String arg0) {
        return null;
    }

    @Override
    public void sendError(int arg0) throws IOException {
    }

    @Override
    public void sendError(int arg0, String arg1) throws IOException {
    }

    @Override
    public void sendRedirect(String arg0) throws IOException {
    }

    @Override
    public void setDateHeader(String arg0, long arg1) {
    }

    @Override
    public void setHeader(String arg0, String arg1) {
    }

    @Override
    public void setIntHeader(String arg0, int arg1) {
    }

    @Override
    public void setStatus(int arg0) {
    }

    @Override
    public void setStatus(int arg0, String arg1) {
    }

    @Override
    public String getHeader(String arg0) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String arg0) {
        return null;
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public void setContentLengthLong(long arg0) {
    }

}
