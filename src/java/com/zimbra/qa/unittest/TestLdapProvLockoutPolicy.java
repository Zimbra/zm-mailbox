/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.ldap.LdapConstants;

public class TestLdapProvLockoutPolicy {
    
    @BeforeClass
    public static void init() throws Exception {
        // CliUtil.toolSetup();
        TestUtil.cliSetup();  // use SoapProvisioning
    }
    
    @Test
    public void lockout() throws Exception {
        String badPassword = "badpasssword";
        String goodPassword = "test123";
        
        String user = "user1";
        Account acct = TestUtil.getAccount(user);
        String acctId = acct.getId();
        
        Provisioning prov = Provisioning.getInstance();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        int lockoutAfterNumFailures = 3;
        int lockoutDurationSeconds = 60;
        
        
        // setup lockout config attrs
        attrs.put(Provisioning.A_zimbraPasswordLockoutEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraPasswordLockoutDuration, lockoutDurationSeconds + "s");
        attrs.put(Provisioning.A_zimbraPasswordLockoutMaxFailures, lockoutAfterNumFailures+"");
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureLifetime, "30s");
        
        // put the account in active mode, clean all lockout attrs that might have been set 
        // in previous test
        attrs.put(Provisioning.A_zimbraAccountStatus, "active");
        attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, "");
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureTime, "");
        
        prov.modifyAttrs(acct, attrs);
        
        // the account should be locked out at the last iteration
        for (int i=0; i<=lockoutAfterNumFailures; i++) {
            
            System.out.println(i);
            
            boolean authFailed = false;
            try {
                prov.authAccount(acct, badPassword, AuthContext.Protocol.test);
            } catch (ServiceException e) {
                if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                    authFailed = true;
                }
            }
            assertTrue(authFailed);
            
            // refresh account, needed if using SoapProvisioning
            acct = prov.get(AccountBy.id, acctId);
            
            if (i >= lockoutAfterNumFailures-1) {
                assertEquals("lockout", acct.getAttr(Provisioning.A_zimbraAccountStatus));
            } else {
                assertEquals("active", acct.getAttr(Provisioning.A_zimbraAccountStatus));
            }
            
            // sleep two seconds
            Thread.sleep(2000);
        }
        
        // try to login with correct password, before lockoutDurationSeconds, should fail
        acct = prov.get(AccountBy.id, acctId);
        boolean authFailed = false;
        try {
            prov.authAccount(acct, goodPassword, AuthContext.Protocol.test);
        } catch (ServiceException e) {
            if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                authFailed = true;
            }
        }
        assertTrue(authFailed);
        
        // wait for lockoutDurationSeconds
        System.out.println("Sleep for " + lockoutDurationSeconds + " seconds");
        Thread.sleep(lockoutDurationSeconds * 1000);
        
        // try login with correct password again, should be successful
        prov.authAccount(acct, goodPassword, AuthContext.Protocol.test);
        
    }
}
