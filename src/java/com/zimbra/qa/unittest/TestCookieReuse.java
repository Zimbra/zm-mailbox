package com.zimbra.qa.unittest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.commons.httpclient.HttpStatus;

import com.zimbra.client.ZDocument;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.account.message.EndSessionRequest;

import junit.framework.TestCase;

public class TestCookieReuse extends TestCase {
    private static final String NAME_PREFIX = TestUserServlet.class.getSimpleName();
    private static final String USER_NAME = "user1";


    @Override
    public void setUp()
    throws Exception {
        cleanUp();
        // Add a test message, in case the account is empty.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
    }

    @Override
    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestCookieReuse.class);
    }
    
    /**
     * Verify that we can use the cookie for REST session if the session is valid
     */
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
     * Verify that we canNOT RE-use the cookie for REST session if the session is valid
     */
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
     * Verify that we canNOT RE-use the cookie for REST session if the session is valid
     * @throws URISyntaxException 
     * @throws InterruptedException 
     */
    public void testLogOut() throws ServiceException, IOException, URISyntaxException, InterruptedException {
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
    @Test
    public void testTokenRegistration () throws Exception {
    	Account a = TestUtil.getAccount(USER_NAME);
    	ZimbraAuthToken at = new ZimbraAuthToken(a);
    	at.register();
    	Assert.assertTrue("token should be registered", at.isRegistered());
    }

    @Test
    public void testTokenDeregistration () throws Exception {
    	Account a = TestUtil.getAccount(USER_NAME);
    	ZimbraAuthToken at = new ZimbraAuthToken(a);
    	at.register();
    	Assert.assertTrue("token should be registered", at.isRegistered());
    	at.deRegister();
    	Assert.assertFalse("token should not be registered", at.isRegistered());
    }
    
    @Test
    public void testTokenExpiredTokenDeregistration() throws Exception {
    	Account a = TestUtil.getAccount(USER_NAME);
    	ZimbraAuthToken at = new ZimbraAuthToken(a, System.currentTimeMillis() - 1000);
    	at.register();
    	
    	ZimbraAuthToken at2 = new ZimbraAuthToken(a, System.currentTimeMillis() + 10000);
    	at2.register();
    	
    	Assert.assertTrue("First token should be registered", at.isRegistered());
    	Assert.assertTrue("Second token should be registered", at2.isRegistered());
    	a.cleanExpiredTokens();
    	Assert.assertFalse("First token should not be registered after cleanup", at.isRegistered());
    	Assert.assertTrue("Second token should be registered after cleanup", at2.isRegistered());
    }
    
    @Test
    public void testClearCookies () throws Exception {
    	Account a = TestUtil.getAccount(USER_NAME);
    	a.setForceClearCookies(true);
    	ZimbraAuthToken at = new ZimbraAuthToken(a);
    	at.register();
    	Assert.assertTrue("token should be registered", at.isRegistered());
    	at.deRegister();
    	Assert.assertFalse("token should not be registered", at.isRegistered());	
    }
}
