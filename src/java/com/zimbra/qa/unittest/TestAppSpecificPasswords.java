package com.zimbra.qa.unittest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Options;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailConnection;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.pop3.Pop3Config;
import com.zimbra.cs.mailclient.pop3.Pop3Connection;
import com.zimbra.cs.mailclient.util.SSLUtil;
import com.zimbra.qa.unittest.prov.soap.SoapTest;
import com.zimbra.soap.account.message.AppSpecificPasswordData;
import com.zimbra.soap.account.message.ChangePasswordRequest;
import com.zimbra.soap.account.message.ChangePasswordResponse;
import com.zimbra.soap.account.message.CreateAppSpecificPasswordRequest;
import com.zimbra.soap.account.message.CreateAppSpecificPasswordResponse;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;
import com.zimbra.soap.account.message.GetAppSpecificPasswordsRequest;
import com.zimbra.soap.account.message.GetAppSpecificPasswordsResponse;
import com.zimbra.soap.account.message.RevokeAppSpecificPasswordRequest;
import com.zimbra.soap.admin.message.GetAllServersRequest;
import com.zimbra.soap.admin.message.GetAllServersResponse;
import com.zimbra.soap.admin.message.GetCosRequest;
import com.zimbra.soap.admin.message.GetCosResponse;
import com.zimbra.soap.admin.message.ModifyCosRequest;
import com.zimbra.soap.admin.message.ModifyServerRequest;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.CosSelector.CosBy;
import com.zimbra.soap.admin.type.ServerInfo;
import com.zimbra.soap.type.AccountBy;
import com.zimbra.soap.type.AccountSelector;

public class TestAppSpecificPasswords extends TestCase {
    private static String HOST = "localhost";
    private static int IMAP_PORT = 7143;
    private static String AUTH_MECH = "PLAIN";
    private static final int POP3_PORT = 7110;
    private static final int SSL_PORT = 7995;
    private static String USER = "user1";
    private static String PASSWORD = "test123";
    private static String CHANGED_PASSWORD = "newpassword";
    private static String APP_NAME = "testapp";
    private static Boolean DEFAULT_REVOKE_ON_PASSWORD_CHANGE = true;
    private static String DEFAULT_APP_LIFETIME = "0";
    private static SoapTransport transport;
    private ZMailbox mbox;

    @BeforeClass
    @Override
    public void setUp() throws ServiceException, IOException {
        mbox = TestUtil.getZMailbox(USER);
        EnableTwoFactorAuthResponse resp = mbox.enableTwoFactorAuth(PASSWORD, TestUtil.getDefaultAuthenticator());
        //reauthenticating to get new auth token
        mbox = TestUtil.getZMailbox(USER, resp.getScratchCodes().remove(0));
        transport = TestUtil.getAdminSoapTransport();
        enableAppSpecificPasswords();
        revokeAllAppPasswords();
    }

    private void enableAppSpecificPasswords() throws ServiceException, IOException {
        toggleAppSpecificPasswords(true);
    }

    private void disableAppSpecificPasswords() throws ServiceException, IOException {
        toggleAppSpecificPasswords(false);
    }

