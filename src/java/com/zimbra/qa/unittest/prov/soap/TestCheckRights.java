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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.account.message.CheckRightsRequest;
import com.zimbra.soap.account.message.CheckRightsResponse;
import com.zimbra.soap.account.message.GrantRightsRequest;
import com.zimbra.soap.account.message.GrantRightsResponse;
import com.zimbra.soap.account.type.AccountACEInfo;
import com.zimbra.soap.account.type.CheckRightsRightInfo;
import com.zimbra.soap.account.type.CheckRightsTargetInfo;
import com.zimbra.soap.account.type.CheckRightsTargetSpec;
import com.zimbra.soap.type.GranteeType;
import com.zimbra.soap.type.TargetBy;
import com.zimbra.soap.type.TargetType;

public class TestCheckRights extends SoapTest {
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
    
    private void grantRight(Account target, GranteeType granteeType, 
            NamedEntry grantee, String right) 
    throws Exception {
        SoapTransport transport = authUser(target.getName());
        
        GrantRightsRequest req = new GrantRightsRequest();
        
        AccountACEInfo ace = new AccountACEInfo(granteeType, right);
        ace.setZimbraId(grantee.getId());
        req.addAce(ace);
        GrantRightsResponse resp = invokeJaxb(transport, req);
    }
    
    @Test
    public void basic() throws Exception {
        String right = Right.RT_loginAs;
        
        Account acct = provUtil.createAccount(genAcctNameLocalPart("target"), domain);
        Account target1 = provUtil.createAccount(genAcctNameLocalPart("target1"), domain);
        Account target2 = provUtil.createAccount(genAcctNameLocalPart("target2"), domain);
        
        // target1 grant right to acct, target2 does not
        grantRight(target1, GranteeType.usr, acct, right);
        
        SoapTransport transport = authUser(acct.getName());
        
        CheckRightsRequest req = new CheckRightsRequest();
        
        req.addTarget(new CheckRightsTargetSpec(TargetType.account, TargetBy.name, target1.getName(),
                Lists.newArrayList(right)));
        req.addTarget(new CheckRightsTargetSpec(TargetType.account, TargetBy.name, target2.getName(),
                Lists.newArrayList(right)));
        CheckRightsResponse resp = invokeJaxb(transport, req);
        
        Set<String> target1Result = Sets.newHashSet();
        Set<String> target2Result = Sets.newHashSet();
        
        List<CheckRightsTargetInfo> targets = resp.getTargets();
        for (CheckRightsTargetInfo target : targets) {
            assertEquals(TargetType.account, target.getTargetType());
            assertEquals(TargetBy.name, target.getTargetBy());
            
            String key = target.getTargetKey();
            
            Set<String> result = null;
            
            if (target1.getName().equals(key)) {
                assertEquals(true, target.getAllow());
                result = target1Result;
            } else if (target2.getName().equals(key)) {
                assertEquals(false, target.getAllow());
                result = target2Result;
            } else {
                fail();
            }
            
            List<CheckRightsRightInfo> rights = target.getRights();
            for (CheckRightsRightInfo rightInfo : rights) {
                result.add(rightInfo.getRight() + ":" + rightInfo.getAllow());
            }

        }
        
        Verify.verifyEquals(Sets.newHashSet(right + ":" + true), target1Result);
        Verify.verifyEquals(Sets.newHashSet(right + ":" + false), target2Result);
    }
    
