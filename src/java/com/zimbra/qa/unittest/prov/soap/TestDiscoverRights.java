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
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.qa.QA.Bug;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.account.message.DiscoverRightsRequest;
import com.zimbra.soap.account.message.DiscoverRightsResponse;
import com.zimbra.soap.account.type.DiscoverRightsInfo;
import com.zimbra.soap.account.type.DiscoverRightsTarget;
import com.zimbra.soap.type.TargetBy;

public class TestDiscoverRights extends SoapTest {

    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    private static String DOMAIN_NAME;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
        DOMAIN_NAME = domain.getName();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    
    private String getAddress(String localpart) {
        return TestUtil.getAddress(localpart, DOMAIN_NAME);
    }
    
    /*
     * verify display name is returned in DiscoverRights and discovered targets 
     * are sorted by displayName 
     */
    @Test 
    @Bug(bug=68225)
    public void displayName() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        
        String GROUP_1_NAME = getAddress(genGroupNameLocalPart("1"));
        String GROUP_1_DISPLAY_NAME = "third";
        String GROUP_2_NAME = getAddress(genGroupNameLocalPart("2"));
        String GROUP_2_DISPLAY_NAME = "first";
        String GROUP_3_NAME = getAddress(genGroupNameLocalPart("3"));
        String GROUP_3_DISPLAY_NAME = "first";
        
        Group group1 = provUtil.createGroup(GROUP_1_NAME, 
                Collections.singletonMap(
                Provisioning.A_displayName, (Object)GROUP_1_DISPLAY_NAME), false);
        Group group2 = provUtil.createGroup(GROUP_2_NAME, 
                Collections.singletonMap(
                Provisioning.A_displayName, (Object)GROUP_2_DISPLAY_NAME), false);
        Group group3 = provUtil.createGroup(GROUP_3_NAME, 
                Collections.singletonMap(
                Provisioning.A_displayName, (Object)GROUP_3_DISPLAY_NAME), false);
        
        String RIGHT_NAME = User.R_ownDistList.getName();
        
        prov.grantRight(TargetType.dl.getCode(), TargetBy.name, group1.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null, 
                RIGHT_NAME, null);
        prov.grantRight(TargetType.dl.getCode(), TargetBy.name, group2.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null, 
                RIGHT_NAME, null);
        prov.grantRight(TargetType.dl.getCode(), TargetBy.name, group3.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null, 
                RIGHT_NAME, null);
        
        SoapTransport transport = authUser(acct.getName());
        
        DiscoverRightsRequest req = new DiscoverRightsRequest(
                Collections.singletonList(RIGHT_NAME));
        DiscoverRightsResponse resp = invokeJaxb(transport, req);
        
        List<DiscoverRightsInfo> rightsInfo = resp.getDiscoveredRights();
        assertEquals(1, rightsInfo.size());
        
        List<String> result = Lists.newArrayList();
        
        for (DiscoverRightsInfo rightInfo : rightsInfo) {
            List<DiscoverRightsTarget> targets = rightInfo.getTargets();
            
            for (DiscoverRightsTarget target : targets) {
                String id = target.getId();
                String name = target.getName();
                String displayName = target.getDisplayName();
                
                result.add(Verify.makeResultStr(id, name, displayName));
            }
        }
        
        // result should be sorted by displayName.  
        // If displayName are the same, sorted by entry.getLabel()
        Verify.verifyEquals(
                Lists.newArrayList(
                        Verify.makeResultStr(group2.getId(), group2.getName(), group2.getDisplayName()),
                        Verify.makeResultStr(group3.getId(), group3.getName(), group3.getDisplayName()),
                        Verify.makeResultStr(group1.getId(), group1.getName(), group1.getDisplayName())), 
                result);
    }
    
    @Test
    public void granteeAll() throws Exception {
        Domain testDomain = provUtil.createDomain(genDomainName(domain.getName()));
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), testDomain);
        
        String RIGHT_NAME = Right.RT_createDistList;
        
        prov.grantRight(TargetType.domain.getCode(), TargetBy.name, testDomain.getName(), 
                GranteeType.GT_AUTHUSER.getCode(), null, null, null, 
                RIGHT_NAME, null);
        
        SoapTransport transport = authUser(acct.getName());
        
        DiscoverRightsRequest req = new DiscoverRightsRequest(
                Collections.singletonList(RIGHT_NAME));
        DiscoverRightsResponse resp = invokeJaxb(transport, req);
        
        List<DiscoverRightsInfo> rightsInfo = resp.getDiscoveredRights();
        
        List<String> result = Lists.newArrayList();
        for (DiscoverRightsInfo rightInfo : rightsInfo) {
            List<DiscoverRightsTarget> targets = rightInfo.getTargets();
            
            for (DiscoverRightsTarget target : targets) {
                String id = target.getId();
                String name = target.getName();
                
                result.add(Verify.makeResultStr(id, name));
            }
        }
        
        Verify.verifyEquals(
                Lists.newArrayList(Verify.makeResultStr(testDomain.getId(), testDomain.getName())), 
                result);
        
        provUtil.deleteAccount(acct);
        provUtil.deleteDomain(testDomain);
    }
    
}
