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

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.junit.*;

import static org.junit.Assert.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ProvisioningConstants;

public class TestProvValidator extends LdapTest {
    
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static String BASE_DOMAIN_NAME;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        BASE_DOMAIN_NAME = baseDomainName();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(BASE_DOMAIN_NAME);
    }
    
    private String makeCosLimit(Cos cos, int limit) {
        return cos.getId() + ":" + limit;
    }
    
    private String makeFeatureLimit(String feature, int limit) {
        return feature + ":" + limit;
    }
    
    private Domain createDomainWithCosLimit(String domainName, Cos cos, int limit) 
    throws Exception {
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        String cosLimit = makeCosLimit(cos, limit); 
        domainAttrs.put(Provisioning.A_zimbraDomainCOSMaxAccounts, cosLimit);
        
        Domain domain = provUtil.createDomain(domainName + "." + BASE_DOMAIN_NAME, domainAttrs);
        return domain;
    }
    
    private Domain createDomainWithFeatureLimit(String domainName, String feature, int limit) 
    throws Exception {
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        String featureLimit = makeFeatureLimit(feature, limit); 
        domainAttrs.put(Provisioning.A_zimbraDomainFeatureMaxAccounts, featureLimit);
        
        Domain domain = provUtil.createDomain(domainName + "." + BASE_DOMAIN_NAME, domainAttrs);
        return domain;
    }
    
    @Test
    public void testCOSMaxCreateAccount() throws Exception {
        
        final int COS_MAX_ACCOUNTS = 2;
        
        Cos cos = provUtil.createCos("testCOSMaxCreateAccount");
        Domain domain = createDomainWithCosLimit("testCOSMaxCreateAccount", cos, COS_MAX_ACCOUNTS);
        
        for (int i = 0; i <= COS_MAX_ACCOUNTS; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
            
            boolean caughtLimitExceeded = false;
            try {
                Account acct = provUtil.createAccount("acct-" + i, domain, attrs);
            } catch (ServiceException e) {
                if (AccountServiceException.TOO_MANY_ACCOUNTS.equals(e.getCode())) {
                    caughtLimitExceeded = true;
                } else {
                    throw e;
                }
            }
            
            if (i < COS_MAX_ACCOUNTS) {
                assertFalse(caughtLimitExceeded);
            } else {
                assertTrue(caughtLimitExceeded);
            }
        }
    }
    
    @Test
    public void testCOSMaxModifyAccount() throws Exception {
        final int COS_MAX_ACCOUNTS = 2;
        
        Cos cos = provUtil.createCos("testCOSMaxModifyAccount");
        Domain domain = createDomainWithCosLimit("testCOSMaxModifyAccount", cos, COS_MAX_ACCOUNTS);
        
        for (int i = 0; i < COS_MAX_ACCOUNTS; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
            Account acct = provUtil.createAccount("acct-" + i, domain, attrs);
        }
        
        // create an account on the default cos
        Account acct = provUtil.createAccount("acct-on-default-cos", domain, null);
        
        boolean caughtLimitExceeded = false;
        try {
            // attempt to change the account to the cos at limit
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
            prov.modifyAttrs(acct, attrs);
        } catch (ServiceException e) {
            if (AccountServiceException.TOO_MANY_ACCOUNTS.equals(e.getCode())) {
                caughtLimitExceeded = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtLimitExceeded);
    }
    
    @Test
    public void testFeatureMaxCreateAccount() throws Exception {
        final String FEATURE = Provisioning.A_zimbraFeatureAdvancedSearchEnabled;
        final int FEATURE_MAX_ACCOUNTS = 2;
        
        Domain domain = createDomainWithFeatureLimit("testFeatureMaxCreateAccount", FEATURE, FEATURE_MAX_ACCOUNTS);
        
        for (int i = 0; i <= FEATURE_MAX_ACCOUNTS; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(FEATURE, ProvisioningConstants.TRUE);
            
            boolean caughtLimitExceeded = false;
            try {
                Account acct = provUtil.createAccount("acct-" + i, domain, null);
            } catch (ServiceException e) {
                if (AccountServiceException.TOO_MANY_ACCOUNTS.equals(e.getCode())) {
                    caughtLimitExceeded = true;
                } else {
                    throw e;
                }
            }
            
            if (i < FEATURE_MAX_ACCOUNTS) {
                assertFalse(caughtLimitExceeded);
            } else {
                assertTrue(caughtLimitExceeded);
            }
        }
    }
    
    @Test
    @Ignore  // bug existed prior to unboundid SDK work
    public void testFeatureMaxModifyAccount() throws Exception {
        final String FEATURE = Provisioning.A_zimbraFeatureAdvancedSearchEnabled;
        final int FEATURE_MAX_ACCOUNTS = 2;
        
        Domain domain = createDomainWithFeatureLimit("testFeatureMaxModifyAccount", FEATURE, FEATURE_MAX_ACCOUNTS);
        
        List<String> acctIds = new ArrayList<String /* account id */>();
        for (int i = 0; i <= FEATURE_MAX_ACCOUNTS; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(FEATURE, ProvisioningConstants.FALSE);
            Account acct = provUtil.createAccount("acct-" + i, domain, attrs);
        }
        
        for (int i = 0; i <= FEATURE_MAX_ACCOUNTS; i++) {
            String acctId = acctIds.get(i);
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(FEATURE, ProvisioningConstants.TRUE);
            Account acct = prov.get(AccountBy.id, acctId);
            
            boolean caughtLimitExceeded = false;
            try {
                prov.modifyAttrs(acct, attrs);
            } catch (ServiceException e) {
                if (AccountServiceException.TOO_MANY_ACCOUNTS.equals(e.getCode())) {
                    caughtLimitExceeded = true;
                } else {
                    throw e;
                }
            }
            
            if (i == FEATURE_MAX_ACCOUNTS) {
                assertTrue(caughtLimitExceeded);
            } else {
                assertFalse(caughtLimitExceeded);
            }
        }
    }

}
