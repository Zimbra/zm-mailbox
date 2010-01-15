/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcSearchRequest;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.common.soap.SoapFaultException;

public class TestAuthentication
extends TestCase {
    private static String USER_NAME = "testauthentication";
    private static String PASSWORD = "test123";
    
    private Provisioning mProv;
    private Long mMboxId;
    
    @Override public void setUp()
    throws Exception {
        mProv = Provisioning.getInstance();
        cleanUp();

        // Create temporary account
        String address = TestUtil.getAddress(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraMailHost", TestUtil.getDomain());
        attrs.put("cn", "Unit test temporary user");
        attrs.put("displayName", "Unit test temporary user");
        Account account = mProv.createAccount(address, PASSWORD, attrs);
        assertNotNull("Could not create account", account);
        mMboxId = MailboxManager.getInstance().getMailboxByAccount(account).getId();
    }
    
    @Override protected void tearDown()
    throws Exception {
        cleanUp();
    }

    private Account getAccount()
    throws Exception {
        String address = TestUtil.getAddress(USER_NAME);
        return Provisioning.getInstance().get(AccountBy.name, address);
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
        Account account = getAccount();
        assertNotNull("Account does not exist", account);
        mProv.deleteAccount(account.getId());
        
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
        
        // Deactivate the account
        Account account = getAccount();
        mProv.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        
        // Submit another request and make sure it fails with an auth error
        try {
            req.invoke(TestUtil.getSoapUrl());
        } catch (SoapFaultException ex) {
            String substring = "auth credentials have expired";
            String msg = String.format("Error message '%s' does not contain '%s'", ex.getMessage(), substring);
            assertTrue(msg, ex.getMessage().contains(substring));
        }
    }

    /**
     * Deletes the account and the associated mailbox.
     */
    private void cleanUp()
    throws Exception {
        Account account = getAccount();
        if (account != null) {
            Provisioning.getInstance().deleteAccount(account.getId());
        }
        
        if (mMboxId != null) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMboxId);
            mbox.deleteMailbox();
        }
    }
}
