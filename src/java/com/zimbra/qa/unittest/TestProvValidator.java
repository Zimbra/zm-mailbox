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

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ProvisioningConstants;

public class TestProvValidator extends TestProv {
    
    private String makeCosLimit(Cos cos, int limit) {
        return cos.getId() + ":" + limit;
    }
    
    private String makeFeatureLimit(String feature, int limit) {
        return feature + ":" + limit;
    }
    
    private Domain createDomainWithCosLimit(Cos cos, int limit) throws Exception {
        Domain domain = createDomain();
        
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        String cosLomit = makeCosLimit(cos, limit); 
        domain.addDomainCOSMaxAccounts(cosLomit, domainAttrs);
        mProv.modifyAttrs(domain, domainAttrs);
        
        return domain;
    }
    
    private Domain createDomainWithFeatureLimit(String feature, int limit) throws Exception {
        Domain domain = createDomain();
        
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        String featureLimit = makeFeatureLimit(feature, limit); 
        domain.addDomainFeatureMaxAccounts(featureLimit, domainAttrs);
        mProv.modifyAttrs(domain, domainAttrs);
        
        return domain;
    }
    
    @Test
    public void testCOSMaxCreateAccount() throws Exception {
        
        final int COS_MAX_ACCOUNTS = 2;
        
        Cos cos = createCos();
        Domain domain = createDomainWithCosLimit(cos, COS_MAX_ACCOUNTS);
        
        for (int i = 0; i <= COS_MAX_ACCOUNTS; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
            
            boolean caughtLimitExceeded = false;
            try {
                Account acct = createAccount("acct-" + i, domain, attrs);
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
        
        deleteAllEntries();
        
    }
    
    @Test
    public void testCOSMaxModifyAccount() throws Exception {
        final int COS_MAX_ACCOUNTS = 2;
        
        Cos cos = createCos();
        Domain domain = createDomainWithCosLimit(cos, COS_MAX_ACCOUNTS);
        
        for (int i = 0; i < COS_MAX_ACCOUNTS; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
            Account acct = createAccount("acct-" + i, domain, attrs);
        }
        
        // create an account on the default cos
        Account acct = createAccount("acct-on-default-cos", domain, null);
        
        boolean caughtLimitExceeded = false;
        try {
            // attempt to change the account to the cos at limit
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
            mProv.modifyAttrs(acct, attrs);
        } catch (ServiceException e) {
            if (AccountServiceException.TOO_MANY_ACCOUNTS.equals(e.getCode())) {
                caughtLimitExceeded = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtLimitExceeded);
        
        deleteAllEntries();
    }
    
    @Test
    public void testFeatureMaxCreateAccount() throws Exception {
        final String FEATURE = Provisioning.A_zimbraFeatureAdvancedSearchEnabled;
        final int FEATURE_MAX_ACCOUNTS = 2;
        
        Domain domain = createDomainWithFeatureLimit(FEATURE, FEATURE_MAX_ACCOUNTS);
        
        for (int i = 0; i <= FEATURE_MAX_ACCOUNTS; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(FEATURE, ProvisioningConstants.TRUE);
            
            boolean caughtLimitExceeded = false;
            try {
                Account acct = createAccount("acct-" + i, domain, null);
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
        
        deleteAllEntries();
    }
    
    @Test
    @Ignore  // bug existed prior to unboundid SDK work
    public void testFeatureMaxModifyAccount() throws Exception {
        final String FEATURE = Provisioning.A_zimbraFeatureAdvancedSearchEnabled;
        final int FEATURE_MAX_ACCOUNTS = 2;
        
        Domain domain = createDomainWithFeatureLimit(FEATURE, FEATURE_MAX_ACCOUNTS);
        
        List<String> acctIds = new ArrayList<String /* account id */>();
        for (int i = 0; i <= FEATURE_MAX_ACCOUNTS; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(FEATURE, ProvisioningConstants.FALSE);
            Account acct = createAccount("acct-" + i, domain, attrs);
        }
        
        for (int i = 0; i <= FEATURE_MAX_ACCOUNTS; i++) {
            String acctId = acctIds.get(i);
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(FEATURE, ProvisioningConstants.TRUE);
            Account acct = mProv.get(AccountBy.id, acctId);
            
            boolean caughtLimitExceeded = false;
            try {
                mProv.modifyAttrs(acct, attrs);
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
        
        deleteAllEntries();
    }

}
