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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.*;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntryType;

import static org.junit.Assert.*;

public class TestLdapProvAttrCallback extends TestLdap {

    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = Provisioning.getInstance();
        domain = TestLdapProvDomain.createDomain(prov, baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return TestLdapProvAttrCallback.class.getName().toLowerCase();
    }
    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) throws Exception {
        return TestLdapProvAccount.createAccount(prov, localPart, domain, attrs);
    }
    
    private void deleteAccount(Account acct) throws Exception {
        TestLdapProvAccount.deleteAccount(prov, acct);
    }
    
    private DistributionList createDistributionList(String localpart) throws Exception {
        return createDistributionList(localpart, null);
    }
    
    private DistributionList createDistributionList(String localPart, Map<String, Object> attrs) 
    throws Exception {
        return TestLdapProvDistributionList.createDistributionList(prov, localPart, domain, attrs);
    }
    
    private DynamicGroup createDynamicGroup(String localPart, Map<String, Object> attrs) 
    throws Exception {
        return TestLdapProvDynamicGroup.createDynamicGroup(prov, localPart, domain, attrs);
    }
    
    private DynamicGroup createDynamicGroup(String localPart) 
    throws Exception {
        return createDynamicGroup(localPart, null);
    }
    
    private void deleteDynamicGroup(DynamicGroup group) throws Exception {
        TestLdapProvDynamicGroup.deleteDynamicGroup(prov, group);
    }
    
    private DynamicGroup refresh(DynamicGroup group) throws Exception {
        prov.flushCache(CacheEntryType.group, null);
        group = (DynamicGroup) prov.getGroup(Key.DistributionListBy.id, group.getId());
        assertNotNull(group);
        return group;
    }
    
    private Domain createDomain(String subDomainSegment, Map<String, Object> attrs) throws Exception {
        return TestLdapProvDomain.createDomain(prov, subDomainSegment + "." + baseDomainName(), attrs);
    }
    
    @Test
    public void accountStatus() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("accountStatus-account");
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        
        String ALIAS_LOCALPART_1 = TestLdap.makeAliasNameLocalPart("accountStatus-alias-1");
        String ALIAS_NAME_1 = TestUtil.getAddress(ALIAS_LOCALPART_1, domain.getName()).toLowerCase();
        String ALIAS_LOCALPART_2 = TestLdap.makeAliasNameLocalPart("accountStatus-alias-2");
        String ALIAS_NAME_2 = TestUtil.getAddress(ALIAS_LOCALPART_2, domain.getName()).toLowerCase();
        
        prov.addAlias(acct, ALIAS_NAME_1);
        prov.addAlias(acct, ALIAS_NAME_2);
        
        String DL_NAME_LOCALPART = TestLdap.makeDLNameLocalPart("accountStatus-dl");
        DistributionList dl = createDistributionList(DL_NAME_LOCALPART);
        
        dl.addMembers(new String[] {acct.getName(), ALIAS_NAME_1, ALIAS_NAME_2} );
        
        prov.flushCache(CacheEntryType.account, null);
        prov.flushCache(CacheEntryType.group, null);
        
        Set<String> allMembers = dl.getAllMembersSet();
        assertEquals(3, allMembers.size());
        assertTrue(allMembers.contains(acct.getName()));
        assertTrue(allMembers.contains(ALIAS_NAME_1));
        assertTrue(allMembers.contains(ALIAS_NAME_2));
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, ZAttrProvisioning.AccountStatus.closed.name());
        prov.modifyAttrs(acct, attrs);
        
        prov.flushCache(CacheEntryType.account, null);
        prov.flushCache(CacheEntryType.group, null);
        
        dl = prov.get(Key.DistributionListBy.id, dl.getId());
        
        allMembers = dl.getAllMembersSet();
        assertEquals(0, allMembers.size());
        
        deleteAccount(acct);
    }
    
    private void verifyIsACLGroup(DynamicGroup group) {
        assertEquals(ProvisioningConstants.TRUE, group.getAttr(Provisioning.A_zimbraIsACLGroup));
        assertEquals(group.getDefaultMemberURL(), group.getAttr(Provisioning.A_memberURL));
    }
    
    private void verifyIsNotACLGroup(DynamicGroup group, String expectedMemberURL) {
        assertEquals(ProvisioningConstants.FALSE, group.getAttr(Provisioning.A_zimbraIsACLGroup));
        assertEquals(expectedMemberURL, group.getAttr(Provisioning.A_memberURL));
    }
    
    @Test
    public void zimbraIsACLGroupAndMemberURLCreate() throws Exception {
        String SOME_URL = "blah";
        
        Map<String, Object> attrs = Maps.newHashMap();
        boolean caughtException;
        DynamicGroup group;
        
        // 1. specify zimbraIsACLGroup to true -> OK
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        group = createDynamicGroup("isACLGroupAndMemberURLCreate-1", attrs);
        verifyIsACLGroup(group);
        deleteDynamicGroup(group);
        
        
        // 2. specify zimbraIsACLGroup to false without providing a memberURL -> OK 
        //    server will still generate a default memberURL in the case
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        group = createDynamicGroup("isACLGroupAndMemberURLCreate-2", attrs);
        verifyIsNotACLGroup(group, group.getDefaultMemberURL());
        deleteDynamicGroup(group);
        
        
        // 3. specify memberURL and set zimbraIsACLGroup to false -> OK
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup("isACLGroupAndMemberURLCreate-3", attrs);
        verifyIsNotACLGroup(group, SOME_URL);
        deleteDynamicGroup(group);
        
        // 4. specify memberURL and set zimbraIsACLGroup to true -> FAIL
        caughtException = false;
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        try {
            group = createDynamicGroup("isACLGroupAndMemberURLCreate-4", attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
        
        
        // 5. specify memberURL without setting zimbraIsACLGroup -> FAIL
        caughtException = false;
        attrs.clear();
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        try {
            group = createDynamicGroup("isACLGroupAndMemberURLCreate-5", attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
    }
    
    @Test
    public void zimbraIsACLGroupAndMemberURLModify() throws Exception {
        String SOME_URL = "blah";
        String SOME_URL_2 = "blah blah";
        
        Map<String, Object> attrs = Maps.newHashMap();
        boolean caughtException;
        DynamicGroup group;
        
        /*
         * 1. Testing modifying a default dynamic group
         * 
         * i.e. zimbraIsACLGroup is TRUE
         *      memberURL is the default URL
         */

        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-1");
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        prov.modifyAttrs(group, attrs);
        group = refresh(group);
        verifyIsACLGroup(group);
        deleteDynamicGroup(group);
        
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-2");
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        prov.modifyAttrs(group, attrs);
        group = refresh(group);
        verifyIsNotACLGroup(group, group.getDefaultMemberURL());
        deleteDynamicGroup(group);
        
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-3");
        attrs.clear();
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        caughtException = false;
        try {
            prov.modifyAttrs(group, attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
        deleteDynamicGroup(group);
        
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-4");
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        caughtException = false;
        try {
            prov.modifyAttrs(group, attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
        deleteDynamicGroup(group);
        
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-5");
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        prov.modifyAttrs(group, attrs);
        verifyIsNotACLGroup(group, SOME_URL);
        deleteDynamicGroup(group);
        
        
        /*
         * 2. Testing modifying a non-default dynamic group
         * 
         * i.e. zimbraIsACLGroup is FALSE
         *      memberURL is the some URL
         */
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-6", attrs);
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        prov.modifyAttrs(group, attrs);
        group = refresh(group);
        verifyIsACLGroup(group);
        deleteDynamicGroup(group);
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-7", attrs);
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        prov.modifyAttrs(group, attrs);
        group = refresh(group);
        verifyIsNotACLGroup(group, SOME_URL);
        deleteDynamicGroup(group);
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-8", attrs);
        attrs.clear();
        attrs.put(Provisioning.A_memberURL, SOME_URL_2);
        prov.modifyAttrs(group, attrs);
        verifyIsNotACLGroup(group, SOME_URL_2);
        deleteDynamicGroup(group);
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-9", attrs);
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        caughtException = false;
        try {
            prov.modifyAttrs(group, attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
        deleteDynamicGroup(group);
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup("zimbraIsACLGroupAndMemberURLModify-10", attrs);
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL_2);
        prov.modifyAttrs(group, attrs);
        verifyIsNotACLGroup(group, SOME_URL_2);
        deleteDynamicGroup(group);
    }
    
    @Test
    public void authMech() throws Exception {
        // good mech
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraAuthMech, "custom");
        Domain testDomain = createDomain("authMech", attrs);
        
        // bad mech
        attrs.clear();
        attrs.put(Provisioning.A_zimbraAuthMech, "bad");
        boolean caughtException = false;
        try {
            prov.modifyAttrs(testDomain, attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
    }
    
}
