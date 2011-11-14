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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;

import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.RightAggregation;
import com.zimbra.cs.account.accesscontrol.RightCommand.RightsByTargetType;
import com.zimbra.soap.type.TargetBy;


public class TestACEffectiveRights extends TestAC {

    @Test
    public void getEffectiveRights() throws Exception {
        Domain domain = createDomain();
        Account target = createUserAccount(domain);
        Account grantee = createDelegatedAdminAccount(domain);
        Account grantingAccount = getGlobalAdminAcct();
        
        TargetType targetType = TargetType.getTargetType(target);
        GranteeType granteeType = GranteeType.GT_USER;
        Right right = ADMIN_PRESET_ACCOUNT;
            
        RightCommand.grantRight(
                mProv, grantingAccount,
                targetType.getCode(), TargetBy.name, target.getName(),
                granteeType.getCode(), Key.GranteeBy.name, grantee.getName(), null,
                right.getName(), null);
    
        EffectiveRights effRights = RightCommand.getEffectiveRights(
                mProv,
                TargetType.account.getCode(), TargetBy.name, target.getName(),
                Key.GranteeBy.name, grantee.getName(),
                false, false);
        
        assertTrue(effRights.presetRights().contains(right.getName()));
        
    }
    

    @Test
    public void getAllEffectiveRights() throws Exception {
        Domain domain = createDomain();
        Account target = createUserAccount(domain);
        Account grantee = createDelegatedAdminAccount(domain);
        Account grantingAccount = getGlobalAdminAcct();
        
        TargetType targetType = TargetType.getTargetType(target);
        GranteeType granteeType = GranteeType.GT_USER;
        Right right = ADMIN_PRESET_ACCOUNT;
            
        RightCommand.grantRight(
                mProv, grantingAccount,
                targetType.getCode(), TargetBy.name, target.getName(),
                granteeType.getCode(), Key.GranteeBy.name, grantee.getName(), null,
                right.getName(), null);
        
        AllEffectiveRights allEffRights = RightCommand.getAllEffectiveRights(
                mProv,
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
