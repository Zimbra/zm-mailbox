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
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.ZAttrProvisioning.CalResType;
import com.zimbra.cs.account.soap.SoapProvisioning;


public class TestSearchCalendarResources {

    private static final String BASE_DOMAIN_NAME = "search-calendar-resource";
    private static final String DOMAIN_LDAP = "ldap." + BASE_DOMAIN_NAME;
    private static final String DOMAIN_GSA = "gsa." + BASE_DOMAIN_NAME;
    private static final String AUTHED_USER = "user1";
    
    private static final String KEY_FOR_SEARCH_BY_NAME = "meeting";
    private static final String ROOM_1 = "meeting-room-1";
    private static final String ROOM_2 = "meeting-room-2";
    private static final String ROOM_3 = "meeting-room-3";
    
    private static final String SITE_WORD_1 = "Palo";
    private static final String SITE_WORD_2 = "Alto";
    private static final String SITE = SITE_WORD_1 + " " + SITE_WORD_2;
    
    private static class MockCalendarResource extends CalendarResource {
        MockCalendarResource(Element e) throws ServiceException {
            super(e.getAttribute(AdminConstants.A_NAME), e.getAttribute(AdminConstants.A_ID), 
                    SoapProvisioning.getAttrs(e), null, null);
        }
    }
    
   
    private void searchByName(boolean ldap, String domainName) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        TestSearchGal.authUser(transport, TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST);
        
        request.addAttribute(MailConstants.A_QUERY_OFFSET, 1);
        request.addAttribute(MailConstants.A_QUERY_LIMIT, 2);
        
        request.addElement(AccountConstants.E_NAME).setText(KEY_FOR_SEARCH_BY_NAME);
        
        Element response = transport.invoke(request);
        
        boolean paginationSupported = response.getAttributeBool(AccountConstants.A_PAGINATION_SUPPORTED);
        
            
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;
        
        List<CalendarResource> resources = new ArrayList<CalendarResource>();
        for (Element eResource: response.listElements(AccountConstants.E_CALENDAR_RESOURCE)) {
            CalendarResource resource = new MockCalendarResource(eResource);
            resources.add(resource);
            
            if (resource.getName().equals(TestUtil.getAddress(ROOM_1, domainName))) {
                found1 = true;
            }
            
            if (resource.getName().equals(TestUtil.getAddress(ROOM_2, domainName))) {
                found2 = true;
            }
            
            if (resource.getName().equals(TestUtil.getAddress(ROOM_3, domainName))) {
                found3 = true;
            }
            
        }
        
