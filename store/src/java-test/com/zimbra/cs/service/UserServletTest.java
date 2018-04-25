/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2018 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.common.util.L10nUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.servlet.util.AuthUtil;


/**
 * @author zimbra
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({UserServlet.class, ZimbraServlet.class, L10nUtil.class, ResourceBundle.class, AuthUtil.class})
public class UserServletTest {


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer("");
        Provisioning prov = Provisioning.getInstance();
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, "pending");
        prov.createAccount("testbug39481@zimbra.com", "secret", attrs);
    }

    /**
     * Test method for {@link com.zimbra.cs.service.UserServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */

    @Test
    public void testDoGet() {
        HttpServletRequest request = PowerMockito.mock(HttpServletRequest.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserServlet userServlet = new UserServlet();
        try {
            
            PowerMockito.spy(ZimbraServlet.class);
            PowerMockito.spy(UserServlet.class);

            PowerMockito.mockStatic(L10nUtil.class);
            PowerMockito.when(request.getPathInfo()).thenReturn("/testbug3948@zimbra.com");
            PowerMockito.when(request.getRequestURI()).thenReturn("service/home/");
            PowerMockito.when(request.getParameter("auth")).thenReturn("basic");
            PowerMockito.when(request.getParameter("loc")).thenReturn("en_US");
            PowerMockito.when(request.getHeader("Authorization")).thenReturn("Basic dGVzdDM0ODg6dGVzdDEyMw==");
            PowerMockito.when(request.getQueryString()).thenReturn("auth=basic&view=text&id=261");

            userServlet.doGet(request, response);
            Assert.assertEquals(401, response.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("No exception should be thrown.");
        } 
        
        
    }
    

    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {

        }
    }
   
    
    public class MockHttpServletResponse implements HttpServletResponse {

        private int status = 0;
        private String msg = null;
        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#flushBuffer()
         */
        @Override
        public void flushBuffer() throws IOException {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#getBufferSize()
         */
        @Override
        public int getBufferSize() {
            return 0;
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#getCharacterEncoding()
         */
        @Override
        public String getCharacterEncoding() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#getContentType()
         */
        @Override
        public String getContentType() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#getLocale()
         */
        @Override
        public Locale getLocale() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#getOutputStream()
         */
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#getWriter()
         */
        @Override
        public PrintWriter getWriter() throws IOException {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#isCommitted()
         */
        @Override
        public boolean isCommitted() {
            return false;
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#reset()
         */
        @Override
        public void reset() {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#resetBuffer()
         */
        @Override
        public void resetBuffer() {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#setBufferSize(int)
         */
        @Override
        public void setBufferSize(int arg0) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
         */
        @Override
        public void setCharacterEncoding(String arg0) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#setContentLength(int)
         */
        @Override
        public void setContentLength(int arg0) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#setContentLengthLong(long)
         */
        @Override
        public void setContentLengthLong(long arg0) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#setContentType(java.lang.String)
         */
        @Override
        public void setContentType(String arg0) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#setLocale(java.util.Locale)
         */
        @Override
        public void setLocale(Locale arg0) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie)
         */
        @Override
        public void addCookie(Cookie arg0) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#addDateHeader(java.lang.String, long)
         */
        @Override
        public void addDateHeader(String arg0, long arg1) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#addHeader(java.lang.String, java.lang.String)
         */
        @Override
        public void addHeader(String arg0, String arg1) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#addIntHeader(java.lang.String, int)
         */
        @Override
        public void addIntHeader(String arg0, int arg1) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
         */
        @Override
        public boolean containsHeader(String arg0) {
            return false;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String)
         */
        @Override
        public String encodeRedirectURL(String arg0) {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
         */
        @Override
        public String encodeRedirectUrl(String arg0) {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
         */
        @Override
        public String encodeURL(String arg0) {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
         */
        @Override
        public String encodeUrl(String arg0) {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#getHeader(java.lang.String)
         */
        @Override
        public String getHeader(String arg0) {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#getHeaderNames()
         */
        @Override
        public Collection<String> getHeaderNames() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#getHeaders(java.lang.String)
         */
        @Override
        public Collection<String> getHeaders(String arg0) {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#getStatus()
         */
        @Override
        public int getStatus() {
            return status;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#sendError(int)
         */
        @Override
        public void sendError(int arg0) throws IOException {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#sendError(int, java.lang.String)
         */
        @Override
        public void sendError(int status, String msg) throws IOException {
            this.status = status;
            this.msg = msg;
            
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
         */
        @Override
        public void sendRedirect(String arg0) throws IOException {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#setDateHeader(java.lang.String, long)
         */
        @Override
        public void setDateHeader(String arg0, long arg1) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String)
         */
        @Override
        public void setHeader(String arg0, String arg1) {
         // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#setIntHeader(java.lang.String, int)
         */
        @Override
        public void setIntHeader(String arg0, int arg1) {
            // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#setStatus(int)
         */
        @Override
        public void setStatus(int arg0) {
         // No implementation required
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletResponse#setStatus(int, java.lang.String)
         */
        @Override
        public void setStatus(int arg0, String arg1) {
            // No implementation required
        }

        
        public String getMsg() {
            return msg;
        }
    }
}