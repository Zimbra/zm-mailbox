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

package com.zimbra.qa.unittest.prov.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.ldap.LdapConstants;

public class TestAccountLockout extends LdapTest {
    
    private final String BAD_PASSWORD = "badpasssword";
    private final String GOOD_PASSWORD = "test123";
    private final int LOCKOUT_AFTER_NUM_FAILURES = 3;
    private final int LOCKOUT_DURATION_SECONDS = 10;
    
    
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private Account createAccount(String localPart) throws Exception {
        return provUtil.createAccount(localPart, domain);
    }

    private void deleteAccount(Account acct) throws Exception {
        provUtil.deleteAccount(acct);
    }
    
    public void lockout(Account acct) throws Exception {
        String acctId = acct.getId();
        
        Map<String, Object> attrs = Maps.newHashMap();
        
        // setup lockout config attrs
        attrs.put(Provisioning.A_zimbraPasswordLockoutEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraPasswordLockoutDuration, LOCKOUT_DURATION_SECONDS + "s");
        attrs.put(Provisioning.A_zimbraPasswordLockoutMaxFailures, LOCKOUT_AFTER_NUM_FAILURES+"");
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureLifetime, "30s");
        
        // put the account in active mode, clean all lockout attrs that might have been set 
        // in previous test
        attrs.put(Provisioning.A_zimbraAccountStatus, "active");
        attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, "");
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureTime, "");
        
        prov.modifyAttrs(acct, attrs);
        
        // the account should be locked out at the last iteration
        for (int i=0; i<=LOCKOUT_AFTER_NUM_FAILURES; i++) {
            
            boolean authFailed = false;
            try {
                prov.authAccount(acct, BAD_PASSWORD, AuthContext.Protocol.test);
            } catch (ServiceException e) {
                if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                    authFailed = true;
                }
            }
            assertTrue(authFailed);
            
            // refresh account, needed if using SoapProvisioning
            acct = prov.get(AccountBy.id, acctId);
            
            if (i >= LOCKOUT_AFTER_NUM_FAILURES-1) {
                assertEquals("lockout", acct.getAttr(Provisioning.A_zimbraAccountStatus));
            } else {
                assertEquals("active", acct.getAttr(Provisioning.A_zimbraAccountStatus));
            }
            
            // sleep two seconds
            Thread.sleep(2000);
        }
    }
    
    @Test
    public void successfulLogin() throws Exception {
        Account acct = createAccount("successfulLogin");
        lockout(acct);
        
        // try to login with correct password, before lockoutDurationSeconds, should fail
        boolean authFailed = false;
        try {
            prov.authAccount(acct, GOOD_PASSWORD, AuthContext.Protocol.test);
        } catch (ServiceException e) {
            if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                authFailed = true;
            }
        }
        assertTrue(authFailed);
        
        // wait for lockoutDurationSeconds
        int wait = LOCKOUT_DURATION_SECONDS + 1;
        System.out.println("Sleep for " + wait + " seconds");
        Thread.sleep(wait * 1000);
        
        // try login with correct password again, should be successful
        prov.authAccount(acct, GOOD_PASSWORD, AuthContext.Protocol.test);
        Provisioning.AccountStatus status = acct.getAccountStatus();
        Assert.assertEquals(Provisioning.AccountStatus.active, status);
        
        deleteAccount(acct);
    }
    
    @Test
    public void ssoWhenAccountIsLockedout() throws Exception {
        Account acct = createAccount("successfulLogin");
        lockout(acct);
        
        boolean authFailed = false;
        try {
            prov.ssoAuthAccount(acct, AuthContext.Protocol.test, null);
        } catch (AccountServiceException e) {
            if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                authFailed = true;
            }
        }
        Assert.assertTrue(authFailed);
        
        deleteAccount(acct);
    }

}
