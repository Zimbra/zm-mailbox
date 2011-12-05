/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.qa.unittest.prov.soap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.gal.ZimbraGalSearchBase.PredefinedSearchBase;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.BinaryLdapData;

public class TestSearchGal extends SoapTest {
    private static final boolean DO_CREATE_DOMAINS = true;
        
    private static String DOMAIN_LDAP;
    private static String DOMAIN_GSA;
    private static String SUB1_DOMAIN_LDAP;
    private static String SUB1_DOMAIN_GSA;
    private static String SUB2_DOMAIN_LDAP;
    private static String SUB2_DOMAIN_GSA;
    
    private static final String AUTHED_USER = "user1";
    
    private static final String KEY_FOR_SEARCH_BY_NAME = "account";
    private static final String ACCOUNT_PREFIX = "account";
    private static final String DISTRIBUTION_LIST_PREFIX = "dl";
    private static final String DYNAMIC_GROUP_PREFIX = "dyngroup";
    private static final String DEPARTMENT_PREFIX = "engineering";
    
    private static final String BINARY_LDAP_ATTR = Provisioning.A_userSMIMECertificate;
    private static final String BINARY_GALCONTACT_FIELD = ContactConstants.A_userSMIMECertificate;
    private static final int ACCOUNT_CONTAINS_BINARY_DATA = 5;
    private static final int NUM_BYTES_IN_BINARY_DATA = 100;
    private static final BinaryLdapData.Content BINARY_CONTENT_1 = 
        BinaryLdapData.Content.generateContent(NUM_BYTES_IN_BINARY_DATA);
    private static final BinaryLdapData.Content BINARY_CONTENT_2 = 
        BinaryLdapData.Content.generateContent(NUM_BYTES_IN_BINARY_DATA);
    
    private static final int NUM_ACCOUNTS = 10; 
    private static final int NUM_DISTRIBUTION_LISTS = 10; 
    private static final int NUM_DYNAMIC_GROUPS = 10; 
    private static final int NUM_ALL_OBJECTS = 
        NUM_ACCOUNTS + NUM_DISTRIBUTION_LISTS + NUM_DYNAMIC_GROUPS; 
    
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        
        String BASE_DOMAIN_NAME = baseDomainName();
        
        // domains for fallback to LDAP test
        DOMAIN_LDAP = "ldap." + BASE_DOMAIN_NAME;
        SUB1_DOMAIN_LDAP = "sub1." + DOMAIN_LDAP;
        SUB2_DOMAIN_LDAP = "sub2." + SUB1_DOMAIN_LDAP;
        
        // domains for mailbox search test
        DOMAIN_GSA = "gsa." + BASE_DOMAIN_NAME;
        SUB1_DOMAIN_GSA = "sub1." + DOMAIN_GSA;
        SUB2_DOMAIN_GSA = "sub2." + SUB1_DOMAIN_GSA;
        
