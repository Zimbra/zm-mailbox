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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.ZAttrProvisioning.CalResType;

import junit.framework.TestCase;

public class TestSearchCalendarResources extends TestCase {

/*
<SearchCalendarResourcesRequest>
    [attrs="a1,a2,a3"] [sortBy="{sortBy}"] [sortAscending="{sortAscending}"] [limit="..."] [offset="..."]>

  [<name>...</name>]
  <searchFilter>
    <conds [not="1|0"] [or="1|0"] >
      [<cond> or <conds>]+
    </conds>  (exactly one instance of <conds>)

    -- or --

    <cond [not="1|0"] attr="{attr}" op="{op}" value="{value}" />  (exactly one instance of <cond>)
  </searchFilter>

</SearchCalendarResourcesRequest>

<SearchCalendarResourcesResponse [paginationSupported="1|0"]>
  <calresource name="{name}" id="{id}">
    <a n="...">...</a>+
  </calresource>*
</SearchCalendarResourcesResponse>

*/
    private static final String DOMAIN_LDAP = "ldap.galtest";
    private static final String DOMAIN_GSA = "gsa.galtest";
    private static final String USER = "user1";
    
    private static final String KEY_FOR_SEARCH_BY_NAME = "meeting";
    private static final String ROOM_1 = "meeting-room-1";
    private static final String ROOM_2 = "meeting-room-2";
    private static final String ROOM_3 = "meeting-room-3";
    private static final String SITE = "PaloAlto";
    
    private boolean mAllDone;
    
