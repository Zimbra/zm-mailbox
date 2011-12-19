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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.qa.QA.Bug;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.account.message.CreateDistributionListRequest;
import com.zimbra.soap.account.message.CreateDistributionListResponse;
import com.zimbra.soap.account.message.DistributionListActionRequest;
import com.zimbra.soap.account.message.DistributionListActionResponse;
import com.zimbra.soap.account.message.GetAccountMembershipRequest;
import com.zimbra.soap.account.message.GetAccountMembershipResponse;
import com.zimbra.soap.account.message.GetDistributionListRequest;
import com.zimbra.soap.account.message.GetDistributionListResponse;
import com.zimbra.soap.account.message.SubscribeDistributionListRequest;
import com.zimbra.soap.account.message.SubscribeDistributionListResponse;
import com.zimbra.soap.account.type.DistributionListAction;
import com.zimbra.soap.account.type.DistributionListInfo;
import com.zimbra.soap.account.type.DistributionListGranteeSelector;
import com.zimbra.soap.account.type.DistributionListSubscribeOp;
import com.zimbra.soap.account.type.DistributionListSubscribeStatus;
import com.zimbra.soap.account.type.DistributionListAction.Operation;
import com.zimbra.soap.account.type.DLInfo;
import com.zimbra.soap.admin.message.AddDistributionListAliasRequest;
import com.zimbra.soap.admin.message.AddDistributionListAliasResponse;
import com.zimbra.soap.admin.message.AddDistributionListMemberRequest;
import com.zimbra.soap.admin.message.AddDistributionListMemberResponse;
import com.zimbra.soap.admin.message.DeleteDistributionListRequest;
import com.zimbra.soap.admin.message.DeleteDistributionListResponse;
import com.zimbra.soap.admin.message.ModifyDistributionListRequest;
import com.zimbra.soap.admin.message.ModifyDistributionListResponse;
import com.zimbra.soap.admin.message.RemoveDistributionListAliasRequest;
import com.zimbra.soap.admin.message.RemoveDistributionListAliasResponse;
import com.zimbra.soap.admin.message.RemoveDistributionListMemberRequest;
import com.zimbra.soap.admin.message.RemoveDistributionListMemberResponse;
import com.zimbra.soap.base.DistributionListGranteeInfoInterface;
import com.zimbra.soap.type.DistributionListGranteeBy;
import com.zimbra.soap.type.DistributionListGranteeType;
import com.zimbra.soap.type.DistributionListSelector;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.TargetBy;

public class TestDelegatedDL extends SoapTest {
        
    private static String DOMAIN_NAME;
    private static String ADMIN;
    private static String USER_CREATOR;
    private static String USER_OWNER;
    private static String USER_NOT_OWNER;
    private static String DL_NAME;
        
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        DOMAIN_NAME = baseDomainName();
        ADMIN = TestUtil.getAddress("admin", DOMAIN_NAME);
        USER_CREATOR = TestUtil.getAddress("creator", DOMAIN_NAME);
        USER_OWNER = TestUtil.getAddress("owner", DOMAIN_NAME);
        USER_NOT_OWNER = TestUtil.getAddress("not-owner", DOMAIN_NAME);
        DL_NAME = TestUtil.getAddress("dl", DOMAIN_NAME);
        
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        
        domain = provUtil.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        
        Account admin = provUtil.createGlobalAdmin(ADMIN);
        
        Account creator = provUtil.createAccount(USER_CREATOR, new HashMap<String, Object>());
        
        Account owner = provUtil.createAccount(USER_OWNER, new HashMap<String, Object>());
        
        Account notOwner = provUtil.createAccount(USER_NOT_OWNER, new HashMap<String, Object>());
        
        prov.grantRight(TargetType.domain.getCode(), TargetBy.name, domain.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, creator.getName(), null, 
                User.R_createDistList.getName(), null);
        
