package com.zimbra.qa.unittest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.WebClientServiceUtil;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.JaxbUtil;

import junit.framework.Assert;

public class TestServiceServlet {
    private static final String NAME_PREFIX = TestUserServlet.class.getSimpleName();
    private static final String USER_NAME = NAME_PREFIX + "user1";
    private static Account delegatedAdminWithRights;
    private static Account delegatedAdminWithoutRights;
    private static String DELEGATED_ADMIN_WITH_RIGHTS = "TestServiceServletDelegatedAdmin1";
    private static String DELEGATED_ADMIN_WITHOUT_RIGHTS = "TestServiceServletDelegatedAdmin2";
    private static String TEST_ZIMLET = "com_zimbra_unittest";
    private static String TEST_ZIMLET_PATH = "/opt/zimbra/unittest/zimlets/com_zimbra_unittest.zip";
    private static Provisioning prov;
    private static Server localServer;
    private static String baseURL;
    @BeforeClass
    public static void before() throws Exception {
        cleanup();
        TestUtil.createAccount(USER_NAME);
        baseURL = TestUtil.getBaseUrl() + "/fromservice/";
        prov = Provisioning.getInstance();
        localServer = prov.getLocalServer();
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "accountListView");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "downloadsView");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "DLListView");
        delegatedAdminWithoutRights = TestUtil.createAccount(TestUtil.addDomainIfNecessary(DELEGATED_ADMIN_WITHOUT_RIGHTS), attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "accountListView");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "downloadsView");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "DLListView");
        delegatedAdminWithRights = TestUtil.createAccount(TestUtil.addDomainIfNecessary(DELEGATED_ADMIN_WITH_RIGHTS), attrs);

        SoapProvisioning adminSoapProv = TestUtil.newSoapProvisioning();
        TestUtil.grantRightToAdmin(adminSoapProv,
                com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.server.toString()),
                localServer.getName(), delegatedAdminWithRights.getName(), Admin.R_deployZimlet.getName());
        TestUtil.grantRightToAdmin(adminSoapProv,
                com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.server.toString()),
                localServer.getName(), delegatedAdminWithRights.getName(), Admin.R_flushCache.getName());

        HttpGet method = new HttpGet(String.format("%sflushacl",baseURL));
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int code = httpResp.getStatusLine().getStatusCode();
        if(HttpStatus.SC_OK != code) {
            Assert.fail(String.format("Failed to flush all cache in /zimbra web app. Response code: %d", code));
        }
    }

    private static void cleanup() throws Exception {
        if(TestUtil.accountExists(USER_NAME)) {
            TestUtil.deleteAccount(USER_NAME);
        }
        if(TestUtil.accountExists(DELEGATED_ADMIN_WITH_RIGHTS)) {
            TestUtil.deleteAccount(DELEGATED_ADMIN_WITH_RIGHTS);
        }
        if(TestUtil.accountExists(DELEGATED_ADMIN_WITHOUT_RIGHTS)) {
            TestUtil.deleteAccount(DELEGATED_ADMIN_WITHOUT_RIGHTS);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        cleanup();
    }

    private static void addAuthTokenHeader(HttpRequestBase method, String token) {
        method.addHeader(WebClientServiceUtil.PARAM_AUTHTOKEN, token);
    }

    private String getAdminAuthToken(String adminName) throws ServiceException, IOException, HttpException {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                adminName, TestUtil.DEFAULT_PASSWORD);
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        return authResp.getAuthToken();
    }

    private void verifyAdminGET(String url) throws ServiceException, AuthTokenException, HttpException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpGet method = new HttpGet(url);
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code without an auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpGet(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpGet(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpGet(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithRights.getName()));
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with permitted delegated admin's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpGet(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithoutRights.getName()));
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 401 with unpermitted delegated admin's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);
    }

    private void verifyNonAdminGET(String url) throws ServiceException, AuthTokenException, HttpException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpGet method = new HttpGet(url);
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code without an auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpGet(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with user's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpGet(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpGet(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithRights.getName()));
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with permitted delegated admin's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpGet(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithoutRights.getName()));
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with unpermitted delegated admin's auth token", HttpStatus.SC_OK, respCode);
    }

    private void deployAdminVersionCheck() throws AuthTokenException, ServiceException, HttpException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        String url = baseURL + "deployzimlet";
        HttpPost method = new HttpPost(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_adminversioncheck");
        File zimletFile = new File("/opt/zimbra/zimlets/com_zimbra_adminversioncheck.zip"); //standard admin extension, should always be there. No harm in redeploying it
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Super admin should be able to deploy com_zimbra_adminversioncheck admin extension", HttpStatus.SC_OK, respCode);
    }

    @Test
    public void testFlushZimlets() throws Exception {
        String url = baseURL + "flushzimlets";
        verifyAdminGET(url);
    }

    @Test
    public void testFlushSkins() throws Exception {
        String url = baseURL + "flushskins";
        verifyAdminGET(url);
    }

    @Test
    public void testFlushStrings() throws Exception {
        String url = baseURL + "flushuistrings";
        verifyAdminGET(url);
    }

    @Test
    public void testLoadSkins() throws Exception {
        String url = baseURL + "loadskins";
        verifyNonAdminGET(url);
    }

    @Test
    public void testLoadLocales() throws Exception {
        String url = baseURL + "loadlocales";
        verifyNonAdminGET(url);
    }

    @Test
    public void testDeployZimlet() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = HttpClientBuilder.create().build();
        String url = baseURL + "deployzimlet";
        HttpPost method = new HttpPost(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, TEST_ZIMLET);
        File zimletFile = new File(TEST_ZIMLET_PATH);
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, TEST_ZIMLET);
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithRights.getName()));
        method.addHeader(ZimletUtil.PARAM_ZIMLET, TEST_ZIMLET);
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with permitted delegated admin's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithoutRights.getName()));
        method.addHeader(ZimletUtil.PARAM_ZIMLET, TEST_ZIMLET);
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        httpResp= HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 401 with unpermitted delegated admin's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);
    }

    @Test
    public void testDeployBadZimletName() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = HttpClientBuilder.create().build();
        String url = baseURL + "deployzimlet";
        HttpPost method = new HttpPost(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "../conf/nginx.key");
        File zimletFile = new File(TEST_ZIMLET_PATH);
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "../conf/nginx.key");
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 400 with super admin's auth token w/o upload", HttpStatus.SC_BAD_REQUEST, respCode);
    }

    @Test
    public void testUnDeployZimlet() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = HttpClientBuilder.create().build();
        String url = baseURL + "undeployzimlet";
        HttpPost method = new HttpPost(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, TEST_ZIMLET);
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, TEST_ZIMLET);
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with super admin's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithRights.getName()));
        method.addHeader(ZimletUtil.PARAM_ZIMLET, TEST_ZIMLET);
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 200 with permitted delegated admin's auth token", HttpStatus.SC_OK, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithoutRights.getName()));
        method.addHeader(ZimletUtil.PARAM_ZIMLET, TEST_ZIMLET);
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 401 with unpermitted delegated admin's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);
    }

    @Test
    public void testUnDeployBadZimletname() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = HttpClientBuilder.create().build();
        String url = baseURL + "undeployzimlet";
        HttpPost method = new HttpPost(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "../conf/nginx.key");
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "../conf/nginx.key");
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 400 with super admin's auth token", HttpStatus.SC_BAD_REQUEST, respCode);
    }

    @Test
    public void testDeployAdminExtension() throws Exception {
        deployAdminVersionCheck();

        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = HttpClientBuilder.create().build();
        String url = baseURL + "deployzimlet";
        HttpPost method = new HttpPost(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_phone");
        File zimletFile = new File("/opt/zimbra/zimlets/com_zimbra_adminversioncheck.zip"); //standard zimlet, should always be there. No harm in redeploying it
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithRights.getName()));
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_adminversioncheck");
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 401 with permitted delegated admin's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithoutRights.getName()));
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_adminversioncheck");
        if(zimletFile.exists()) {
            InputStream targetStream = new FileInputStream(zimletFile);
            method.setEntity(new InputStreamEntity(targetStream));
        }
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting code 401 with unpermitted delegated admin's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);
    }

    @Test
    public void testUnDeployAdminExtension() throws Exception {
        deployAdminVersionCheck();

        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        HttpClient client = HttpClientBuilder.create().build();
        String url = baseURL + "undeployzimlet";
        HttpPost method = new HttpPost(url);
        addAuthTokenHeader(method, mbox.getAuthToken().getValue());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_adminversioncheck");
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, method);
        int respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Should be getting error code with user's auth token", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithRights.getName()));
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_adminversioncheck");
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Delegated admin should not be allowed to undeploy admin extensions even with deployZimlet right", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, getAdminAuthToken(delegatedAdminWithoutRights.getName()));
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_adminversioncheck");
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Delegated admin should not be allowed to undeploy admin extensions with or without deployZimlet right", HttpStatus.SC_UNAUTHORIZED, respCode);

        method = new HttpPost(url);
        addAuthTokenHeader(method, AuthProvider.getAdminAuthToken().getEncoded());
        method.addHeader(ZimletUtil.PARAM_ZIMLET, "com_zimbra_adminversioncheck");
        httpResp = HttpClientUtil.executeMethod(client, method);
        respCode = httpResp.getStatusLine().getStatusCode();
        Assert.assertEquals("Super admin should be able to undeploy com_zimbra_adminversioncheck admin extension", HttpStatus.SC_OK, respCode);
    }
}