    private static class MockCalendarResource extends CalendarResource {
        MockCalendarResource(Element e) throws ServiceException {
            super(e.getAttribute(AdminConstants.A_NAME), e.getAttribute(AdminConstants.A_ID), 
                    SoapProvisioning.getAttrs(e), null, null);
        }
    }
    
   
    public void searchByName(boolean ldap, String domainName) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        TestSearchGal.authUser(transport, getAddress(USER, domainName));
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST);
        
        request.addAttribute(MailConstants.A_QUERY_OFFSET, 1);
        request.addAttribute(MailConstants.A_QUERY_LIMIT, 2);
        
        request.addElement(AccountConstants.E_NAME).setText(KEY_FOR_SEARCH_BY_NAME);
        
        Element response = transport.invoke(request);
        
        boolean paginationSupported = response.getAttributeBool(AccountConstants.A_PAGINATION_SUPPORTED);
        
            
        boolean found1 = false;
        boolean found2 = false;
        boolean found3= false;
        
        List<CalendarResource> resources = new ArrayList<CalendarResource>();
        for (Element eResource: response.listElements(AccountConstants.E_CALENDAR_RESOURCE)) {
            CalendarResource resource = new MockCalendarResource(eResource);
            resources.add(resource);
            
            if (resource.getName().equals(getAddress(ROOM_1, domainName))) {
                found1 = true;
            }
            
            if (resource.getName().equals(getAddress(ROOM_2, domainName))) {
                found2 = true;
            }
            
            if (resource.getName().equals(getAddress(ROOM_3, domainName))) {
                found3 = true;
            }
            
        }
        
        if (ldap) {
            // pagination is not supported
            assertFalse(paginationSupported);
            
            // offset and limit are not honored
            assertEquals(3, resources.size());
            assertTrue(found1);
            assertTrue(found2);
            assertTrue(found3);
        } else {
            // pagination is supported
            assertFalse(!paginationSupported);
            
            // offset and limit are honored
            assertEquals(2, resources.size());
            assertTrue(found1);
            assertTrue(found2);
            assertTrue(!found3); // at offset 0 (gal sync acount mailbox search is by dateDesc order) 
                                 // - not within specified offset 
        }
    }
    
    public void searchByFilter(boolean ldap, String domainName) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        TestSearchGal.authUser(transport, getAddress(USER, domainName));
        
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
        eCondResSite.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, SITE);
        
        Element response = transport.invoke(request);
        
        boolean paginationSupported = response.getAttributeBool(AccountConstants.A_PAGINATION_SUPPORTED);
        
        boolean found1 = false;
        boolean found2 = false;
        boolean found3= false;
        
        List<CalendarResource> resources = new ArrayList<CalendarResource>();
        for (Element eResource: response.listElements(AccountConstants.E_CALENDAR_RESOURCE)) {
            CalendarResource resource = new MockCalendarResource(eResource);
            resources.add(resource);
            
            if (resource.getName().equals(getAddress(ROOM_1, domainName))) {
                found1 = true;
            }
            
            if (resource.getName().equals(getAddress(ROOM_2, domainName))) {
                found2 = true;
            }
            
            if (resource.getName().equals(getAddress(ROOM_3, domainName))) {
                found3 = true;
            }

        }
        
        if (ldap) {
            // pagination is not supported
            assertEquals(ldap, !paginationSupported);

            // offset and limit are not honored
            assertEquals(2, resources.size());
            assertTrue(!found1);  // not matching capacity requirement
            assertTrue(found2); 
            assertTrue(found3);
        } else {
            // pagination is supported
            assertEquals(ldap, !paginationSupported);
            
            // offset and limit are honored
            assertEquals(1, resources.size());
            assertTrue(!found1);   // not matching capacity requirement   
            assertTrue(found2);   
            assertTrue(!found3);  // at offset 0 (gal sync acount mailbox search is by dateDesc order) 
                                  // - not within specified offset  
        }
    }
    
    private String getAddress(String local, String domain) {
        return local + "@" + domain;
    }
    
    private void createDomainObjects(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        if (prov.get(DomainBy.name, domainName) == null) {
            prov.createDomain(domainName, new HashMap<String, Object>());
        }
        
        if (prov.get(AccountBy.name, getAddress(USER, domainName)) == null) {
            prov.createAccount(getAddress(USER, domainName), "test123", null);
        }
        
        String calResName;
        
        calResName = getAddress(ROOM_1, domainName);
        if (prov.get(CalendarResourceBy.name, calResName) == null) {
            TestSearchGal.createCalendarResource(calResName, ROOM_1, CalResType.Location, 10, SITE);
        }
        
        calResName = getAddress(ROOM_2, domainName);
        if (prov.get(CalendarResourceBy.name, calResName) == null) {
            TestSearchGal.createCalendarResource(calResName, ROOM_2, CalResType.Location, 20, SITE);
        }
        
        calResName = getAddress(ROOM_3, domainName);
        if (prov.get(CalendarResourceBy.name, calResName) == null) {
            TestSearchGal.createCalendarResource(calResName, ROOM_3, CalResType.Location, 100, SITE);
        }
    }
    
    private void deleteDomainObjects(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        TestSearchGal.disableGalSyncAccount(domainName);
        
        Account acct = prov.get(AccountBy.name, getAddress(USER, domainName));
        if (acct != null) {
            prov.deleteAccount(acct.getId());
        }
        
        CalendarResource resource;
        String calResName;
        
        calResName = getAddress(ROOM_1, domainName);
        resource = prov.get(CalendarResourceBy.name, calResName);
        if (resource != null) {
            prov.deleteCalendarResource(resource.getId());
        }
        
        calResName = getAddress(ROOM_2, domainName);
        resource = prov.get(CalendarResourceBy.name, calResName);
        if (resource != null) {
            prov.deleteCalendarResource(resource.getId());
        }
        
        calResName = getAddress(ROOM_3, domainName);
        resource = prov.get(CalendarResourceBy.name, calResName);
        if (resource != null) {
            prov.deleteCalendarResource(resource.getId());
        }
        
        Domain domain = prov.get(DomainBy.name, domainName);
        if (domain != null) {
            prov.deleteDomain(domain.getId());
        }
    }
    
    public void setUp() throws Exception {
        createDomainObjects(DOMAIN_LDAP);
        createDomainObjects(DOMAIN_GSA);
    }
    
    public void tearDown() throws Exception {
        if (!mAllDone) {
            return;
        }
        
        deleteDomainObjects(DOMAIN_LDAP);
        deleteDomainObjects(DOMAIN_GSA);
    }
    
    public void testGalSyncAccountSerarhByName() throws Exception {
        TestSearchGal.enableGalSyncAccount(DOMAIN_GSA);
        searchByName(false, DOMAIN_GSA);
    }
    
    public void testGalSyncAccountSerarhByFilter() throws Exception {
        TestSearchGal.enableGalSyncAccount(DOMAIN_GSA);
        searchByFilter(false, DOMAIN_GSA);
    }
    
    public void testLdapSerarhByName() throws Exception {
        searchByName(true, DOMAIN_LDAP);
    }
    
    public void testLdapSerarhByFilter() throws Exception {
        searchByFilter(true, DOMAIN_LDAP);
    }
    
    public void testDone() throws Exception {
        mAllDone = true;
    }
    
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSearchCalendarResources.class);
    }
    
}