        // create a DL for get/action tests
        List<KeyValuePair> attrs = Lists.newArrayList(new KeyValuePair(
                Provisioning.A_zimbraDistributionListSubscriptionPolicy, 
                ZAttrProvisioning.DistributionListSubscriptionPolicy.ACCEPT.name()));
        Group group = createGroupAndAddOwner(DL_NAME, attrs);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private static Group createDelegatedGroup(SoapTransport transport, String groupName, 
            List<KeyValuePair> attrs) 
    throws Exception {
        boolean dynamic = false;
        
        Group group = prov.getGroup(Key.DistributionListBy.name, groupName);
        assertNull(group);
        
        CreateDistributionListRequest req = new CreateDistributionListRequest(
                groupName, attrs, dynamic);
        CreateDistributionListResponse resp = invokeJaxb(transport, req);
        
        group = prov.getGroup(Key.DistributionListBy.name, groupName);
        assertNotNull(group);
        assertEquals(groupName, group.getName());
        assertNotNull(group.getAttr(Provisioning.A_zimbraMailHost));

        return group;
    }
    
    private static Group createGroupAndAddOwner(String groupName) throws Exception {
        return createGroupAndAddOwner(groupName, null);
    }
    
    private static Group createGroupAndAddOwner(String groupName, List<KeyValuePair> attrs) 
    throws Exception {
        boolean dynamic = false;
        
        Group group = prov.getGroup(Key.DistributionListBy.name, groupName);
        assertNull(group);
        
        SoapTransport transport = authUser(USER_CREATOR);
        
        CreateDistributionListRequest req = new CreateDistributionListRequest(
                groupName, attrs, dynamic);
        CreateDistributionListResponse resp = invokeJaxb(transport, req);
        
        group = prov.getGroup(Key.DistributionListBy.name, groupName);
        assertNotNull(group);
        assertEquals(groupName, group.getName());
        assertNotNull(group.getAttr(Provisioning.A_zimbraMailHost));
        
        /*
         * USER_CREATOR is automatically an owner now.
         */
        
        // add USER_OWNER as an owner
        DistributionListAction action = new DistributionListAction(Operation.addOwners);
        DistributionListActionRequest actionReq = new DistributionListActionRequest(
                DistributionListSelector.fromName(groupName), action);
        
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, USER_OWNER));
        DistributionListActionResponse actionResp = invokeJaxb(transport, actionReq);
        
        // remove USER_CREATOR from the owner list
        action = new DistributionListAction(Operation.removeOwners);
        actionReq = new DistributionListActionRequest(
                DistributionListSelector.fromName(groupName), action);
        
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, USER_CREATOR));
        actionResp = invokeJaxb(transport, actionReq);
        
        return group;
    }
    
    private String getAddress(String localpart) {
        return TestUtil.getAddress(localpart, DOMAIN_NAME);
    }
    
    private void verifyCaughtProxyError(SoapTransport transport, Object jaxbObject) 
    throws Exception {
        boolean caughtProxyError = false;
        try {
            invokeJaxb(transport, jaxbObject);
        } catch (ServiceException e) {
            if (ServiceException.PROXY_ERROR.equals(e.getCode())) {
                caughtProxyError = true;
            }
        }
        assertTrue(caughtProxyError);
    }
    
    /*
     * Test the owners element in zimbraAdmin:GetDistributionList
     */
    @Test
    public void getDistributionListAdmin() throws Exception {
        SoapTransport transport = authAdmin(ADMIN);
        
        com.zimbra.soap.admin.message.GetDistributionListRequest req = 
            new com.zimbra.soap.admin.message.GetDistributionListRequest(
                    com.zimbra.soap.admin.type.DistributionListSelector.fromName(DL_NAME));
        
        com.zimbra.soap.admin.message.GetDistributionListResponse resp = invokeJaxb(transport, req);
        com.zimbra.soap.admin.type.DistributionListInfo dlInfo = resp.getDl();
        
        String dlId = dlInfo.getId();
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertNotNull(group);
        assertEquals(group.getId(), dlId);

        /*
        System.out.println("\nAttrs:");
        List<com.zimbra.soap.admin.type.Attr> attrs = dlInfo.getAttrList();
        for (com.zimbra.soap.admin.type.Attr attr : attrs) {
            System.out.println(attr.getN() + ", " + attr.getValue());
        }
        */
        
        List<? extends DistributionListGranteeInfoInterface> dlOwners = dlInfo.getOwners();
        assertEquals(1, dlOwners.size());
        for (DistributionListGranteeInfoInterface owner : dlOwners) {
            DistributionListGranteeType type = owner.getType();
            String id = owner.getId();
            String name = owner.getName();
            
            assertEquals(DistributionListGranteeType.usr, type);
            assertEquals(USER_OWNER, name);
        }
    }
    
    @Test
    public void createDistributionListPermDenied() throws Exception {
        String dlName = getAddress(genGroupNameLocalPart());
        boolean dynamic = true;
        
        SoapTransport transport = authUser(USER_OWNER);
        
        CreateDistributionListRequest req = new CreateDistributionListRequest(
                dlName, null, dynamic);
        
        boolean caughtPermDenied = false;
        try {
            CreateDistributionListResponse resp = invokeJaxb(transport, req);
        } catch (SoapFaultException e) {
            String code = e.getCode();
            if (ServiceException.PERM_DENIED.equals(code)) {
                caughtPermDenied = true;
            }
        }
        
        assertTrue(caughtPermDenied);
    }
    
    /*
     * verify the ownDistList right can only be granted on group target,
     * not domain, not globalgrant
     */
    @Test
    public void ownDistListRightTarget() throws Exception {
        Group group = provUtil.createGroup(genGroupNameLocalPart("group"), domain, false);
        Account acct = provUtil.createAccount(genAcctNameLocalPart("acct"), domain);
        
        String right = Group.GroupOwner.GROUP_OWNER_RIGHT.getName();
        
        // grant on group should work
        prov.grantRight(TargetType.dl.getCode(), TargetBy.name, group.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null, 
                right, null);
        prov.checkRight(TargetType.dl.getCode(), TargetBy.name, group.getName(), 
                GranteeBy.name, acct.getName(), right, null, null);
        
        
        // grant on domain, should fail
        boolean cauchtException  = false;
        try {
            prov.grantRight(TargetType.domain.getCode(), TargetBy.name, domain.getName(), 
                    GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null, 
                    right, null);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                cauchtException = true;
            }
        }
        assertTrue(cauchtException);
        
        
        // grant on globalgrant, should fail
        cauchtException  = false;
        try {
            prov.grantRight(TargetType.global.getCode(), null, null, 
                    GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null, 
                    right, null);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                cauchtException = true;
            }
        }
        assertTrue(cauchtException);
    }
    
    @Test
    public void createDistributionList() throws Exception {
        String dlName = getAddress(genGroupNameLocalPart());
        boolean dynamic = true;
        
        SoapTransport transport = authUser(USER_CREATOR);
        
        CreateDistributionListRequest req = new CreateDistributionListRequest(
                dlName, null, dynamic);
        
        List<KeyValuePair> attrsCreate = Lists.newArrayList(new KeyValuePair(
                Provisioning.A_zimbraDistributionListSubscriptionPolicy, 
                ZAttrProvisioning.DistributionListSubscriptionPolicy.ACCEPT.name()));
        
        req.setKeyValuePairs(attrsCreate);
        CreateDistributionListResponse resp = invokeJaxb(transport, req);
        DistributionListInfo dlInfo = resp.getDl();
        
        String dlId = dlInfo.getId();
        
        Group group = prov.getGroup(Key.DistributionListBy.name, dlName);
        assertNotNull(group);
        assertEquals(group.getId(), dlId);
        
        List<? extends DistributionListGranteeInfoInterface> dlOwners = dlInfo.getOwners();
        assertEquals(1, dlOwners.size());
        for (DistributionListGranteeInfoInterface owner : dlOwners) {
            DistributionListGranteeType type = owner.getType();
            String id = owner.getId();
            String name = owner.getName();
            
            assertEquals(DistributionListGranteeType.usr, type);
            assertEquals(USER_CREATOR, name);
        }
        
        boolean seenExpectedMail = false;
        boolean seenExpectedSubsPolicy = false;
        List<? extends KeyValuePair> attrs = dlInfo.getAttrList();
        for (KeyValuePair attr : attrs) {
            String name = attr.getKey();
            String value = attr.getValue();
            if (Provisioning.A_mail.equals(name)) {
                assertEquals(group.getName(), value);
                seenExpectedMail = true;
            }
            
            if (Provisioning.A_zimbraDistributionListSubscriptionPolicy.equals(name)) {
                assertEquals(ZAttrProvisioning.DistributionListSubscriptionPolicy.ACCEPT.name(), value);
                seenExpectedSubsPolicy = true;
            }
        }
        assertTrue(seenExpectedMail);
        assertTrue(seenExpectedSubsPolicy);
    }
    
    @Test
    public void getDistributionList() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        GetDistributionListRequest req = new GetDistributionListRequest(
                DistributionListSelector.fromName(DL_NAME), Boolean.TRUE);
        
        GetDistributionListResponse resp = invokeJaxb(transport, req);
        assertTrue(resp.isOwner());
        assertFalse(resp.isMember());
        
        DistributionListInfo dlInfo = resp.getDl();
        
        String dlId = dlInfo.getId();
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertNotNull(group);
        assertEquals(group.getId(), dlId);

        boolean seenMail = false;
        boolean seenSubsPolicy = false;
        boolean seenUnsubsPolicy = false;
        List<? extends KeyValuePair> attrs = dlInfo.getAttrList();
        for (KeyValuePair attr : attrs) {
            String name = attr.getKey();
            String value = attr.getValue();
            if (Provisioning.A_mail.equals(name)) {
                assertEquals(group.getName(), value);
                seenMail = true;
            }
            if (Provisioning.A_zimbraDistributionListSubscriptionPolicy.equals(name)) {
                assertEquals(ZAttrProvisioning.DistributionListSubscriptionPolicy.ACCEPT.name(), value);
                seenSubsPolicy = true;
            }
            if (Provisioning.A_zimbraDistributionListUnsubscriptionPolicy.equals(name)) {
                assertEquals(ZAttrProvisioning.DistributionListUnsubscriptionPolicy.REJECT.name(), value);
                seenUnsubsPolicy = true;
            }
        }
        
        /*
        assertTrue(seenMail);
        assertTrue(seenSubsPolicy);
        assertTrue(seenUnsubsPolicy);
        */
        
        List<? extends DistributionListGranteeInfoInterface> dlOwners = dlInfo.getOwners();
        assertEquals(1, dlOwners.size());
        for (DistributionListGranteeInfoInterface owner : dlOwners) {
            DistributionListGranteeType type = owner.getType();
            String id = owner.getId();
            String name = owner.getName();
            
            assertEquals(DistributionListGranteeType.usr, type);
            assertEquals(USER_OWNER, name);
        }
    }
    
    /*
     * verify and request is allowed, but isOwner is false 
     */
    @Test
    public void getDistributionListByGlobalAdmin() throws Exception {
        SoapTransport transport = authAdmin(ADMIN);
        
        GetDistributionListRequest req = new GetDistributionListRequest(
                DistributionListSelector.fromName(DL_NAME), Boolean.TRUE);
        
        GetDistributionListResponse resp = invokeJaxb(transport, req);
        assertFalse(resp.isOwner());
        
        DistributionListInfo dlInfo = resp.getDl();
        
        String dlId = dlInfo.getId();
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertNotNull(group);
        assertEquals(group.getId(), dlId);
    }

    @Test
    public void distributionListActionAddRemoveMembers() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        // addMembers
        DistributionListAction action = new DistributionListAction(Operation.addMembers);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        String MEMBER1 = "member1@test.com";
        String MEMBER2 = "member2@test.com";
        action.addMember(MEMBER1);
        action.addMember(MEMBER2);
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        Set<String> members = group.getAllMembersSet();
        assertEquals(2, members.size());
        assertTrue(members.contains(MEMBER1));
        assertTrue(members.contains(MEMBER2));
        
        // removeMembers
        action = new DistributionListAction(Operation.removeMembers);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        action.addMember(MEMBER1);
        action.addMember(MEMBER2);
        resp = invokeJaxb(transport, req);
        
        group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        members = group.getAllMembersSet();
        assertEquals(0, members.size());
    }

    @Test
    public void distributionListActionAddRemoveAlias() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        // addAlias
        DistributionListAction action = new DistributionListAction(Operation.addAlias);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        String ALIAS = getAddress("alias");
        action.setAlias(ALIAS);
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        // get the DL by alias
        Group group = prov.getGroup(Key.DistributionListBy.name, ALIAS);
        assertEquals(DL_NAME, group.getName());
        
        // removeAlias
        action = new DistributionListAction(Operation.removeAlias);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        action.setAlias(ALIAS);
        resp = invokeJaxb(transport, req);
        
        // get the DL by alias, should no longer be found
        group = prov.getGroup(Key.DistributionListBy.name, ALIAS);
        assertNull(group);
    }
    
    @Test
    public void distributionListActionAddRemoveOwners() throws Exception {
        String GROUP_NAME = getAddress(genGroupNameLocalPart("group"));
        Group group = createGroupAndAddOwner(GROUP_NAME);
        
        Account owner1 = provUtil.createAccount(genAcctNameLocalPart("1"), domain);
        Account owner2 = provUtil.createAccount(genAcctNameLocalPart("2"), domain);
        
        SoapTransport transport = authUser(USER_OWNER);
        
        //
        // addOwners
        //
        DistributionListAction action = new DistributionListAction(Operation.addOwners);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(GROUP_NAME), action);
        
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, USER_OWNER));
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, owner1.getName()));
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, owner2.getName()));
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        //
        // verify owners are added
        //
        GetDistributionListRequest getDLReq = new GetDistributionListRequest(
                DistributionListSelector.fromName(GROUP_NAME), Boolean.TRUE);
        GetDistributionListResponse getDLResp = invokeJaxb(transport, getDLReq);
        DistributionListInfo dlInfo = getDLResp.getDl();
        List<? extends DistributionListGranteeInfoInterface> owners = dlInfo.getOwners();
        Set<String> ownerNames = Sets.newHashSet();
        for (DistributionListGranteeInfoInterface owner : owners) {
            if (owner.getType() == DistributionListGranteeType.usr) {
                ownerNames.add(owner.getName());
            }
        }
        assertEquals(3, owners.size());
        Verify.verifyEquals(
                Sets.newHashSet(USER_OWNER, owner1.getName(), owner2.getName()), 
                ownerNames);
        
        
        // 
        // removeOwners
        //
        action = new DistributionListAction(Operation.removeOwners);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(GROUP_NAME), action);
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, owner1.getName()));
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, owner2.getName()));
        resp = invokeJaxb(transport, req);
        
        //
        // verify owners are removed
        //
        getDLReq = new GetDistributionListRequest(
                DistributionListSelector.fromName(GROUP_NAME), Boolean.TRUE);
        getDLResp = invokeJaxb(transport, getDLReq);
        dlInfo = getDLResp.getDl();
        owners = dlInfo.getOwners();
        ownerNames = Sets.newHashSet();
        for (DistributionListGranteeInfoInterface owner : owners) {
            if (owner.getType() == DistributionListGranteeType.usr) {
                ownerNames.add(owner.getName());
            }
        }
        assertEquals(1, owners.size());
        Verify.verifyEquals(Sets.newHashSet(USER_OWNER), ownerNames);
    }
    
    @Test
    public void distributionListActionSetOwners() throws Exception {
        String GROUP_NAME = getAddress(genGroupNameLocalPart("group"));
        Group group = createGroupAndAddOwner(GROUP_NAME);
        
        Account owner1 = provUtil.createAccount(genAcctNameLocalPart("1"), domain);
        Account owner2 = provUtil.createAccount(genAcctNameLocalPart("2"), domain);
        Account owner3 = provUtil.createAccount(genAcctNameLocalPart("3"), domain);
        
        SoapTransport transport = authUser(USER_OWNER);
        
        //
        // setOwners
        //
        DistributionListAction action = new DistributionListAction(Operation.setOwners);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(GROUP_NAME), action);
        
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, USER_OWNER));
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, owner1.getName()));
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, owner2.getName()));
        action.addOwner(new DistributionListGranteeSelector(DistributionListGranteeType.usr, 
                DistributionListGranteeBy.name, owner3.getName()));
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        //
        // verify owners are replaced
        //
        GetDistributionListRequest getDLReq = new GetDistributionListRequest(
                DistributionListSelector.fromName(GROUP_NAME), Boolean.TRUE);
        GetDistributionListResponse getDLResp = invokeJaxb(transport, getDLReq);
        DistributionListInfo dlInfo = getDLResp.getDl();
        List<? extends DistributionListGranteeInfoInterface> owners = dlInfo.getOwners();
        Set<String> ownerNames = Sets.newHashSet();
        for (DistributionListGranteeInfoInterface owner : owners) {
            if (owner.getType() == DistributionListGranteeType.usr) {
                ownerNames.add(owner.getName());
            }
        }
        assertEquals(4, owners.size());
        Verify.verifyEquals(
                Sets.newHashSet(USER_OWNER, owner1.getName(), owner2.getName(), owner3.getName()), 
                ownerNames);
        
        
        // remove all owners
        action = new DistributionListAction(Operation.setOwners);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(GROUP_NAME), action);
        resp = invokeJaxb(transport, req);
        
        getDLReq = new GetDistributionListRequest(
                DistributionListSelector.fromName(GROUP_NAME), Boolean.TRUE);
        getDLResp = invokeJaxb(transport, getDLReq);
        dlInfo = getDLResp.getDl();
        owners = dlInfo.getOwners();
        assertEquals(0, owners.size());
    }
    
    
    @Test
    public void distributionListActionModify() throws Exception {
        String ATTR = Provisioning.A_description;
        String VALUE = "test description";
        
        SoapTransport transport = authUser(USER_OWNER);
        
        DistributionListAction action = new DistributionListAction(Operation.modify);
        
        List<KeyValuePair> attrs = Lists.newArrayList(new KeyValuePair(ATTR, VALUE));
        action.setKeyValuePairs(attrs);
        
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertEquals(VALUE, group.getAttr(ATTR));
    }
    
    @Test
    public void distributionListActionRename() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        DistributionListAction action = new DistributionListAction(Operation.rename);
        String DL_NEW_NAME = getAddress(genGroupNameLocalPart("new-name"));
        action.setNewName(DL_NEW_NAME);
        
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NEW_NAME);
        assertEquals(DL_NEW_NAME, group.getName());
        
        // rename it back
        action = new DistributionListAction(Operation.rename);
        action.setNewName(DL_NAME);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NEW_NAME), action);
        resp = invokeJaxb(transport, req);
        group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertEquals(DL_NAME, group.getName());
    }
    
    @Test
    public void distributionListActionDelete() throws Exception {
        // create a group for the delete test
        String NAME = getAddress(genGroupNameLocalPart());
        Group group = createGroupAndAddOwner(NAME);
        
        SoapTransport transport = authUser(USER_OWNER);
        
        DistributionListAction action = new DistributionListAction(Operation.delete);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(NAME), action);
        
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        group = prov.getGroup(Key.DistributionListBy.name, NAME);
        assertNull(null);
    }
    
    @Test
    public void subscribeDistributionList() throws Exception {
        SoapTransport transport = authUser(USER_NOT_OWNER);
        
        // subscribe
        SubscribeDistributionListRequest req = new SubscribeDistributionListRequest(
                DistributionListSelector.fromName(DL_NAME),
                DistributionListSubscribeOp.subscribe);
        
        SubscribeDistributionListResponse resp = invokeJaxb(transport, req);
        assertEquals(DistributionListSubscribeStatus.subscribed, resp.getStatus());
        
        // unsubscribe
        req = new SubscribeDistributionListRequest(
                DistributionListSelector.fromName(DL_NAME),
                DistributionListSubscribeOp.unsubscribe);
        
        boolean caughtPermDenied = false;
        try {
            resp = invokeJaxb(transport, req);
        } catch (ServiceException e) {
            String code = e.getCode();
            if (ServiceException.PERM_DENIED.equals(code)) {
                caughtPermDenied = true;
            }
        }
        assertTrue(caughtPermDenied);
    }
    
    @Test
    public void getAccountMembership() throws Exception {
        String GROUP_NAME = getAddress(genGroupNameLocalPart());
        String GROUP_DISPLAY_NAME = "DISPLAY";
        
        List<KeyValuePair> attrs = Lists.newArrayList(new KeyValuePair(
                Provisioning.A_displayName, GROUP_DISPLAY_NAME));
        Group group = createGroupAndAddOwner(GROUP_NAME, attrs);
        
        // add a member
        prov.addGroupMembers(group, new String[]{USER_NOT_OWNER});
        
        SoapTransport transport = authUser(USER_NOT_OWNER);
        GetAccountMembershipRequest req = new GetAccountMembershipRequest(Boolean.TRUE);
        GetAccountMembershipResponse resp = invokeJaxb(transport, req);
        
        boolean seenGroup = false;
        List<DLInfo> groups = resp.getDlList();
        for (DLInfo dlInfo : groups) {
            String id = dlInfo.getId();
            String name = dlInfo.getName();
            String displayName = dlInfo.getDisplayName();
            
            if (group.getId().equals(id) && 
                name.equals(GROUP_NAME) && 
                displayName.equals(GROUP_DISPLAY_NAME)) {
                seenGroup = true;
            }
        }
        assertTrue(seenGroup);
        
        // rename group
        String GROUP_NEW_NAME = getAddress(genGroupNameLocalPart("new"));
        prov.renameGroup(group.getId(), GROUP_NEW_NAME);
        
        // get membership again, should show the new name
        resp = invokeJaxb(transport, req);
        seenGroup = false;
        groups = resp.getDlList();
        for (DLInfo dlInfo : groups) {
            String id = dlInfo.getId();
            String name = dlInfo.getName();
            String displayName = dlInfo.getDisplayName();
            
            if (group.getId().equals(id) && 
                name.equals(GROUP_NEW_NAME) &&
                displayName.equals(GROUP_DISPLAY_NAME)) {
                seenGroup = true;
            }
        }
        assertTrue(seenGroup);
    }

    /*
     * Verify groups without a home server will get PROXY_ERROR for zimbraAccount 
     * SOAP calls.
     */
    @Test
    @Bug(bug=66412)
    public void noHomeServerZimbraAccount() throws Exception {
        String groupName = TestUtil.getAddress(genGroupNameLocalPart(), DOMAIN_NAME);
        Group group = provUtil.createGroup(groupName, false);
        
        // remove zimbraMailHost
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, null);
        prov.modifyAttrs(group, attrs);
        
        SoapTransport transport = authUser(USER_OWNER);
        
        /*
         * GetDistributionList
         */
        GetDistributionListRequest getDLreq = new GetDistributionListRequest(
                DistributionListSelector.fromName(groupName), Boolean.TRUE);
        verifyCaughtProxyError(transport, getDLreq);
        
        
        /*
         * DistributionListAction
         */
        DistributionListActionRequest DLActionReq = new DistributionListActionRequest(
                DistributionListSelector.fromName(groupName), 
                new DistributionListAction(Operation.addMembers));
        verifyCaughtProxyError(transport, DLActionReq);
        
        /*
         * SubscribeDistributionList
         */
        SubscribeDistributionListRequest subsDLReq = new SubscribeDistributionListRequest(
                DistributionListSelector.fromName(groupName),
                DistributionListSubscribeOp.subscribe);
        verifyCaughtProxyError(transport, subsDLReq);
        
        provUtil.deleteGroup(group);
    }
    
    /*
     * Verify groups without a home server will get executed for zimbraAdmin 
     * SOAP calls.
     */
    @Test
    @Bug(bug=66412)
    public void noHomeServerZimbraAdmin() throws Exception {
        String groupName = TestUtil.getAddress(genGroupNameLocalPart(), DOMAIN_NAME);
        Group group = provUtil.createGroup(groupName, false);
        String groupId = group.getId();
        
        // remove zimbraMailHost
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, null);
        prov.modifyAttrs(group, attrs);
        
        SoapTransport transport = authAdmin(ADMIN);
        Object req;
        com.zimbra.soap.admin.type.DistributionListInfo dlInfo;
        
        /*
         * GetDistributionList
         */
        req = new com.zimbra.soap.admin.message.GetDistributionListRequest(
                    com.zimbra.soap.admin.type.DistributionListSelector.fromName(groupName));
        com.zimbra.soap.admin.message.GetDistributionListResponse getDLResp = invokeJaxb(transport, req);
        dlInfo = getDLResp.getDl();
        assertEquals(groupId, dlInfo.getId());
        
        /*
         * ModifyDistributionList
         */
        req = new ModifyDistributionListRequest(groupId);
        ModifyDistributionListResponse modifyDLResp = invokeJaxb(transport, req);
        dlInfo = modifyDLResp.getDl();
        assertEquals(groupId, dlInfo.getId());
        
        /*
         * AddDistributionAlias
         */
        req = new AddDistributionListAliasRequest(groupId, 
                TestUtil.getAddress(genGroupNameLocalPart("alias"), DOMAIN_NAME));
        AddDistributionListAliasResponse addDLAliasResp = invokeJaxb(transport, req);
        
        /*
         * RemoveDistributionAlias
         */
        req = new RemoveDistributionListAliasRequest(groupId, 
                TestUtil.getAddress(genGroupNameLocalPart("alias"), DOMAIN_NAME));
        RemoveDistributionListAliasResponse removeDLAliasResp = invokeJaxb(transport, req);
        
        /*
         * AddDistributionListMember
         */
        req = new AddDistributionListMemberRequest(groupId, 
                Collections.singleton(TestUtil.getAddress(genAcctNameLocalPart("member"), DOMAIN_NAME)));
        AddDistributionListMemberResponse addDLMemberResp = invokeJaxb(transport, req);
        
        /*
         * RemoveDistributionListMember
         */
        req = new RemoveDistributionListMemberRequest(groupId, 
                Collections.singleton(TestUtil.getAddress(genAcctNameLocalPart("member"), DOMAIN_NAME)));
        RemoveDistributionListMemberResponse removeDLMemberResp = invokeJaxb(transport, req);
        
        /*
         * DeleteDistributionList
         */
        req = new DeleteDistributionListRequest(groupId);
        DeleteDistributionListResponse deleteDLResp = invokeJaxb(transport, req);

    }
    
    /*
     * owner of a group is a group
     */
    @Test
    public void ownerIsGroup() throws Exception {
        boolean dynamic = false;
        Group ownedGroup = provUtil.createGroup(genGroupNameLocalPart("owned"), domain, dynamic);
        Group owningGroup = provUtil.createGroup(genGroupNameLocalPart("owning"), domain, dynamic);
        
        /*
         * add members to owning group
         */
        Account acctInOwningGroup = provUtil.createAccount("acctInOwningGroup", domain);
        prov.addGroupMembers(owningGroup, new String[]{acctInOwningGroup.getName()});
        
        /*
         * grant ownDistList right to owningGroup on ownedGroup
         */
        prov.grantRight(TargetType.dl.getCode(), TargetBy.name, ownedGroup.getName(), 
                GranteeType.GT_GROUP.getCode(), GranteeBy.name, owningGroup.getName(), null, 
                Group.GroupOwner.GROUP_OWNER_RIGHT.getName(), null);
        
        /*
         * auth as acctInOwningGroup
         */
        SoapTransport transport = authUser(acctInOwningGroup.getName());
        
        /*
         * try to add member in ownedGroup
         */
        // addMembers
        DistributionListAction action = new DistributionListAction(Operation.addMembers);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(ownedGroup.getName()), action);
        
        String MEMBER1 = "member1@test.com";
        String MEMBER2 = "member2@test.com";
        action.addMember(MEMBER1);
        action.addMember(MEMBER2);
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        Group group = prov.getGroup(Key.DistributionListBy.name, ownedGroup.getName());
        Set<String> members = group.getAllMembersSet();
        assertEquals(2, members.size());
        assertTrue(members.contains(MEMBER1));
        assertTrue(members.contains(MEMBER2));
        
        provUtil.deleteAccount(acctInOwningGroup);
        provUtil.deleteGroup(owningGroup);
        provUtil.deleteGroup(ownedGroup);
    }

}
