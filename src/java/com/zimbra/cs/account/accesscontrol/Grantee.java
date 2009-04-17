/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;

public class Grantee {
    NamedEntry mGrantee;
    GranteeType mGranteeType;
    Domain mGranteeDomain;
    Set<String> mIdAndGroupIds;
    
    public static Grantee makeGrantee(NamedEntry grantee) throws ServiceException {
        GranteeType granteeType = null;
        if (grantee instanceof Account)
            granteeType = GranteeType.GT_USER;
        else if (grantee instanceof DistributionList)
            granteeType = GranteeType.GT_GROUP;
        else
            return null;
        
        if (!RightChecker.isValidGranteeForAdminRights(granteeType, grantee))
            return null;
        else
            return new Grantee(granteeType, grantee);
    }
    
    private Grantee(GranteeType granteeType, NamedEntry grantee) throws ServiceException {
        mGranteeType = granteeType;
        mGrantee = grantee;
        
        Provisioning prov = grantee.getProvisioning();
        mIdAndGroupIds = new HashSet<String>();
        
        AclGroups granteeGroups;
        
        switch (granteeType) {
        case GT_USER:
            mGranteeDomain = prov.getDomain((Account)grantee);
            granteeGroups = prov.getAclGroups((Account)grantee, true);
            break;
        case GT_GROUP:
            mGranteeDomain = prov.getDomain((DistributionList)grantee);
            granteeGroups = prov.getAclGroups((DistributionList)grantee, true);
            break;
        default:
            throw ServiceException.FAILURE("internal error", null);
        }
                    
        if (mGranteeDomain == null)
            throw ServiceException.FAILURE("internal error, cannot get domain for grantee", null);
        
        // setup grantees ids 
        mIdAndGroupIds.add(grantee.getId());
        mIdAndGroupIds.addAll(granteeGroups.groupIds());
    }
    
    String getId() {
        return mGrantee.getId();
    }
    
    String getName() {
        return mGrantee.getName();
    }
    
    boolean isAccount() {
        return mGranteeType == GranteeType.GT_USER;
    }
    
    Account getAccount() throws ServiceException {
        if (mGranteeType != GranteeType.GT_USER)
            throw ServiceException.FAILURE("internal error", null);
        return (Account)mGrantee;
    }
    
    DistributionList getGroup() throws ServiceException {
        if (mGranteeType != GranteeType.GT_GROUP)
            throw ServiceException.FAILURE("internal error", null);
        return (DistributionList)mGrantee;
    }
    
    Domain getDomain() {
        return mGranteeDomain;
    }
    
    Set<String> getIdAndGroupIds() {
        return mIdAndGroupIds;
    }
}
