/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.ldap.LdapConstants;

public class TestAccountLockout extends TestLdap {
    
    private static final String ACCT = "testlockout";
    private static final String PASSWORD = "test123";
    private static final int LOCKOUT_AFTER_NUM_FAILURES = 3;
    
    @BeforeClass
    public static void init() throws Exception {
        Account acct = getAccount();
        Assert.assertNull(acct);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        // setup lockout config attrs
        attrs.put(Provisioning.A_zimbraPasswordLockoutEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraPasswordLockoutDuration, "15s");
        attrs.put(Provisioning.A_zimbraPasswordLockoutMaxFailures, LOCKOUT_AFTER_NUM_FAILURES + "");
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureLifetime, "30s");
        
        acct = Provisioning.getInstance().createAccount(getAccountName(), PASSWORD, attrs);
        Assert.assertNotNull(acct);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Account acct = getAccount();
        Assert.assertNotNull(acct);
        
        Provisioning.getInstance().deleteAccount(acct.getId());
    }
    
    private static String getAccountName() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        String domainName = TestUtil.getDomain();
        return TestUtil.getAddress(ACCT, domainName);
    }
    
    private static Account getAccount() throws Exception {
        return Provisioning.getInstance().get(AccountBy.name, getAccountName());
    }
    
    private void lockoutAccount() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account acct = getAccount();
        
        for (int i = 0; i <= LOCKOUT_AFTER_NUM_FAILURES; i++) {
            boolean caughtAuthFailed = false;
            try {
                prov.authAccount(acct, PASSWORD + "-not", AuthContext.Protocol.test, null);
            } catch (AccountServiceException e) {
                if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                    caughtAuthFailed = true;
                }
            }
            Assert.assertTrue(caughtAuthFailed);
            Thread.sleep(1000);
        }
        
        Provisioning.AccountStatus status = acct.getAccountStatus();
        Assert.assertEquals(Provisioning.AccountStatus.lockout, status);
    }
    
    @Test
    public void successfulLogin() throws Exception {
        lockoutAccount();
        
        Provisioning prov = Provisioning.getInstance();
        Account acct = getAccount();
        
        long milliSecondsToWait = acct.getPasswordLockoutDuration() + 2000;
        System.out.println("Waiting " + milliSecondsToWait + " milli seconds");
        Thread.sleep(milliSecondsToWait);
        
        prov.authAccount(acct, PASSWORD , AuthContext.Protocol.test, null);
        Provisioning.AccountStatus status = acct.getAccountStatus();
        Assert.assertEquals(Provisioning.AccountStatus.active, status);
    }
    
    @Test
    public void ssoWhenAccountIsLockedout() throws Exception {
        lockoutAccount();
        
        Provisioning prov = Provisioning.getInstance();
        Account acct = getAccount();
        
        boolean caughtAuthFailed = false;
        try {
            prov.ssoAuthAccount(acct, AuthContext.Protocol.test, null);
        } catch (AccountServiceException e) {
            if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                caughtAuthFailed = true;
            }
        }
        Assert.assertTrue(caughtAuthFailed);
    }

}
