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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;

public class CheckAttrRight extends CheckRight {
    private static final Log sLog = LogFactory.getLog(CheckAttrRight.class);

    private Grantee mGrantee;
    private AttrRight mAttrRightNeeded; // just to save a casting from Right to AttrRight
    
    static enum CollectAttrsResult {
        SOME(false),
        ALLOW_ALL(true),
        DENY_ALL(true);
        
        private boolean mIsAll;
        
        CollectAttrsResult(boolean isAll) {
            mIsAll = isAll;
        }
        
        boolean isAll() {
            return mIsAll;
        }
    }
    
    // public only for unittest
    public static AllowedAttrs accessibleAttrs(Grantee grantee, Entry target, 
            AttrRight rightNeeded, boolean canDelegateNeeded) throws ServiceException {
        
        CheckAttrRight checker = new CheckAttrRight(grantee, target, 
                rightNeeded, canDelegateNeeded);
        
        return checker.computeAccessibleAttrs();
    }
    
    private CheckAttrRight(Grantee grantee, Entry target, 
            AttrRight rightNeeded, boolean canDelegateNeeded) throws ServiceException {
        super(target, rightNeeded, canDelegateNeeded);
        
        mGrantee = grantee;
        mTargetType = TargetType.getTargetType(mTarget);
        mAttrRightNeeded = rightNeeded;  // just to save a casting
    }
    
