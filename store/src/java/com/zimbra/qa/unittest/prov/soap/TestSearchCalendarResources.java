/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ZAttrProvisioning.CalResType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.qa.QA.Bug;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.account.message.SearchCalendarResourcesRequest;
import com.zimbra.soap.account.message.SearchCalendarResourcesResponse;
import com.zimbra.soap.account.type.CalendarResourceInfo;
import com.zimbra.soap.account.type.EntrySearchFilterInfo;
import com.zimbra.soap.account.type.EntrySearchFilterMultiCond;
import com.zimbra.soap.account.type.EntrySearchFilterSingleCond;

public class TestSearchCalendarResources extends SoapTest {

    private static String DOMAIN_LDAP;
    private static String DOMAIN_GSA;
    private static final String AUTHED_USER = "user1";
    
    private static final String KEY_FOR_SEARCH_BY_NAME = "meeting";
    private static final String ROOM_1 = "meeting-room-1";
    private static final String ROOM_2 = "meeting-room-2";
    private static final String ROOM_3 = "meeting-room-3";
    
    private static final String SITE_WORD_1 = "Palo";
    private static final String SITE_WORD_2 = "Alto";
    private static final String SITE = SITE_WORD_1 + " " + SITE_WORD_2;
    
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
        
        String BASE_DOMAIN_NAME = baseDomainName();
        DOMAIN_LDAP = "ldap." + BASE_DOMAIN_NAME;
        DOMAIN_GSA = "gsa." + BASE_DOMAIN_NAME;
        
        createDomainObjects(DOMAIN_LDAP);
        createDomainObjects(DOMAIN_GSA);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private static class MockCalendarResource extends CalendarResource {
        MockCalendarResource(Element e) throws ServiceException {
            super(e.getAttribute(AdminConstants.A_NAME), e.getAttribute(AdminConstants.A_ID), 
                    SoapProvisioning.getAttrs(e), null, null);
        }
    }
    
    private static void createCalendarResource(String localPart, Domain domain,
            String displayName, CalResType type, int capacity, String site) 
    throws Exception {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_displayName, displayName);
        attrs.put(Provisioning.A_zimbraCalResType, type.name());
        attrs.put(Provisioning.A_zimbraCalResCapacity, String.valueOf(capacity));
        attrs.put(Provisioning.A_zimbraCalResSite, site);
        
