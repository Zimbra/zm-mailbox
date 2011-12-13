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

import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.qa.QA.Bug;
import com.zimbra.soap.account.message.DiscoverRightsRequest;
import com.zimbra.soap.account.message.DiscoverRightsResponse;
import com.zimbra.soap.account.type.DiscoverRightsInfo;
import com.zimbra.soap.account.type.DiscoverRightsTarget;
import com.zimbra.soap.type.TargetBy;

public class TestDiscoverRights extends SoapTest {

    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    /*
     * verify display name is returned in DiscoverRights
     */
    @Test 
    @Bug(bug=68225)
    public void displayName() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart("acct"), domain);
        
        String GROUP_DISPLAY_NAME = "DISPLAY";
        Group group = provUtil.createGroup(genGroupNameLocalPart("dl"), domain,
                Collections.singletonMap(Provisioning.A_displayName, (Object)GROUP_DISPLAY_NAME),
                false);
        
        String RIGHT_NAME = User.R_ownDistList.getName();
        prov.grantRight(TargetType.dl.getCode(), TargetBy.name, group.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null, 
                RIGHT_NAME, null);
        
        SoapTransport transport = authUser(acct.getName());
        
        DiscoverRightsRequest req = new DiscoverRightsRequest(
                Collections.singletonList(RIGHT_NAME));
        DiscoverRightsResponse resp = invokeJaxb(transport, req);
        
        List<DiscoverRightsInfo> rightsInfo = resp.getDiscoveredRights();
        assertEquals(1, rightsInfo.size());
        
        boolean seenGrant = false;
        for (DiscoverRightsInfo rightInfo : rightsInfo) {
            List<DiscoverRightsTarget> targets = rightInfo.getTargets();
            assertEquals(1, targets.size());
            for (DiscoverRightsTarget target : targets) {
                String id = target.getId();
                String name = target.getName();
                String displayName = target.getDisplayName();
                
                if (group.getId().equals(id) &&
                    group.getName().equals(name) &&
                    GROUP_DISPLAY_NAME.equals(displayName)) {
                    seenGrant = true;
                }
            }
        }
        assertTrue(seenGrant);
    }
    
}
