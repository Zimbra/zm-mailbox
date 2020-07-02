package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.service.admin.DeployZimlet;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.DeployZimletRequest;
import com.zimbra.soap.admin.message.DeployZimletResponse;
import com.zimbra.soap.admin.message.UndeployZimletRequest;
import com.zimbra.soap.admin.message.UndeployZimletResponse;
import com.zimbra.soap.admin.type.AttachmentIdAttrib;

/**
 * copy test data from data/unittest to /opt/zimbra/unittest before running this test 
 * 
 */
public class TestDeployZimlet {
    private static String ADMIN_UPLOAD_URL;
    private static Provisioning prov;
    private static Server localServer;
    private static Account delegatedAdmin;
    private static String DELEGATED_ADMIN = "TestDeployZimletDelegatedAdmin";
    @BeforeClass
    public static void init() throws Exception {
        int port = 7071;
        prov = Provisioning.getInstance();
        localServer = prov.getLocalServer();
        port = localServer.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        ADMIN_UPLOAD_URL = "https://" + localServer.getServiceHostname() + ":" + port + "/service/upload?fmt=raw";
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "accountListView");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "downloadsView");
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "DLListView");
        
        delegatedAdmin = TestUtil.createAccount(TestUtil.addDomainIfNecessary(DELEGATED_ADMIN), attrs);
        TestUtil.grantRightToAdmin(TestUtil.newSoapProvisioning(), com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.server
                .toString()), localServer.getName(), delegatedAdmin.getName(), Admin.R_deployZimlet.getName());
        File rogueFile = new File("/opt/zimbra/conf/rogue.file");
        if(rogueFile.exists()) {
            rogueFile.delete();
        }
        File legitFile = new File("/opt/zimbra/zimlets-deployed/absolute/opt/zimbra/conf/rogue.file");
        if(legitFile.exists()) {
            legitFile.delete();
        }
    }

    @AfterClass
    public static void after() throws Exception {
        if(TestUtil.accountExists(DELEGATED_ADMIN)) {
            TestUtil.deleteAccount(DELEGATED_ADMIN);
        }
    }
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testValidZimlet() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();

        String aid = adminUpload(authToken, "com_zimbra_mailarchive.zip", "/opt/zimbra/zimlets/com_zimbra_mailarchive.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        Element res = transport.invoke(req);
        DeployZimletResponse deployResp =  JaxbUtil.elementToJaxb(res);
        assertNotNull(deployResp);
        String status = deployResp.getProgresses().get(0).getStatus();
        assertTrue("should be getting 'pending' or 'succeeded' status", status.equals(DeployZimlet.sPENDING) || status.equals(DeployZimlet.sSUCCEEDED)); 

        int waitMs = 10000;
        while(waitMs > 0) {
            DeployZimletRequest statusReq = new DeployZimletRequest(AdminConstants.A_STATUS, false, true, att);
            req = JaxbUtil.jaxbToElement(statusReq);
            res = transport.invoke(req);
            DeployZimletResponse statusResp =  JaxbUtil.elementToJaxb(res);
            assertNotNull(statusResp);
            status = statusResp.getProgresses().get(0).getStatus();
            assertTrue("should be getting 'pending' or 'succeeded' status", status.equals(DeployZimlet.sPENDING) || status.equals(DeployZimlet.sSUCCEEDED));
            if(status.equals(DeployZimlet.sSUCCEEDED)) {
                break;
            }
            Thread.sleep(500);
            waitMs -=500;
        }
        assertTrue("should be getting 'succeeded' status. Status is: " + status, status.equals(DeployZimlet.sSUCCEEDED));
    }

    @Test
    public void testEmptyAid() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        AttachmentIdAttrib att = new AttachmentIdAttrib("");
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testNoAid() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, null);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testBadAid() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();

        AttachmentIdAttrib att = new AttachmentIdAttrib("invalidaid");
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testInvalidAction() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();

        String aid = adminUpload(authToken, "com_zimbra_mailarchive.zip", "/opt/zimbra/zimlets/com_zimbra_mailarchive.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest("invalidaction", false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testBadZimletName() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String aid = adminUpload(authToken, "attack.zip", "/opt/zimbra/unittest/zimlets/attack.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testValidAdminExtension() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String aid = adminUpload(authToken, "adminextension.zip", "/opt/zimbra/unittest/zimlets/adminextension.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        Element res = transport.invoke(req);
        DeployZimletResponse deployResp =  JaxbUtil.elementToJaxb(res);
        assertNotNull(deployResp);
        String status = deployResp.getProgresses().get(0).getStatus();
        assertTrue("should be getting 'pending' or 'succeeded' status", status.equals(DeployZimlet.sPENDING) || status.equals(DeployZimlet.sSUCCEEDED));

        UndeployZimletRequest undeployReq = new UndeployZimletRequest("adminextension", AdminConstants.A_DEPLOYALL);
        req = JaxbUtil.jaxbToElement(undeployReq);
        res = transport.invoke(req);
        UndeployZimletResponse undeployResp =  JaxbUtil.elementToJaxb(res);
        assertNotNull(undeployResp);
    }

    @Test
    public void testAdminExtensionDelegatedAdmin() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                delegatedAdmin.getName(), TestUtil.DEFAULT_PASSWORD);
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String aid = adminUpload(authToken, "adminextension.zip", "/opt/zimbra/unittest/zimlets/adminextension.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            DeployZimletResponse deployResp = JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException. Instead received " + deployResp.toString());
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testUndeployBadName() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        UndeployZimletRequest undeployReq = new UndeployZimletRequest("../data/something", AdminConstants.A_DEPLOYALL);
        Element req = JaxbUtil.jaxbToElement(undeployReq);
        try {
            Element res = transport.invoke(req);
            UndeployZimletResponse undeployResp = JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException. Instead received " + undeployResp.toString());
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testZimletDelegatedAdmin() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                delegatedAdmin.getName(), TestUtil.DEFAULT_PASSWORD);
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String aid = adminUpload(authToken, "com_zimbra_mailarchive.zip", "/opt/zimbra/zimlets/com_zimbra_mailarchive.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        Element res = transport.invoke(req);
        DeployZimletResponse deployResp =  JaxbUtil.elementToJaxb(res);
        assertNotNull(deployResp);
        String status = deployResp.getProgresses().get(0).getStatus();
        assertTrue("should be getting 'pending' or 'succeeded' status", status.equals(DeployZimlet.sPENDING) || status.equals(DeployZimlet.sSUCCEEDED)); 
    }

    @Test
    public void testZipWithTraversal() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String aid = adminUpload(authToken, "attack.zip", "/opt/zimbra/unittest/zimlets/com_zimbra_url.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testZipWithInvalidCharacter() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String aid = adminUpload(authToken, "jelmer.zip", "/opt/zimbra/unittest/zimlets/jelmer.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }
    }

    @Test
    public void testZipWithAbsolutePath() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String aid = adminUpload(authToken, "absolute.zip", "/opt/zimbra/unittest/zimlets/absolute.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }

        //check that file did not get extracted to absolute path
        File rogueFile = new File("/opt/zimbra/conf/rogue.file");
        assertFalse("/opt/zimbra/conf/rogue.file should not have been created", rogueFile.exists());
    }

    @Test
    public void testPhilsZip() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest authReq = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        authReq.setCsrfSupported(false);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(authReq, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String aid = adminUpload(authToken, "phil.zip", "/opt/zimbra/unittest/zimlets/phil.zip");
        assertNotNull("Attachment ID should not be null", aid);

        AttachmentIdAttrib att = new AttachmentIdAttrib(aid);
        transport.setAdmin(true);
        transport.setAuthToken(authToken);
        DeployZimletRequest deployReq = new DeployZimletRequest(AdminConstants.A_DEPLOYLOCAL, false, true, att);
        Element req = JaxbUtil.jaxbToElement(deployReq);
        try {
            Element res = transport.invoke(req);
            JaxbUtil.elementToJaxb(res);
            fail("Should throw SoapFaultException");
        } catch (SoapFaultException e) {
            //expected
        }
    }

    public String adminUpload(String authToken, String fileName, String filePath) throws Exception {
        HttpPost post = new HttpPost(ADMIN_UPLOAD_URL);
        
        String contentType = "application/x-msdownload";
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        BasicCookieStore state = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie(ZimbraCookie.authTokenCookieName(true), authToken);
        cookie.setDomain(localServer.getServiceHostname());
        cookie.setPath("/");
        cookie.setSecure(false);
        state.addCookie(cookie);

        clientBuilder.setDefaultCookieStore(state);
        RequestConfig reqConfig = RequestConfig.copy(
            ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
            .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();
        clientBuilder.setDefaultRequestConfig(reqConfig);
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(fileName, new File(filePath), ContentType.create(contentType), fileName);
        HttpEntity httpEntity = builder.build();
        post.setEntity(httpEntity);
        HttpClient client = clientBuilder.build();
        
       
        HttpResponse response = HttpClientUtil.executeMethod(client, post);
        int statusCode = response.getStatusLine().getStatusCode();
        assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK, statusCode);
        String resp = EntityUtils.toString(response.getEntity());
        assertNotNull("Response should not be empty", resp);
        ZimbraLog.test.debug("Upload response " + resp);
        String[] responseParts = resp.split(",", 3);
        String aid = null;
        if (responseParts.length == 3) {
            aid = responseParts[2].trim();
            if (aid.startsWith("'") || aid.startsWith("\"")) {
                aid = aid.substring(1);
            }
            if (aid.endsWith("'") || aid.endsWith("\"")) {
                aid = aid.substring(0, aid.length() - 1);
            }
        }
        return aid;
    }
}