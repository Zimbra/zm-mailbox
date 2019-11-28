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

package com.zimbra.cs.servlet.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.mailbox.MailboxTestUtil;




/**
 * @author zimbra
 *
 */
public class CsrfUtilTest {

    private static final long AUTH_TOKEN_EXPR = System.currentTimeMillis() + 60 * 1000 * 60;
    private static final int CSRFTOKEN_SALT = 5;
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        byte[] nonce = new byte[16];
        Random nonceGen = new Random();
        nonceGen.nextBytes(nonce);
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, ACCOUNT_ID);
        prov.createAccount("test@zimbra.com", "secret", attrs);

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

        List<String> urlsWithDisabledCsrfCheck = new ArrayList<String>();
        urlsWithDisabledCsrfCheck.add("/AuthRequest");
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getMethod()).andReturn("POST");
        EasyMock.expect(request.getRequestURI()).andReturn(
            "service/soap/AuthRequest");
        try {
            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.doCsrfCheck(request,
                null);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForGet() {

        List<String> urlsWithDisabledCsrfCheck = new ArrayList<String>();
        urlsWithDisabledCsrfCheck.add("/AuthRequest");
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getMethod()).andReturn("GET");
        EasyMock.expect(request.getRequestURI()).andReturn(
            "service/soap/AuthRequest");
        try {
            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.doCsrfCheck(request,
                null);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }


    @Test
    public final void testDecodeValidCsrfToken() {
        try {
            Account acct = Provisioning.getInstance().getAccountByName(
                "test@zimbra.com");
            AuthToken authToken = new ZimbraAuthToken(acct);

            String csrfToken = CsrfUtil.generateCsrfToken(acct.getId(),
                AUTH_TOKEN_EXPR, CSRFTOKEN_SALT, authToken);
            Pair<String, String> tokenParts = CsrfUtil.parseCsrfToken(csrfToken);
            assertNotNull(tokenParts.getFirst());
            assertNotNull(tokenParts.getSecond());
            assertEquals("0", tokenParts.getSecond());

        } catch (ServiceException | AuthTokenException e) {
            fail("Should not throw exception.");
        }
    }

    @Test
    public final void testIsValidCsrfTokenForAccountWithMultipleTokens() {
        try {
            Account acct = Provisioning.getInstance().getAccountByName(
                "test@zimbra.com");
            AuthToken authToken = new ZimbraAuthToken(acct);

            String csrfToken1 = CsrfUtil.generateCsrfToken(acct.getId(),
                AUTH_TOKEN_EXPR, CSRFTOKEN_SALT, authToken);
            boolean validToken = CsrfUtil.isValidCsrfToken(csrfToken1, authToken);
            assertTrue(validToken);


        } catch (ServiceException  e) {
            fail("Should not throw exception.");
        }
    }

    @Test
    public final void testIsValidCsrfTokenForAccountWithNullAuthToken() {
        try {
            Account acct = Provisioning.getInstance().getAccountByName(
                "test@zimbra.com");
            AuthToken authToken = new ZimbraAuthToken(acct);

            String csrfToken1 = CsrfUtil.generateCsrfToken(acct.getId(),
                AUTH_TOKEN_EXPR, CSRFTOKEN_SALT, authToken);
            boolean validToken = CsrfUtil.isValidCsrfToken(csrfToken1, null);
            assertEquals(false, validToken);


        } catch (ServiceException  e) {
            fail("Should not throw exception.");
        }
    }


    @Test
    public final void testIsCsrfRequestWhenCsrfCheckIsTurnedOn() {

        String []  allowedRefHost = new String [1];
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn(
            "example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getMethod()).andReturn("POST");
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(null);

        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequestBasedOnReferrer(request, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForSameReferer() {

        String []  allowedRefHost = new String [1];
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn(
            "www.example.com");
        EasyMock.expect(request.getServerName()).andReturn("www.example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
            "http://www.example.com/zimbra/#15");
        EasyMock.expect(request.getMethod()).andReturn("POST");

        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequestBasedOnReferrer(request, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForRefererInMatchHost() {

        String []  allowedRefHost = new String [3];
        allowedRefHost[0] = "www.newexample.com";
        allowedRefHost[1] = "www.zimbra.com:8080";
        allowedRefHost[2] = "www.abc.com";
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn(
            "example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
            "http://www.newexample.com");
        EasyMock.expect(request.getMethod()).andReturn("POST");

        try {
            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequestBasedOnReferrer(request, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForAllowedRefHostListEmptyAndNonMatchingHost() {

        String []  allowedRefHost = new String [1];
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn(
            "example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
            "http://www.newexample.com");
        EasyMock.expect(request.getMethod()).andReturn("POST");

        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequestBasedOnReferrer(request, allowedRefHost);
            assertEquals(true, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForRefererNotInMatchHost() {

        String []  allowedRefHost = new String [3];
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn(
            "example.com");
        EasyMock.expect(request.getServerName()).andReturn("example.com");
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
            "http://www.newexample.com");
        EasyMock.expect(request.getMethod()).andReturn("POST");

        try {
            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequestBasedOnReferrer(request, allowedRefHost);
            assertEquals(true, csrfReq);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForSameRefererWithUrlHavingPort() {

        String []  allowedRefHost = new String [1];
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn(
            "www.example.com:7070");
        EasyMock.expect(request.getServerName()).andReturn(
            "www.example.com:7070");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
            "http://www.example.com:7070");
        EasyMock.expect(request.getMethod()).andReturn("POST");
        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequestBasedOnReferrer(request, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForSameRefererWithHttpsUrl() {

        String []  allowedRefHost = new String [1];
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(HttpHeaders.HOST)).andReturn(
            "mail.zimbra.com");
        EasyMock.expect(request.getServerName()).andReturn("mail.zimbra.com");
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(null);
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
            "https://mail.zimbra.com/zimbra/");
        EasyMock.expect(request.getMethod()).andReturn("POST");
        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequestBasedOnReferrer(request, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

    @Test
    public final void testIsCsrfRequestForSameRefererWithXFowardedHostHdr() {

        String []  allowedRefHost = new String [1];
        HttpServletRequest request = EasyMock
            .createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader("X-Forwarded-Host")).andReturn(
            "mail.zimbra.com");
        EasyMock.expect(request.getHeader(HttpHeaders.REFERER)).andReturn(
            "https://mail.zimbra.com/zimbra/");
        EasyMock.expect(request.getMethod()).andReturn("POST");
        try {

            EasyMock.replay(request);
            boolean csrfReq = CsrfUtil.isCsrfRequestBasedOnReferrer(request, allowedRefHost);
            assertEquals(false, csrfReq);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not throw exception. ");
        }
    }

}
