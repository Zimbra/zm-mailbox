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

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.account.message.SearchGalRequest;
import com.zimbra.soap.account.message.SearchGalResponse;
import com.zimbra.soap.account.type.ContactInfo;
import com.zimbra.soap.type.ContactAttr;
import com.zimbra.soap.type.TargetBy;

public class TestSearchGalGroups extends SoapTest {
    private static final String PASSWORD = "test123";
    
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
     * acct is an owner and a member of the group.
     * 
     * Verify isOwner and isMember flags are returned correctly.
     */
    private void testSearchGroup(Account acct, Group group, 
            Boolean needIsOwner, Boolean needIsMember) throws Exception {
        SoapTransport transport = authUser(acct.getName());
        
        SearchGalRequest req = new SearchGalRequest();
        req.setName(group.getName());
        req.setNeedIsOwner(needIsOwner);
        req.setNeedIsMember(needIsMember);
        
        SearchGalResponse resp = invokeJaxb(transport, req);
        
        List<String> result = Lists.newArrayList();
        List<String> expected = Lists.newArrayList(
                Verify.makeResultStr(
                        group.getName(), 
                        ContactConstants.TYPE_GROUP,
                        needIsOwner,
                        needIsMember));
        
        List<ContactInfo> entries = resp.getContacts();
        for (ContactInfo entry : entries) {
            List<ContactAttr> attrs = entry.getAttrs();
            
            String email = null;
            String type = null;
            for (ContactAttr attr : attrs) {
                String key = attr.getKey();
                if (ContactConstants.A_email.equals(key)) {
                    email = attr.getValue();
                } else if (ContactConstants.A_type.equals(key)) {
                    type = attr.getValue();
                }
            }
            
            Boolean isOwner = entry.isOwner();
            Boolean isMember = entry.isMember();
            
            result.add(Verify.makeResultStr(email, type, isOwner, isMember));
        }
        Verify.verifyEquals(expected, result);
    }
    
    @Test
    public void searchGroup() throws Exception {
        Group group = provUtil.createGroup(genGroupNameLocalPart(), domain, false);
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        
        // make acct owner of the the group
        String right = Group.GroupOwner.GROUP_OWNER_RIGHT.getName();
        prov.grantRight(TargetType.dl.getCode(), TargetBy.name, group.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null, 
                right, null);
        
        // make acct member of the group
        prov.addGroupMembers(group, new String[]{acct.getName()});
        
        // test with GSA on
        GalTestUtil.enableGalSyncAccount(prov, domain.getName());
        testSearchGroup(acct, group, Boolean.TRUE, Boolean.TRUE);
        testSearchGroup(acct, group, Boolean.TRUE, null);
        testSearchGroup(acct, group, null, Boolean.TRUE);
        testSearchGroup(acct, group, null, null);
        
        // test with GSA off
        GalTestUtil.disableGalSyncAccount(prov, domain.getName());
        testSearchGroup(acct, group, Boolean.TRUE, Boolean.TRUE);
        testSearchGroup(acct, group, Boolean.TRUE, null);
        testSearchGroup(acct, group, null, Boolean.TRUE);
        testSearchGroup(acct, group, null, null);
        
        provUtil.deleteGroup(group);
        provUtil.deleteAccount(acct);
    }
}
