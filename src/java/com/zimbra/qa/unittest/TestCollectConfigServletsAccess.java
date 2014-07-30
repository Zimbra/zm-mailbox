package com.zimbra.qa.unittest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.type.TargetBy;
/**
 *
 * @author gsolovyev
 * CollectLDAPConfigServlet should be accessible only by global admins: https://bugzilla.zimbra.com/show_bug.cgi?id=93800
 */
public class TestCollectConfigServletsAccess extends TestCase {

    private static String TEST_ADMIN_NAME = "TestAdmin";
    private static String PASSWORD = "test123";

    @Override
    public void setUp() throws Exception {
        cleanUp();

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, ProvisioningConstants.TRUE);
        attrs.put("displayName", "delegated admin for a unit tests");
        Provisioning.getInstance().createAccount(TestUtil.getAddress(TEST_ADMIN_NAME), PASSWORD, attrs);
        Provisioning.getInstance().grantRight(TargetType.domain.getCode(), TargetBy.name, TestUtil.getDomain(), com.zimbra.cs.account.accesscontrol.GranteeType.GT_USER
                .getCode(), GranteeBy.name, TestUtil.getAddress(TEST_ADMIN_NAME),null, RightConsts.RT_domainAdminConsoleRights,null);
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, TestUtil.getAddress(TEST_ADMIN_NAME));
        if (account != null) {
            prov.deleteAccount(account.getId());
        }
    }

    /**
     * Verify that global admin can access servlet at /service/collectconfig/
     * @throws Exception
     */
  /*  @Test
    public void testConfigGlobalAdmin() throws Exception {
        ZAuthToken at = TestUtil.getAdminSoapTransport().getAuthToken();
        URI servletURI = new URI(getConfigServletUrl());
        HttpState initialState = HttpClientUtil.newHttpState(at, servletURI.getHost(), true);
        HttpClient restClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        restClient.setState(initialState);
        restClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        GetMethod get = new GetMethod(servletURI.toString());
        int statusCode = HttpClientUtil.executeMethod(restClient, get);
        if(statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            fail("collectconfig servlet is failing. Likely Zimbra SSH access is not properly configured. " + get.getResponseHeader("X-Zimbra-Fault-Message").getValue());
        } else {
            assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK,statusCode);
        }
    }*/


    /**
     * Verify that delegated admin canNOT access servlet at /service/collectconfig/
     * @throws Exception
     */
    @Test
    public void testConfigDelegatedAdmin() throws Exception {
        ZAuthToken at = TestUtil.getAdminSoapTransport(TEST_ADMIN_NAME,PASSWORD).getAuthToken();
        URI servletURI = new URI(getConfigServletUrl());
        HttpState initialState = HttpClientUtil.newHttpState(at, servletURI.getHost(), true);
        HttpClient restClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        restClient.setState(initialState);
        restClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        GetMethod get = new GetMethod(servletURI.toString());
        int statusCode = HttpClientUtil.executeMethod(restClient, get);
        assertEquals("This request should NOT succeed. Getting status code " + statusCode, HttpStatus.SC_UNAUTHORIZED,statusCode);
    }

    /**
     * Verify that global admin can access servlet at /service/collectldapconfig/
     * @throws Exception
     */
  /*  @Test
    public void testLDAPConfigGlobalAdmin() throws Exception {
        ZAuthToken at = TestUtil.getAdminSoapTransport().getAuthToken();
        URI servletURI = new URI(getLDAPConfigServletUrl());
        HttpState initialState = HttpClientUtil.newHttpState(at, servletURI.getHost(), true);
        HttpClient restClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        restClient.setState(initialState);
        restClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        GetMethod get = new GetMethod(servletURI.toString());
        int statusCode = HttpClientUtil.executeMethod(restClient, get);
        if(statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            fail("collectldapconfig servlet is failing. Likely Zimbra SSH access is not properly configured. " + get.getResponseHeader("X-Zimbra-Fault-Message").getValue());
        } else {
            assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK,statusCode);
        }
    }*/

    /**
     * Verify that delegated admin canNOT access servlet at /service/collectldapconfig/
     * @throws Exception
     */
    @Test
    public void testLDAPConfigDelegatedAdmin() throws Exception {
        ZAuthToken at = TestUtil.getAdminSoapTransport(TEST_ADMIN_NAME,PASSWORD).getAuthToken();
        URI servletURI = new URI(getLDAPConfigServletUrl());
        HttpState initialState = HttpClientUtil.newHttpState(at, servletURI.getHost(), true);
        HttpClient restClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        restClient.setState(initialState);
        restClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        GetMethod get = new GetMethod(servletURI.toString());
        int statusCode = HttpClientUtil.executeMethod(restClient, get);
        assertEquals("This request should NOT succeed. Getting status code " + statusCode, HttpStatus.SC_UNAUTHORIZED,statusCode);
    }

    /**
     * Verify that an HTTP client canNOT access servlet at /service/collectldapconfig/ without an auth token
     * @throws Exception
     */
    @Test
    public void testLDAPConfigNoToken() throws Exception {
        URI servletURI = new URI(getLDAPConfigServletUrl());
        HttpClient restClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        restClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        GetMethod get = new GetMethod(servletURI.toString());
        int statusCode = HttpClientUtil.executeMethod(restClient, get);
        assertEquals("This request should NOT succeed. Getting status code " + statusCode, HttpStatus.SC_UNAUTHORIZED,statusCode);
    }

    /**
     * Verify that an HTTP client canNOT access servlet at /service/collectconfig/ without an auth token
     * @throws Exception
     */
    @Test
    public void testConfigNoToken() throws Exception {
        URI servletURI = new URI(getConfigServletUrl());
        HttpClient restClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        restClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        GetMethod get = new GetMethod(servletURI.toString());
        int statusCode = HttpClientUtil.executeMethod(restClient, get);
        assertEquals("This request should NOT succeed. Getting status code " + statusCode, HttpStatus.SC_UNAUTHORIZED,statusCode);
    }

    private static String getConfigServletUrl() throws ServiceException {
        int port;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
            port = LC.zimbra_admin_service_port.intValue();
        }
        return "https://localhost:" + port + "/service/collectconfig/?host="+TestUtil.getServerAttr("name");
    }

    private static String getLDAPConfigServletUrl() throws ServiceException {
        int port;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
            port = LC.zimbra_admin_service_port.intValue();
        }
        return "https://localhost:" + port + "/service/collectldapconfig/";
    }
}
