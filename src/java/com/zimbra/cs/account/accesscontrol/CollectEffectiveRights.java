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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightBearer.GlobalAdmin;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.service.account.ToXML;

public class CollectEffectiveRights {
    
    private static final Log sLog = ZimbraLog.acl;
    
    private RightBearer mRightBearer;
    private Entry mTarget;
    private TargetType mTargetType;
    private boolean mExpandSetAttrs;
    private boolean mExpandGetAttrs;
    private RightCommand.EffectiveRights mResult;
    
    /**
     * 
     * @param rightBearer
     * @param target
     * @param targetType
     * @param expandSetAttrs
     * @param expandGetAttrs
     * @param result
     * @throws ServiceException
     */
    static void getEffectiveRights(RightBearer rightBearer, 
            Entry target, TargetType targetType,
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException{
        
        CollectEffectiveRights cer = new CollectEffectiveRights(rightBearer, 
                target, targetType, expandSetAttrs, expandGetAttrs, result);
        cer.collect();
    }
    
    static void getEffectiveRights(RightBearer rightBearer, Entry target, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException {
        
        TargetType targetType = TargetType.getTargetType(target);
        getEffectiveRights(rightBearer, target, targetType, 
                expandSetAttrs, expandGetAttrs, result);
    }
            
    private CollectEffectiveRights(RightBearer rightBearer, 
            Entry target, TargetType targetType,
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) {
        
        mRightBearer = rightBearer;
        mTarget = target;
        mTargetType = targetType;
        mExpandSetAttrs = expandSetAttrs;
        mExpandGetAttrs = expandGetAttrs;
        mResult = result;
        
    }
    
    private boolean isGlobalAdmin() {
        return (mRightBearer instanceof GlobalAdmin);
    }
    
    private Grantee getGrantee() {
        return (Grantee) mRightBearer;
    }
    
    private void collect() throws ServiceException {
        
        Set<Right> presetRights;
        AllowedAttrs allowSetAttrs;
        AllowedAttrs allowGetAttrs;
        
        if (isGlobalAdmin()) {
            // all preset rights on the target type
            presetRights = getAllExecutableAdminPresetRights();
            
            // all attrs on the target type
            allowSetAttrs = AllowedAttrs.ALLOW_ALL_ATTRS();
            
            // all attrs on the target type
            allowGetAttrs = AllowedAttrs.ALLOW_ALL_ATTRS();
            
        } else {
            // get effective preset rights
            presetRights = getEffectiveAdminPresetRights();
            
            // get effective setAttrs rights
            allowSetAttrs = CheckAttrRight.accessibleAttrs(
                    getGrantee(), mTarget, AdminRight.PR_SET_ATTRS, false);
            
            // get effective getAttrs rights
            allowGetAttrs = CheckAttrRight.accessibleAttrs(
                    getGrantee(), mTarget, AdminRight.PR_GET_ATTRS, false);
        }
        
        // finally, populate our result 
        
        // preset rights
        Set<String> rights= new HashSet<String>();
        for (Right r : presetRights) {
            rights.add(r.getName());
        }
        mResult.setPresetRights(setToSortedList(rights));
        
        // setAttrs
        if (allowSetAttrs.getResult() == AllowedAttrs.Result.ALLOW_ALL) {
            mResult.setCanSetAllAttrs();
            if (mExpandSetAttrs) {
                mResult.setCanSetAttrs(expandAttrs(AdminRight.PR_SET_ATTRS));
            }
        } else if (allowSetAttrs.getResult() == AllowedAttrs.Result.ALLOW_SOME) {
            mResult.setCanSetAttrs(fillDefault(allowSetAttrs, AdminRight.PR_SET_ATTRS));
        }
        
        // getAttrs
        if (allowGetAttrs.getResult() == AllowedAttrs.Result.ALLOW_ALL) {
            mResult.setCanGetAllAttrs();
            if (mExpandGetAttrs) {
                mResult.setCanGetAttrs(expandAttrs(AdminRight.PR_GET_ATTRS));
            }
        } else if (allowGetAttrs.getResult() == AllowedAttrs.Result.ALLOW_SOME) {
            mResult.setCanGetAttrs(fillDefault(allowGetAttrs, AdminRight.PR_GET_ATTRS));
        }
    }
    
    /*
     * get all executable preset rights on a target type
     * combo rights are expanded
     */
    private Set<Right> getAllExecutableAdminPresetRights() throws ServiceException {
        Map<String, AdminRight> allRights = RightManager.getInstance().getAllAdminRights();
        
        Set<Right> rights = new HashSet<Right>();
        
        for (Map.Entry<String, AdminRight> right : allRights.entrySet()) {
            Right r = right.getValue();
            if (r.isPresetRight()) {
                if (r.executableOnTargetType(mTargetType)) {
                    rights.add(r);
                }
                
            } else if (r.isComboRight()) {
                ComboRight comboRight = (ComboRight)r;
                for (Right rt : comboRight.getPresetRights()) {
                    if (rt.executableOnTargetType(mTargetType)) {
                        rights.add(rt);
                    }
                }
                
            }
        }
        return rights;
    }
    
    private Set<Right> getEffectiveAdminPresetRights() throws ServiceException {
        
        Provisioning prov = Provisioning.getInstance();
        
        Grantee grantee = getGrantee();
        TargetType targetType = TargetType.getTargetType(mTarget);
        
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        Integer relativity = Integer.valueOf(1);
        
        //
        // collecting phase
        //
        CheckAttrRight.CollectAttrsResult car = CheckAttrRight.CollectAttrsResult.SOME;
        
        // check the target entry itself
        List<ZimbraACE> acl = ACLUtil.getAllACEs(mTarget);
        if (acl != null) {
            collectAdminPresetRightOnTarget(acl, targetType, relativity, false, allowed, denied);
            relativity += 2;
        }
        
        //
        // if the target is a domain-ed entry, get the domain of the target.
        // It is need for checking the cross domain right.
        //
        Domain targetDomain = TargetType.getTargetDomain(prov, mTarget);
        
        // check grants granted on entries from which the target entry can inherit from
        boolean expandTargetGroups = CheckRight.allowGroupTarget(AdminRight.PR_ADMIN_PRESET_RIGHT);
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, mTarget, expandTargetGroups);
        Entry grantedOn;
            
        GroupACLs groupACLs = null;
            
        while ((grantedOn = iter.next()) != null && (!car.isAll())) {
            acl = ACLUtil.getAllACEs(grantedOn);
                
            if (grantedOn instanceof Group) {
                if (acl == null)
                    continue;
                    
                boolean skipPositiveGrants = false;
                // check cross domain right if we are checking rights for an account
                // skip cross domain rights if we are checking rights for a group, because
                // members in the group can be in different domains, no point checking it.
                if (grantee.isAccount()) {
                    skipPositiveGrants = 
                        !CrossDomain.crossDomainOK(prov, grantee.getAccount(), 
                                grantee.getDomain(), targetDomain, (Group)grantedOn);
                }
                
                // don't check yet, collect all acls on all target groups
                if (groupACLs == null) {
                    groupACLs = new GroupACLs();
                }
                groupACLs.collectACL(grantedOn, skipPositiveGrants);
                    
            } else {
                // end of group targets, put all collected denied and allowed grants into one list, as if 
                // they are granted on the same entry, then check.  We put denied in the front, so it is 
                // consistent with ZimbraACL.getAllACEs
                if (groupACLs != null) {
                    List<ZimbraACE> aclsOnGroupTargets = groupACLs.getAllACLs();
                    if (aclsOnGroupTargets != null) {
                        collectAdminPresetRightOnTarget(aclsOnGroupTargets, targetType, 
                                relativity, false, allowed, denied);
                        relativity += 2;
                    }
                        
                    // set groupACLs to null, we are done with group targets
                    groupACLs = null;
                }
                    
                if (acl == null) {
                    continue;
                }
                
                boolean subDomain = (mTargetType == TargetType.domain && (grantedOn instanceof Domain));
                collectAdminPresetRightOnTarget(acl, targetType, relativity, subDomain, allowed, denied);
                relativity += 2;
            }
        }
        
        if (sLog.isDebugEnabled()) {
            StringBuilder sbAllowed = new StringBuilder();
            for (Map.Entry<Right, Integer> a : allowed.entrySet()) {
                sbAllowed.append("(" + a.getKey().getName() + ", " + a.getValue() + ") ");
            }
            sLog.debug("allowed: " + sbAllowed.toString());
            
            StringBuilder sbDenied = new StringBuilder();
            for (Map.Entry<Right, Integer> a : denied.entrySet()) {
                sbDenied.append("(" + a.getKey().getName() + ", " + a.getValue() + ") ");
            }
            sLog.debug("denied: " + sbDenied.toString());
        }
        
        Set<Right> conflicts = SetUtil.intersect(allowed.keySet(), denied.keySet());
        if (!conflicts.isEmpty()) {
            for (Right right : conflicts) {
                if (denied.get(right) <= allowed.get(right))
                    allowed.remove(right);
            }
        }
        
        return allowed.keySet();
    }

    private void collectAdminPresetRightOnTarget(List<ZimbraACE> acl, TargetType targeType,
            Integer relativity, boolean subDomain,
            Map<Right, Integer> allowed, Map<Right, Integer> denied) 
    throws ServiceException {
        // as an individual: user
        short granteeFlags = (short)(GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);
        collectAdminPresetRights(acl, targeType, granteeFlags, relativity, 
                subDomain, allowed,  denied);
        
        // as a group member, bump up the relativity
        granteeFlags = (short)(GranteeFlag.F_GROUP | GranteeFlag.F_ADMIN);
        collectAdminPresetRights(acl, targeType, granteeFlags, relativity, 
                subDomain, allowed,  denied);
    }
    
    private void collectAdminPresetRights(List<ZimbraACE> acl, TargetType targetType,
            short granteeFlags, Integer relativity, boolean subDomain,
            Map<Right, Integer> allowed, Map<Right, Integer> denied) 
    throws ServiceException {
        
        for (ZimbraACE ace : acl) {
            GranteeType granteeType = ace.getGranteeType();
            if (!granteeType.hasFlags(granteeFlags))
                continue;
                
            if (!RightBearer.matchesGrantee(getGrantee(), ace)) {
                continue;
            }
            
            if (!ace.deny()) {
                if (subDomain != ace.subDomain())
                    continue;
            }
            
            Right right = ace.getRight();
            
            if (right.isUserRight())
                continue;
                    
            if (right.isComboRight()) {
                ComboRight comboRight = (ComboRight)right;
                for (Right r : comboRight.getPresetRights()) {
                    if (r.executableOnTargetType(targetType)) {
                        collectPresetRightIfMoreRelevant(r, ace.deny(), relativity, allowed, denied);
                    }
                }
            } else if (right.isPresetRight()) {
                if (right.executableOnTargetType(targetType)) {
                    collectPresetRightIfMoreRelevant(right, ace.deny(), relativity, allowed, denied);
                }
            } 
        }
    }
    
    private void collectPresetRightIfMoreRelevant(
            Right right, boolean negative, Integer relativity,
            Map<Right, Integer> allowed, Map<Right, Integer> denied) {
        Map<Right, Integer> map = negative ? denied : allowed;
        
        Integer mostRelevant = map.get(right);
        if (mostRelevant == null || relativity < mostRelevant) {
            map.put(right, relativity);
        }
    }
    
    private List<String> setToSortedList(Set<String> set) {
        List<String> list = new ArrayList<String>(set);
        Collections.sort(list);
        return list;
    }
    
    private SortedMap<String, RightCommand.EffectiveAttr> fillDefault(AllowedAttrs allowSetAttrs, 
            AttrRight rightNeeded) throws ServiceException {
        return fillDefaultAndConstratint(allowSetAttrs.getAllowed(), rightNeeded);
    }
    
    private SortedMap<String, RightCommand.EffectiveAttr> expandAttrs(AttrRight rightNeeded) 
    throws ServiceException {
        return fillDefaultAndConstratint(TargetType.getAttrsInClass(mTarget), rightNeeded);
    }
    
    /**
     * 
     * @param attrs
     * @param rightNeeded PR_GET_ATTRS or PR_SET_ATTRS
     * @return
     * @throws ServiceException
     */
    private SortedMap<String, RightCommand.EffectiveAttr> fillDefaultAndConstratint(
            Set<String> attrs, AttrRight rightNeeded) throws ServiceException {
        SortedMap<String, RightCommand.EffectiveAttr> effAttrs = 
            new TreeMap<String, RightCommand.EffectiveAttr>();
        
        Entry constraintEntry = AttributeConstraint.getConstraintEntry(mTarget);
        Map<String, AttributeConstraint> constraints = (constraintEntry==null)?null:
            AttributeConstraint.getConstraint(constraintEntry);
        
        boolean hasConstraints = (constraints != null && !constraints.isEmpty());

        for (String attrName : attrs) {
            if (rightNeeded == AdminRight.PR_SET_ATTRS && !isGlobalAdmin() && 
                HardRules.isForbiddenAttr(attrName)) {
                continue;
            }
            
            Set<String> defaultValues = null;
            
            Object defaultValue = mTarget.getAttrDefault(attrName);
            if (defaultValue instanceof String) {
                defaultValue = ToXML.fixupZimbraPrefTimeZoneId(attrName, (String)defaultValue);
                defaultValues = new HashSet<String>();
                defaultValues.add((String)defaultValue);
            } else if (defaultValue instanceof String[]) {
                defaultValues = new HashSet<String>(Arrays.asList((String[])defaultValue));
            }

            AttributeConstraint constraint = (hasConstraints)?constraints.get(attrName):null;
            effAttrs.put(attrName, new RightCommand.EffectiveAttr(attrName, defaultValues, constraint));
        }
        return effAttrs;
    }
}