    private void toggleAppSpecificPasswords(boolean bool) throws ServiceException, IOException {
        GetAllServersRequest req = new GetAllServersRequest();
        req.setService((String) null);
        GetAllServersResponse resp = SoapTest.invokeJaxb(transport, req);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraFeatureAppSpecificPasswordsEnabled, bool ? ProvisioningConstants.TRUE : ProvisioningConstants.FALSE);
        for (ServerInfo server: resp.getServerList()) {
            ModifyServerRequest modifyRequest = new ModifyServerRequest();
            modifyRequest.setId(server.getId());
            modifyRequest.setAttrs(attrs);
            SoapTest.invokeJaxb(transport, modifyRequest);
        }
    }

    private String generatePassword(String name) throws ServiceException, IOException {
        CreateAppSpecificPasswordResponse resp = mbox.invokeJaxb(new CreateAppSpecificPasswordRequest(name));
        return resp.getPassword();
    }

    private void testPassword(String password) throws IOException {
        testPassword(password, true, null);
    }

    private void testPassword(String password, String errMessage) throws IOException {
        testPassword(password, true, errMessage);
    }

    private void testPassword(String password, boolean shouldWork) throws IOException {
        testPassword(password, shouldWork, null);
    }

    private void testConnection(MailConnection conn, String password, boolean shouldWork, String errMessage) throws IOException {
        try {
            conn.connect();
            conn.login(password);
        } catch (CommandFailedException e) {
            if (!shouldWork) {
                String err = e.getError();
                if (errMessage == null) {
                    assertEquals(err, "LOGIN failed");
                } else {
                    assertEquals(errMessage, err, "LOGIN failed");
                }
            }
        }
    }

    private void testPop3(String password, boolean shouldWork, String errMessage) throws IOException {
        Pop3Config config = new Pop3Config(HOST);
        config.setPort(POP3_PORT);
        config.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        config.setMechanism(AUTH_MECH);
        config.setAuthenticationId(USER);
        Pop3Connection conn = new Pop3Connection(config);
        testConnection(conn, password, shouldWork, errMessage);
    }

    private void testImap(String password, boolean shouldWork, String errMessage) throws IOException {
        ImapConfig config = new ImapConfig(HOST);
        config.setPort(IMAP_PORT);
        config.setMechanism(AUTH_MECH);
        config.setAuthenticationId(USER);
        ImapConnection conn = new ImapConnection(config);
        testConnection(conn, password, shouldWork, errMessage);
    }

    private void testPassword(String password, boolean shouldWork, String errMessage) throws IOException {


        try {
            TestUtil.testAuth(mbox, USER, password);
            fail("app-specific passwords shouldn't work over SOAP");
        } catch (ServiceException e) {}
        testImap(password, shouldWork, errMessage);
        testPop3(password, shouldWork, errMessage);
    }

    @Test
    public void testAppSpecificPassword() throws ServiceException, InterruptedException, IOException {
        String password = generatePassword(APP_NAME);
        assertNotNull(password);
        try {
            generatePassword(APP_NAME);
            fail("should not be able to generate two passwords for the same app name");
        } catch (ServiceException e) {
        }

        testPassword(password);
        // test that it's registered and that the last used date was incremented
        GetAppSpecificPasswordsResponse getResp = mbox.invokeJaxb(new GetAppSpecificPasswordsRequest());
        List<AppSpecificPasswordData> appPasswords = getResp.getAppSpecificPasswords();
        assertEquals(1, appPasswords.size());
        AppSpecificPasswordData passwordData = appPasswords.get(0);
        assertEquals(APP_NAME, passwordData.getAppName());
        Long firstUsedDate = passwordData.getDateLastUsed();
        assertTrue(firstUsedDate > passwordData.getDateCreated());

        // test incrementing timestamp again
        testPassword(password);
        getResp = mbox.invokeJaxb(new GetAppSpecificPasswordsRequest());
        appPasswords = getResp.getAppSpecificPasswords();
        assertEquals(1, appPasswords.size());
        passwordData = appPasswords.get(0);
        assertEquals(APP_NAME, passwordData.getAppName());
        assertTrue(passwordData.getDateLastUsed() > firstUsedDate);

        // revoke
        mbox.invokeJaxb(new RevokeAppSpecificPasswordRequest(APP_NAME));
        try {
            TestUtil.testAuth(mbox, USER, password);
            fail("this password should not work anymore");
        } catch (ServiceException e) {
        }
    }

    @Test
    public void testTooManyAppPasswords() throws ServiceException, IOException {
        GetAppSpecificPasswordsResponse resp = mbox.invokeJaxb(new GetAppSpecificPasswordsRequest());
        int maxPasswords = resp.getMaxAppPasswords();
        for (int i = 0; i < maxPasswords; i++) {
            generatePassword(APP_NAME + String.valueOf(i));
        }
        try {
            generatePassword(APP_NAME + "_overflow");
            fail("this should not work");
        } catch (ServiceException e) {
        }
    }

    private void setRevokeOnPasswordChange(String value) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraRevokeAppSpecificPasswordsOnPasswordChange, value);
        ModifyCosRequest modifyRequest = new ModifyCosRequest();
        modifyRequest.setId(getCosId());
        modifyRequest.setAttrs(attrs);
        SoapTest.invokeJaxb(transport, modifyRequest);
    }

    private void changePassword(String from, String to) throws ServiceException, IOException {
        Options options = new Options();
        options.setAccount(USER);
        options.setAccountBy(Key.AccountBy.name);
        options.setPassword(from);
        options.setNewPassword(to);
        options.setUri(TestUtil.getSoapUrl());
        AccountSelector selector = new AccountSelector(AccountBy.name, USER);
        ChangePasswordRequest req = new ChangePasswordRequest(selector, from, to);
        ChangePasswordResponse resp = mbox.invokeJaxb(req);
        // reauthenticate with new password
        options = new ZMailbox.Options();
        options.setAccount(AccountTestUtil.getAddress(USER));
        options.setAccountBy(Key.AccountBy.name);
        options.setPassword(to);
        options.setUri(TestUtil.getSoapUrl());
        options.setAuthToken(resp.getAuthToken());
        mbox = ZMailbox.getMailbox(options);
    }

    private void changeAppPasswordLifetime(String lifetime) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAppSpecificPasswordDuration, lifetime);
        ModifyCosRequest modifyRequest = new ModifyCosRequest();
        modifyRequest.setId(getCosId());
        modifyRequest.setAttrs(attrs);
        SoapTest.invokeJaxb(transport, modifyRequest);
    }

    @Test
    public void testRevokeOnPasswordChange() throws Exception {
        setRevokeOnPasswordChange(ProvisioningConstants.TRUE);
        String password = generatePassword(APP_NAME);
        testPassword(password);
        changePassword(PASSWORD, CHANGED_PASSWORD);
        String oldPassword = PASSWORD;
        PASSWORD = CHANGED_PASSWORD;
        try {
            testPassword(password, false);
        } finally {
            changePassword(PASSWORD, oldPassword);
            PASSWORD = oldPassword;
            if (DEFAULT_REVOKE_ON_PASSWORD_CHANGE) {
                setRevokeOnPasswordChange(ProvisioningConstants.FALSE);
            }
        }
    }

    @Test
    public void testAppPasswordLifetime() throws Exception {
        String password = generatePassword(APP_NAME);
        testPassword(password);
        changeAppPasswordLifetime("1s");
        Thread.sleep(1000);
        try {
            testPassword(password, false);
        } finally {
            changeAppPasswordLifetime(DEFAULT_APP_LIFETIME);
        }
    }

    private List<AppSpecificPasswordData> getAppSpecificPasswords() throws ServiceException, IOException {
        GetAppSpecificPasswordsResponse resp = mbox.invokeJaxb(new GetAppSpecificPasswordsRequest());
        return resp.getAppSpecificPasswords();
    }

    private void revokeAllAppPasswords() throws ServiceException, IOException {
        for (AppSpecificPasswordData password : getAppSpecificPasswords()) {
            mbox.invokeJaxb(new RevokeAppSpecificPasswordRequest(password.getAppName()));
        }
    }

    private static String getCosId() throws Exception {
        GetCosRequest cosRequest = new GetCosRequest();
        cosRequest.setCos(new CosSelector(CosBy.name, "default"));
        GetCosResponse cosResponse = SoapTest.invokeJaxb(transport, cosRequest);
        return cosResponse.getCos().getId();
    }

    @AfterClass
    @Override
    public void tearDown() throws ServiceException, IOException {
        revokeAllAppPasswords();
        mbox.disableTwoFactorAuth(PASSWORD);
    }
}
