package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.qa.unittest.prov.soap.SoapTest;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;
import com.zimbra.soap.admin.message.GetCosRequest;
import com.zimbra.soap.admin.message.GetCosResponse;
import com.zimbra.soap.admin.message.ModifyCosRequest;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.CosSelector.CosBy;

public class TestTwoFactorAuthAdmin extends TestCase {
    private static final String USER_NAME = "user1";
    private static final String PASSWORD = "test123";
    private static ZMailbox mbox;
    private String secret;
    private List<String> scratchCodes;
    private static SoapProvisioning prov;
    private static SoapTransport transport;
    private static Cos cos;

    @BeforeClass
    @Override
    public void setUp() throws Exception {
        transport = TestUtil.getAdminSoapTransport();
        mbox = TestUtil.getZMailbox(USER_NAME);
        enableTwoFactorAuthRequired();
    }

    private static String getCosId() throws Exception {
        GetCosRequest cosRequest = new GetCosRequest();
        cosRequest.setCos(new CosSelector(CosBy.name, "default"));
        GetCosResponse cosResponse = SoapTest.invokeJaxb(transport, cosRequest);
        return cosResponse.getCos().getId();
    }

    private static void setTwoFactorStatus(String value) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraFeatureTwoFactorAuthRequired, value);

        ModifyCosRequest modifyRequest = new ModifyCosRequest();
        modifyRequest.setId(getCosId());
        modifyRequest.setAttrs(attrs);
        SoapTest.invokeJaxb(transport, modifyRequest);
    }

    private static void enableTwoFactorAuthRequired() throws Exception {
        setTwoFactorStatus(ProvisioningConstants.TRUE);
    }

    private static void disableTwoFactorAuthRequired() throws Exception {
        setTwoFactorStatus(ProvisioningConstants.FALSE);
        mbox.disableTwoFactorAuth(PASSWORD);
    }

    @AfterClass
    @Override
    public void tearDown() throws Exception {
        disableTwoFactorAuthRequired();
    }

    @Test
    public void testTwoFactorSetupRequired() {
        try {
            TestUtil.testAuth(mbox, USER_NAME, PASSWORD);
            fail("should not be able to authenticate without a code");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.TWO_FACTOR_SETUP_REQUIRED, e.getCode());
        }
    }

    @Test
    public void testEnableTwoFactorAuth() throws ServiceException {
        try {
            EnableTwoFactorAuthResponse resp = mbox.enableTwoFactorAuth(PASSWORD, TestUtil.getDefaultAuthenticator());
            //have to re-authenticate since the previous auth token was invalidated by enabling two-factor auth
            mbox = TestUtil.getZMailbox(USER_NAME, resp.getScratchCodes().get(0));
        } catch (ServiceException e) {
            fail("should be able to enable two-factor auth");
        }
        try {
            mbox.disableTwoFactorAuth(PASSWORD);
            fail("should not be able to disable two-factor auth");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.CANNOT_DISABLE_TWO_FACTOR_AUTH, e.getCode());
        }
    }

    @Test
    public void testCannotDisableTwoFactorAuth() {
        try {
            mbox.disableTwoFactorAuth(PASSWORD);
            fail("should not be able to disable two-factor auth");
        } catch (ServiceException e) {
            assertEquals(ServiceException.CANNOT_DISABLE_TWO_FACTOR_AUTH, e.getCode());
        }
    }
}
