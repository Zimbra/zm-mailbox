/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.soap.SoapProvisioning;

public class TestCos extends TestCase {
    
    Provisioning mProv= Provisioning.getInstance();
    
    private String DOMAIN_NAME = "test.com";
    private String ACCT_NAME = "user@" + DOMAIN_NAME;
    private String COS1_NAME = "cos1";
    private String COS2_NAME = "cos2";
    private String DOMAIN_DEFAULT_COS_NAME = "domain-default-cos";
    
    private Account ACCT;
    private Cos COS1;
    private Cos COS2;
    private Cos DOMAIN_DEFAULT_COS;

    private long DEFAULT_COS_VALUE = 0;
    private Domain DOMAIN;
    
    private String ATTR_NAME = Provisioning.A_zimbraMailQuota;
    private long COS1_VALUE = 10000;
    private long COS2_VALUE = 20000;
    private long DOMAIN_DEFAULT_COS_VALUE = 30000;
    private long ACCOUNT_VALUE = 40000;
    
    public void setUp() throws Exception {
        cleanup();
        
        COS1 = createCos(COS1_NAME, COS1_VALUE);
        COS2 = createCos(COS2_NAME, COS2_VALUE);
        DOMAIN_DEFAULT_COS = createCos(DOMAIN_DEFAULT_COS_NAME, DOMAIN_DEFAULT_COS_VALUE);
        
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        domainAttrs.put(Provisioning.A_zimbraDomainDefaultCOSId, DOMAIN_DEFAULT_COS.getId());
        DOMAIN = mProv.createDomain(DOMAIN_NAME, domainAttrs);
        
        ACCT = mProv.createAccount(ACCT_NAME, "test123", null);
        
        // initialize the cached cos on the account
        Cos cos = mProv.getCOS(ACCT);
    }
    
    public void tearDown() throws ServiceException {
        cleanup();   
    }
    
    private void cleanup() {
        try {
            Cos cos1 = mProv.get(Provisioning.CosBy.name, COS1_NAME);
            if (cos1 != null) {
                // System.out.println("deleting COS " + cos1.getName());
                mProv.deleteCos(cos1.getId());
            }
        } catch (ServiceException e) {
        }
        
        try {
            Cos cos2 = mProv.get(Provisioning.CosBy.name, COS2_NAME);
            if (cos2 != null) {
                // System.out.println("deleting COS " + cos2.getName());
                mProv.deleteCos(cos2.getId());
            }
        } catch (ServiceException e) {
        }
        
        try {
            Cos domainDefaultCos = mProv.get(Provisioning.CosBy.name, DOMAIN_DEFAULT_COS_NAME);
            if (domainDefaultCos != null) {
                // System.out.println("deleting COS " + domainDefaultCos.getName());
                mProv.deleteCos(domainDefaultCos.getId());
            }
        } catch (ServiceException e) {
        }
        
        try {
            Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_NAME);
            if (acct != null) {
                // System.out.println("deleting COS " + acct.getName());
                mProv.deleteAccount(acct.getId());
            }
        } catch (ServiceException e) {
        }
        
        try {
            Domain domain = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME);
            if (domain != null) {
                // System.out.println("deleting COS " + domain.getName());
                mProv.deleteDomain(domain.getId());
            }
        } catch (ServiceException e) {
        }

    }
    
    private void reloadAccountIfSoap() throws ServiceException {
        if (mProv instanceof SoapProvisioning)
            ACCT = mProv.get(Provisioning.AccountBy.name, ACCT_NAME);
    }
    
    public void testDomainCos() throws Exception {
        assertEquals(DOMAIN_DEFAULT_COS_VALUE, getAttrValue());  
        
        // modify value on the domain default cos, should get the new value
        long newValue = DOMAIN_DEFAULT_COS_VALUE -1;
        modifyCosValue(DOMAIN_DEFAULT_COS, newValue);
        reloadAccountIfSoap();
        assertEquals(newValue, getAttrValue()); 
        
        // modify the value back
        modifyCosValue(DOMAIN_DEFAULT_COS, DOMAIN_DEFAULT_COS_VALUE);
        reloadAccountIfSoap();
        assertEquals(DOMAIN_DEFAULT_COS_VALUE, getAttrValue());  
        
        // modify domain default cos, should still be the old cos value
        // known bug A. this is fine for now - we don't have a use case/bug for this, have to fix if needed
        modifyDomainDefaultCos(COS1);
        assertEquals(DOMAIN_DEFAULT_COS_VALUE, getAttrValue()); 
        
        // flush cache, we now should get the updated cos value
        flushAccountCache();
        assertEquals(COS1_VALUE, getAttrValue());  // broken before
        
        // remove domain default cos, should still be the old cos value (known bug A)
        modifyDomainDefaultCos(null);
        assertEquals(COS1_VALUE, getAttrValue());
        
        // flush cache, we now should get the updated cos value
        flushAccountCache();
        assertEquals(DEFAULT_COS_VALUE, getAttrValue());     // broken before
    }
    
    public void testAcctCos() throws ServiceException {
        assertEquals(DOMAIN_DEFAULT_COS_VALUE, getAttrValue()); 
        
        // set cos on account
        modifyAccountCos(COS1);
        assertEquals(COS1_VALUE, getAttrValue());  // broken before (cause for bug 41875)
        
        // modify cos on account
        modifyAccountCos(COS2);
        assertEquals(COS2_VALUE, getAttrValue());  // broken before
        
        // remove cos from account, should get the domain default cos
        modifyAccountCos(null);
        assertEquals(DOMAIN_DEFAULT_COS_VALUE, getAttrValue()); // broken before
        
        // set cos on account again
        modifyAccountCos(COS1);
        assertEquals(COS1_VALUE, getAttrValue());  // broken before
        
        // set the attr directly on account
        modifyAccountValue("" + ACCOUNT_VALUE);
        assertEquals(ACCOUNT_VALUE, getAttrValue()); 
        
        // remove the account value, should get cos value
        modifyAccountValue("");
        assertEquals(COS1_VALUE, getAttrValue());
    }
    
    private long getAttrValue() {
        return ACCT.getLongAttr(ATTR_NAME, 0);
    }
    
    private Cos createCos(String cosName, long value) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ATTR_NAME, "" + value);
        return mProv.createCos(cosName, attrs);
    }
    
    private void modifyCosValue(Cos cos, long newValue) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ATTR_NAME, "" + newValue);
        mProv.modifyAttrs(cos, attrs);
    }
    
    private void modifyAccountValue(String newValue) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ATTR_NAME, newValue);
        mProv.modifyAttrs(ACCT, attrs);
    }
    
    private void modifyDomainDefaultCos(Cos cos) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (cos == null)
            attrs.put(Provisioning.A_zimbraDomainDefaultCOSId, "");
        else
            attrs.put(Provisioning.A_zimbraDomainDefaultCOSId, cos.getId());
        
        mProv.modifyAttrs(DOMAIN, attrs);
    }
    
    private void modifyAccountCos(Cos cos) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (cos == null)
            attrs.put(Provisioning.A_zimbraCOSId, "");
        else
            attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        
        mProv.modifyAttrs(ACCT, attrs);
    }
    
    
    private void flushAccountCache() throws ServiceException {
        mProv.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(CacheEntryBy.name, ACCT_NAME)});
        // reload the account
        ACCT = mProv.get(Provisioning.AccountBy.name, ACCT_NAME);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws ServiceException {
        // TestUtil.cliSetup(); // SoapProvisioning
        CliUtil.toolSetup();    // LdapProvisioning
        TestUtil.runTest(new TestSuite(TestCos.class));
    }

}