    @Test
    public void defaultPermission() throws Exception {
        
        // pair of <right, default permission>
        Pair<String, Boolean> right1 = new Pair<String, Boolean>(Right.RT_invite, Boolean.TRUE);
        Pair<String, Boolean> right2 = new Pair<String, Boolean>(Right.RT_viewFreeBusy, Boolean.TRUE);
        Pair<String, Boolean> right3 = new Pair<String, Boolean>(Right.RT_loginAs, Boolean.FALSE);
        
        Account acct = provUtil.createAccount(genAcctNameLocalPart("acct"), domain);
        String targetKey = "not-exist@test.com";
        
        SoapTransport transport = authUser(acct.getName());
        
        CheckRightsRequest req = new CheckRightsRequest();
        
        CheckRightsTargetSpec targetSpec = 
            new CheckRightsTargetSpec(TargetType.account, TargetBy.name, targetKey,
                Lists.newArrayList(right1.getFirst(), right2.getFirst(), right3.getFirst()));
        
        req.addTarget(targetSpec);
        
        CheckRightsResponse resp = invokeJaxb(transport, req);
        
        List<CheckRightsTargetInfo> targets = resp.getTargets();
        for (CheckRightsTargetInfo target : targets) {
           
            assertEquals(TargetType.account, target.getTargetType());
            assertEquals(TargetBy.name, target.getTargetBy());
            assertEquals(targetKey, target.getTargetKey());
            assertEquals(right1.getSecond() && right2.getSecond() && right3.getSecond(), 
                    target.getAllow());
            
            Set<String> actual = Sets.newHashSet();
            List<CheckRightsRightInfo> rights = target.getRights();
            for (CheckRightsRightInfo rightInfo : rights) {
                actual.add(rightInfo.getRight() + ":" + rightInfo.getAllow());
            }
            Verify.verifyEquals(Sets.newHashSet(
                    right1.getFirst() + ":" + right1.getSecond(),
                    right2.getFirst() + ":" + right2.getSecond(),
                    right3.getFirst() + ":" + right3.getSecond()), 
                    actual);
        }
    }
    
    @Test
    public void delegatedSendRight() throws Exception {
        String right = Right.RT_sendAs;
        
        Account acct = provUtil.createAccount(genAcctNameLocalPart("target"), domain);
        
        Account targetAcct = provUtil.createAccount(genAcctNameLocalPart("target-acct"), domain);
        String alias1 = TestUtil.getAddress(genAcctNameLocalPart("target-acct-alias-1"), domain.getName());
        String alias2 = TestUtil.getAddress(genAcctNameLocalPart("target-acct-alias-2"), domain.getName());
        prov.addAlias(targetAcct, alias1);
        prov.addAlias(targetAcct, alias2);
        prov.modifyAttrs(targetAcct, 
                Collections.singletonMap(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, alias1));
        
        grantRight(targetAcct, GranteeType.usr, acct, right);
        
        SoapTransport transport = authUser(acct.getName());
        
        CheckRightsRequest req = new CheckRightsRequest();
        
        req.addTarget(new CheckRightsTargetSpec(TargetType.account, TargetBy.name, targetAcct.getName(),
                Lists.newArrayList(right)));
        req.addTarget(new CheckRightsTargetSpec(TargetType.account, TargetBy.name, alias1,
                Lists.newArrayList(right)));
        req.addTarget(new CheckRightsTargetSpec(TargetType.account, TargetBy.name, alias2,
                Lists.newArrayList(right)));
        CheckRightsResponse resp = invokeJaxb(transport, req);
        
        Set<String> primaryMailResult = Sets.newHashSet();
        Set<String> alias1Result = Sets.newHashSet();
        Set<String> alias2Result = Sets.newHashSet();
        
        List<CheckRightsTargetInfo> targets = resp.getTargets();
        for (CheckRightsTargetInfo target : targets) {
            assertEquals(TargetType.account, target.getTargetType());
            assertEquals(TargetBy.name, target.getTargetBy());
            
            String key = target.getTargetKey();
            
            Set<String> result = null;
            
            if (targetAcct.getName().equals(key)) {
                assertEquals(false, target.getAllow());
                result = primaryMailResult;
            } else if (alias1.equals(key)) {
                assertEquals(true, target.getAllow());
                result = alias1Result;
            } else if (alias2.equals(key)) {
                assertEquals(false, target.getAllow());
                result = alias2Result;
            } else {
                fail();
            }
            
            List<CheckRightsRightInfo> rights = target.getRights();
            for (CheckRightsRightInfo rightInfo : rights) {
                result.add(rightInfo.getRight() + ":" + rightInfo.getAllow());
            }

        }
        
        Verify.verifyEquals(Sets.newHashSet(right + ":" + false), primaryMailResult);
        Verify.verifyEquals(Sets.newHashSet(right + ":" + true), alias1Result);
        Verify.verifyEquals(Sets.newHashSet(right + ":" + false), alias2Result);
    }
}