        provUtil.createCalendarResource(localPart, domain, attrs);
    }
    
    private static void createDomainObjects(String domainName) throws Exception {
        Domain domain = provUtil.createDomain(domainName);
        provUtil.createAccount(AUTHED_USER, domain);
        
        createCalendarResource(ROOM_1, domain, ROOM_1, CalResType.Location, 10, SITE);
        createCalendarResource(ROOM_2, domain, ROOM_2, CalResType.Location, 20, SITE);
        createCalendarResource(ROOM_3, domain, ROOM_3, CalResType.Location, 100, SITE);
    }
   
    private void searchByName(boolean ldap, String domainName) throws Exception {
        SoapTransport transport = authUser(TestUtil.getAddress(AUTHED_USER, domainName));
        
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
            Assert.assertTrue(!found1);// not within specified offset, at offset 0 
                                       // (gal sync account mailbox search is by nameAcs order)
            Assert.assertTrue(found2);
            Assert.assertTrue(found3); 
        }
    }
    
    private void searchByFilter(boolean ldap, String domainName) throws Exception {
        SoapTransport transport = authUser(TestUtil.getAddress(AUTHED_USER, domainName));
        
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
            Assert.assertTrue(!found1); // not matching capacity requirement   
            Assert.assertTrue(!found2);   
            Assert.assertTrue(found3);  // not within specified offset, at offset 0 
                                        // (gal sync account mailbox search is by dateDesc order)
        }
    }
    

    @Test
    public void testGSASerarhByName() throws Exception {
        SKIP_FOR_INMEM_LDAP_SERVER(SkipTestReason.DN_SUBTREE_MATCH_FILTER);
        
        GalTestUtil.enableGalSyncAccount(prov, DOMAIN_GSA);
        searchByName(false, DOMAIN_GSA);
    }
    
    @Test
    public void testGSASerarhByFilter() throws Exception {
        SKIP_FOR_INMEM_LDAP_SERVER(SkipTestReason.DN_SUBTREE_MATCH_FILTER);
        
        GalTestUtil.enableGalSyncAccount(prov, DOMAIN_GSA);
        searchByFilter(false, DOMAIN_GSA);
    }
    
    @Test
    public void testLdapSerarhByName() throws Exception {
        SKIP_FOR_INMEM_LDAP_SERVER(SkipTestReason.DN_SUBTREE_MATCH_FILTER);
        
        searchByName(true, DOMAIN_LDAP);
    }
    
    @Test
    public void testLdapSerarhByFilter() throws Exception {
        SKIP_FOR_INMEM_LDAP_SERVER(SkipTestReason.DN_SUBTREE_MATCH_FILTER);
        
        searchByFilter(true, DOMAIN_LDAP);
    }
    
    @Test
    @Bug(bug=67045)
    public void reservedChar() throws Exception {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_displayName, "name()");
        attrs.put(Provisioning.A_zimbraCalResType, Provisioning.CalResType.Location.name());
        attrs.put(Provisioning.A_zimbraCalResSite, "Site()");
        attrs.put(Provisioning.A_zimbraCalResCapacity, "10");
        attrs.put(Provisioning.A_zimbraCalResBuilding, "Building()");
        attrs.put(Provisioning.A_zimbraNotes, "Notes()");
        attrs.put(Provisioning.A_zimbraCalResFloor, "()");
        CalendarResource cr = provUtil.createCalendarResource("reservedChar", domain, attrs);
        String crName = cr.getName();
        
        /*
        <SearchCalendarResourcesRequest xmlns="urn:zimbraAccount" 
            attrs="fullName,email,zimbraCalResLocationDisplayName,zimbraCalResContactEmail,notes,zimbraCalResType">
            <name>()</name>
            <searchFilter>
                <conds>
                  <cond op="eq" value="Location" attr="zimbraCalResType"/>
                  <cond op="has" value="()" attr="zimbraCalResSite"/>
                  <cond op="ge" value="9" attr="zimbraCalResCapacity"/>
                  <cond op="has" value="()" attr="zimbraCalResBuilding"/>
                  <cond op="has" value="()" attr="zimbraNotes"/>
                  <cond op="eq" value="()" attr="zimbraCalResFloor"/>
                </conds>
            </searchFilter>
        </SearchCalendarResourcesRequest>
        
        Expected filer:
        filter="(&(&(&(|(displayName=*\28\29*)(cn=*\28\29*)(sn=*\28\29*)(givenName=*\28\29*)(mail=*\28\29*)(zimbraMailDeliveryAddress=*\28\29*)(zimbraMailAlias=*\28\29*))(objectClass=zimbraCalendarResource)(zimbraAccountStatus=active))(!(zimbraHideInGal=TRUE))(!(zimbraIsSystemResource=TRUE)))(&(zimbraCalResType=location)(zimbraCalResSite=*\28\29*)(zimbraCalResCapacity>=9)(zimbraCalResBuilding=*\28\29*)(zimbraNotes=*\28\29*)(zimbraCalResFloor=\28\29)))"
        */
        
        SoapTransport transport = authUser("user1@phoebe.mbp");
        
        SearchCalendarResourcesRequest req = new SearchCalendarResourcesRequest("()");
        EntrySearchFilterMultiCond conds = new EntrySearchFilterMultiCond();
        EntrySearchFilterSingleCond cond;
        
        cond = new EntrySearchFilterSingleCond();
        cond.setAttr(Provisioning.A_zimbraCalResType);
        cond.setOp("eq");
        cond.setValue("Location");
        conds.addCondition(cond);
        
        cond = new EntrySearchFilterSingleCond();
        cond.setAttr(Provisioning.A_zimbraCalResSite);
        cond.setOp("has");
        cond.setValue("()");
        conds.addCondition(cond);
        
        cond = new EntrySearchFilterSingleCond();
        cond.setAttr(Provisioning.A_zimbraCalResCapacity);
        cond.setOp("ge");
        cond.setValue("9");
        conds.addCondition(cond);
        
        cond = new EntrySearchFilterSingleCond();
        cond.setAttr(Provisioning.A_zimbraCalResBuilding);
        cond.setOp("has");
        cond.setValue("()");
        conds.addCondition(cond);
        
        cond = new EntrySearchFilterSingleCond();
        cond.setAttr(Provisioning.A_zimbraNotes);
        cond.setOp("has");
        cond.setValue("()");
        conds.addCondition(cond);
        
        cond = new EntrySearchFilterSingleCond();
        cond.setAttr(Provisioning.A_zimbraCalResFloor);
        cond.setOp("eq");
        cond.setValue("()");
        conds.addCondition(cond);
        
        EntrySearchFilterInfo filter = new EntrySearchFilterInfo();
        filter.setCondition(conds);
        req.setSearchFilter(filter);
        
        SearchCalendarResourcesResponse resp = invokeJaxb(transport, req);
        List<CalendarResourceInfo> crInfo = resp.getCalendarResources();
        assertEquals(1, crInfo.size());
        assertEquals(crName, crInfo.get(0).getName());
    }
    
    @Test
    public void chineseChar() throws Exception {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_displayName, "\u4e2d\u6587");
        attrs.put(Provisioning.A_zimbraCalResType, Provisioning.CalResType.Location.name());
        attrs.put(Provisioning.A_zimbraCalResSite, "\u4e2d\u6587");
        CalendarResource cr = provUtil.createCalendarResource("chineseChar", domain, attrs);
        String crName = cr.getName();
        
        /*
         * 
         filter="(&(&(&(|(displayName=*\E4\B8\AD\E6\96\87*)(cn=*\E4\B8\AD\E6\96\87*)(sn=*\E4\B8\AD\E6\96\87*)(givenName=*\E4\B8\AD\E6\96\87*)(?mail=*\E4\B8\AD\E6\96\87*)(?zimbraMailDeliveryAddress=*\E4\B8\AD\E6\96\87*)(?zimbraMailAlias=*\E4\B8\AD\E6\96\87*))(objectClass=zimbraCalendarResource)(zimbraAccountStatus=active))(!(zimbraHideInGal=TRUE))(!(zimbraIsSystemResource=TRUE)))(&(zimbraCalResType=location)(zimbraCalResSite=*\E4\B8\AD\E6\96\87*)))"
         */
        SoapTransport transport = authUser("user1@phoebe.mbp");
        
        SearchCalendarResourcesRequest req = new SearchCalendarResourcesRequest("\u4e2d\u6587");
        EntrySearchFilterMultiCond conds = new EntrySearchFilterMultiCond();
        EntrySearchFilterSingleCond cond;
        
        cond = new EntrySearchFilterSingleCond();
        cond.setAttr(Provisioning.A_zimbraCalResType);
        cond.setOp("eq");
        cond.setValue("Location");
        conds.addCondition(cond);
        
        cond = new EntrySearchFilterSingleCond();
        cond.setAttr(Provisioning.A_zimbraCalResSite);
        cond.setOp("has");
        cond.setValue("\u4e2d\u6587");
        conds.addCondition(cond);
        
        EntrySearchFilterInfo filter = new EntrySearchFilterInfo();
        filter.setCondition(conds);
        req.setSearchFilter(filter);
        
        SearchCalendarResourcesResponse resp = invokeJaxb(transport, req);
        List<CalendarResourceInfo> crInfo = resp.getCalendarResources();
        assertEquals(1, crInfo.size());
        assertEquals(crName, crInfo.get(0).getName());
    }
}