        if (ldap) {
            // pagination is not supported
            Assert.assertFalse(paginationSupported);
            
            // offset and limit are not honored
            Assert.assertEquals(3, resources.size());
            Assert.assertTrue(found1);
            Assert.assertTrue(found2);
            Assert.assertTrue(found3);
        } else {
            // pagination is supported
            Assert.assertTrue(paginationSupported);
            
            // offset and limit are honored
            Assert.assertEquals(2, resources.size());
            Assert.assertTrue(found1);
            Assert.assertTrue(found2);
            Assert.assertTrue(!found3); // at offset 0 (gal sync acount mailbox search is by dateDesc order) 
                                 // - not within specified offset 
        }
    }
    
    private void searchByFilter(boolean ldap, String domainName) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        TestSearchGal.authUser(transport, TestUtil.getAddress(AUTHED_USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST);
        
        request.addAttribute(MailConstants.A_QUERY_OFFSET, 1);
        request.addAttribute(MailConstants.A_QUERY_LIMIT, 1);
        
        Element eSearchFilter = request.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
        Element eConds = eSearchFilter.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
        
        Element eCondResType = eConds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, "zimbraCalResType");
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, Operator.eq.name());
        eCondResType.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "Location");
        
        Element eCondResCapacity = eConds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        eCondResCapacity.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, "zimbraCalResCapacity");
        eCondResCapacity.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, Operator.ge.name());
        eCondResCapacity.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "15");
        
        Element eCondResSite = eConds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, "zimbraCalResSite");
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, Operator.has.name());
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, SITE_WORD_1);
        
        Element response = transport.invoke(request);
        
        boolean paginationSupported = response.getAttributeBool(AccountConstants.A_PAGINATION_SUPPORTED);
        
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;
        
        List<CalendarResource> resources = new ArrayList<CalendarResource>();
        for (Element eResource: response.listElements(AccountConstants.E_CALENDAR_RESOURCE)) {
            CalendarResource resource = new MockCalendarResource(eResource);
            resources.add(resource);
            
            if (resource.getName().equals(TestUtil.getAddress(ROOM_1, domainName))) {
                found1 = true;
            }
            
            if (resource.getName().equals(TestUtil.getAddress(ROOM_2, domainName))) {
                found2 = true;
            }
            
            if (resource.getName().equals(TestUtil.getAddress(ROOM_3, domainName))) {
                found3 = true;
            }

        }
        
        if (ldap) {
            // pagination is not supported
            Assert.assertFalse(paginationSupported);

            // offset and limit are not honored
            Assert.assertEquals(2, resources.size());
            Assert.assertTrue(!found1);  // not matching capacity requirement
            Assert.assertTrue(found2); 
            Assert.assertTrue(found3);
        } else {
            // pagination is supported
            Assert.assertTrue(paginationSupported);
            
            // offset and limit are honored
            Assert.assertEquals(1, resources.size());
            Assert.assertTrue(!found1);   // not matching capacity requirement   
            Assert.assertTrue(found2);   
            Assert.assertTrue(!found3);  // at offset 0 (gal sync acount mailbox search is by dateDesc order) 
                                         // - not within specified offset  
        }
    }
    
    private static void createCalendarResource(String name, String displayName,
            CalResType type, int capacity, String site) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, displayName);
        attrs.put(Provisioning.A_zimbraCalResType, type.name());
        attrs.put(Provisioning.A_zimbraCalResCapacity, String.valueOf(capacity));
        attrs.put(Provisioning.A_zimbraCalResSite, site);
        
        prov.createCalendarResource(name, "test123", attrs);
    }
    
    private static void createDomainObjects(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        if (prov.get(Key.DomainBy.name, domainName) == null) {
            ZimbraLog.test.info("Creating domain " + domainName);
            prov.createDomain(domainName, new HashMap<String, Object>());
        }
        
        if (prov.get(AccountBy.name, TestUtil.getAddress(AUTHED_USER, domainName)) == null) {
            prov.createAccount(TestUtil.getAddress(AUTHED_USER, domainName), "test123", null);
        }
        
        String calResName;
        
        calResName = TestUtil.getAddress(ROOM_1, domainName);
        if (prov.get(Key.CalendarResourceBy.name, calResName) == null) {
            createCalendarResource(calResName, ROOM_1, CalResType.Location, 10, SITE);
        }
        
        calResName = TestUtil.getAddress(ROOM_2, domainName);
        if (prov.get(Key.CalendarResourceBy.name, calResName) == null) {
            createCalendarResource(calResName, ROOM_2, CalResType.Location, 20, SITE);
        }
        
        calResName = TestUtil.getAddress(ROOM_3, domainName);
        if (prov.get(Key.CalendarResourceBy.name, calResName) == null) {
            createCalendarResource(calResName, ROOM_3, CalResType.Location, 100, SITE);
        }
    }
    
    private static void deleteDomainObjects(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        TestSearchGal.disableGalSyncAccount(prov, domainName);
        
        Account acct = prov.get(AccountBy.name, TestUtil.getAddress(AUTHED_USER, domainName));
        if (acct != null) {
            prov.deleteAccount(acct.getId());
        }
        
        CalendarResource resource;
        String calResName;
        
        calResName = TestUtil.getAddress(ROOM_1, domainName);
        resource = prov.get(Key.CalendarResourceBy.name, calResName);
        if (resource != null) {
            prov.deleteCalendarResource(resource.getId());
        }
        
        calResName = TestUtil.getAddress(ROOM_2, domainName);
        resource = prov.get(Key.CalendarResourceBy.name, calResName);
        if (resource != null) {
            prov.deleteCalendarResource(resource.getId());
        }
        
        calResName = TestUtil.getAddress(ROOM_3, domainName);
        resource = prov.get(Key.CalendarResourceBy.name, calResName);
        if (resource != null) {
            prov.deleteCalendarResource(resource.getId());
        }
        
        Domain domain = prov.get(Key.DomainBy.name, domainName);
        if (domain != null) {
            ZimbraLog.test.info("Deleting domain " + domainName);
            prov.deleteDomain(domain.getId());
        }
    }
    
    @BeforeClass 
    public static void init() throws Exception {
        TestUtil.cliSetup(); // use SoapProvisioning
        
        createDomainObjects(DOMAIN_LDAP);
        createDomainObjects(DOMAIN_GSA);
    }
    
    @AfterClass 
    public static void cleanup() throws Exception {
        deleteDomainObjects(DOMAIN_LDAP);
        deleteDomainObjects(DOMAIN_GSA);
        
        // can't do this, it needs LdapProv
        // TestLdap.deleteEntireBranch(BASE_DOMAIN_NAME);
    }
    
    @Test
    public void testGSASerarhByName() throws Exception {
        TestSearchGal.enableGalSyncAccount(Provisioning.getInstance(), DOMAIN_GSA);
        searchByName(false, DOMAIN_GSA);
    }
    
    @Test
    public void testGSASerarhByFilter() throws Exception {
        TestSearchGal.enableGalSyncAccount(Provisioning.getInstance(), DOMAIN_GSA);
        searchByFilter(false, DOMAIN_GSA);
    }
    
    @Test
    public void testLdapSerarhByName() throws Exception {
        searchByName(true, DOMAIN_LDAP);
    }
    
    @Test
    public void testLdapSerarhByFilter() throws Exception {
        searchByFilter(true, DOMAIN_LDAP);
    }

}
