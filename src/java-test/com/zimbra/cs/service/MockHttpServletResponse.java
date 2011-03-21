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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class MockHttpServletResponse implements HttpServletResponse {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Override
    public void flushBuffer() throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getBufferSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    class MockServletOutputStream extends ServletOutputStream {
        @Override
        public void write(int b) throws IOException {
            output.write(b);
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resetBuffer() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBufferSize(int arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setCharacterEncoding(String arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setContentLength(int arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setContentType(String arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLocale(Locale arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addCookie(Cookie arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addDateHeader(String arg0, long arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addHeader(String arg0, String arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addIntHeader(String arg0, int arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean containsHeader(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String encodeRedirectURL(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String encodeRedirectUrl(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String encodeURL(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String encodeUrl(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void sendError(int arg0) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendError(int arg0, String arg1) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendRedirect(String arg0) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDateHeader(String arg0, long arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setHeader(String arg0, String arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setIntHeader(String arg0, int arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setStatus(int arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setStatus(int arg0, String arg1) {
        // TODO Auto-generated method stub
        
    }

}