        if (DO_CREATE_DOMAINS) {
            createDomainObjects(DOMAIN_LDAP, null); // search base should default to DOMAIN
            createDomainObjects(SUB1_DOMAIN_LDAP, PredefinedSearchBase.SUBDOMAINS);
            createDomainObjects(SUB2_DOMAIN_LDAP, PredefinedSearchBase.DOMAIN);
            
            createDomainObjects(DOMAIN_GSA, null); // search base should default to DOMAIN
            createDomainObjects(SUB1_DOMAIN_GSA, PredefinedSearchBase.SUBDOMAINS);
            createDomainObjects(SUB2_DOMAIN_GSA, PredefinedSearchBase.DOMAIN);
        }
    }
    
    @AfterClass 
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    /*
    static void authAdmin(SoapTransport transport, String acctName) throws Exception {
        
        Element request = Element.create(transport.getRequestProtocol(), AdminConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(acctName);
        request.addElement(AccountConstants.E_PASSWORD).setText("test123");
        
        Element response = transport.invoke(request);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
    }
    */
    
    private void searchWithDot(boolean ldap, String domainName, int expectedNumEntries) throws Exception {
        SoapTransport transport = authUser(TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        request.addElement(AccountConstants.E_NAME).setText(".");
        
        Element response = transport.invoke(request);
        
        boolean paginationSupported = response.getAttributeBool(AccountConstants.A_PAGINATION_SUPPORTED);
        
        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        if (ldap) {
            // pagination is not supported
            Assert.assertFalse(paginationSupported);
        } else {
            // pagination is supported
            Assert.assertTrue(paginationSupported);
        }
        
        // should find all objects, plus the authed user
        Assert.assertEquals(expectedNumEntries, result.size());
    }
    
    private void searchWithOffsetLimit(boolean ldap, String domainName) throws Exception {
        SoapTransport transport = authUser(TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        request.addElement(AccountConstants.E_NAME).setText(".");
        
        int offset = 5;
        int limit = 3;
        request.addAttribute(MailConstants.A_QUERY_OFFSET, offset);
        request.addAttribute(MailConstants.A_QUERY_LIMIT, limit);
        request.addAttribute(MailConstants.A_SORTBY, "nameAsc");
        
        Element response = transport.invoke(request);
        
        boolean paginationSupported = response.getAttributeBool(AccountConstants.A_PAGINATION_SUPPORTED);
        
        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        if (ldap) {
            // pagination is not supported
            Assert.assertFalse(paginationSupported);
            
            // limit is ignored, ldap search is limited by zimbraGalMaxResults
            // should find all objects, plus the authed user
            Assert.assertEquals(NUM_ALL_OBJECTS + 1, result.size());
        } else {
            // pagination is supported
            Assert.assertTrue(paginationSupported);
            
            Assert.assertEquals(limit, result.size());
            for (int i = 0; i < limit; i++) {
                Assert.assertEquals(getAcctEmail(offset + i, domainName), result.get(i).getSingleAttr(ContactConstants.A_email));
            }
        }
        
    }
    
    private void searchByName(boolean ldap, String domainName) throws Exception {
        SoapTransport transport = authUser(TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        request.addElement(AccountConstants.E_NAME).setText(KEY_FOR_SEARCH_BY_NAME);
        
        Element response = transport.invoke(request);
        
        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        Assert.assertEquals(NUM_ACCOUNTS, result.size());
    }
    
    private void searchByFilter(boolean ldap, String domainName) throws Exception {
        SoapTransport transport = authUser(TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        request.addElement(AccountConstants.E_NAME).setText(".");

        
        Element eSearchFilter = request.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
        Element eConds = eSearchFilter.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
        
        int acctToMatch = 8;
        Element eCondResType = eConds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, ContactConstants.A_email);
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, Operator.has.name());
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, acctToMatch);
        
        String matchDepartment = getDepartment(acctToMatch, domainName);
        Element eCondResSite = eConds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, ContactConstants.A_department);
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, Operator.has.name());
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, matchDepartment);
        
        Element response = transport.invoke(request);

        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(getAcctEmail(acctToMatch, domainName), result.get(0).getSingleAttr(ContactConstants.A_email));
    }

    private void binaryDataInEntry(boolean ldap, String domainName, boolean wantSMIMECert) throws Exception {
        SoapTransport transport = authUser(TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
        if (wantSMIMECert) {
            request.addAttribute(AccountConstants.A_NEED_SMIME_CERTS, true);
        }
        request.addElement(AccountConstants.E_NAME).setText(getAcctEmail(ACCOUNT_CONTAINS_BINARY_DATA, domainName));
        
        Element response = transport.invoke(request);
        
        List<GalContact> result = new ArrayList<GalContact>();
        for (Element e: response.listElements(AdminConstants.E_CN)) {
            result.add(new GalContact(AdminConstants.A_ID, SoapProvisioning.getAttrs(e)));
        }
  
        Assert.assertEquals(1, result.size());
        GalContact galContact = result.get(0);
        Map<String, Object> fields = galContact.getAttrs();
        Object value = fields.get(BINARY_GALCONTACT_FIELD);
        
        if (!wantSMIMECert) {
            Assert.assertNull(value);
            return;
        }
        
        Assert.assertTrue(value instanceof String[]);
        String[] values = (String[])value;
        Assert.assertEquals(2, values.length);
        
        boolean foundContent1 = false;
        boolean foundContent2 = false;
        for (String valueAsString : values) {
            if (BINARY_CONTENT_1.equals(valueAsString)) {
                foundContent1 = true;
            }
            
            if (BINARY_CONTENT_2.equals(valueAsString)) {
                foundContent2 = true;
            }
        }
        
        Assert.assertTrue(foundContent1);
        Assert.assertTrue(foundContent2);
    }
    
    private static String getAcctEmail(int index, String domainName) {
        return TestUtil.getAddress(ACCOUNT_PREFIX + "-" + index, domainName);
    }
    
    private static String getDistributionListEmail(int index, String domainName) {
        return TestUtil.getAddress(DISTRIBUTION_LIST_PREFIX + "-" + index, domainName);
    }
    
    private static String getDynamicGroupEmail(int index, String domainName) {
        return TestUtil.getAddress(DYNAMIC_GROUP_PREFIX + "-" + index, domainName);
    }
    
    private static String getDepartment(int index, String domainName) {
        return TestUtil.getAddress(DEPARTMENT_PREFIX + "-" + index, domainName);
    }
    
    private static void createDomainObjects(String domainName, 
            PredefinedSearchBase searchBase) 
    throws Exception {
        Map<String, Object> attrs;
        
        ZimbraLog.test.info("Creating domain " + domainName);
        attrs = null;
        if (searchBase != null) {
            attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraGalInternalSearchBase, searchBase.name());
        }
        prov.createDomain(domainName, attrs);
        
        prov.createAccount(TestUtil.getAddress(AUTHED_USER, domainName), "test123", null);
        
        
        /*
         * create accounts
         */
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            String acctName = getAcctEmail(i, domainName);
            attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_ou, getDepartment(i, domainName));
                
            if (ACCOUNT_CONTAINS_BINARY_DATA == i) {
                StringUtil.addToMultiMap(attrs, BINARY_LDAP_ATTR, BINARY_CONTENT_1.getString());
                StringUtil.addToMultiMap(attrs, BINARY_LDAP_ATTR, BINARY_CONTENT_2.getString());
            }
            prov.createAccount(acctName, "test123", attrs);
        }
        // create a hideInGal account
        String acctName = TestUtil.getAddress(ACCOUNT_PREFIX + "-hide-in-gal", domainName);
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, ProvisioningConstants.TRUE);
        prov.createAccount(acctName, "test123", attrs);
        
        
        /*
         * create distribution lists
         */
        for (int i = 0; i < NUM_DISTRIBUTION_LISTS; i++) {
            String dlName = getDistributionListEmail(i, domainName);
            prov.createGroup(dlName, null, false);
        }
        // create a hideInGal DL
        String dlName = TestUtil.getAddress(DISTRIBUTION_LIST_PREFIX + "-hide-in-gal", domainName);
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, ProvisioningConstants.TRUE);
        prov.createGroup(dlName, attrs, false);
        
        
        /*
         * create dynamic groups
         */
        for (int i = 0; i < NUM_DYNAMIC_GROUPS; i++) {
            String dynGroupName = getDynamicGroupEmail(i, domainName);
            prov.createGroup(dynGroupName, null, true);
        }
        // create a hideInGal dynamic group
        String dynGroupName = TestUtil.getAddress(DYNAMIC_GROUP_PREFIX + "-hide-in-gal", domainName);
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, ProvisioningConstants.TRUE);
        prov.createGroup(dynGroupName, attrs, true);
        
    }
    
    @Test
    public void GSASerarhWithDot() throws Exception {
        GalTestUtil.enableGalSyncAccount(prov, DOMAIN_GSA);
        // expect all accounts, dls, dynamic groups in the domain, plus the authed user
        searchWithDot(false, DOMAIN_GSA, NUM_ALL_OBJECTS + 1);
    }
    
    @Test
    public void GSASerarhWithDotSubDomain() throws Exception {
        GalTestUtil.enableGalSyncAccount(prov, SUB1_DOMAIN_GSA);
        // expect all objects in SUB1_DOMAIN_GSA *and* SUB2_DOMAIN_GSA
        searchWithDot(false, SUB1_DOMAIN_GSA, (NUM_ALL_OBJECTS + 1)*2);
    }
    
    @Test
    public void GSASerarhWithOffsetlimit() throws Exception {
        GalTestUtil.enableGalSyncAccount(prov, DOMAIN_GSA);
        searchWithOffsetLimit(false, DOMAIN_GSA);
    }
    
    @Test
    public void GSASerarhByName() throws Exception {
        GalTestUtil.enableGalSyncAccount(prov, DOMAIN_GSA);
        searchByName(false, DOMAIN_GSA);
    }
    
    @Test
    public void GSASerarhByFilter() throws Exception {
        GalTestUtil.enableGalSyncAccount(prov, DOMAIN_GSA);
        searchByFilter(false, DOMAIN_GSA);
    }
    
    @Test
    public void GSASerarhEntryWithBinaryData() throws Exception {
        GalTestUtil.enableGalSyncAccount(prov, DOMAIN_GSA);
        binaryDataInEntry(false, DOMAIN_GSA, true);
        binaryDataInEntry(false, DOMAIN_GSA, false);
    }

    @Test
    public void fallbackToLDAPSerarhWithDot() throws Exception {
        searchWithDot(true, DOMAIN_LDAP, NUM_ALL_OBJECTS + 1);
    }
    
    @Test
    public void fallbackToLDAPSerarhWithDotSubDomain() throws Exception {
        // expect all objects in SUB1_DOMAIN_LDAP *and* SUB2_DOMAIN_LDAP
        searchWithDot(true, SUB1_DOMAIN_LDAP, (NUM_ALL_OBJECTS + 1)*2);
    }
    
    @Test
    public void fallbackToLDAPSerarhWithOffsetlimit() throws Exception {
        searchWithOffsetLimit(true, DOMAIN_LDAP);
    }
    
    @Test
    public void fallbackToLDAPSerarhByName() throws Exception {
        searchByName(true, DOMAIN_LDAP);
    }
    
    @Test
    public void fallbackToLDAPSerarhByFilter() throws Exception {
        searchByFilter(true, DOMAIN_LDAP);
    }
    
    @Test
    public void fallbackToLDAPSerarhEntryWithBinaryData() throws Exception {
        binaryDataInEntry(true, DOMAIN_LDAP, true);
        binaryDataInEntry(true, DOMAIN_LDAP, false);
    }

}