    private AllowedAttrs computeAccessibleAttrs() throws ServiceException {
        if (mGrantee == null)
            return AllowedAttrs.DENY_ALL_ATTRS();
                
        Map<String, Integer> allowSome = new HashMap<String, Integer>();
        Map<String, Integer> denySome = new HashMap<String, Integer>();
        Integer relativity = Integer.valueOf(1);
        
        // we iterate through all the targets from which grants can be inherited
        // by the perspective target.  More specific targets are visited before 
        // less specific targets.  For each target, there are two "ranks" of 
        // grantee types: individual and group.   Therefore, each time when we 
        // visit the next target, we bump up the relativity by 2.
        int granteeRanksPerTarget = 2;
        
        //
        // collecting phase
        //
        CollectAttrsResult car = CollectAttrsResult.SOME;
        
        // check the target entry itself
        List<ZimbraACE> acl = ACLUtil.getAllACEs(mTarget);
        if (acl != null) {
            car = checkTarget(acl, relativity, false, allowSome, denySome);
            relativity += granteeRanksPerTarget;
        }
        
        //
        // if the target is a domain-ed entry, get the domain of the target.
        // It is need for checking the cross domain right.
        //
        Domain targetDomain = TargetType.getTargetDomain(mProv, mTarget);
        
        if (!car.isAll()) {
            // check grants granted on entries from which the target entry can inherit
            boolean expandTargetGroups = CheckRight.allowGroupTarget(mRightNeeded);
            TargetIterator iter = TargetIterator.getTargetIeterator(mProv, mTarget, expandTargetGroups);
            Entry grantedOn;
            
            GroupACLs groupACLs = null;
            
            while ((grantedOn = iter.next()) != null && (!car.isAll())) {
                acl = ACLUtil.getAllACEs(grantedOn);
                
                if (grantedOn instanceof DistributionList) {
                    if (acl == null)
                        continue;
                    
                    boolean skipPositiveGrants = false;
                    // check cross domain right if we are checking rights for an account
                    // skip cross domain rights if we are checking rights for a group, because
                    // members in the group can be in different domains, no point checking it.
                    if (mGrantee.isAccount())
                        skipPositiveGrants = !CrossDomain.crossDomainOK(mProv, 
                                mGrantee.getAccount(), mGrantee.getDomain(), 
                                targetDomain, (DistributionList)grantedOn);
                    
                    // don't check yet, collect all acls on all target groups
                    if (groupACLs == null)
                        groupACLs = new GroupACLs();
                    groupACLs.collectACL(grantedOn, skipPositiveGrants);
                    
                } else {
                    // end of group targets, put all collected denied and allowed grants into 
                    // one list, as if they are granted on the same entry, then check.  
                    // We put denied in the front, so it is consistent with ZimbraACL.getAllACEs
                    if (groupACLs != null) {
                        List<ZimbraACE> aclsOnGroupTargets = groupACLs.getAllACLs();
                        if (aclsOnGroupTargets != null) {
                            car = checkTarget(aclsOnGroupTargets, relativity, false, allowSome, denySome);
                            relativity += granteeRanksPerTarget;
                            if (car.isAll()) 
                                break;
                            // else continue with the next target 
                        }
                        
                        // set groupACLs to null, we are done with group targets
                        groupACLs = null;
                    }
                    
                    if (acl == null)
                        continue;
                    
                    boolean subDomain = (mTargetType == TargetType.domain && (grantedOn instanceof Domain));
                    car = checkTarget(acl, relativity, subDomain, allowSome, denySome);
                    relativity += granteeRanksPerTarget;
                }
            }
        }
        
        // log collecting phase result
        if (sLog.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Allowed: {");
            for (Map.Entry<String, Integer> as : allowSome.entrySet())
                sb.append("(" + as.getKey() + ", " + as.getValue() + ")");
            sb.append("}");
            sb.append(" Denied: {");
            for (Map.Entry<String, Integer> ds : denySome.entrySet())
                sb.append("(" + ds.getKey() + ", " + ds.getValue() + ")");
            sb.append("}");
            sLog.debug("accessibleAttrs: " + car.name() + ". " + sb.toString());
        }
        
        //
        // computing phase
        //
        
        AllowedAttrs result;
        
        AttributeClass klass = TargetType.getAttributeClass(mTarget);
        
        if (car == CollectAttrsResult.ALLOW_ALL)
            result = processAllowAll(allowSome, denySome, klass);
        else if (car == CollectAttrsResult.DENY_ALL)
            result = processDenyAll(allowSome, denySome, klass);
        else {
            // now allowSome and denySome contain attrs allowed/denied and their shortest distance 
            // to the target, remove denied ones from allowed if they've got a shorter distance
            Set<String> conflicts = SetUtil.intersect(allowSome.keySet(), denySome.keySet());
            if (!conflicts.isEmpty()) {
                for (String attr : conflicts) {
                    if (denySome.get(attr) <= allowSome.get(attr))
                        allowSome.remove(attr);
                }
            }
            result = AllowedAttrs.ALLOW_SOME_ATTRS(allowSome.keySet());
        }
        
        // computeCanDo(result, target, rightNeeded, attrs);
        return result;

    }
    
    private CollectAttrsResult checkTarget(
            List<ZimbraACE> acl, Integer relativity, boolean subDomain,
            Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
        
        CollectAttrsResult result = null;
        
        // as an individual: user
        short granteeFlags = (short)(GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);
        result = expandACLToAttrs(acl, granteeFlags, relativity, subDomain, allowSome, denySome);
        if (result.isAll()) 
            return result;
        
        // as a group member, bump up the relativity
        granteeFlags = (short)(GranteeFlag.F_GROUP);
        result = expandACLToAttrs(acl, granteeFlags, relativity+1, subDomain, allowSome, denySome);

        return result;
    }
    
    /**
     * expand attr rights on this collection of ACEs into attributes.
     * for each grant(ACE):
     *     - checks if grantee matches 
     *     - if preset right - ignore
     *       if attrs right - expand into attributes
     *       if combo right - go through all attr rights of the combo right
     *                        and expand them into attributes 
     * 
     * @param acl                the collection of grants
     * @param rightTypeNeeded    getAttrs or setAttrs
     * @param granteeFlags       which kinds of grantees to look for 
     * @param relativity         how relevant this set of grant is to the perspective target entry
     *                           e.g. On the target side, if the perspective target is an account, 
     *                                then grants on the account entry is more relevant than grants 
     *                                on groups the account is a member of, and which is more relevant 
     *                                than the domain the account is in.
     *                                On the grantee side, it is more relevant if the grantee matches 
     *                                the grant because of the grant is directly granted to the grantee;
     *                                it is less relevant if the grantee is a member of a group to which 
     *                                the grant is granted.  
     *                           This is needed for computing the net set of attributes that should 
     *                           be allowed/denied after all attr rights are expanded.  If an attribute 
     *                           is denied by a more relevant grant and allowed by a less relevant grant,
     *                           it will be denied.
     * @param allowSome          output param for allowed attributes, set when CollectAttrsResult.SOME is returned
     * @param denySome           output param for denied attributes, set when CollectAttrsResult.SOME is returned
     * @return if the acl:
     *             allow all(CollectAttrsResult.ALLOW_ALL), or
     *             deny all(CollectAttrsResult.DENY_ALL), or 
     *             allow/deny some(CollectAttrsResult.SOME) attributes
     *         to be get/set on the target.
     *         
     *         If CollectAttrsResult.SOME is returned, attributes allowed
     *         are put in the allowSome param, attributes specifically denied 
     *         are put in the denySome param.
     *         
     * @throws ServiceException
     */
    private CollectAttrsResult expandACLToAttrs(
            List<ZimbraACE> acl, short granteeFlags, Integer relativity, boolean subDomain,
            Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
                                                
        CollectAttrsResult result = null;
        
        // set of zimbraIds the grantee in question can assume: including 
        //   - zimbraId of the grantee
        //   - all admin groups the grantee is in 
        Set<String> granteeIds = mGrantee.getIdAndGroupIds();
        
        for (ZimbraACE ace : acl) {
            GranteeType granteeType = ace.getGranteeType();
            if (!granteeType.hasFlags(granteeFlags))
                continue;
            
            if (!granteeIds.contains(ace.getGrantee()))
                continue;
            
            Right rightGranted = ace.getRight();
            if (rightGranted.isPresetRight())
                continue;
            
            /*
             * if the grant is executable only and canDelegateNeeded is true,
             * skip the grant and let the flow continue to check other grants, 
             */
            if (mCanDelegateNeeded && ace.canExecuteOnly())
                continue; // just ignore the grant

            
            // negative grants are always effective on sub domains
            if (!ace.deny()) {
                if (subDomain != ace.subDomain())
                    continue;
            }
            
            if (rightGranted.isAttrRight()) {
                AttrRight attrRight = (AttrRight)rightGranted;
                result = expandAttrsGrantToAttrs(ace, attrRight, relativity, allowSome, denySome);
                
                /*
                 * denied grants appear before allowed grants
                 * - if we see a deny all, return, because on the same target/grantee-relativity deny 
                 *   take precedence over allowed.
                 * - if we see an allow all, return, because all deny-some grants have been processed, 
                 *   and have been put in the denySome map, those will be remove from the allowed ones.
                 */
                if (result != null && result.isAll())
                    return result;
                
            } else if (rightGranted.isComboRight()) {
                ComboRight comboRight = (ComboRight)rightGranted;
                for (AttrRight attrRight : comboRight.getAttrRights()) {
                    result = expandAttrsGrantToAttrs(ace, attrRight, relativity, allowSome, denySome);
                    // return if we see an allow-all/deny-all, same reason as above.
                    if (result != null && result.isAll())
                        return result;
                }
            }
        }
        
        return CollectAttrsResult.SOME;
    }
    
    private CollectAttrsResult expandAttrsGrantToAttrs(
            ZimbraACE ace, AttrRight attrRightGranted, Integer relativity,
            Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
        
        /*
         * note: do NOT call ace.getRight in this method, ace is passed in for deny() and getTargetType() 
         * calls only.  
         *
         * Because if the granted right is a combo right, ace.getRight will return the combo right, 
         * not the attr rights in the combo right.  If the grant contains a combo right, each attr 
         * rights of the combo right will be passed in as the attrRight arg.
         * 
         * - AttrRight specific changes are done in this method.
         *   (i.e. checks can only be done on a AttrRight, not on combo right)
         *
         * - ZimbraACE level checks are done in the caller (expandACLToAttrs).
         */
        
        Right.RightType rightTypeNeeded = mAttrRightNeeded.getRightType();
        
        /*
         * check if get/set matches
         */
        if (!attrRightGranted.suitableFor(rightTypeNeeded))
            return null;
        
        /*
         * check if the granted attrs right is indeed applicable on the target type
         * this is a sanity check in case someone somehow sneaked in a zimbraACE on 
         * the wrong target.  e.g. put a setAttrs server right on a cos entry.  
         * This should not happen if the grant is done via RightCommand.grantRight.
         */
         if (!CheckRight.rightApplicableOnTargetType(mTargetType, attrRightGranted, mCanDelegateNeeded))
         // if (!attrRightGranted.executableOnTargetType(targetType))
            return null;
         
        /*
         * If canDelegateNeeded is true, rightApplicableOnTargetType return true 
         * if right.grantableOnTargetType() is true.  This creates a problem for 
         * granting attr right when:
         * 
         *   e.g. grant on domain d.com: {id-of-adminA} usr +set.domain.zimbraMailStatus
         *        Note the grant is for the zimbraMailStatus on domain, not account(the 
         *        attr is also on account)
         *        
         *        now, adminA trying to "ma usr1@d.com zimbraMailStatus enabled" is PERM_DENIED, 
         *        this is correct.  It is blocked in rightApplicableOnTargetType because 
         *        the decision was made on right.executableOnTargetType (the path when 
         *        canDelegateNeeded is false)
         *        
         *        but, adminA trying to "grr domain d.com usr adminB set.account.zimbraMailStatus" 
         *        is will be allowed, this is *wrong*.  The right adminA has is setting 
         *        zimbraMailStatus on domain, not on account, but it can end up granting the 
         *        set.account.zimbraMailStatus to admin B.  The following check fixes this 
         *        problem.
         *        
         * This is not a problem for granting preset right because each preset right has a 
         * specific name and designated target.  
         * 
         * e.g. if on domain d.com: {id-of-adminA} usr +deleteDomain   
         *      then when adminA is trying to grant the deleteAccount right, he can't because
         *      the right is simply not there.  deleteAccount and deleteDomain are totally 
         *      two different rights.
         *      
         */
        if (mAttrRightNeeded != AdminRight.PR_GET_ATTRS && mAttrRightNeeded != AdminRight.PR_SET_ATTRS) {
            // not pseudo right, that means we are checking for a specific right
            if (SetUtil.intersect(attrRightGranted.getTargetTypes(), mAttrRightNeeded.getTargetTypes()).isEmpty())
                return null;
        }
        
        if (attrRightGranted.allAttrs()) {
            // all attrs, return if we hit a grant with all attrs
            if (ace.deny()) {
                /*
                 * if the grant is setAttrs and the needed right is getAttrs:
                 *    - negative setAttrs grant does *not* negate any attrs explicitly 
                 *      allowed for getAttrs grants
                 *    - positive setAttrs grant *does* automatically give getAttrs right
                 */
                if (attrRightGranted.getRightType() == rightTypeNeeded)
                    return CollectAttrsResult.DENY_ALL;  // right type granted === right type needed
                else
                    return null;  // just ignore the grant
            } else {
                return CollectAttrsResult.ALLOW_ALL;
            }
        } else {
            // some attrs
            if (ace.deny()) {
                if (attrRightGranted.getRightType() == rightTypeNeeded) {
                    for (String attrName : attrRightGranted.getAttrs())
                        denySome.put(attrName, relativity);  // right type granted === right type needed
                } else
                    return null;  // just ignore the grant
            } else {
                for (String attrName : attrRightGranted.getAttrs())
                    allowSome.put(attrName, relativity);
            }
            return CollectAttrsResult.SOME;
        }
    }
    
    private AllowedAttrs processDenyAll(Map<String, Integer> allowSome, Map<String, Integer> denySome, 
            AttributeClass klass) throws ServiceException {
        if (allowSome.isEmpty())
            return AllowedAttrs.DENY_ALL_ATTRS();
        else {
            Set<String> allowed = allowSome.keySet();
            return AllowedAttrs.ALLOW_SOME_ATTRS(allowed);
        }
    }

    private AllowedAttrs processAllowAll(Map<String, Integer> allowSome, Map<String, Integer> denySome, 
            AttributeClass klass) throws ServiceException {
        
        if (denySome.isEmpty()) {
            return AllowedAttrs.ALLOW_ALL_ATTRS();
        } else {
            // get all attrs that can appear on the target entry
            Set<String> allowed = new HashSet<String>();
            allowed.addAll(AttributeManager.getInstance().getAllAttrsInClass(klass));
            
            // remove denied from all
            for (String d : denySome.keySet())
                allowed.remove(d);
            return AllowedAttrs.ALLOW_SOME_ATTRS(allowed);
        }
    }
}
