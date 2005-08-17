package com.liquidsys.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.client.LmcSession;
import com.liquidsys.coco.client.soap.LmcSearchRequest;
import com.zimbra.soap.SoapFaultException;


/**
 * @author bburtin
 */
public class TestAuthentication
extends TestCase {
    private static String USER_NAME = "TestAuthentication";
    private static String PASSWORD = "test123";
    
    private Provisioning mProv;
    private Account mAccount;
    
    public void setUp()
    throws Exception {
        mProv = Provisioning.getInstance();
        cleanUp();

        // Create temporary account
        String address = TestUtil.getAddress(USER_NAME);
        Map attrs = new HashMap();
        attrs.put("liquidMailHost", TestUtil.getDomain());
        attrs.put("cn", "Unit test temporary user");
        attrs.put("displayName", "Unit test temporary user");
        mAccount = mProv.createAccount(address, PASSWORD, attrs);
        assertNotNull("Could not create account", mAccount);
    }

    /**
     * Attempts to access a deleted account and confirms that the attempt
     * fails with an auth error.
     */
    public void testAccessDeletedAccount()
    throws Exception {
        // Log in and check the inbox
        LmcSession session = TestUtil.getSoapSession(USER_NAME);
        LmcSearchRequest req = new LmcSearchRequest();
        req.setQuery("in:inbox");
        req.setSession(session);
        req.invoke(TestUtil.getSoapUrl());
        
        // Delete the account
        mProv.deleteAccount(mAccount.getId());
        
        // Submit another request and make sure it fails with an auth error
        try {
            req.invoke(TestUtil.getSoapUrl());
        } catch (SoapFaultException ex) {
            assertTrue("Unexpected error: " + ex.getMessage(),
                ex.getMessage().indexOf("auth credentials have expired") >= 0);
        }
    }

    /**
     * Attempts to access a deleted account and confirms that the attempt
     * fails with an auth error.
     */
    public void testAccessInactiveAccount()
    throws Exception {
        // Log in and check the inbox
        LmcSession session = TestUtil.getSoapSession(USER_NAME);
        LmcSearchRequest req = new LmcSearchRequest();
        req.setQuery("in:inbox");
        req.setSession(session);
        req.invoke(TestUtil.getSoapUrl());
        
        // Delete the account
        mProv.modifyAccountStatus(mAccount, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        
        // Submit another request and make sure it fails with an auth error
        try {
            req.invoke(TestUtil.getSoapUrl());
        } catch (SoapFaultException ex) {
            assertTrue("Unexpected error: " + ex.getMessage(),
                ex.getMessage().indexOf("auth credentials have expired") > 0);
        }
    }
    
    public void cleanUp()
    throws Exception {
        String address = TestUtil.getAddress(USER_NAME);
        Account account = Provisioning.getInstance().getAccountByName(address);
        if (account != null) {
            Provisioning.getInstance().deleteAccount(account.getId());
        }
    }
}
