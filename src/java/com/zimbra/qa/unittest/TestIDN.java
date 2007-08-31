/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

public class TestIDN extends TestCase {
    private String TEST_ID;
    private static String TEST_NAME = "test-IDN";
    private static String PASSWORD = "test123";
   
    
    private Provisioning mProv;
    private String BASE_DOMAIN_NAME;
   
    public void setUp() throws Exception {
        
        TEST_ID = TestProvisioningUtil.genTestId();
        
        System.out.println("\nTest " + TEST_ID + " setting up...\n");
        mProv = Provisioning.getInstance();
        BASE_DOMAIN_NAME = "." + TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
    }
    
    class IDNName {
        String mUincodeName;
        String mAsciiName;
        
        IDNName(String uName) {
            mUincodeName = uName;
            mAsciiName = IDNUtil.toAsciiDomainName(uName);
        }
        
        IDNName(String localPart, String uName) {
            mUincodeName = localPart + "@" + uName;
            mAsciiName = localPart + "@" + IDNUtil.toAsciiDomainName(uName);
        }
        
        String uName() { return mUincodeName; } 
        String aName() { return mAsciiName; }
    }
    
    private Domain createDomain(String domainName, String description) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_description, "====="+description+"=====");
        Domain domain = mProv.createDomain(domainName, attrs);
        assertNotNull(domain);
        return domain;
    }
    
    private Account createAccount(String email, String description) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_description, "====="+description+"=====");
        Account acct = mProv.createAccount(email, PASSWORD, attrs);
        assertNotNull(acct);
        return acct;
    }
    
    private CalendarResource createCalendarResource(String email, String description) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_description, "====="+description+"=====");
        attrs.put(Provisioning.A_displayName, email);
        attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        CalendarResource cr = mProv.createCalendarResource(email, PASSWORD, attrs);
        assertNotNull(cr);
        return cr;
    }
    
    private DistributionList createDistributionList(String email, String description) throws Exception {
        Map<String, Object> attrs= new HashMap<String, Object>();
        attrs.put(Provisioning.A_description, "====="+description+"=====");
        DistributionList dl = mProv.createDistributionList(email, attrs);
        assertNotNull(dl);
        return dl;
    }
    
    Domain domainTest() throws Exception {
        // String domainName = "abc.\u5f35\u611b\u73b2" + BASE_DOMAIN_NAME;
        
        IDNName d1Name = new IDNName("domain-1.\u4e2d\u6587" + BASE_DOMAIN_NAME);
        IDNName d2Name = new IDNName("domain-2.\u4e2d\u6587" + BASE_DOMAIN_NAME);
        
        // create domain with unicode name
        Domain domain1 = createDomain(d1Name.uName(), d1Name.uName());
        assertNotNull(domain1);
        try {
            // domain should have existed
            createDomain(d1Name.aName(), "domain " + d1Name.uName());
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(AccountServiceException.DOMAIN_EXISTS))
                fail();
        }
        
        // create domain with ascii name
        Domain domain2 = createDomain(d2Name.aName(), d2Name.uName());
        assertNotNull(domain2);
        try {
            // domain should have existed
            createDomain(d2Name.uName(), d2Name.uName());
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(AccountServiceException.DOMAIN_EXISTS))
                fail();
        }
        
        // get domain by ascii name
        Domain d1Get = mProv.get(Provisioning.DomainBy.name, d1Name.aName());
        assertEquals(domain1.getId(), d1Get.getId());
        
        // get domain by unicode name
        Domain d2Get = mProv.get(Provisioning.DomainBy.name, d2Name.uName());
        assertEquals(domain2.getId(), d2Get.getId());
        
        return domain1;
    }
    
    public void accountTest() throws Exception {
        
        IDNName domainName = new IDNName("domain-acct-test.\u4e2d\u6587" + BASE_DOMAIN_NAME);
        Domain domain = createDomain(domainName.uName(), domainName.uName());
        
        IDNName acct1Name = new IDNName("acct-1", domainName.uName());
        IDNName acct2Name = new IDNName("acct-2", domainName.uName());
        IDNName cr1Name = new IDNName("cr-1", domainName.uName());
        IDNName cr2Name = new IDNName("cr-2", domainName.uName());
        IDNName alias1Name = new IDNName("alias-1-of-acct-1", domainName.uName());
        IDNName alias2Name = new IDNName("alias-2-of-acct-1", domainName.uName());
        /*
         * account
         */
        Account acct1 = createAccount(acct1Name.uName(), acct1Name.uName());
        assertNotNull(acct1);
        try {
            createAccount(acct1Name.aName(), acct1Name.uName());
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(AccountServiceException.ACCOUNT_EXISTS))
                fail();
        }
        
        Account acct2 = createAccount(acct2Name.aName(), acct2Name.uName());
        assertNotNull(acct2);
        try {
            createAccount(acct2Name.uName(), acct2Name.uName());
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(AccountServiceException.ACCOUNT_EXISTS))
                fail();
        }
        
        // get by name
        Account a1GetByUName = mProv.get(Provisioning.AccountBy.name, acct1Name.uName());
        assertEquals(acct1.getId(), a1GetByUName.getId());
        Account a1GetByAName = mProv.get(Provisioning.AccountBy.name, acct1Name.aName());
        assertEquals(acct1.getId(), a1GetByAName.getId());
        
        //aliases
        mProv.addAlias(acct1, alias1Name.uName());  // add alias by uname
        mProv.addAlias(acct1, alias2Name.aName());  // add alias by aname
        // get by alias
        Account a1GetByAliasAName = mProv.get(Provisioning.AccountBy.name, alias1Name.aName());
        assertEquals(acct1.getId(), a1GetByAliasAName.getId());
        Account a1GetByAliasUName = mProv.get(Provisioning.AccountBy.name, alias2Name.uName());
        assertEquals(acct1.getId(), a1GetByAliasUName.getId());
        
        mProv.removeAlias(acct1, alias1Name.aName());
        mProv.removeAlias(acct1, alias2Name.uName());
        a1GetByAliasAName = mProv.get(Provisioning.AccountBy.name, alias1Name.aName());
        assertNull(a1GetByAliasAName);
        a1GetByAliasUName = mProv.get(Provisioning.AccountBy.name, alias2Name.uName());
        assertNull(a1GetByAliasUName);
        
        /*
         * cr
         */
        CalendarResource cr1 = createCalendarResource(cr1Name.uName(), cr1Name.uName());
        assertNotNull(cr1);
        try {
            createCalendarResource(cr1Name.aName(), cr1Name.uName());
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(AccountServiceException.ACCOUNT_EXISTS))
                fail();
        }
        
        CalendarResource cr2 = createCalendarResource(cr2Name.aName(), cr2Name.uName());
        assertNotNull(cr2);
        try {
            createCalendarResource(cr2Name.uName(), cr2Name.uName());
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(AccountServiceException.ACCOUNT_EXISTS))
                fail();
        }
        
        // get by name
        CalendarResource cr1GetByUName = mProv.get(Provisioning.CalendarResourceBy.name, cr1Name.uName());
        assertEquals(cr1.getId(), cr1GetByUName.getId());
        CalendarResource cr1GetByAName = mProv.get(Provisioning.CalendarResourceBy.name, cr1Name.aName());
        assertEquals(cr1.getId(), cr1GetByAName.getId());
    }
    
    public void distributionListTest() throws Exception {
        
        IDNName domainName = new IDNName("domain-dl-test.\u4e2d\u6587" + BASE_DOMAIN_NAME);
        Domain domain = createDomain(domainName.uName(), domainName.uName());
        
        IDNName acct1Name = new IDNName("acct-1", domainName.uName());
        IDNName acct2Name = new IDNName("acct-2", domainName.uName());
        IDNName cr1Name = new IDNName("cr-1", domainName.uName());
        IDNName cr2Name = new IDNName("cr-2", domainName.uName());
        IDNName dl1Name = new IDNName("dl-1", domainName.uName());
        IDNName dl2Name = new IDNName("dl-2", domainName.uName());
        IDNName nestedDl1Name = new IDNName("nested-dl-1", domainName.uName());
        IDNName nestedDl2Name = new IDNName("nested-dl-2", domainName.uName());

        /*
         * dl
         */
        DistributionList dl1 = createDistributionList(dl1Name.uName(), dl1Name.uName());
        assertNotNull(dl1);
        try {
            createDistributionList(dl1Name.aName(), dl1Name.uName());
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(AccountServiceException.DISTRIBUTION_LIST_EXISTS))
                fail();
        }
        
        DistributionList dl2 = createDistributionList(dl2Name.aName(), dl2Name.uName());
        assertNotNull(dl2);
        try {
            createDistributionList(dl2Name.uName(), dl2Name.uName());
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(AccountServiceException.DISTRIBUTION_LIST_EXISTS))
                fail();
        }
        
        DistributionList dl1GetByUName = mProv.get(Provisioning.DistributionListBy.name, dl1Name.uName());
        assertEquals(dl1.getId(), dl1GetByUName.getId());
        DistributionList dl1GetByAName = mProv.get(Provisioning.DistributionListBy.name, dl1Name.aName());
        assertEquals(dl1.getId(), dl1GetByAName.getId());
        
        /*
         * test dl members
         */
        Account a1 = createAccount(acct1Name.uName(), acct1Name.uName());
        Account a2 = createAccount(acct2Name.uName(), acct2Name.uName());
        DistributionList nestedDl1 = createDistributionList(nestedDl1Name.aName(), nestedDl1Name.uName());
        DistributionList nestedDl2 = createDistributionList(nestedDl2Name.aName(), nestedDl2Name.uName());
        
        mProv.addMembers(dl1, new String[]{acct1Name.uName(), acct2Name.aName(), nestedDl1Name.uName(), nestedDl2Name.aName()});
        
        boolean inList;
        inList = mProv.inDistributionList(a1, dl1.getId());
        assertTrue(inList);
        inList = mProv.inDistributionList(a2, dl1.getId());
        assertTrue(inList);
        
        HashMap<String,String> via = new HashMap<String, String>();
        String[] members = dl1.getAllMembers();
        List<String> memberIds = Arrays.asList(members);
        assertTrue(memberIds.contains(acct1Name.aName()));
        assertTrue(memberIds.contains(acct2Name.aName()));
        assertTrue(memberIds.contains(nestedDl1Name.aName()));
        assertTrue(memberIds.contains(nestedDl2Name.aName()));
        
        mProv.removeMembers(dl1, new String[]{acct1Name.uName(), acct2Name.aName(), nestedDl1Name.uName(), nestedDl2Name.aName()});
        members = dl1.getAllMembers();
        assertEquals(0, members.length);
    }
    
    public void testIDN() throws Exception {
        domainTest();
        accountTest();
        distributionListTest();        
        System.out.println("\nTest " + TEST_ID + " done\n");
    }
 
    public static void main(String[] args) throws Exception {
        /*
         * 
         * cliSetup forces instanciating a SoapProvisioning, for now we want LdapProvisioning
         * SOAP: UTF-8 (multi bytes)
         * LDAP: UTF-8 (multi bytes)
         * Java: UTF-16 (2 bytes Unicode) 
         *       The primitive data type char in the Java programming language is an unsigned 16-bit 
         *       integer that can represent a Unicode code point in the range U+0000 to U+FFFF, 
         *       or the code units of UTF-16.
         */
        // TestUtil.cliSetup();
        // TestUtil.runTest(new TestSuite(TestIDN.class));
        
        TestIDN t = new TestIDN();
        t.setUp();
        t.testIDN();
        
    }

}
