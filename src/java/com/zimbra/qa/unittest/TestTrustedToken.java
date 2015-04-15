package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZAuthResult;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Options;
import com.zimbra.client.ZMailbox.TrustedStatus;
import com.zimbra.common.account.Key;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.prov.soap.SoapTest;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;
import com.zimbra.soap.account.message.GetTrustedDevicesRequest;
import com.zimbra.soap.account.message.GetTrustedDevicesResponse;
import com.zimbra.soap.account.message.RevokeOtherTrustedDevicesRequest;
import com.zimbra.soap.account.message.RevokeTrustedDeviceRequest;
import com.zimbra.soap.admin.message.GetCosRequest;
import com.zimbra.soap.admin.message.GetCosResponse;
import com.zimbra.soap.admin.message.ModifyCosRequest;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.CosSelector.CosBy;

public class TestTrustedToken extends TestCase {

    private static String USER = "user1";
    private static String PASSWORD = "test123";
    private static SoapTransport transport;
    private List<String> scratchCodes;
    private ZMailbox mbox;

    @BeforeClass
    @Override
    public void setUp() throws Exception {
        mbox = TestUtil.getZMailbox(USER);
        mbox.invokeJaxb(new RevokeTrustedDeviceRequest());
        mbox.invokeJaxb(new RevokeOtherTrustedDevicesRequest());
        EnableTwoFactorAuthResponse resp = mbox.enableTwoFactorAuth(PASSWORD);
        scratchCodes = resp.getCredentials().getScratchCodes();
        transport = TestUtil.getAdminSoapTransport();
        setTokenLifetime("2d");
    }

