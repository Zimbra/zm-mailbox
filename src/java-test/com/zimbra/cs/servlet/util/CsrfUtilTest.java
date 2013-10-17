/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra, Inc.
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

package com.zimbra.cs.servlet.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.net.HttpHeaders;

/**
 * @author zimbra
 *
 */
public class CsrfUtilTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * Test method for
     * {@link com.zimbra.cs.servlet.util.CsrfUtil#isCsrfRequest(javax.servlet.http.HttpServletRequest, boolean, java.util.List)}
     * .
     */
    @Test
    public final void testIsCsrfRequest() {

        boolean checkReqForCsrf = false;
        List<String> allowedRefHost = null;
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn("example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
            "http://www.texample.com/user");

        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestWhenCsrfCheckIsTurnedOn() {

        boolean checkReqForCsrf = true;
        List<String> allowedRefHost = null;
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn("example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
           null);

        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }


    @Test
    public final void testIsCsrfRequestForSameReferer() {

        boolean checkReqForCsrf = true;
        List<String> allowedRefHost = null;
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn("www.example.com");
        EasyMock.expect(request.getServerName()).andReturn("www.example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
           "http://www.example.com/zimbra/#15");

        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForRefererInMatchHost() {

        boolean checkReqForCsrf = true;
        List<String> allowedRefHost = new ArrayList<String>();
        allowedRefHost.add("www.newexample.com");
        allowedRefHost.add("www.zimbra.com:8080");
        allowedRefHost.add("www.abc.com");
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn("example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
           "http://www.newexample.com");

        try {
            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForAllowedRefHostListEmptyAndNonMatchingHost() {

        boolean checkReqForCsrf = true;
        List<String> allowedRefHost = null;
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn("example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
           "http://www.newexample.com");

        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(true, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }



    @Test
    public final void testIsCsrfRequestForRefererNotInMatchHost() {

        boolean checkReqForCsrf = true;
        List<String> allowedRefHost = new ArrayList<String>();
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn("example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
           "http://www.newexample.com");

        try {
            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(true, csrfReq);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }


    @Test
    public final void testIsCsrfRequestForSameRefererWithUrlHavingPort() {

        boolean checkReqForCsrf = true;
        List<String> allowedRefHost = null;
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn("www.example.com:7070");
        EasyMock.expect(request.getServerName()).andReturn("www.example.com:7070");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
           "http://www.example.com:7070");
        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForSameRefererWithHttpsUrl() {

        boolean checkReqForCsrf = true;
        List<String> allowedRefHost = null;
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn("mail.zimbra.com");
        EasyMock.expect(request.getServerName()).andReturn("mail.zimbra.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
           "https://mail.zimbra.com/zimbra/");
        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForSameRefererWithXFowardedHostHdr() {

        boolean checkReqForCsrf = true;
        List<String> allowedRefHost = null;
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn("mail.zimbra.com");
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
           "https://mail.zimbra.com/zimbra/");
        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequest(request, checkReqForCsrf, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

}
