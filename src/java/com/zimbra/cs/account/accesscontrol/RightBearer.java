/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GroupMembership;

public abstract class RightBearer {
    protected NamedEntry mRightBearer;
    
    static RightBearer newRightBearer(NamedEntry entry) throws ServiceException {
        if (entry instanceof Account && AccessControlUtil.isGlobalAdmin((Account)entry, true)) {
            return new GlobalAdmin(entry);
        } else {
            return new Grantee(entry);
        }
    }
    
    protected RightBearer(NamedEntry grantee) {
        mRightBearer = grantee;
    }
    
    String getId() {
        return mRightBearer.getId();
    }
    
    String getName() {
        return mRightBearer.getName();
    }

    static class GlobalAdmin extends RightBearer {
        private GlobalAdmin(NamedEntry grantee) throws ServiceException {
            super(grantee);
        }
    }
    
    // public only for unit test. TODO: cleanup unit test
    public static class Grantee extends RightBearer {
        GranteeType mGranteeType;
        Domain mGranteeDomain;
        Set<String> mIdAndGroupIds;
        
        public Grantee(NamedEntry grantee) throws ServiceException {
            this(grantee, true);
        }
        
        // public only for unit test. TODO: cleanup unit test
        public Grantee(NamedEntry grantee, boolean adminOnly) throws ServiceException {
            super(grantee);
            
            Provisioning prov = grantee.getProvisioning();
            GroupMembership granteeGroups = null;
            
            if (grantee instanceof Account) {
                mGranteeType = GranteeType.GT_USER;
                mGranteeDomain = prov.getDomain((Account)grantee);
                granteeGroups = prov.getGroupMembership((Account)grantee, adminOnly);
                
            } else if (grantee instanceof DistributionList) {
                mGranteeType = GranteeType.GT_GROUP;
                mGranteeDomain = prov.getDomain((DistributionList)grantee);
                granteeGroups = prov.getGroupMembership((DistributionList)grantee, adminOnly);
                
            } else if (grantee instanceof DynamicGroup) {
                mGranteeType = GranteeType.GT_GROUP;
                mGranteeDomain = prov.getDomain((DynamicGroup)grantee);
                // no need to get membership for dynamic groups
                // dynamic groups cannot be nested, either as a members in another 
                // dynamic group or a distribution list
                
            } else {
                if (adminOnly) {
                    throw ServiceException.INVALID_REQUEST("invalid grantee type", null);
                } else {
                    if (grantee instanceof Domain) {
                        mGranteeType = GranteeType.GT_DOMAIN;
                        mGranteeDomain = (Domain) grantee;
                    }
                }
            }
            
            if (adminOnly) {
                if (!RightBearer.isValidGranteeForAdminRights(mGranteeType, grantee)) {
                    throw ServiceException.INVALID_REQUEST("invalid grantee", null);
                }
            }
            
            if (mGranteeDomain == null) {
                throw ServiceException.FAILURE("internal error, cannot get domain for grantee", null);
            }
            
            // setup grantees ids 
            mIdAndGroupIds = new HashSet<String>();
            mIdAndGroupIds.add(grantee.getId());
            if (granteeGroups != null) {
                mIdAndGroupIds.addAll(granteeGroups.groupIds());
            }
        }
        
        boolean isAccount() {
            return mGranteeType == GranteeType.GT_USER;
        }
        
        Account getAccount() throws ServiceException {
            if (mGranteeType != GranteeType.GT_USER) {
                throw ServiceException.FAILURE("internal error", null);
            }
            return (Account)mRightBearer;
        }
        
        Domain getDomain() {
            return mGranteeDomain;
        }
        
        Set<String> getIdAndGroupIds() {
            return mIdAndGroupIds;
        }
    }

    /**
     * returns true if grantee {@code gt} can be granted Admin Rights
     * 
     * Note: 
     *     - system admins cannot receive grants - they don't need any
     *     
     * @param gt
     * @param grantee
     * @return
     */
    static boolean isValidGranteeForAdminRights(GranteeType gt, NamedEntry grantee) {
        if (gt == GranteeType.GT_USER) {
            return (!grantee.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false) &&
                    grantee.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false));
        } else if (gt == GranteeType.GT_GROUP) {
            return grantee.getBooleanAttr(Provisioning.A_zimbraIsAdminGroup, false);
        } else if (gt == GranteeType.GT_EXT_GROUP) {
            return true;
        } else {
            return false;
        }
    }
    
    static boolean matchesGrantee(Grantee grantee, ZimbraACE ace) throws ServiceException {
        // set of zimbraIds the grantee in question can assume: including 
        //   - zimbraId of the grantee
        //   - all zimbra internal admin groups the grantee is in 
        Set<String> granteeIds = grantee.getIdAndGroupIds();
        if (granteeIds.contains(ace.getGrantee())) {
            return true;
        } else if (ace.getGranteeType() == GranteeType.GT_EXT_GROUP) {
            if (grantee.isAccount()) {
                return ace.matchesGrantee(grantee.getAccount(), true);
            } else {
                // we are collecting rights for a group grantee
                // TODO
                throw ServiceException.FAILURE("Not yet implemented", null);
            }
        } else {
            return false;
        }
    }
    
    
}
