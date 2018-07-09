/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
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

public class TestCookieReuse {

    @Rule
    public TestName testInfo = new TestName();

    private static final String NAME_PREFIX = TestUserServlet.class.getSimpleName();
    private static String USER_NAME;
    private static String UNAUTHORIZED_USER;
    private int currentSupportedAuthVersion;

    @Before
    public void setUp()
    throws Exception {
        currentSupportedAuthVersion =
            Provisioning.getInstance().getLocalServer().getLowestSupportedAuthVersion();

        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName().toLowerCase() + "-";
        USER_NAME = prefix + "user1";
        UNAUTHORIZED_USER = AccountTestUtil.getAddress(prefix + "unauthorized");
        cleanUp();
        TestUtil.createAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        // Add a test message, to make sure the account isn't empty
        TestUtil.addMessage(mbox, NAME_PREFIX);
    }

    @After
    public void tearDown()
    throws Exception {
        cleanUp();
        Provisioning.getInstance().getLocalServer().setLowestSupportedAuthVersion(currentSupportedAuthVersion);
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(UNAUTHORIZED_USER);
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
    public void testValidCookie() throws ServiceException, IOException, HttpException {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");

        HttpClient client = mbox.getHttpClient(uri);
        HttpGet get = new HttpGet(uri.toString());
        HttpResponse response = HttpClientUtil.executeMethod(client, get);
        int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should succeed. Getting status code " + statusCode,
                HttpStatus.SC_OK, statusCode);
    }

    /**
     * Verify that we can RE-use the cookie for REST session if the session is valid
     */
    @Test
    // TO DO fix the compilation error
    public void testValidSessionCookieReuse() throws ServiceException, IOException {
        //establish legitimate connection
//        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
//        URI uri = mbox.getRestURI("Inbox?fmt=rss");
//        DefaultHttpClient alice = mbox.getHttpClient(uri);
//        //create evesdropper's connection
//        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
//        Cookie[] cookies = alice.get
//        BasicCookieStore cookieStore = new BasicCookieStore();
//        for (int i=0;i<cookies.length;i++) {
//
//            BasicClientCookie cookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
//            cookie.setDomain(uri.getHost());
//            cookie.setPath("/");
//            cookie.setSecure(false);
//            cookieStore.addCookie(cookie);
//        }
//        eve.setDefaultCookieStore(cookieStore);
//        HttpGet get = new HttpGet(uri.toString());
//        HttpResponse response = HttpClientUtil.executeMethod(eve.build(), get);
//        int statusCode = response.getStatusLine().getStatusCode();
//        Assert.assertEquals("This request should succeed. Getting status code " + statusCode,
//                HttpStatus.SC_OK, statusCode);
    }

    /**
     * Verify that we canNOT RE-use the cookie for REST session if the session is valid
     * @throws HttpException 
     */
    @Test
    public void testAutoEndSession() throws ServiceException, IOException, HttpException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "TRUE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        HttpClientContext context = HttpClientContext.create();
        HttpClient alice = mbox.getHttpClient(uri);

        //create evesdropper's connection
        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        List<Cookie> cookies = context.getCookieStore().getCookies();
        BasicCookieStore cookieStore = new BasicCookieStore();
        for (Cookie cookie : cookies) {
            BasicClientCookie basicCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            basicCookie.setDomain(uri.getHost());
            basicCookie.setPath("/");
            basicCookie.setSecure(false);
            cookieStore.addCookie(cookie);
        }
        eve.setDefaultCookieStore(cookieStore);
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(true);

