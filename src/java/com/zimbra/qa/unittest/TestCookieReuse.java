/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.SoapUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.account.message.EndSessionRequest;
import com.zimbra.soap.account.message.GetInfoRequest;
import com.zimbra.soap.admin.message.CreateAccountRequest;
import com.zimbra.soap.mail.message.SearchRequest;
import com.zimbra.soap.mail.message.SearchResponse;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.SearchHit;

public class TestCookieReuse  {
    private static final String NAME_PREFIX = TestUserServlet.class.getSimpleName();
    private static final String USER_NAME = "user1";
    private static final String UNAUTHORIZED_USER = "unauthorized@example.com";
    private int currentSupportedAuthVersion = 2;
    @Before
    public void setUp()
    throws Exception {
        cleanUp();
        // Add a test message, in case the account is empty.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
    }

    @After
    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        Provisioning.getInstance().getLocalServer().setLowestSupportedAuthVersion(currentSupportedAuthVersion);
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        if(TestUtil.accountExists(UNAUTHORIZED_USER)) {
            TestUtil.deleteAccount(UNAUTHORIZED_USER);
        }
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestCookieReuse.class);
    }

    /**
     * Verify that we can use the cookie for REST session if the session is valid
     */
    @Test
    public void testValidCookie() throws ServiceException, IOException {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");

        HttpClient client = mbox.getHttpClient(uri);
        GetMethod get = new GetMethod(uri.toString());
        int statusCode = HttpClientUtil.executeMethod(client, get);
        assertEquals("This request sohuld succeed. Getting status code " + statusCode, HttpStatus.SC_OK,statusCode);
    }

    /**
     * Verify that we can RE-use the cookie for REST session if the session is valid
     */
    @Test
    public void testValidSessionCookieReuse() throws ServiceException, IOException {
        //establish legitimate connection
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        HttpClient alice = mbox.getHttpClient(uri);
        //create evesdropper's connection
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        Cookie[] cookies = alice.getState().getCookies();
        HttpState state = new HttpState();
        for (int i=0;i<cookies.length;i++) {
            Cookie cookie = cookies[i];
            state.addCookie(new Cookie(uri.getHost(), cookie.getName(), cookie.getValue(), "/", null, false));
        }
        eve.setState(state);
        GetMethod get = new GetMethod(uri.toString());
        int statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK,statusCode);
    }

    /**
     * Verify that we canNOT RE-use the cookie for REST session if the session is valid
     */
    @Test
    public void testAutoEndSession() throws ServiceException, IOException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "TRUE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        HttpClient alice = mbox.getHttpClient(uri);

        //create evesdropper's connection
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        Cookie[] cookies = alice.getState().getCookies();
        HttpState state = new HttpState();
        for (int i=0;i<cookies.length;i++) {
            Cookie cookie = cookies[i];
            state.addCookie(new Cookie(uri.getHost(), cookie.getName(), cookie.getValue(), "/", null, false));
        }
        eve.setState(state);
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(true);

        EndSessionRequest esr = new EndSessionRequest();
        mbox.invokeJaxb(esr);
        GetMethod get = new GetMethod(uri.toString());
        int statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should not succeed. Getting status code " + statusCode, HttpStatus.SC_UNAUTHORIZED,statusCode);
    }

    /**
     * Verify that we canNOT RE-use the cookie taken from a legitimate HTTP session for a REST request after ending the original session
     */
    @Test
    public void testForceEndSession() throws ServiceException, IOException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "FALSE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        HttpClient alice = mbox.getHttpClient(uri);

        //create evesdropper's connection
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        Cookie[] cookies = alice.getState().getCookies();
        HttpState state = new HttpState();
        for (int i=0;i<cookies.length;i++) {
            Cookie cookie = cookies[i];
            state.addCookie(new Cookie(uri.getHost(), cookie.getName(), cookie.getValue(), "/", null, false));
        }
        eve.setState(state);
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(false);

        EndSessionRequest esr = new EndSessionRequest();
        esr.setLogOff(true);
        mbox.invokeJaxb(esr);
        GetMethod get = new GetMethod(uri.toString());
        int statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should not succeed. Getting status code " + statusCode, HttpStatus.SC_UNAUTHORIZED,statusCode);
    }

    /**
     * Verify that we canNOT RE-use the cookie taken from a legitimate HTTP session for a SOAP request after ending the original session
     */
    @Test
    public void testInvalidSearchRequest() throws ServiceException, IOException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "FALSE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        mbox.getHttpClient(uri);
        ZAuthToken authT = mbox.getAuthToken();

        //create evesdropper's SOAP client
        SoapHttpTransport transport = new HttpCookieSoapTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(authT);

        //check that search returns something
        SearchRequest searchReq = new SearchRequest();
        searchReq.setSearchTypes(MailItem.Type.MESSAGE.toString());
        searchReq.setQuery("in:inbox");

        Element req = JaxbUtil.jaxbToElement(searchReq, SoapProtocol.SoapJS.getFactory());
        Element res = transport.invoke(req);
        SearchResponse searchResp = JaxbUtil.elementToJaxb(res);
        List<SearchHit> searchHits = searchResp.getSearchHits();
        assertFalse("this search request should return some conversations", searchHits.isEmpty());


        //explicitly end cookie session
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(false);
        EndSessionRequest esr = new EndSessionRequest();
        esr.setLogOff(true);
        mbox.invokeJaxb(esr);

        //check that search returns nothing
        transport = new HttpCookieSoapTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(authT);
        searchReq = new SearchRequest();
        searchReq.setSearchTypes(MailItem.Type.MESSAGE.toString());
        searchReq.setQuery("in:inbox");
        try {
            req = JaxbUtil.jaxbToElement(searchReq, SoapProtocol.SoapJS.getFactory());
            res = transport.invoke(req);
            searchResp = JaxbUtil.elementToJaxb(res);
            searchHits = searchResp.getSearchHits();
            assertTrue("this search request should fail", searchHits.isEmpty());
        } catch (SoapFaultException ex) {
            assertEquals("Should be getting 'auth required' exception", ServiceException.AUTH_EXPIRED, ex.getCode());
        }
    }

    /**
     * Verify that we canNOT RE-use the cookie for REST session after logging out of plain HTML client
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    @Test
    public void testWebLogOut() throws ServiceException, IOException, URISyntaxException, InterruptedException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "FALSE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        HttpClient alice = mbox.getHttpClient(uri);

        //create evesdropper's connection
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        Cookie[] cookies = alice.getState().getCookies();
        HttpState state = new HttpState();
        for (int i=0;i<cookies.length;i++) {
            Cookie cookie = cookies[i];
            state.addCookie(new Cookie(uri.getHost(), cookie.getName(), cookie.getValue(), "/", null, false));
        }
        eve.setState(state);
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(false);

        URI logoutUri = new URI("http://" + uri.getHost() +  (uri.getPort() > 80 ? (":" + uri.getPort()) : "") + "/?loginOp=logout");
        GetMethod logoutMethod = new GetMethod(logoutUri.toString());
        int statusCode = alice.executeMethod(logoutMethod);
        assertEquals("Log out request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK,statusCode);
        GetMethod get = new GetMethod(uri.toString());
        statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should not succeed. Getting status code " + statusCode, HttpStatus.SC_UNAUTHORIZED,statusCode);
    }

    /**
     * test registering an authtoken
     * @throws Exception
     */
    @Test
    public void testTokenRegistration () throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        Assert.assertTrue("token should be registered", at.isRegistered());
    }

    /**
     * test de-registering an authtoken
     * @throws Exception
     */
    @Test
    public void testTokenDeregistration () throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        Assert.assertTrue("token should be registered", at.isRegistered());
        at.deRegister();
        Assert.assertFalse("token should not be registered", at.isRegistered());
    }


    /**
     * test de-registering an admin authtoken
     * @throws Exception
     */
    @Test
    public void testAdminTokenDeregistration () throws Exception {
        AuthToken at = AuthProvider.getAdminAuthToken();
        Assert.assertTrue("token should be registered", at.isRegistered());
        at.deRegister();
        Assert.assertFalse("token should not be registered", at.isRegistered());
    }

    /**
     * test token being deregistered after it is expired
     * @throws Exception
     */
    @Test
    public void testTokenExpiredTokenDeregistration() throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        ZimbraAuthToken at = new ZimbraAuthToken(a, System.currentTimeMillis() - 1000);

        ZimbraAuthToken at2 = new ZimbraAuthToken(a, System.currentTimeMillis() + 10000);

        Assert.assertFalse("First token should not be registered", at.isRegistered());
        Assert.assertTrue("Second token should be registered", at2.isRegistered());
    }

    /**
     * Test old behavior: tokens appear to be registered even when they are not registered when lowest supported auth version is set to 1
     * @throws Exception
     */
    @Test
    public void testOldClientSupport() throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        ZimbraAuthToken at = new ZimbraAuthToken(a, System.currentTimeMillis() - 1000);
        Assert.assertTrue("token should be registered", at.isRegistered());
        at.deRegister();
        Assert.assertFalse("token should not be registered", at.isRegistered());

        //lowering supported auth version should allow unregistered cookies
        Provisioning.getInstance().getLocalServer().setLowestSupportedAuthVersion(1);
        Assert.assertTrue("token should appear to be registered", at.isRegistered());

        //raising supported auth version should not allow unregistered cookies
        Provisioning.getInstance().getLocalServer().setLowestSupportedAuthVersion(2);
        Assert.assertFalse("token should not be registered", at.isRegistered());
    }

    /**
     * Verify that when zimbraLowestSupportedAuthVersion is set to 2, authtokens get added to LDAP
     * Verify that when zimbraLowestSupportedAuthVersion is set to 1, authtokens do not get added to LDAP
     * @throws Exception
     */
    @Test
    public void testChangingSupportedAuthVersion() throws Exception {
        Provisioning.getInstance().getLocalServer().setLowestSupportedAuthVersion(2);
        Account a = TestUtil.getAccount(USER_NAME);
        String[] tokens = a.getAuthTokens();

        //call constructor to register a token
        ZimbraAuthToken at1 = new ZimbraAuthToken(a, System.currentTimeMillis() + 10000);
        String[] tokens2 = a.getAuthTokens();
        assertEquals("should have one more registered token", tokens.length+1,tokens2.length);

        Provisioning.getInstance().getLocalServer().setLowestSupportedAuthVersion(1);

        //call constructor again. Should not register a token at this time
        ZimbraAuthToken at2 = new ZimbraAuthToken(a, System.currentTimeMillis() + 10000);
        String[] tokens3 = a.getAuthTokens();
        assertEquals("should have the same number of registered tokens as before", tokens2.length,tokens3.length);

    }

    /**
     * Verify that when zimbraForceClearCookies is set to TRUE authtokens get deregistered
     * @throws Exception
     */
    @Test
    public void testClearCookies () throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(true);
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        Assert.assertTrue("token should be registered", at.isRegistered());
        at.deRegister();
        Assert.assertFalse("token should not be registered", at.isRegistered());
    }

    /**
     * Verify that expired authtokens get cleaned up after a new token is created
     * @throws Exception
     */
    @Test
    public void testAuthTokenCleanup() throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        String[] tokensPre = a.getAuthTokens();
        ZimbraAuthToken at1 = new ZimbraAuthToken(a, System.currentTimeMillis() + 1000);
        assertFalse("token should not be expired yet", at1.isExpired());
        String[] tokensPost = a.getAuthTokens();
        assertEquals("should have one more authtoken now", tokensPre.length+1, tokensPost.length);
        Thread.sleep(2000);
        assertTrue("token should have expired by now", at1.isExpired());

        //get a new authtoken, this should clean up the expired one
        ZimbraAuthToken at2 = new ZimbraAuthToken(a, System.currentTimeMillis() + 1000);
        String[] tokensFinal = a.getAuthTokens();
        assertEquals("should have the same nunber of authtoken now as before", tokensPost.length, tokensFinal.length);
    }

    /**
     * Verify that when an expired authtoken has been removed from LDAP, login still succeeds
     * @throws Exception
     */
    @Test
    public void testLoginClearAuthTokensException() throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        ZimbraAuthToken at1 = new ZimbraAuthToken(a, System.currentTimeMillis() + 1000);
        assertFalse("token should not be expired yet", at1.isExpired());
        Thread.sleep(2000);
        assertTrue("token should have expired by now", at1.isExpired());

        //explicitly clean up expired auth tokens
        String[] tokens = a.getAuthTokens();
        for(String tk : tokens) {
            String[] tokenParts = tk.split("\\|");
            if(tokenParts.length > 0) {
                String szExpire = tokenParts[1];
                Long expires = Long.parseLong(szExpire);
                if(System.currentTimeMillis() > expires) {
                    a.removeAuthTokens(tk);
                }
            }
        }

        //verify that AuthRequest still works
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        AccountSelector acctSel = new AccountSelector(com.zimbra.soap.type.AccountBy.name, a.getName());
        AuthRequest req = new AuthRequest(acctSel, "test123");
        Element resp = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        AuthResponse authResp = JaxbUtil.elementToJaxb(resp);
        String newAuthToken = authResp.getAuthToken();
        assertNotNull("should have received a new authtoken", newAuthToken);
        AuthToken at = ZimbraAuthToken.getAuthToken(newAuthToken);
        assertTrue("new auth token should be registered", at.isRegistered());
        assertFalse("new auth token should not be expired yet", at.isExpired());
    }

    /**
     * Verify that a login request bearing a cookie with invalid token will succeed with voidOnExpired header
     * https://bugzilla.zimbra.com/show_bug.cgi?id=95799
     */
    @Test
    public void testReLogin() throws ServiceException, IOException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "FALSE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        mbox.getHttpClient(uri);
        ZAuthToken authT = mbox.getAuthToken();

        //create client
        HttpCookieSoapTransport transport = new HttpCookieSoapTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(authT);
        transport.setVoidAuthTokenOnExpired(true);

        //explicitly end cookie session
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(false);
        EndSessionRequest esr = new EndSessionRequest();
        esr.setLogOff(true);
        mbox.invokeJaxb(esr);

        //check that login request succeeds
        AuthRequest auth = new AuthRequest();
        AccountSelector account = new AccountSelector(com.zimbra.soap.type.AccountBy.name, USER_NAME);
        auth.setAccount(account);
        auth.setPassword("test123");
        auth.setCsrfSupported(false);
        Element req = JaxbUtil.jaxbToElement(auth, SoapProtocol.SoapJS.getFactory());
        Element res = transport.invoke(req);
        AuthResponse authResp = JaxbUtil.elementToJaxb(res);
        assertNotNull("auth request should return auth token", authResp.getAuthToken());
        assertFalse("auth request should return a new auth token", authResp.getAuthToken().equalsIgnoreCase(authT.toString()));
    }

    /**
     * Verify that a login request bearing a cookie with invalid token will NOT succeed without voidOnExpired header
     * https://bugzilla.zimbra.com/show_bug.cgi?id=95799
     */
    @Test
    public void testReLoginFail() throws ServiceException, IOException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "FALSE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        mbox.getHttpClient(uri);
        ZAuthToken authT = mbox.getAuthToken();

        //create client
        HttpCookieSoapTransport transport = new HttpCookieSoapTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(authT);
        transport.setVoidAuthTokenOnExpired(false);

        //explicitly end cookie session
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(false);
        EndSessionRequest esr = new EndSessionRequest();
        esr.setLogOff(true);
        mbox.invokeJaxb(esr);

        //check that login request succeeds
        AuthRequest auth = new AuthRequest();
        AccountSelector account = new AccountSelector(com.zimbra.soap.type.AccountBy.name, USER_NAME);
        auth.setAccount(account);
        auth.setPassword("test123");
        auth.setCsrfSupported(false);
        try {
            Element req = JaxbUtil.jaxbToElement(auth, SoapProtocol.SoapJS.getFactory());
            Element res = transport.invoke(req);
            AuthResponse authResp = JaxbUtil.elementToJaxb(res);
        } catch (SoapFaultException ex) {
            assertEquals("Should be getting 'auth required' exception", ServiceException.AUTH_EXPIRED, ex.getCode());
        }
    }

    /**
     * Verify that EndSessionRequest does not clean up expired tokens
     * @throws Exception
     */
    @Test
    public void testEndSessionNoCleanup() throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        String[] tokensPre = a.getAuthTokens();
        ZimbraAuthToken at1 = new ZimbraAuthToken(a, System.currentTimeMillis() + 1000);
        ZimbraAuthToken at2 = new ZimbraAuthToken(a, System.currentTimeMillis() + 100000);
        assertFalse("First token should not be expired yet", at1.isExpired());
        assertFalse("Second token should not be expired", at2.isExpired());

        String[] tokensPost = a.getAuthTokens();
        assertEquals("should have two more authtoken now", tokensPre.length+2, tokensPost.length);
        Thread.sleep(2000);
        assertTrue("First token should have expired by now", at1.isExpired());
        assertFalse("Second token should not have expired by now", at2.isExpired());

        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        EndSessionRequest esr = new EndSessionRequest();
        esr.setLogOff(true);
        transport.setAuthToken(at2.getEncoded());
        transport.invoke(JaxbUtil.jaxbToElement(esr, SoapProtocol.SoapJS.getFactory()));
        Provisioning.getInstance().reload(a);
        String[] tokensFinal = a.getAuthTokens();
        assertEquals("should have one less authtoken after EndSessionRequest", tokensPost.length-1, tokensFinal.length);

        //verify that AuthRequest still works
        AccountSelector acctSel = new AccountSelector(com.zimbra.soap.type.AccountBy.name, a.getName());
        AuthRequest req = new AuthRequest(acctSel, "test123");
        transport.setAuthToken("");
        Element resp = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        AuthResponse authResp = JaxbUtil.elementToJaxb(resp);
        String newAuthToken = authResp.getAuthToken();
        assertNotNull("should have received a new authtoken", newAuthToken);
        AuthToken at = ZimbraAuthToken.getAuthToken(newAuthToken);
        assertTrue("new auth token should be registered", at.isRegistered());
        assertFalse("new auth token should ne be expired yet", at.isExpired());
    }

    /**
     * Verify that we CANNOT make an unauthorized admin GET request without an admin cookie
     */
    @Test
    public void testGetWithoutAdminCookie() throws Exception {
        int port = 7071;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
        }
        String getServerConfigURL = "https://localhost:" + port + "/service/collectconfig/?host=" + Provisioning.getInstance().getLocalServer().getName();
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(getServerConfigURL);
        int statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should NOT succeed. Getting status code " + statusCode, HttpStatus.SC_UNAUTHORIZED,statusCode);
    }

    /**
     * Verify that we CAN make a GET request by reusing a valid non-csrf-enabled cookie
     */
    @Test
    public void testReuseUserCookieWithoutCsrf() throws Exception {
        AuthToken at = AuthProvider.getAuthToken(TestUtil.getAccount(USER_NAME));
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss&thief=false");
        at.setCsrfTokenEnabled(false);
        GetMethod get = new GetMethod(uri.toString());
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpState state = HttpClientUtil.newHttpState(new ZAuthToken(at.getEncoded()), uri.getHost(), false);
        eve.setState(state);
        eve.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        int statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should succeed. Getting status code " + statusCode + " Response: " + get.getResponseBodyAsString(), HttpStatus.SC_OK,statusCode);
    }

    /**
     * Verify that we CAN make a GET request by reusing a valid CSRF-enabled cookie
     */
    @Test
    public void testReuseUserCookieWithCsrf() throws Exception {
        AuthToken at = AuthProvider.getAuthToken(TestUtil.getAccount(USER_NAME));
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss&thief=true");
        at.setCsrfTokenEnabled(true);
        GetMethod get = new GetMethod(uri.toString());
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpState state = HttpClientUtil.newHttpState(new ZAuthToken(at.getEncoded()), uri.getHost(), false);
        eve.setState(state);
        eve.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        int statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should succeed. Getting status code " + statusCode + " Response: " + get.getResponseBodyAsString(), HttpStatus.SC_OK,statusCode);
    }

    /**
     * Verify that we CANNOT make an admin POST request by reusing a valid csrf-enabled cookie without a csrf token
     */
    @Test
    public void testUnauthorizedAdminPostWithCsrf() throws Exception {
        AuthToken at = AuthProvider.getAdminAuthToken();
        at.setCsrfTokenEnabled(true);
        SoapTransport transport = TestUtil.getAdminSoapTransport();
        transport.setAuthToken(at.getEncoded());
        Map<String, Object> attrs = null;
        CreateAccountRequest request = new CreateAccountRequest(UNAUTHORIZED_USER, "test123", attrs);
        try {
            transport.invoke(JaxbUtil.jaxbToElement(request));
        } catch (ServiceException e) {
            assertEquals("should be catching AUTH EXPIRED here", ServiceException.AUTH_REQUIRED,e.getCode());
            return;
        }
        fail("should have caught an exception");
    }

    /**
     * Verify that we CAN make an admin GET request by reusing a valid csrf-enabled cookie
     */
    @Test
    public void testReuseAdminCookieWithCsrf() throws Exception {
        AuthToken at = AuthProvider.getAdminAuthToken();
        at.setCsrfTokenEnabled(true);
        int port = 7071;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
        }
        String host =  Provisioning.getInstance().getLocalServer().getName();
        String getServerConfigURL = "https://localhost:" + port + "/service/collectconfig/?host=" + host;
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpState state = new HttpState();
        at.encode(state, true, "localhost");
        eve.setState(state);
        GetMethod get = new GetMethod(getServerConfigURL);
        int statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK,statusCode);
    }

    /**
     * Verify that we CAN make an admin GET request by re-using a valid non-csrf-enabled cookie
     */
    @Test
    public void testReuseAdminCookieWithoutCsrf() throws Exception {
        AuthToken at = AuthProvider.getAdminAuthToken();
        at.setCsrfTokenEnabled(false);
        int port = 7071;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
        }
        String host =  Provisioning.getInstance().getLocalServer().getName();
        String getServerConfigURL = "https://localhost:" + port + "/service/collectconfig/?host=" + host;
        HttpClient eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpState state = new HttpState();
        at.encode(state, true, "localhost");
        eve.setState(state);
        GetMethod get = new GetMethod(getServerConfigURL);
        int statusCode = HttpClientUtil.executeMethod(eve, get);
        assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK,statusCode);
    }

    /**
     * Verify that we CANNOT make an POST request with a non-CSRF-enabled auth token if the auth token has an associated CSRF token
     */
    @Test
    public void testForgedNonCSRFPost() throws Exception {
        AuthToken at = AuthProvider.getAuthToken(TestUtil.getAccount(USER_NAME));
        at.setCsrfTokenEnabled(false);
        CsrfUtil.generateCsrfToken(at.getAccountId(), at.getExpires(), new Random().nextInt() + 1, at);
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(at.getEncoded());
        GetInfoRequest request = new GetInfoRequest();
        try {
            transport.invoke(JaxbUtil.jaxbToElement(request));
        } catch (ServiceException e) {
            assertEquals("should be catching AUTH EXPIRED here", ServiceException.AUTH_REQUIRED,e.getCode());
            return;
        }
        fail("should have caught an exception");
    }

    /**
     * Verify that we CANNOT make an admin POST request with a non-CSRF-enabled auth token if the auth token has an associated CSRF token
     */
    @Test
    public void testForgedNonCSRFAdminPost() throws Exception {
        AuthToken at = AuthProvider.getAdminAuthToken();
        at.setCsrfTokenEnabled(false);
        CsrfUtil.generateCsrfToken(at.getAccountId(), at.getExpires(), new Random().nextInt() + 1, at);
        SoapTransport transport = TestUtil.getAdminSoapTransport();
        transport.setAuthToken(at.getEncoded());
        Map<String, Object> attrs = null;
        CreateAccountRequest request = new CreateAccountRequest(UNAUTHORIZED_USER, "test123", attrs);
        try {
            transport.invoke(JaxbUtil.jaxbToElement(request));
        } catch (ServiceException e) {
            assertEquals("should be catching AUTH EXPIRED here", ServiceException.AUTH_REQUIRED,e.getCode());
            return;
        }
        fail("should have caught an exception");
    }

    /**
     * version of SOAP transport that uses HTTP cookies instead of SOAP message header for transporting Auth Token
     * @author gsolovyev
     */
    private class HttpCookieSoapTransport extends SoapHttpTransport {
        private boolean voidAuthTokenOnExpired = false;

        public void setVoidAuthTokenOnExpired(boolean voidToken) {
            voidAuthTokenOnExpired = voidToken;
        }

        public HttpCookieSoapTransport(String uri) {
            super(uri);
        }

          @Override
        protected final Element generateSoapMessage(Element document, boolean raw, boolean noSession,
                    String requestedAccountId, String changeToken, String tokenType) {

                // don't use the default protocol version if it's incompatible with the passed-in request
                SoapProtocol proto = getRequestProtocol();
                if (proto == SoapProtocol.SoapJS) {
                    if (document instanceof XMLElement)
                        proto = SoapProtocol.Soap12;
                } else {
                    if (document instanceof JSONElement)
                        proto = SoapProtocol.SoapJS;
                }
                SoapProtocol responseProto = getResponseProtocol() == null ? proto : getResponseProtocol();

                String targetId = requestedAccountId != null ? requestedAccountId : getTargetAcctId();
                String targetName = targetId == null ? getTargetAcctName() : null;

                Element context = null;
                context = SoapUtil.toCtxt(proto, null, null);
                if (noSession) {
                    SoapUtil.disableNotificationOnCtxt(context);
                } else {
                    SoapUtil.addSessionToCtxt(context, getAuthToken() == null ? null : getSessionId(), getMaxNotifySeq());
                }
                SoapUtil.addTargetAccountToCtxt(context, targetId, targetName);
                SoapUtil.addChangeTokenToCtxt(context, changeToken, tokenType);
                SoapUtil.addUserAgentToCtxt(context, getUserAgentName(), getUserAgentVersion());
                if (responseProto != proto) {
                    SoapUtil.addResponseProtocolToCtxt(context, responseProto);
                }

                if (voidAuthTokenOnExpired) {
                    Element eAuthTokenControl = context.addElement(HeaderConstants.E_AUTH_TOKEN_CONTROL);
                    eAuthTokenControl.addAttribute(HeaderConstants.A_VOID_ON_EXPIRED, true);
                }

                Element envelope = proto.soapEnvelope(document, context);

                return envelope;
            }
    }
}