    private static void setTrustedTokenLifetime(String lifetime) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraTwoFactorAuthTrustedDeviceTokenLifetime, lifetime);
        ModifyCosRequest modifyRequest = new ModifyCosRequest();
        modifyRequest.setId(getCosId());
        modifyRequest.setAttrs(attrs);
        SoapTest.invokeJaxb(transport, modifyRequest);
    }

    private static void setTokenLifetime(String lifetime) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAuthTokenLifetime, lifetime);
        ModifyCosRequest modifyRequest = new ModifyCosRequest();
        modifyRequest.setId(getCosId());
        modifyRequest.setAttrs(attrs);
        SoapTest.invokeJaxb(transport, modifyRequest);
    }

    @Test
    public void testAuthenticate() throws Exception {
        setTokenLifetime("1s");
        mbox = TestUtil.getZMailbox(USER, scratchCodes.remove(0), TrustedStatus.trusted);
        Thread.sleep(2000);
        String firstToken = mbox.getTrustedToken();
        //should be able to obtain a new token with the old expired one
        String secondToken = testAuthWithTrustedToken(firstToken, true);
        mbox.initTrustedToken(secondToken);
        testAuthWithTrustedToken("badtoken", false);
        testAuthWithTrustedToken("badtoken:1428183166000", false);
        //reauthenticate so that tearDown will work
        mbox = TestUtil.getZMailbox(USER, scratchCodes.remove(0));
    }

    @Test
    public void testRevoke() throws Exception {
        mbox = TestUtil.getZMailbox(USER, scratchCodes.remove(0), TrustedStatus.trusted);
        ZMailbox mbox2 = TestUtil.getZMailbox(USER, scratchCodes.remove(0), TrustedStatus.trusted);
        GetTrustedDevicesResponse getResp = mbox.invokeJaxb(new GetTrustedDevicesRequest());
        assertEquals(new Integer(1), getResp.getNumOtherTrustedDevices());
        assertTrue(getResp.getThisDeviceTrusted());
        getResp = mbox2.invokeJaxb(new GetTrustedDevicesRequest());
        assertEquals(new Integer(1), getResp.getNumOtherTrustedDevices());
        assertTrue(getResp.getThisDeviceTrusted());
        //revoke current device
        mbox.invokeJaxb(new RevokeTrustedDeviceRequest());
        testAuthWithTrustedToken(mbox.getTrustedToken(), false);
        getResp = mbox.invokeJaxb(new GetTrustedDevicesRequest());
        assertEquals(new Integer(1), getResp.getNumOtherTrustedDevices());
        assertFalse(getResp.getThisDeviceTrusted());
        //revoke other device
        mbox.invokeJaxb(new RevokeOtherTrustedDevicesRequest());
        getResp = mbox.invokeJaxb(new GetTrustedDevicesRequest());
        assertEquals(new Integer(0), getResp.getNumOtherTrustedDevices());
    }

    @Test
    public void testAuthenticateWithDeviceId() throws Exception {
        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(TestUtil.getAddress(USER));
        options.setAccountBy(Key.AccountBy.name);
        options.setPassword(PASSWORD);
        options.setUri(TestUtil.getSoapUrl());
        options.setTrustedDevice(true);
        options.setDeviceId("123456");
        options.setTwoFactorScratchCode(scratchCodes.remove(0));
        mbox = new ZMailbox(options);
        Thread.sleep(1000);
        testAuthWithTrustedToken(mbox.getTrustedToken(), false);
        String newToken = testAuthWithTrustedToken(mbox.getTrustedToken(), "123456", true);
        mbox.initTrustedToken(newToken);
    }

    @Test
    public void testAuthenticateWithGeneratedDeviceId() throws Exception {
        setTokenLifetime("1s");
        String scratchCode = scratchCodes.remove(0);
        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(TestUtil.getAddress(USER));
        options.setAccountBy(Key.AccountBy.name);
        options.setPassword(PASSWORD);
        options.setUri(TestUtil.getSoapUrl());
        options.setTwoFactorScratchCode(scratchCode);
        options.setTrustedDevice(true);
        options.setGenerateDeviceId(true);
        mbox = ZMailbox.getMailbox(options);
        Thread.sleep(2000);
        String deviceId = mbox.getAuthResult().getDeviceId();
        String trustedToken = mbox.getTrustedToken();
        testAuthWithTrustedToken(trustedToken, false);
        testAuthWithTrustedToken(trustedToken, "badtoken", false);
        testAuthWithTrustedToken(trustedToken, deviceId, true);
        mbox.initTrustedToken(trustedToken);
    }

    @Test
    public void testExpiredTrustedToken() throws Exception {
        setTokenLifetime("1s");
        setTrustedTokenLifetime("1s");
        mbox = TestUtil.getZMailbox(USER, scratchCodes.remove(0), TrustedStatus.trusted);
        Thread.sleep(1000);
        testAuthWithTrustedToken(mbox.getTrustedToken(), false);
        setTokenLifetime("2d");
        mbox = TestUtil.getZMailbox(USER, scratchCodes.remove(0));
    }

    private String testAuthWithTrustedToken(String trustedToken, boolean shouldWork) {
        return testAuthWithTrustedToken(trustedToken, null, shouldWork);
    }

    private String testAuthWithTrustedToken(String trustedToken, String deviceId, boolean shouldWork) {
        Options options = new Options();
        options.setAccount(USER);
        options.setPassword(PASSWORD);
        options.setTrustedDevice(true);
        if (deviceId != null) {
            options.setDeviceId(deviceId);
        }
        ZAuthToken curToken = mbox.getAuthToken();
        mbox.initAuthToken(null);
        mbox.initTrustedToken(trustedToken);
        try {
            ZAuthResult res = mbox.authByPassword(options, PASSWORD);
            mbox.initAuthToken(res.getAuthToken());
            if (!shouldWork) {
                fail("should not be able to authenticate with this token");
            }
            return res.getTrustedToken();
        } catch (ServiceException e) {
            if (shouldWork) {
                fail("should be able to authenticate with this token");
            }
            //set back previous auth token;
            mbox.initAuthToken(curToken);
            return null;
        }
    }

    private static String getCosId() throws Exception {
        GetCosRequest cosRequest = new GetCosRequest();
        cosRequest.setCos(new CosSelector(CosBy.name, "default"));
        GetCosResponse cosResponse = SoapTest.invokeJaxb(transport, cosRequest);
        return cosResponse.getCos().getId();
    }

    @Override
    public void tearDown() throws Exception {
        setTokenLifetime("2d");
        setTrustedTokenLifetime("30d");
        mbox.disableTwoFactorAuth(PASSWORD);
    }
}