        EndSessionRequest esr = new EndSessionRequest();
        mbox.invokeJaxb(esr);
        HttpGet get = new HttpGet(uri.toString());
        HttpResponse response = HttpClientUtil.executeMethod(eve.build(), get, context);
        int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should not succeed. Getting status code " + statusCode,
                HttpStatus.SC_UNAUTHORIZED, statusCode);
    }

    /**
     * Verify that we canNOT RE-use the cookie taken from a legitimate HTTP session for a REST request
     * after ending the original session
     * @throws HttpException 
     */
    @Test
    public void testForceEndSession() throws ServiceException, IOException, HttpException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "FALSE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        HttpClient alice = mbox.getHttpClient(uri);
        HttpClientContext context = HttpClientContext.create();

        //create evesdropper's connection
        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        List<Cookie> cookies = context.getCookieStore().getCookies();
        BasicCookieStore cookieStore = new BasicCookieStore();
        for (Cookie cookie : cookies) {
            BasicClientCookie basicCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            basicCookie.setDomain(uri.getHost());
            basicCookie.setPath("/");
            basicCookie.setSecure(false);
            cookieStore.addCookie(cookie);
        }
        eve.setDefaultCookieStore(cookieStore);
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(false);

        EndSessionRequest esr = new EndSessionRequest();
        esr.setLogOff(true);
        mbox.invokeJaxb(esr);
        HttpGet get = new HttpGet(uri.toString());
        HttpResponse response = HttpClientUtil.executeMethod(eve.build(), get);
        int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should not succeed. Getting status code " + statusCode,
                HttpStatus.SC_UNAUTHORIZED, statusCode);
    }

    /**
     * Verify that we canNOT RE-use the cookie taken from a legitimate HTTP session for a SOAP request after
     * ending the original session
     */
    @Test
    public void testInvalidSearchRequest() throws ServiceException, IOException, HttpException {
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
        Assert.assertFalse("this search request should return some conversations", searchHits.isEmpty());


        //explicitely end cookie session
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
            Assert.assertTrue("this search request should fail", searchHits.isEmpty());
        } catch (SoapFaultException ex) {
            Assert.assertEquals("Should be getting 'auth required' exception",
                    ServiceException.AUTH_EXPIRED, ex.getCode());
        }
    }

    /**
     * Verify that we canNOT RE-use the cookie for REST session after logging out of plain HTML client
     * @throws URISyntaxException
     * @throws InterruptedException
     * @throws HttpException 
     */
    @Test
    public void testWebLogOut() throws ServiceException, IOException, URISyntaxException, InterruptedException, HttpException {
        //establish legitimate connection
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraForceClearCookies, "FALSE");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getRestURI("Inbox?fmt=rss");
        HttpClientContext context = HttpClientContext.create();
        HttpClient alice = mbox.getHttpClient(uri);

        //create evesdropper's connection
        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        List<Cookie> cookies = context.getCookieStore().getCookies();
        BasicCookieStore cookieStore = new BasicCookieStore();
        for (Cookie cookie : cookies) {
            BasicClientCookie basicCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            basicCookie.setDomain(uri.getHost());
            basicCookie.setPath("/");
            basicCookie.setSecure(false);
            cookieStore.addCookie(cookie);
        }
        eve.setDefaultCookieStore(cookieStore);
        Account a = TestUtil.getAccount(USER_NAME);
        a.setForceClearCookies(false);
        URI logoutUri = new URI(String.format("%s://%s%s/?loginOp=logout",
                uri.getScheme(), uri.getHost(), (uri.getPort() > 80 ? (":" + uri.getPort()) : "")));
        HttpGet logoutMethod = new HttpGet(logoutUri.toString());
        HttpResponse httpResp = alice.execute(logoutMethod);
        int statusCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Log out request should succeed. Getting status code " + statusCode,
                HttpStatus.SC_OK, statusCode);
        
        HttpGet get = new HttpGet(uri.toString());
        httpResp = HttpClientUtil.executeMethod(eve.build(), get, context);
        statusCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should not succeed. Getting status code " + statusCode,
                HttpStatus.SC_UNAUTHORIZED, statusCode);
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
     * Test old behavior: tokens appear to be registered even when they are not registered when lowest
     * supported auth version is set to 1
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
     * Verify that when an expired authtoken has been removed from LDAP, login still succeeds
     * @throws Exception
     */
    @Test
    public void testLoginClearAuthTokensException() throws Exception {
        Account a = TestUtil.getAccount(USER_NAME);
        ZimbraAuthToken at1 = new ZimbraAuthToken(a, System.currentTimeMillis() + 1000);
        Assert.assertFalse("token should not be expired yet", at1.isExpired());
        Thread.sleep(2000);
        Assert.assertTrue("token should have expired by now", at1.isExpired());

        //explicitely clean up expired auth tokens
        a.purgeAuthTokens();

        //verify that AuthRequest still works
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        AccountSelector acctSel = new AccountSelector(com.zimbra.soap.type.AccountBy.name, a.getName());
        AuthRequest req = new AuthRequest(acctSel, "test123");
        Element resp = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        AuthResponse authResp = JaxbUtil.elementToJaxb(resp);
        String newAuthToken = authResp.getAuthToken();
        Assert.assertNotNull("should have received a new authtoken", newAuthToken);
        AuthToken at = ZimbraAuthToken.getAuthToken(newAuthToken);
        Assert.assertTrue("new auth token should be registered", at.isRegistered());
        Assert.assertFalse("new auth token should not be expired yet", at.isExpired());
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
        String getServerConfigURL = "https://localhost:" + port + "/service/collectconfig/?host="
                                        + Provisioning.getInstance().getLocalServer().getName();
        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpGet get = new HttpGet(getServerConfigURL);
        HttpResponse httpResp = HttpClientUtil.executeMethod(eve.build(), get);
        int statusCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should NOT succeed. Getting status code " + statusCode,
                HttpStatus.SC_UNAUTHORIZED, statusCode);
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
        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        BasicCookieStore state = new BasicCookieStore();
        at.encode(state, true, "localhost");
        eve.setDefaultCookieStore(state);
        HttpGet get = new HttpGet(getServerConfigURL);
        HttpResponse httpResp = HttpClientUtil.executeMethod(eve.build(), get);
        int statusCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should succeed. Getting status code " + statusCode,
                HttpStatus.SC_OK, statusCode);
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
        HttpGet get = new HttpGet(uri.toString());
        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        BasicCookieStore state = HttpClientUtil.newHttpState(new ZAuthToken(at.getEncoded()), uri.getHost(), false);
        eve.setDefaultCookieStore(state);
        RequestConfig reqConfig = RequestConfig.copy(
            ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
            .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

       eve.setDefaultRequestConfig(reqConfig);

       HttpResponse httpResp = HttpClientUtil.executeMethod(eve.build(), get);
       int statusCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should succeed. Getting status code "
                    + statusCode + " Response: " + httpResp.getEntity().getContent(),
                    HttpStatus.SC_OK, statusCode);
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
        HttpGet get = new HttpGet(uri.toString());
        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        BasicCookieStore state = HttpClientUtil.newHttpState(new ZAuthToken(at.getEncoded()), uri.getHost(), false);
        eve.setDefaultCookieStore(state);
        RequestConfig reqConfig = RequestConfig.copy(
            ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
            .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();
       eve.setDefaultRequestConfig(reqConfig);
       HttpResponse httpResp = HttpClientUtil.executeMethod(eve.build(), get);
       int statusCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should succeed. Getting status code "
                + statusCode + " Response: " + EntityUtils.toString(httpResp.getEntity()),
                HttpStatus.SC_OK, statusCode);
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
        HttpClientBuilder eve = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        BasicCookieStore state = new BasicCookieStore();
        at.encode(state, true, "localhost");
        eve.setDefaultCookieStore(state);
        eve.setDefaultCookieStore(state);
        HttpGet get = new HttpGet(getServerConfigURL);
        HttpResponse httpResp = HttpClientUtil.executeMethod(eve.build(), get);
        int statusCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("This request should succeed. Getting status code " + statusCode,
                HttpStatus.SC_OK, statusCode);
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
            Assert.assertEquals("should be catching AUTH EXPIRED here", ServiceException.AUTH_REQUIRED,e.getCode());
            return;
        }
        Assert.fail("should have caught an exception");
    }

    /**
     * Verify that we CANNOT make an POST request with a non-CSRF-enabled auth token if the auth token
     * has an associated CSRF token
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
            Assert.assertEquals("should be catching AUTH EXPIRED here", ServiceException.AUTH_REQUIRED,e.getCode());
            return;
        }
        Assert.fail("should have caught an exception");
    }


    /**
     * Verify that we CANNOT make an admin POST request with a non-CSRF-enabled auth token if
     * the auth token has an associated CSRF token
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
            Assert.assertEquals("should be catching AUTH EXPIRED here", ServiceException.AUTH_REQUIRED,e.getCode());
            return;
        }
        Assert.fail("should have caught an exception");
    }

    /**
     * version of SOAP transport that uses HTTP cookies instead of SOAP message header for transporting Auth Token
     * @author gsolovyev
     */
    private class HttpCookieSoapTransport extends SoapHttpTransport {

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
                if (generateContextHeader()) {
                    context = SoapUtil.toCtxt(proto, null, null);
                    if (noSession) {
                        SoapUtil.disableNotificationOnCtxt(context);
                    } else {
                        SoapUtil.addSessionToCtxt(context, getAuthToken() == null ? null : getSessionId(),
                                getMaxNotifySeq());
                    }
                    SoapUtil.addTargetAccountToCtxt(context, targetId, targetName);
                    SoapUtil.addChangeTokenToCtxt(context, changeToken, tokenType);
                    SoapUtil.addUserAgentToCtxt(context, getUserAgentName(), getUserAgentVersion());
                    if (responseProto != proto) {
                        SoapUtil.addResponseProtocolToCtxt(context, responseProto);
                    }

                }
                Element envelope = proto.soapEnvelope(document, context);
                return envelope;
            }
    }
}