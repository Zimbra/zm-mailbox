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
package com.zimbra.qa.unittest.prov.ldap;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;

import com.zimbra.cs.account.accesscontrol.InlineAttrRight;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveAttr;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.RightsByTargetType;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.qa.QA.Bug;
import com.zimbra.soap.type.TargetBy;

public class TestACLEffectiveRights extends LdapTest {
    
    private static Right ADMIN_PRESET_ACCOUNT;
    
    private static LdapProvTestUtil provUtil;
    private static LdapProv prov;
    private static Domain domain;
    private static String BASE_DOMAIN_NAME;
    private static Account globalAdmin;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName(), null);
        BASE_DOMAIN_NAME = domain.getName();
        globalAdmin = provUtil.createGlobalAdmin("globaladmin", domain);
        
        ADMIN_PRESET_ACCOUNT = getRight("test-preset-account");
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private static Right getRight(String right) throws ServiceException {
        return RightManager.getInstance().getRight(right);
    }

    @Test
    public void getEffectiveRights() throws Exception {
        Domain domain = provUtil.createDomain(genDomainSegmentName() + "." + BASE_DOMAIN_NAME);
        Account target = provUtil.createAccount(genAcctNameLocalPart("user"), domain);
        Account grantee = provUtil.createDelegatedAdmin(genAcctNameLocalPart("da"), domain);
        Account grantingAccount = globalAdmin;
        
        TargetType targetType = TargetType.getTargetType(target);
        GranteeType granteeType = GranteeType.GT_USER;
        Right right = ADMIN_PRESET_ACCOUNT;
            
        RightCommand.grantRight(
                prov, grantingAccount,
                targetType.getCode(), TargetBy.name, target.getName(),
                granteeType.getCode(), Key.GranteeBy.name, grantee.getName(), null,
                right.getName(), null);
    
        EffectiveRights effRights = RightCommand.getEffectiveRights(
                prov,
                TargetType.account.getCode(), TargetBy.name, target.getName(),
                Key.GranteeBy.name, grantee.getName(),
                false, false);
        
        assertTrue(effRights.presetRights().contains(right.getName()));
        
    }
    

    @Test
    public void getAllEffectiveRights() throws Exception {
        Domain domain = provUtil.createDomain(genDomainSegmentName() + "." + BASE_DOMAIN_NAME);
        Account target = provUtil.createAccount(genAcctNameLocalPart("user"), domain);
        Account grantee = provUtil.createDelegatedAdmin(genAcctNameLocalPart("da"), domain);
        Account grantingAccount = globalAdmin;
        
        TargetType targetType = TargetType.getTargetType(target);
        GranteeType granteeType = GranteeType.GT_USER;
        Right right = ADMIN_PRESET_ACCOUNT;
            
        RightCommand.grantRight(
                prov, grantingAccount,
                targetType.getCode(), TargetBy.name, target.getName(),
                granteeType.getCode(), Key.GranteeBy.name, grantee.getName(), null,
                right.getName(), null);
        
        AllEffectiveRights allEffRights = RightCommand.getAllEffectiveRights(
                prov,
                granteeType.getCode(), Key.GranteeBy.name, grantee.getName(),
                false, false);
        
        Map<TargetType, RightsByTargetType> rbttMap = allEffRights.rightsByTargetType();
        RightsByTargetType rbtt = rbttMap.get(targetType);
        
        boolean found = false;
        for (RightCommand.RightAggregation rightsByEntries : rbtt.entries()) {
            Set<String> targetNames = rightsByEntries.entries();
            
            if (targetNames.contains(target.getName())) {
                // this RightAggregation contains our target
                // see if it contains out right
                EffectiveRights effRights = rightsByEntries.effectiveRights();
                List<String> presetRights = effRights.presetRights();
                if (presetRights.contains(right.getName())) {
                    found = true;
                }
            }
        }
        assertTrue(found);
    }
    
    @Bug(bug=70206)
    @Test
    public void bug70206() throws Exception {
        Account acct = provUtil.createDelegatedAdmin(genAcctNameLocalPart(), domain);
        Group group = provUtil.createGroup(genGroupNameLocalPart(), domain, false);
        
        Account grantingAccount = globalAdmin;
        
        String presetRightUnderTest = Right.RT_deleteDistributionList;
        
        String attrUnderTest = Provisioning.A_zimbraHideInGal;
        String attrRightUnderTest = 
            InlineAttrRight.composeSetRight(TargetType.dl, attrUnderTest);
        
        // grant a combo right on global level
        RightCommand.grantRight(
                prov, grantingAccount,
                TargetType.global.getCode(), null, null,
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, acct.getName(), null,
                Right.RT_adminConsoleDLRights, null);
        
        // deny a preset right (in the combo right) on global level
        RightCommand.grantRight(
                prov, grantingAccount,
                TargetType.global.getCode(), null, null,
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, acct.getName(), null,
                presetRightUnderTest, RightModifier.RM_DENY);
        
        // grant the preset right on the target
        RightCommand.grantRight(
                prov, grantingAccount,
                TargetType.dl.getCode(), TargetBy.name, group.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, acct.getName(), null,
                attrRightUnderTest, null);
        
        // deny an attr right (in the combo right) on global level
        RightCommand.grantRight(
                prov, grantingAccount,
                TargetType.global.getCode(), null, null,
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, acct.getName(), null,
                attrRightUnderTest, RightModifier.RM_DENY);
        
        // grant the attr right on the target
        RightCommand.grantRight(
                prov, grantingAccount,
                TargetType.dl.getCode(), TargetBy.name, group.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, acct.getName(), null,
                presetRightUnderTest, null);
        
        EffectiveRights effRights = RightCommand.getEffectiveRights(
                prov,
                TargetType.dl.getCode(), TargetBy.name, group.getName(),
                Key.GranteeBy.name, acct.getName(),
                false, false);
        
        List<String> presetRights = effRights.presetRights();
        SortedMap<String, EffectiveAttr> setAttrRights = effRights.canSetAttrs();
        
        /*
        for (String right : presetRights) {
            System.out.println(right);
        }
        */
        assertTrue(presetRights.contains(Right.RT_deleteDistributionList));
        assertTrue(setAttrRights.containsKey(attrUnderTest));
    }
    
}
