package com.zimbra.qa.unittest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import junit.framework.Assert;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.WebClientServiceUtil;
import com.zimbra.cs.zimlet.ZimletUtil;

public class TestServiceServlet {
    private static final String NAME_PREFIX = TestUserServlet.class.getSimpleName();
    private static final String USER_NAME = NAME_PREFIX + "user1";
    private static String baseURL; 
    @BeforeClass
    public static void before() throws Exception {
        TestUtil.createAccount(USER_NAME);
        baseURL = TestUtil.getBaseUrl() + "/fromservice/";
    }
        
    private static void cleanup() throws Exception {
        if(TestUtil.accountExists(USER_NAME)) {
            TestUtil.deleteAccount(USER_NAME);
        }
    }
    @AfterClass
    public static void tearDown() throws Exception {
        cleanup();
    }

    @Test
    public void testFlushZimlets() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "flushzimlets";
        GetMethod method = new GetMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        int respCode = HttpClientUtil.executeMethod(client, method);
        //TODO: flushzimlets sometimes fails with error 400 when ran repeatedly
        Assert.assertTrue("Should be getting error code with user's auth token", respCode >= 400);

        method = new GetMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);
    }

    @Test
    public void testFlushSkins() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "flushskins";
        GetMethod method = new GetMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new GetMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);
    }
    
    @Test
    public void testFlushStrings() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "flushuistrings";
        GetMethod method = new GetMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new GetMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);
    }
    
    @Test
    public void testLoadSkins() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "loadskins";
        GetMethod method = new GetMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with user's auth token", HttpStatus.SC_OK, respCode);

        method = new GetMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);
    }
    
    @Test
    public void testLoadLocales() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "loadlocales";
        GetMethod method = new GetMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with user's auth token", HttpStatus.SC_OK, respCode);

        method = new GetMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);
    }
    
    @Test
    public void testDeployZimlet() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "deployzimlet";
        PostMethod method = new PostMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_phone");
        File zimletFile = new File("/opt/zimbra/zimlets/com_zimbra_phone.zip"); //standard zimlet, should always be there. No harm in redeploying it
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setRequestBody(targetStream);
        }
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new PostMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_phone");
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setRequestBody(targetStream);
        }
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);
    }

    @Test
    public void testDeployBadZimletName() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "deployzimlet";
        PostMethod method = new PostMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, "../conf/nginx.key");
        File zimletFile = new File("/opt/zimbra/zimlets/com_zimbra_phone.zip"); //standard zimlet, should always be there. No harm in redeploying it
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setRequestBody(targetStream);
        }
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new PostMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, "../conf/nginx.key");
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setRequestBody(targetStream);
        }
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 400 with super admin's auth token w/o upload", HttpStatus.SC_BAD_REQUEST, respCode);
    }

    @Test
    public void testUnDeployZimlet() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "undeployzimlet";
        PostMethod method = new PostMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_archive");
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new PostMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_archive");
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);
    }
    
    @Test
    public void testUnDeployBadZimletname() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = new HttpClient();
        String url = baseURL + "undeployzimlet";
        PostMethod method = new PostMethod(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, "../conf/nginx.key");
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new PostMethod(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, "../conf/nginx.key");
        respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals("Should be getting code 400 with super admin's auth token", HttpStatus.SC_BAD_REQUEST, respCode);
    }

    private static void addAuthTokenHeader(HttpMethod method, String token) {
        method.addRequestHeader(WebClientServiceUtil.PARAM_AUTHTOKEN, token);
    }
}