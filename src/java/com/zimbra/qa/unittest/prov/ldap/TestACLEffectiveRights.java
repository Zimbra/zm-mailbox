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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.accesscontrol.GranteeType;

import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.RightsByTargetType;
import com.zimbra.cs.account.ldap.LdapProv;
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
    
    /*
    @Test
    public void getCreateObjectAttrs() {
        getCreateObjectAttrs(
                Provisioning prov, String targetType,
                Key.DomainBy domainBy, String domainStr,
                Key.CosBy cosBy, String cosStr,
                Key.GranteeBy granteeBy, String grantee)
    }
    
    */
}
