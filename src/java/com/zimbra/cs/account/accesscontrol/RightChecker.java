/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.RightBearer.GlobalAdmin;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.accesscontrol.SearchGrants.GrantsOnTarget;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapFilter;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.service.account.ToXML;

public class RightChecker {

    private static final Log sLog = LogFactory.getLog(RightChecker.class);
   
    private static class SeenRight {
        private boolean mSeenRight;
        
        void setSeenRight() {
            mSeenRight = true;
        }
        
        boolean seenRight() {
            return mSeenRight;
        }
    }
    
    /*
     * aux class for collecting ACLs on all groups the perspective target entry
     * is a direct/indirect member of. 
     */
    private static class GroupACLs {
        private Set<ZimbraACE> aclsOnGroupTargetsAllowedNotDelegable = null;
        private Set<ZimbraACE> aclsOnGroupTargetsAllowedDelegable = null;
        private Set<ZimbraACE> aclsOnGroupTargetsDenied = null;
        
        void collectACL(Entry grantedOn, boolean skipPositiveGrants) throws ServiceException {
            if (aclsOnGroupTargetsAllowedNotDelegable == null)
                aclsOnGroupTargetsAllowedNotDelegable = new HashSet<ZimbraACE>();
            if (aclsOnGroupTargetsAllowedDelegable == null)
                aclsOnGroupTargetsAllowedDelegable = new HashSet<ZimbraACE>();
            if (aclsOnGroupTargetsDenied == null)
                aclsOnGroupTargetsDenied = new HashSet<ZimbraACE>();
            
            Set<ZimbraACE> allowedNotDelegable = ACLUtil.getAllowedNotDelegableACEs(grantedOn);
            Set<ZimbraACE> allowedDelegable = ACLUtil.getAllowedDelegableACEs(grantedOn);
            Set<ZimbraACE> denied = ACLUtil.getDeniedACEs(grantedOn);
            
            if (allowedNotDelegable != null && !skipPositiveGrants)
                aclsOnGroupTargetsAllowedNotDelegable.addAll(allowedNotDelegable);
            
            if (allowedDelegable != null && !skipPositiveGrants)
                aclsOnGroupTargetsAllowedDelegable.addAll(allowedDelegable);
            
            if (denied != null)
                aclsOnGroupTargetsDenied.addAll(denied);
        }
        
        /*
         * put all denied and allowed grants into one list, as if they are granted 
         * on the same entry.   We put denied in the front, followed by allowed and 
         * delegable, followed by allowed but not delegable, so it is consistent with 
         * ZimbraACL.getAllACEs
         */
        List<ZimbraACE> getAllACLs() {
            if ((aclsOnGroupTargetsAllowedNotDelegable != null && !aclsOnGroupTargetsAllowedNotDelegable.isEmpty()) ||
                (aclsOnGroupTargetsAllowedDelegable != null && !aclsOnGroupTargetsAllowedDelegable.isEmpty()) ||   
                (aclsOnGroupTargetsDenied != null && !aclsOnGroupTargetsDenied.isEmpty())) {
                    
                List<ZimbraACE> aclsOnGroupTargets = new ArrayList<ZimbraACE>();
                aclsOnGroupTargets.addAll(aclsOnGroupTargetsDenied);
                aclsOnGroupTargets.addAll(aclsOnGroupTargetsAllowedDelegable);
                aclsOnGroupTargets.addAll(aclsOnGroupTargetsAllowedNotDelegable);
                    
                return aclsOnGroupTargets;
            } else
                return null;
        }
    }

    private static boolean crossDomainOK(Provisioning prov, Account grantee, Domain granteeDomain, 
            Domain targetDomain, DistributionList grantedOn) throws ServiceException {
        
       if (!CrossDomain.checkCrossDomain(prov, granteeDomain, targetDomain, 
                (DistributionList)grantedOn)) {
            sLog.info("No cross domain right for " + grantee.getName() + " on domain " +
                    targetDomain.getName() + 
                    ", skipping positive grants on dl " + ((DistributionList)grantedOn).getName());
            
            return false;
        } else
            return true;
    }
    
    /**
     * Check if grantee is allowed for rightNeeded on target entry.
     * 
     * @param grantee
     * @param target
     * @param rightNeeded
     * @param canDelegateNeeded if we are checking for "can delegate" the right
     * @param via if not null, will be populated with the grant info via which the result was decided.
     * @return Boolean.TRUE if allowed, 
     *         Boolean.FALSE if denied, 
     *         null if there is no grant applicable to the rightNeeded.
     * @throws ServiceException
     */
    static Boolean checkPresetRight(Account grantee, Entry target, 
            Right rightNeeded, boolean canDelegateNeeded, ViaGrant via) throws ServiceException {
        if (!rightNeeded.isPresetRight())
            throw ServiceException.INVALID_REQUEST("RightChecker.canDo can only check preset right, right " + 
                    rightNeeded.getName() + " is a " + rightNeeded.getRightType() + " right",  null);
        
        boolean adminRight = !rightNeeded.isUserRight();
        
        Provisioning prov = Provisioning.getInstance();
        Domain granteeDomain = null; 
        
        if (adminRight) {
            // if the grantee is no longer legitimate, e.g. not an admin any more, ignore all his grants
            if (!isValidGranteeForAdminRights(GranteeType.GT_USER, grantee))
                return null;
            
            granteeDomain = prov.getDomain(grantee);
            // if we ever get here, the grantee must have a domain
            if (granteeDomain == null)
                throw ServiceException.FAILURE("internal error, cannot find domain for " + grantee.getName(), null);
                 
            // should only come from granting/revoking check
            if (rightNeeded == Admin.R_crossDomainAdmin)
                return CrossDomain.checkCrossDomainAdminRight(prov, granteeDomain, target, canDelegateNeeded);
        }

        
        Boolean result = null;
        SeenRight seenRight = new SeenRight();
        
        // only get admin groups 
        AclGroups granteeGroups = prov.getAclGroups(grantee, adminRight);
        
        TargetType targetType = TargetType.getTargetType(target);
            
        // This path is called from AccessManager.canDo, the target object can be a 
        // DistributionList obtained from prov.get(DistributionListBy).  
        // We require one from prov.getAclGroup(DistributionListBy) here, call getAclGroup to be sure.
        if (target instanceof DistributionList)
            target = prov.getAclGroup(DistributionListBy.id, ((DistributionList)target).getId());
        
        // check grants explicitly granted on the target entry
        // we don't return the target entry itself in TargetIterator because if 
        // target is a dl, we need to know if the dl returned from TargetIterator 
        // is the target itself or one of the groups the target is in.  So we check 
        // the actual target separately
        List<ZimbraACE> acl = ACLUtil.getAllACEs(target);
        if (acl != null) {
            result = checkTargetPresetRight(acl, targetType, grantee, granteeGroups, 
                    rightNeeded, canDelegateNeeded, via, seenRight);
            if (result != null) 
                return result;
        }
        
        //
        // if the target is a domain-ed entry, get the domain of the target.
        // It is need for checking the cross domain right.
        //
        Domain targetDomain = TargetType.getTargetDomain(prov, target);
        
        // check grants granted on entries from which the target entry can inherit from
        TargetIterator iter = TargetIterator.getTargetIeterator(Provisioning.getInstance(), target);
        Entry grantedOn;
        
        GroupACLs groupACLs = null;
        
        while ((grantedOn = iter.next()) != null) {
            acl = ACLUtil.getAllACEs(grantedOn);
            
            if (grantedOn instanceof DistributionList) {
                if (acl == null)
                    continue;
                
                boolean skipPositiveGrants = false;
                if (adminRight)
                    skipPositiveGrants = !crossDomainOK(prov, grantee, granteeDomain, 
                        targetDomain, (DistributionList)grantedOn);
                
                // don't check yet, collect all acls on all target groups
                if (groupACLs == null)
                    groupACLs = new GroupACLs();
                groupACLs.collectACL(grantedOn, skipPositiveGrants);
                
            } else {
                // end of group targets, put all collected denied and allowed grants into one 
                // list, as if they are granted on the same entry, then check.  
                // We put denied in the front, so it is consistent with ZimbraACL.getAllACEs
                if (groupACLs != null) {
                    List<ZimbraACE> aclsOnGroupTargets = groupACLs.getAllACLs();
                    if (aclsOnGroupTargets != null)
                        result = checkTargetPresetRight(aclsOnGroupTargets, targetType, grantee, granteeGroups, 
                                rightNeeded, canDelegateNeeded, via, seenRight);
                    if (result != null) 
                        return result;
                    
                    // set groupACLs to null, we are done with group targets
                    groupACLs = null;
                }
                
                // didn't encounter any group grantedOn, or none of them matches, just check this grantedOn entry
                if (acl == null)
                    continue;
                result = checkTargetPresetRight(acl, targetType, grantee, granteeGroups, 
                        rightNeeded, canDelegateNeeded, via, seenRight);
                if (result != null) 
                    return result;
            }
        }
        
        if (seenRight.seenRight())
            return Boolean.FALSE;
        else
            return null;
    }
    
    private static Boolean checkTargetPresetRight(List<ZimbraACE> acl, 
            TargetType targetType, 
            Account grantee, AclGroups granteeGroups, 
            Right rightNeeded, boolean canDelegateNeeded,
            ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        
        // if the right is user right, checking for individual match will
        // only check for user grantees, if there are any guest or key grantees
        // (there should *not* be any), they are ignored.
        short adminFlag = (rightNeeded.isUserRight()? 0 : GranteeFlag.F_ADMIN);
        
        // as an individual: user, guest, key
        result = checkPresetRight(acl, targetType, grantee, rightNeeded, canDelegateNeeded, 
                (short)(GranteeFlag.F_INDIVIDUAL | adminFlag), via, seenRight);
        if (result != null) 
            return result;
        
        // as a group member
        result = checkGroupPresetRight(acl, targetType, granteeGroups, grantee, rightNeeded, canDelegateNeeded, 
                (short)(GranteeFlag.F_GROUP), via, seenRight);
        if (result != null) 
            return result;
       
        // if right is an user right, check authed users and public
        if (rightNeeded.isUserRight()) {
            // all authed zimbra user
            result = checkPresetRight(acl, targetType, grantee, rightNeeded, canDelegateNeeded, 
                    (short)(GranteeFlag.F_AUTHUSER), via, seenRight);
            if (result != null) 
                return result;
            
            // public
            result = checkPresetRight(acl, targetType, grantee, rightNeeded, canDelegateNeeded, 
                    (short)(GranteeFlag.F_PUBLIC), via, seenRight);
            if (result != null) 
                return result;
        }
        
        return null;
    }
    
    /*
     * check if rightNeeded is applicable on target type 
     */
    static boolean rightApplicableOnTargetType(TargetType targetType, 
            Right rightNeeded, boolean canDelegateNeeded) {
        if (canDelegateNeeded) {
            // check if the right is grantable on the target
            if (!rightNeeded.grantableOnTargetType(targetType))
                return false;
        } else {
            // check if the right is executable on the target
            if (!rightNeeded.executableOnTargetType(targetType))
                return false;
        }
        return true;
    }
    
    /*
     * checks 
     *     - if the grant matches required granteeFlags
     *     - if the granted right is applicable to the entry on which it is granted
     *     - if the granted right matches the requested right
     *     
     *     Note: if canDelegateNeeded is true but the granted right is not delegable,
     *           we skip the grant (i.e. by returning false) and let the flow continue 
     *           to check other grants, instead of denying the granting attempt.
     *            
     *           That is, a more relevant executable only grant will *not* take away
     *           the grantable property of a less relevant grantable grant.
     *                 
     */
    private static boolean matchesPresetRight(ZimbraACE ace, TargetType targetType,
            Right rightNeeded, boolean canDelegateNeeded, short granteeFlags) throws ServiceException {
        GranteeType granteeType = ace.getGranteeType();
        if (!granteeType.hasFlags(granteeFlags))
            return false;
            
        if (!rightApplicableOnTargetType(targetType, rightNeeded, canDelegateNeeded))
            return false;
        
        if (canDelegateNeeded && ace.canExecuteOnly())
            return false;
            
        Right rightGranted = ace.getRight();
        if ((rightGranted.isPresetRight() && rightGranted == rightNeeded) ||
             rightGranted.isComboRight() && ((ComboRight)rightGranted).containsPresetRight(rightNeeded)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * go through each grant in the ACL
     *     - checks if the right/target of the grant matches the right/target of the grant
     *       and 
     *       if the grantee type(specified by granteeFlags) is one of the grantee type 
     *       we are interested in this call.
     *       
     *     - if so marks the right "seen" (so callsite default(only used by user right callsites) 
     *       won't be honored)
     *     - check if the Account (the grantee parameter) matches the grantee of the grant
     *       
     * @param acl
     * @param targetType
     * @param grantee
     * @param rightNeeded
     * @param canDelegateNeeded
     * @param granteeFlags       For admin rights, because of negative grants and the more "specific" 
     *                           grantee takes precedence over the less "specific" grantee, we can't 
     *                           just do a single ZimbraACE.match to see if a grantee matches the grant.
     *                           Instead, we need to check more specific grantee types first, then 
     *                           go on the the less specific ones.  granteeFlags specifies the 
     *                           grantee type(s) we are checking for this call.
     *                           e.g. an ACL has:
     *                                       adminA deny  rightR  - grant1
     *                                       groupG allow rightR  - grant2
     *                                and adminA is in groupG, we want to check grant1 before grant2.
     *                                       
     * @param via
     * @param seenRight
     * @return
     * @throws ServiceException
     */
    private static Boolean checkPresetRight(List<ZimbraACE> acl, TargetType targetType, 
            Account grantee, Right rightNeeded, boolean canDelegateNeeded,
            short granteeFlags, ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : acl) {
            if (!matchesPresetRight(ace, targetType, rightNeeded, canDelegateNeeded, granteeFlags))
                continue;
            
            // if we get here, the right matched, mark it in seenRight.  
            // This is so callsite default will not be honored.
            seenRight.setSeenRight();
                
            if (ace.matchesGrantee(grantee))
                return gotResult(ace, grantee, rightNeeded, canDelegateNeeded, via);
        }
       
        return result;
    }
    
    /*
     * Like checkPresetRight, but checks group grantees.  Instead of calling ZimbraACE.match, 
     * which checks group grants by call inDistributionList, we do it the other way around 
     * by passing in an AclGroups object that contains all the groups the account is in that 
     * are "eligible" for the grant.   We check if the grantee of the grant is one of the 
     * "eligible" group the account is in.  
     *
     * Eligible:
     *   - for admin rights granted to a group, the grant iseffective only if the group has
     *     zimbraIsAdminGroup=TRUE.  The the group's zimbraIsAdminGroup is set to false after 
     *     if grant is made, the grant is still there on the targe entry, but becomes useless.
     */
    private static Boolean checkGroupPresetRight(List<ZimbraACE> acl, TargetType targetType, 
            AclGroups granteeGroups, Account grantee, 
            Right rightNeeded, boolean canDelegateNeeded,
            short granteeFlags, ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : acl) {
            if (!matchesPresetRight(ace, targetType, rightNeeded, canDelegateNeeded, granteeFlags))
                continue;
            
            // if we get here, the right matched, mark it in seenRight.  
            // This is so callsite default will not be honored.
            seenRight.setSeenRight();
            
            if (granteeGroups.groupIds().contains(ace.getGrantee()))   
                return gotResult(ace, grantee, rightNeeded, canDelegateNeeded, via);
        }
        return result;
    }
    
    private static Boolean gotResult(ZimbraACE ace, Account grantee, Right right, boolean canDelegateNeeded, 
            ViaGrant via) throws ServiceException {
        if (ace.deny()) {
            if (sLog.isDebugEnabled())
                sLog.debug("Right " + "[" + right.getName() + "]" + " DENIED to " + grantee.getName() + 
                           " via grant: " + ace.dump() + " on: " + ace.getTargetType().getCode() + ace.getTargetName());
            if (via != null)
                via.setImpl(new ViaGrantImpl(ace.getTargetType(),
                                             ace.getTargetName(),
                                             ace.getGranteeType(),
                                             ace.getGranteeDisplayName(),
                                             ace.getRight(),
                                             ace.deny()));
            return Boolean.FALSE;
        } else {
            if (sLog.isDebugEnabled())
                sLog.debug("Right " + "[" + right.getName() + "]" + " ALLOWED to " + grantee.getName() + 
                           " via grant: " + ace.dump() + " on: " + ace.getTargetType().getCode() + ace.getTargetName());
            if (via != null)
                via.setImpl(new ViaGrantImpl(ace.getTargetType(),
                                             ace.getTargetName(),
                                             ace.getGranteeType(),
                                             ace.getGranteeDisplayName(),
                                             ace.getRight(),
                                             ace.deny()));
            return Boolean.TRUE;
        }
    }
    
    /**
     * 
     * @param grantee
     * @param target
     * @param rightNeeded
     * @param attrs
     * @return
     * @throws ServiceException
     */
    // public only for unittest
    public static AllowedAttrs accessibleAttrs(Grantee grantee, Entry target, 
            AttrRight rightNeeded, boolean canDelegateNeeded) throws ServiceException {
        if (grantee == null)
            return AllowedAttrs.DENY_ALL_ATTRS();
        
        Provisioning prov = Provisioning.getInstance();
        
        Set<String> granteeIds = grantee.getIdAndGroupIds();
        TargetType targetType = TargetType.getTargetType(target);
        
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
        List<ZimbraACE> acl = ACLUtil.getAllACEs(target);
        if (acl != null) {
            car = checkTargetAttrsRight(acl, targetType, granteeIds, 
                    rightNeeded, canDelegateNeeded, relativity, allowSome, denySome);
            relativity += granteeRanksPerTarget;
        }
        
        //
        // if the target is a domain-ed entry, get the domain of the target.
        // It is need for checking the cross domain right.
        //
        Domain targetDomain = TargetType.getTargetDomain(prov, target);
        
        if (!car.isAll()) {
            // check grants granted on entries from which the target entry can inherit
            TargetIterator iter = TargetIterator.getTargetIeterator(prov, target);
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
                    if (grantee.isAccount())
                        skipPositiveGrants = !crossDomainOK(prov, grantee.getAccount(), grantee.getDomain(), 
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
                            car = checkTargetAttrsRight(aclsOnGroupTargets, targetType, granteeIds, 
                                    rightNeeded, canDelegateNeeded, relativity, allowSome, denySome);
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
                    car = checkTargetAttrsRight(acl, targetType, granteeIds, 
                            rightNeeded, canDelegateNeeded, relativity, allowSome, denySome);
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
        
        AttributeClass klass = TargetType.getAttributeClass(target);
        
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
    
    private static AllowedAttrs processDenyAll(Map<String, Integer> allowSome, Map<String, Integer> denySome, 
            AttributeClass klass) throws ServiceException {
        if (allowSome.isEmpty())
            return AllowedAttrs.DENY_ALL_ATTRS();
        else {
            Set<String> allowed = allowSome.keySet();
            return AllowedAttrs.ALLOW_SOME_ATTRS(allowed);
        }
    }

    private static AllowedAttrs processAllowAll(Map<String, Integer> allowSome, Map<String, Integer> denySome, 
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
    
    /**
     * 
     * @param acl
     * @param targetType
     * @param granteeIds
     * @param rightNeeded
     * @param canDelegateNeeded
     * @param relativity
     * @param allowSome
     * @param denySome
     * @return
     * @throws ServiceException
     */
    private static CollectAttrsResult checkTargetAttrsRight(
            List<ZimbraACE> acl, TargetType targetType,
            Set<String> granteeIds, 
            AttrRight rightNeeded, boolean canDelegateNeeded,
            Integer relativity,
            Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
        
        CollectAttrsResult result = null;
        
        // as an individual: user
        short granteeFlags = (short)(GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);
        result = expandACLToAttrs(acl, targetType, granteeIds, rightNeeded, 
                canDelegateNeeded, granteeFlags, relativity, allowSome, denySome);
        if (result.isAll()) 
            return result;
        
        // as a group member, bump up the relativity
        granteeFlags = (short)(GranteeFlag.F_GROUP);
        result = expandACLToAttrs(acl, targetType, granteeIds, rightNeeded, 
                canDelegateNeeded, granteeFlags, relativity+1, allowSome, denySome);

        return result;
    }

    private static enum CollectAttrsResult {
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
    
    private static CollectAttrsResult expandAttrsGrantToAttrs(
            ZimbraACE ace, TargetType targetType, 
            AttrRight attrRightGranted, 
            AttrRight rightNeeded, boolean canDelegateNeeded,
            Integer relativity,
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
        
        Right.RightType rightTypeNeeded = rightNeeded.getRightType();
        
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
         if (!rightApplicableOnTargetType(targetType, attrRightGranted, canDelegateNeeded))
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
        if (rightNeeded != AdminRight.PR_GET_ATTRS && rightNeeded != AdminRight.PR_SET_ATTRS) {
            // not pseudo right, that means we are checking for a specific right
            if (SetUtil.intersect(attrRightGranted.getTargetTypes(), rightNeeded.getTargetTypes()).isEmpty())
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
     * @param targetType         target type of interest
     * @param granteeIds         set of zimbraIds the grantee in question can assume: including 
     *                               - zimbraId of the grantee
     *                               - all admin groups the grantee is in 
     * @param rightTypeNeeded    getAttrs or setAttrs
     * @param canDelegateNeeded  if this check is for checking if a right can be delegated to others
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
    private static CollectAttrsResult expandACLToAttrs(
            List<ZimbraACE> acl, TargetType targetType,
            Set<String> granteeIds, 
            AttrRight rightNeeded, boolean canDelegateNeeded,
            short granteeFlags, Integer relativity,
            Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
                                                
        CollectAttrsResult result = null;
        
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
            if (canDelegateNeeded && ace.canExecuteOnly())
                continue; // just ignore the grant

            
            if (rightGranted.isAttrRight()) {
                AttrRight attrRight = (AttrRight)rightGranted;
                result = expandAttrsGrantToAttrs(ace, targetType, attrRight, 
                        rightNeeded, canDelegateNeeded, relativity, allowSome, denySome);
                
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
                    result = expandAttrsGrantToAttrs(ace, targetType, attrRight, 
                            rightNeeded, canDelegateNeeded, relativity, allowSome, denySome);
                    // return if we see an allow-all/deny-all, same reason as above.
                    if (result != null && result.isAll())
                        return result;
                }
            }
        }
        
        return CollectAttrsResult.SOME;
    }
    
    

    static void getEffectiveRights(
            RightBearer rightBearer, Entry target, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException {
        
        TargetType targetType = TargetType.getTargetType(target);
        getEffectiveRights(rightBearer, target, targetType, 
                expandSetAttrs, expandGetAttrs, result);
    }

    
    /**
     * 
     * @param effectiveACLs
     * @param grantee
     * @param klass
     * @param result
     * @return
     * @throws ServiceException
     */
    private static void getEffectiveRights(
            RightBearer rightBearer, Entry target, TargetType targetType,
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException {
        
        Set<Right> presetRights;
        AllowedAttrs allowSetAttrs;
        AllowedAttrs allowGetAttrs;
        
        if (rightBearer instanceof GlobalAdmin) {
            // all preset rights on the target type
            presetRights = getAllExecutablePresetRights(targetType);
            
            // all attrs on the target type
            allowSetAttrs = AllowedAttrs.ALLOW_ALL_ATTRS();
            
            // all attrs on the target type
            allowGetAttrs = AllowedAttrs.ALLOW_ALL_ATTRS();
            
        } else {
            Grantee grantee = (Grantee)rightBearer;
            
            // get effective preset rights
            presetRights = getEffectivePresetRights(grantee, target);
            
            // get effective setAttrs rights
            allowSetAttrs = accessibleAttrs(grantee, target, AdminRight.PR_SET_ATTRS, false);
            
            // get effective getAttrs rights
            allowGetAttrs = accessibleAttrs(grantee, target, AdminRight.PR_GET_ATTRS, false);
        }
        
        // finally, populate our result 
        
        // preset rights
        Set<String> rights= new HashSet<String>();
        for (Right r : presetRights) {
            rights.add(r.getName());
        }
        result.setPresetRights(setToSortedList(rights));
        
        // setAttrs
        if (allowSetAttrs.getResult() == AllowedAttrs.Result.ALLOW_ALL) {
            result.setCanSetAllAttrs();
            if (expandSetAttrs)
                result.setCanSetAttrs(expandAttrs(target));
        } else if (allowSetAttrs.getResult() == AllowedAttrs.Result.ALLOW_SOME) {
            result.setCanSetAttrs(fillDefault(target, allowSetAttrs));
        }
        
        // getAttrs
        if (allowGetAttrs.getResult() == AllowedAttrs.Result.ALLOW_ALL) {
            result.setCanGetAllAttrs();
            if (expandGetAttrs)
                result.setCanGetAttrs(expandAttrs(target));
        } else if (allowGetAttrs.getResult() == AllowedAttrs.Result.ALLOW_SOME) {
            result.setCanGetAttrs(fillDefault(target, allowGetAttrs));
        }
    }
    
    private static List<String> setToSortedList(Set<String> set) {
        List<String> list = new ArrayList<String>(set);
        Collections.sort(list);
        return list;
    }
    
    private static SortedMap<String, RightCommand.EffectiveAttr> fillDefault(Entry target, 
            AllowedAttrs allowSetAttrs) throws ServiceException {
        return fillDefaultAndConstratint(target, allowSetAttrs.getAllowed());
    }
    
    private static SortedMap<String, RightCommand.EffectiveAttr> expandAttrs(Entry target) 
    throws ServiceException {
        return fillDefaultAndConstratint(target, TargetType.getAttrsInClass(target));
    }
    
    private static SortedMap<String, RightCommand.EffectiveAttr> fillDefaultAndConstratint(Entry target, 
            Set<String> attrs) throws ServiceException {
        SortedMap<String, RightCommand.EffectiveAttr> effAttrs = new TreeMap<String, RightCommand.EffectiveAttr>();
        
        Entry constraintEntry = AttributeConstraint.getConstraintEntry(target);
        Map<String, AttributeConstraint> constraints = (constraintEntry==null)?null:
            AttributeConstraint.getConstraint(constraintEntry);
        
        boolean hasConstraints = (constraints != null && !constraints.isEmpty());

        for (String attrName : attrs) {
            Set<String> defaultValues = null;
            
            Object defaultValue = target.getAttrDefault(attrName);
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
    
    private static Set<Right> getEffectivePresetRights(Grantee grantee, Entry target) throws ServiceException {
        
        Provisioning prov = Provisioning.getInstance();
        
        Set<String> granteeIds = grantee.getIdAndGroupIds();
        TargetType targetType = TargetType.getTargetType(target);
        
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        Integer relativity = Integer.valueOf(1);
        
        //
        // collecting phase
        //
        CollectAttrsResult car = CollectAttrsResult.SOME;
        
        // check the target entry itself
        List<ZimbraACE> acl = ACLUtil.getAllACEs(target);
        if (acl != null) {
            collectPresetRightOnTarget(acl, targetType, granteeIds, relativity, allowed, denied);
            relativity += 2;
        }
        
        //
        // if the target is a domain-ed entry, get the domain of the target.
        // It is need for checking the cross domain right.
        //
        Domain targetDomain = TargetType.getTargetDomain(prov, target);
        
        // check grants granted on entries from which the target entry can inherit from
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, target);
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
                if (grantee.isAccount())
                    skipPositiveGrants = !crossDomainOK(prov, grantee.getAccount(), grantee.getDomain(), 
                        targetDomain, (DistributionList)grantedOn);
                
                // don't check yet, collect all acls on all target groups
                if (groupACLs == null)
                    groupACLs = new GroupACLs();
                groupACLs.collectACL(grantedOn, skipPositiveGrants);
                    
            } else {
                // end of group targets, put all collected denied and allowed grants into one list, as if 
                // they are granted on the same entry, then check.  We put denied in the front, so it is 
                // consistent with ZimbraACL.getAllACEs
                if (groupACLs != null) {
                    List<ZimbraACE> aclsOnGroupTargets = groupACLs.getAllACLs();
                    if (aclsOnGroupTargets != null) {
                        collectPresetRightOnTarget(aclsOnGroupTargets, targetType, granteeIds, relativity, allowed, denied);
                        relativity += 2;
                    }
                        
                    // set groupACLs to null, we are done with group targets
                    groupACLs = null;
                }
                    
                if (acl == null)
                    continue;
                collectPresetRightOnTarget(acl, targetType, granteeIds, relativity, allowed, denied);
                relativity += 2;
            }
        }
        
        if (sLog.isDebugEnabled()) {
            StringBuilder sbAllowed = new StringBuilder();
            for (Map.Entry<Right, Integer> a : allowed.entrySet())
                sbAllowed.append("(" + a.getKey().getName() + ", " + a.getValue() + ") ");
            sLog.debug("allowed: " + sbAllowed.toString());
            
            StringBuilder sbDenied = new StringBuilder();
            for (Map.Entry<Right, Integer> a : allowed.entrySet())
                sbDenied.append("(" + a.getKey().getName() + ", " + a.getValue() + ") ");
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
    
    private static void collectPresetRightOnTarget(List<ZimbraACE> acl, TargetType targeType,
            Set<String> granteeIds, Integer relativity,
            Map<Right, Integer> allowed, Map<Right, Integer> denied) throws ServiceException {
        // as an individual: user
        short granteeFlags = (short)(GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);
        collectPresetRights(acl, targeType, granteeIds, granteeFlags, relativity, allowed,  denied);
        
        // as a group member, bump up the relativity
        granteeFlags = (short)(GranteeFlag.F_GROUP);
        collectPresetRights(acl, targeType, granteeIds, granteeFlags, relativity, allowed,  denied);
    }
    

    private static void collectPresetRights(List<ZimbraACE> acl, TargetType targetType,
            Set<String> granteeIds, short granteeFlags, 
            Integer relativity,
            Map<Right, Integer> allowed, Map<Right, Integer> denied) throws ServiceException {
        
        for (ZimbraACE ace : acl) {
            GranteeType granteeType = ace.getGranteeType();
            if (!granteeType.hasFlags(granteeFlags))
                continue;
                
            if (!granteeIds.contains(ace.getGrantee()))
                continue;
            
            Right right = ace.getRight();
                    
            if (right.isComboRight()) {
                ComboRight comboRight = (ComboRight)right;
                for (Right r : comboRight.getPresetRights()) {
                    if (r.executableOnTargetType(targetType)) {
                        if (ace.deny())
                            denied.put(r, relativity);
                        else
                            allowed.put(r, relativity);
                    }
                }
            } else if (right.isPresetRight()) {
                if (right.executableOnTargetType(targetType)) {
                    if (ace.deny())
                        denied.put(right, relativity);
                    else
                        allowed.put(right, relativity);
                }
            } 
        }
    }
    
    /*
     * get all executable preset rights on a target type
     * combo rights are expanded
     */
    private static Set<Right> getAllExecutablePresetRights(TargetType targetType) throws ServiceException {
        Map<String, AdminRight> allRights = RightManager.getInstance().getAllAdminRights();
        
        Set<Right> rights = new HashSet<Right>();
        
        for (Map.Entry<String, AdminRight> right : allRights.entrySet()) {
            Right r = right.getValue();
            if (r.isPresetRight()) {
                if (r.executableOnTargetType(targetType))
                    rights.add(r);
                
            } else if (r.isComboRight()) {
                ComboRight comboRight = (ComboRight)r;
                for (Right rt : comboRight.getPresetRights()) {
                    if (rt.executableOnTargetType(targetType))
                        rights.add(rt);
                }
                
            }
        }
        return rights;
    }
    
    //
    // util methods
    //
    
    /**
     * returns if grantee is an admin account or admin group
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
        } else
            return false;
    }

    private static boolean isSubTarget(Provisioning prov, Entry targetSup, Entry targetSub) throws ServiceException {
       
        if (targetSup instanceof Domain) {
            Domain domain = (Domain)targetSup;
            Domain targetSubInDomain = TargetType.getTargetDomain(prov, targetSub);
            if (targetSubInDomain == null)
                return false;  // not a domain-ed entry
            else {
                if (domain.getId().equals(targetSubInDomain.getId()))
                    return true;
                else {
                    // see if targetSub is in a group that is in the domain
                    AclGroups groups = null;
                    if (targetSub instanceof Account)
                        groups = prov.getAclGroups((Account)targetSub, false);
                    else if (targetSub instanceof DistributionList)
                        groups = prov.getAclGroups((DistributionList)targetSub, false);
                    else 
                        return false;
                    
                    for (String groupId : groups.groupIds()) {
                        DistributionList group = prov.getAclGroup(DistributionListBy.id, groupId);
                        Domain groupInDomain = prov.getDomain(group);
                        if (groupInDomain!= null &&  // hmm, log a warn if groupInDomain is null? throw internal err?
                            domain.getId().equals(groupInDomain.getId()))
                            return true;
                    }
                }
            }
            return false;
            
        } else if (targetSup instanceof DistributionList) {
            DistributionList dl = (DistributionList)targetSup;
            
            String subId = null;
            if (targetSub instanceof Account)  // covers cr too
                return prov.inDistributionList((Account)targetSub, dl.getId());
            else if (targetSub instanceof DistributionList)
                return prov.inDistributionList((DistributionList)targetSub, dl.getId());
            else
                return false;        
            
        } else if (targetSup instanceof GlobalGrant)
            return true;
        else {
            /*
             * is really an error, somehow our logic of finding sub-targets
             * is wrong, throw FAILURE and fix if we get here.  The granting attemp 
             * will be denied, but that's fine.
             */
            throw ServiceException.FAILURE("internal error, unexpected entry type: " + targetSup.getLabel(), null); 
        }
    }
    
    /**
     * check rights denied to grantee or admin groups the grantee belongs
     * 
     * exactly one of granteeId and granteeGroups is not null, and the other is null.
     * 
     * if granteeGroups is not null, we check for grants granted to the groups
     * 
     * @param prov
     * @param targetToGrant
     * @param rightToGrant
     * @param sgr
     * @param granteeId
     * @param granteeGroups
     * @throws ServiceException
     */
    static void checkDenied(Provisioning prov, Entry targetToGrant, Right rightToGrant,
            Set<GrantsOnTarget> grantsOnTargets, 
            String granteeId, Set<String> granteeGroups) throws ServiceException {
        
        for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
            Entry grantedOnEntry = grantsOnTarget.getTargetEntry();
            
            if (isSubTarget(prov, targetToGrant, grantedOnEntry)) {
                ZimbraACL grants = grantsOnTarget.getAcl();
                
                // check denied grants
                for (ZimbraACE ace : grants.getDeniedACEs()) {
                    if ((granteeId != null && granteeId.equals(ace.getGrantee())) ||
                        (granteeGroups != null && granteeGroups.contains(ace.getGrantee()))) {   

                        if (rightToGrant.overlaps(ace.getRight()))
                            throw ServiceException.PERM_DENIED("insuffcient right to grant. " + 
                                    "Right " + ace.getRight().getName() + 
                                    " is denied to grp/usr " + ace.getGrantee() + 
                                    " on target " + grantedOnEntry.getLabel());
                    }
                }
            }
        }
    }
    
    static void getAllGrantableTargetTypes(Right right, Set<TargetType> result) throws ServiceException {

        if (right.isPresetRight() || right.isAttrRight()) {
            result.addAll(right.getGrantableTargetTypes());
        } else if (right.isComboRight()) {
            //
            // Note: call getTargetTypesSpanByRight recursively instead of 
            // calling getGrantableTargetTypes on the combo right
            // 
            // ComboRight.getGrantableTargetTypes returns the intersect of 
            // all the rights, but here we want the union of all the target 
            // types of all the sub-rights of the combo right.
            //
            // e.g. a ComboRight that include rights on domain, dl, account, 
            //      cr can only be granted on domain or global config 
            //      (what ComboRight.getGrantableTargetTypes returns)
            //      But here we wnat target types dl, account, cr too.
            // 
            ComboRight cr = (ComboRight)right;
            for (Right r : cr.getAllRights())
                getAllGrantableTargetTypes(r, result);
        }
    }
    
    /**
     * Returns if rightToGrant is (partically) denied to grantor(or groups it belongs) 
     * on sub-targets of targetToGrant.
     * 
     * @param grantor              the "grantor" of the granting attempt
     * @param targetTypeToGrant    the target type of the granting attempt 
     * @param targetToGrant        the target of the granting attempt
     * @param rightToGrant  the right of the granting attremp
     * @throws ServiceException
     */
    static void checkPartiallyDenied(Account grantor, TargetType targetTypeToGrant, 
            Entry targetToGrant, Right rightToGrant) throws ServiceException {
        
        if (AccessControlUtil.isGlobalAdmin(grantor, true))
            return;
        
        Provisioning prov = Provisioning.getInstance();
        
        // set of sub target types
        Set<TargetType> subTargetTypes = targetTypeToGrant.subTargetTypes();
        
        // set of target types any sub-right can be granted
        Set<TargetType> subRightsGrantableOnTargetTypes = new HashSet<TargetType>();
        getAllGrantableTargetTypes(rightToGrant, subRightsGrantableOnTargetTypes);
        
        // get the interset of the two, that would be the target types to search for
        Set<TargetType> targetTypesToSearch = 
            SetUtil.intersect(subTargetTypes, subRightsGrantableOnTargetTypes);
        
        // if the intersect is empty, no need to search
        if (targetTypesToSearch.isEmpty())
            return;
        
        // get the set of zimbraId of the grantees to search for
        Grantee grantee = new Grantee(grantor);
        Set<String> granteeIdsToSearch = grantee.getIdAndGroupIds();
        
        SearchGrants searchGrants = new SearchGrants(prov, targetTypesToSearch, granteeIdsToSearch);
        Set<GrantsOnTarget> grantsOnTargets = searchGrants.doSearch().getResults();
        
        // check grants granted to the grantor
        checkDenied(prov, targetToGrant, rightToGrant, grantsOnTargets, grantor.getId(), null);
        
        // check grants granted to any groups of the grantor
        checkDenied(prov, targetToGrant, rightToGrant, grantsOnTargets, null, granteeIdsToSearch);
        
        // all is well, or else PERM_DENIED would've been thrown in one of the checkDenied calls
        // yes, you can grant the rightToGrant on targetToGrant.
    }
    
    private static void computeRightsInheritedFromGlobalGrant(Provisioning prov, Grantee grantee,
            boolean expandSetAttrs, boolean expandGetAttrs, AllEffectiveRights aer) throws ServiceException {
        
        for (TargetType tt : TargetType.values()) {
            Entry targetEntry;
            if (tt == TargetType.global)
                targetEntry = prov.getGlobalGrant();
            else if (tt == TargetType.config)
                targetEntry = prov.getConfig();
            else
                targetEntry = PseudoTarget.createPseudoTarget(prov, tt, null, null, true, null, null);
            
            EffectiveRights er = new EffectiveRights(
                    tt.getCode(), TargetType.getId(targetEntry), targetEntry.getLabel(), 
                    grantee.getId(), grantee.getName());
            
            RightChecker.getEffectiveRights(grantee, targetEntry, expandSetAttrs, expandGetAttrs, er);
            
            aer.setAll(tt, er);
        }
    }
    
    private static void computeRightsInheritedFromDomain(
            Provisioning prov, Grantee grantee, 
            TargetType targetType, Domain grantedOnDomain,
            boolean expandSetAttrs, boolean expandGetAttrs, AllEffectiveRights aer) throws ServiceException {
        
        String domainId = TargetType.getId(grantedOnDomain);
        String domainName = grantedOnDomain.getLabel();
        
        // create a pseudo object(account, cr, dl) in this domain
        Entry pseudoTarget = PseudoTarget.createPseudoTarget(prov, targetType, DomainBy.id, grantedOnDomain.getId(), false, null, null);
        
        // get effective rights on the pseudo target
        EffectiveRights er = new EffectiveRights(
                targetType.getCode(), TargetType.getId(pseudoTarget), pseudoTarget.getLabel(), 
                grantee.getId(), grantee.getName());
        RightChecker.getEffectiveRights(grantee, pseudoTarget, expandSetAttrs, expandGetAttrs, er);
        
        // add to the domianed scope in AllEffectiveRights
        aer.addDomainEntry(targetType, domainName, er);
    }
    
    private static void computeRightsInheritedFromDomain(Provisioning prov, Grantee grantee, Domain grantedOnDomain,
            boolean expandSetAttrs, boolean expandGetAttrs, AllEffectiveRights aer) throws ServiceException {
        
        computeRightsInheritedFromDomain(
                prov, grantee, TargetType.account, grantedOnDomain, expandSetAttrs, expandGetAttrs, aer);
        
        computeRightsInheritedFromDomain(
                prov, grantee, TargetType.calresource, grantedOnDomain, expandSetAttrs, expandGetAttrs, aer);
        
        computeRightsInheritedFromDomain(
                prov, grantee, TargetType.dl, grantedOnDomain, expandSetAttrs, expandGetAttrs, aer);
    }
    
    /*
     * We do not have a group scope in AllEffectiveRights. 
     * 
     * Reasons:
     *     1. If we return somethings like: 
     *           have effective rights X, Y, Z on members in groups A, B, C
     *           have effective rights P, Q, R on members in groups M, N
     *        then client will have to figure out if an account/cr/dl are in which groups.   
     *    
     *     2. If a group-ed(i.e. account/cr/dl) are in multiple groups, that's even messier  
     *        for the client (admin console).
     *    
     * Instead, we classify group-ed entries in groups with grants into "shapes", and 
     * represent them in a RightAggregation, like:
     *       - has effective rights X, Y on accounts user1, user5, user8
     *       - has effective rights X on accounts user2, user3, user4   
     *       - has effective rights on calendar resources cr1, cr88
     *       - has effective rights on distribution lists dl38, dl99     
     */
    private static void computeRightsOnGroupShape(Provisioning prov, Grantee grantee, 
            TargetType targetType, Set<GroupShape> groupShapes,
            boolean expandSetAttrs, boolean expandGetAttrs, AllEffectiveRights aer,
            Set<String> entryIdsHasGrants) throws ServiceException {
        
        for (GroupShape shape : groupShapes) {
            // get any one member in the shape and use that as a pilot target to get 
            // an EffectiveRights.  Note, the pilot target entry itself cannot have 
            // any grants or else it will not result in the same EffectiveRights for 
            // the group shape.  Entries have grants will be recorded in stage 3; and 
            // will overwrite the entry rights recorded here.
            //
            // if for some reason the member cannot be found (e.g. account is deleted 
            // but somehow not removed from a group, l=not likely though), just skip 
            // to use another one in the shape.
            //
            //
            
            Entry target = null;
            EffectiveRights er = null;
            for (String memberName : shape.getMembers()) {
                target = TargetType.lookupTarget(prov, targetType, TargetBy.name, memberName, false);
                if (target != null) {
                    String targetId = TargetType.getId(target);
                    if (!entryIdsHasGrants.contains(targetId)) {
                        er = new EffectiveRights(
                                targetType.getCode(), targetId, target.getLabel(), grantee.getId(), grantee.getName());
                        RightChecker.getEffectiveRights(grantee, target, expandSetAttrs, expandGetAttrs, er);
                        break;
                    } // else the member itself has grants, skip it for being used as a pilot target entry
                }
            }
            
            if (er != null)
                aer.addAggregation(targetType, shape.getMembers(), er);
        }
    }
    
    private static void computeRightsOnEntry(Provisioning prov, Grantee grantee, 
            TargetType grantedOnTargetType, Entry grantedOnEntry,
            boolean expandSetAttrs, boolean expandGetAttrs, AllEffectiveRights aer) throws ServiceException {
        String targetId = TargetType.getId(grantedOnEntry);
        String targetName = grantedOnEntry.getLabel();
        
        EffectiveRights er = new EffectiveRights(
                grantedOnTargetType.getCode(), targetId, targetName, grantee.getId(), grantee.getName());
        
        RightChecker.getEffectiveRights(grantee, grantedOnEntry, expandSetAttrs, expandGetAttrs, er);
        aer.addEntry(grantedOnTargetType, targetName, er);
    }
    
    private static class Visitor implements LdapUtil.SearchLdapVisitor {
        private LdapDIT mLdapDIT;
        
        // set of names
        private Set<String> mNames = new HashSet<String>();

        Visitor(LdapDIT ldapDIT) {
            mLdapDIT = ldapDIT;
        }
        
        public void visit(String dn, Map<String, Object> attrs, Attributes ldapAttrs) {
            try {
                String name = mLdapDIT.dnToEmail(dn, ldapAttrs);
                mNames.add(name);
            } catch (ServiceException e) {
                ZimbraLog.acl.warn("canno get name from dn [" + dn + "]", e);
            }
        }
        
        private Set<String> getResult() {
            return mNames;
        }
    }
    
    private static Set<String> getAllGroups(Provisioning prov) throws ServiceException {
        LdapDIT ldapDIT = ((LdapProvisioning)prov).getDIT();
        String base = ldapDIT.mailBranchBaseDN();
        String query = LdapFilter.allDistributionLists();
        
        // hack, see LDAPDIT.dnToEmail, for now we get naming rdn for both default and possible custom DIT
        String[] returnAttrs = new String[] {Provisioning.A_cn, Provisioning.A_uid}; 
        
        Visitor visitor = new Visitor(ldapDIT);
        LdapUtil.searchLdapOnMaster(base, query, returnAttrs, visitor);
        return visitor.getResult();
    }
    
    private static Set<String> getAllCalendarResources(Provisioning prov) throws ServiceException {
        LdapDIT ldapDIT = ((LdapProvisioning)prov).getDIT();
        String base = ldapDIT.mailBranchBaseDN();
        String query = LdapFilter.allCalendarResources();
        
        // hack, see LDAPDIT.dnToEmail, for now we get naming rdn for both default and possible custom DIT
        String[] returnAttrs = new String[] {Provisioning.A_cn, Provisioning.A_uid}; 
        
        Visitor visitor = new Visitor(ldapDIT);
        LdapUtil.searchLdapOnMaster(base, query, returnAttrs, visitor);
        return visitor.getResult();
    }
    
    private static void getAllGroupMembers(
            Provisioning prov,
            DistributionList group,
            Set<String> allGroups, Set<String> allCalendarResources, 
            AllGroupMembers result) 
    throws ServiceException {
        
        Set<String> members = group.getAllMembersSet();
        Set<String> accountMembers = new HashSet<String>(members);  // make a copy, assumcing all members are account
                
        // expand if a member is a group
        for (String member : members) {
            // if member is a group, remove it from the result
            // and expand the group if it has not been expanded yet
            if (allGroups.contains(member)) {
                // remove it from the accountMembers
                accountMembers.remove(member);
                
                // haven't expaned this group yet
                if (!result.getMembers(TargetType.dl).contains(member)) {
                    result.getMembers(TargetType.dl).add(member);
                    DistributionList grp = prov.get(DistributionListBy.name, member);
                    if (grp != null) {
                        getAllGroupMembers(prov, grp, allGroups, allCalendarResources, result);
                    }
                }
            } else if (allCalendarResources.contains(member)) {
                accountMembers.remove(member);
                result.getMembers(TargetType.calresource).add(member);
            }
        }
        result.getMembers(TargetType.account).addAll(accountMembers);
    }
    
    private static class AllGroupMembers {
        String mGroupName; // name of the group
        
        AllGroupMembers(String groupName) {
            mGroupName = groupName;
        }
        
        Set<String> mAccounts = new HashSet<String>();
        Set<String> mCalendarResources = new HashSet<String>();
        Set<String> mDistributionLists = new HashSet<String>();
        
        String getGroupName() {
            return mGroupName;
        }
        
        Set<String> getMembers(TargetType targetType) throws ServiceException {
            switch (targetType) {
            case account:
                return mAccounts;
            case calresource:
                return mCalendarResources;
            case dl:    
                return mDistributionLists;
            }
            throw ServiceException.FAILURE("internal error", null);
        }
    }
    
    /*
     * represents a "shape" of groups.  Entries in the same "shape" belong to 
     * all groups in the shape.
     * 
     * e.g. if we are calculating shapes for group A, B, C, D, the possible shapes are:
     *      A, B, C, D, AB, AC, AD, BC, BD, CD, ABC, ABD, ACD, BCD, ABCD
     *      
     *      If groups are shaped in the order of A, B, C, D, the resulting shapes(at most) would look like:
     *      (numbers are the order a shape is spawn.)
     *      
     *      when members in group A is being shaped: A(1)
     *      when members in group B is being shaped: A(1)                      AB(2)                           B(3)
     *      when members in group C is being shaped: A(1)        AC(4)         AB(2)          ABC(5)           B(3)         BC(6)          C(7)
     *      when members in group D is being shaped: A(1) AD(8)  AC(4) ACD(9)  AB(2) ABD(10)  ABC(5) ABCD(11)  B(3) BD(12)  BC(6) BCD(13)  C(7) CD(14)  D(15)
     *      
     */
    private static class GroupShape {
        Set<String> mGroups = new HashSet<String>();   // groups all entries in mMembers are a member of
        Set<String> mMembers = new HashSet<String>();  // members belongs to all entries of mGroups
        
        private void addGroups(Set<String> groups) {
            mGroups.addAll(groups);
        }
        
        private void addGroup(String group) {
            mGroups.add(group);
        }
        
        private Set<String> getGroups() {
            return mGroups;
        }
        
        private void addMembers(Set<String> members) {
            mMembers.addAll(members);
        }
        
        private void addMember(String member) {
            mMembers.add(member);
        }
        
        private Set<String> getMembers() {
            return mMembers;
        }
        
        private boolean removeMemberIfPresent(String member) {
            if (mMembers.contains(member)) {
                mMembers.remove(member);
                return true;
            } else
                return false;
        }
        
        private boolean hasMembers() {
            return !mMembers.isEmpty();
        }
        
        static void shapeMembers(TargetType targetType, Set<GroupShape> shapes, AllGroupMembers group) throws ServiceException {

            // Stage newly spawn GroupShape's in a seperate Set so 
            // we don't add entries to shapes while iterating through it.
            // Add entries in the new Set back into shapes after iterating 
            // through it for this group. 
            // 
            Set<GroupShape> newShapes = new HashSet<GroupShape>();
                
            // holds members in the group being shaped that 
            // do not belong to any shapes in the current discovered shapes
            GroupShape newShape = new GroupShape();
            newShape.addGroup(group.getGroupName());
            newShape.addMembers(group.getMembers(targetType));
               
            for (GroupShape shape : shapes) {
                // holds intersect of members in this shape and 
                // in the group being shaped.
                GroupShape intersectShape = new GroupShape();
                    
                for (String member : group.getMembers(targetType)) {
                    if (shape.removeMemberIfPresent(member)) {
                        intersectShape.addMember(member);
                        newShape.removeMemberIfPresent(member);
                    }
                }
                    
                if (intersectShape.hasMembers()) {
                    // found a new shape
                        
                    // describe it
                    intersectShape.addGroups(shape.getGroups());
                    intersectShape.addGroup(group.getGroupName());
                        
                    // keep it 
                    newShapes.add(intersectShape);
                }
                // no intersect, toss the intersectShape
            }
                
            // add newly spawn GroupShape's in shapes
            shapes.addAll(newShapes);
                
            // add the new shape that contain members that are not in 
            // any other shapes 
            if (newShape.hasMembers())
                shapes.add(newShape);
                
        }
    }
    
    private static AllGroupMembers getAllGroupMembers(Provisioning prov, DistributionList group) throws ServiceException {
        /*
         * get all groups and crs in the front and use the Set.contains method
         * to test if a member name is a cr/group
         * 
         * much more efficient than doing Provisioning.get(...), which has a lot
         * more overhead and may cause extra LDAP searches if the entry is not in cache. 
         */
        Set<String> allGroups = getAllGroups(prov);
        Set<String> allCalendarResources = getAllCalendarResources(prov);
        
        AllGroupMembers allMembers = new AllGroupMembers(group.getName());
        getAllGroupMembers(prov, group, allGroups, allCalendarResources, allMembers);
        
        return allMembers;
    }
    
 
    /**
     * 
     * @param grantee an Account or a group
     * @throws ServiceException
     */
    static void getAllEffectiveRights(RightBearer rightBearer, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            AllEffectiveRights aer) throws ServiceException {
        
        if (rightBearer instanceof GlobalAdmin) {
            for (TargetType tt : TargetType.values()) {
                EffectiveRights er = new EffectiveRights(
                        tt.getCode(), null, null, 
                        rightBearer.getId(), rightBearer.getName());
                
                RightChecker.getEffectiveRights(rightBearer, null, tt, expandSetAttrs, expandGetAttrs, er);
                aer.setAll(tt, er);
            }
            return;
        }
        
        Grantee grantee = (Grantee)rightBearer;
        
        Provisioning prov = Provisioning.getInstance();
        
        // we want all target types
        Set<TargetType> targetTypesToSearch = new HashSet<TargetType>(Arrays.asList(TargetType.values()));

        // get the set of zimbraId of the grantees to search for
        Set<String> granteeIdsToSearch = grantee.getIdAndGroupIds();
        
        SearchGrants searchGrants = new SearchGrants(prov, targetTypesToSearch, granteeIdsToSearch);
        Set<GrantsOnTarget> grantsOnTargets = searchGrants.doSearch().getResults();
        
        // staging for group grants
        Set<DistributionList> groupsWithGrants = new HashSet<DistributionList>();
        
        //
        // Stage1
        //
        // process grants granted on inheritable entries:
        //     globalgrant - populate the "all" field in AllEffectiveRights
        //     domains     - populate the "all entries in this domain" field in AllEffectiveRights
        //     groups      - remember the groups and process them in stage 2.
        //
        for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
            Entry grantedOnEntry = grantsOnTarget.getTargetEntry();
            ZimbraACL acl = grantsOnTarget.getAcl();
            TargetType targetType = TargetType.getTargetType(grantedOnEntry);
            
            if (targetType == TargetType.global)
                computeRightsInheritedFromGlobalGrant(prov, grantee, expandSetAttrs, expandGetAttrs, aer);
            else if (targetType == TargetType.domain)
                computeRightsInheritedFromDomain(prov, grantee, (Domain)grantedOnEntry, expandSetAttrs, expandGetAttrs, aer);
            else if (targetType == TargetType.dl)
                groupsWithGrants.add((DistributionList)grantedOnEntry);
        }
        
        //
        // Stage 2
        //
        // process group grants
        //
        // first, shape all members in all groups with grants into "shapes"
        //
        Set<GroupShape> accountShapes = new HashSet<GroupShape>();
        Set<GroupShape> calendarResourceShapes = new HashSet<GroupShape>();
        Set<GroupShape> distributionListShapes = new HashSet<GroupShape>();
        for (DistributionList group : groupsWithGrants) {
            // group is an AclGroup, which contains only upward membership, not downward membership.
            // re-get the DistributionList object, which has the downward membership.
            DistributionList dl = prov.get(DistributionListBy.id, group.getId());
            AllGroupMembers allMembers = getAllGroupMembers(prov, dl);
            GroupShape.shapeMembers(TargetType.account, accountShapes, allMembers);
            GroupShape.shapeMembers(TargetType.calresource, calendarResourceShapes, allMembers);
            GroupShape.shapeMembers(TargetType.dl, distributionListShapes, allMembers);
        }
        
        //
        // then, for each group shape, generate a RightAggregation and record in the AllEffectiveRights
        // if any of the entries in a shape also have grants as an individual, the effective rigths for 
        // those entries will be replaced in stage 3.
        //
        Set entryIdsHasGrants = new HashSet<String>();
        for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
            Entry grantedOnEntry = grantsOnTarget.getTargetEntry();
            if (grantedOnEntry instanceof NamedEntry) {
                entryIdsHasGrants.add(((NamedEntry)grantedOnEntry).getId());
            }
        }
        
        computeRightsOnGroupShape(prov, grantee, TargetType.account, accountShapes, expandSetAttrs, expandGetAttrs, aer, entryIdsHasGrants);
        computeRightsOnGroupShape(prov, grantee, TargetType.calresource, calendarResourceShapes, expandSetAttrs, expandGetAttrs, aer, entryIdsHasGrants);
        computeRightsOnGroupShape(prov, grantee, TargetType.dl, distributionListShapes, expandSetAttrs, expandGetAttrs, aer, entryIdsHasGrants);
        
        //
        // Stage 3
        //
        // process grants on the granted entry
        //
        for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
            Entry grantedOnEntry = grantsOnTarget.getTargetEntry();
            ZimbraACL acl = grantsOnTarget.getAcl();
            TargetType targetType = TargetType.getTargetType(grantedOnEntry);
            
            if (targetType != TargetType.global)
                computeRightsOnEntry(prov, grantee, targetType, grantedOnEntry, expandSetAttrs, expandGetAttrs, aer);
        }
    }
    
    
    /*
     * ==========
     * unit tests
     * ==========
     */
    private static void groupTest() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        DistributionList dl = prov.get(DistributionListBy.name, "group1@phoebe.mac");
        AllGroupMembers allMembers = getAllGroupMembers(prov, dl);
        
        System.out.println("\naccounts");
        for (String member : allMembers.getMembers(TargetType.account))
            System.out.println("  " + member);
        
        System.out.println("\ncalendar resources");
        for (String member : allMembers.getMembers(TargetType.calresource))
            System.out.println("  " + member);
        
        System.out.println("\ngroups");
        for (String member : allMembers.getMembers(TargetType.dl))
            System.out.println("  " + member);
    }
    
    private static void setupShapeTest() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        // create test
        String domainName = "test.com";
        Domain domain = prov.createDomain(domainName, new HashMap<String, Object>());
        
        DistributionList groupA = prov.createDistributionList("groupA@"+domainName, new HashMap<String, Object>());
        DistributionList groupB = prov.createDistributionList("groupB@"+domainName, new HashMap<String, Object>());
        DistributionList groupC = prov.createDistributionList("groupC@"+domainName, new HashMap<String, Object>());
        DistributionList groupD = prov.createDistributionList("groupD@"+domainName, new HashMap<String, Object>());
        
        String pw = "test123";
        Account A = prov.createAccount("A@"+domainName, pw, null);
        Account B = prov.createAccount("B@"+domainName, pw, null);
        Account C = prov.createAccount("C@"+domainName, pw, null);
        Account D = prov.createAccount("D@"+domainName, pw, null);
        Account AB = prov.createAccount("AB@"+domainName, pw, null);
        Account AC = prov.createAccount("AC@"+domainName, pw, null);
        Account AD = prov.createAccount("AD@"+domainName, pw, null);
        Account BC = prov.createAccount("BC@"+domainName, pw, null);
        Account BD = prov.createAccount("BD@"+domainName, pw, null);
        Account CD = prov.createAccount("CD@"+domainName, pw, null);
        Account ABC = prov.createAccount("ABC@"+domainName, pw, null);
        Account ABD = prov.createAccount("ABD@"+domainName, pw, null);
        Account ACD = prov.createAccount("ACD@"+domainName, pw, null);
        Account BCD = prov.createAccount("BCD@"+domainName, pw, null);
        Account ABCD = prov.createAccount("ABCD@"+domainName, pw, null);
        
        groupA.addMembers(new String[]{A.getName(), 
                                       AB.getName(), AC.getName(), AD.getName(),
                                       ABC.getName(), ABD.getName(), ACD.getName(),
                                       ABCD.getName()});
        
        groupB.addMembers(new String[]{B.getName(), 
                                       AB.getName(), BC.getName(), BD.getName(),
                                       ABC.getName(), ABD.getName(), BCD.getName(),
                                       ABCD.getName()});
        
        groupC.addMembers(new String[]{C.getName(), 
                                       AC.getName(), BC.getName(), CD.getName(),
                                       ABC.getName(), ACD.getName(), BCD.getName(),
                                       ABCD.getName()});
        
        groupD.addMembers(new String[]{D.getName(), 
                                       AD.getName(), BD.getName(), CD.getName(),
                                       ABD.getName(), ACD.getName(), BCD.getName(),
                                       ABCD.getName()});
    }
    
    private static void shapeTest() throws ServiceException {
        setupShapeTest();
        
        Provisioning prov = Provisioning.getInstance();
        
        // create test
        Set<DistributionList> groupsWithGrants = new HashSet<DistributionList>();
        String domainName = "test.com";
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupA@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupB@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupC@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupD@"+domainName));
        
        Set<GroupShape> accountShapes = new HashSet<GroupShape>();
        Set<GroupShape> calendarResourceShapes = new HashSet<GroupShape>();
        Set<GroupShape> distributionListShapes = new HashSet<GroupShape>();
        
        for (DistributionList group : groupsWithGrants) {
            // group is an AclGroup, which contains only upward membership, not downward membership.
            // re-get the DistributionList object, which has the downward membership.
            DistributionList dl = prov.get(DistributionListBy.id, group.getId());
            AllGroupMembers allMembers = getAllGroupMembers(prov, dl);
            GroupShape.shapeMembers(TargetType.account, accountShapes, allMembers);
            GroupShape.shapeMembers(TargetType.calresource, calendarResourceShapes, allMembers);
            GroupShape.shapeMembers(TargetType.dl, distributionListShapes, allMembers);
        }
        
        int count = 1;
        for (GroupShape shape : accountShapes) {
            System.out.println("\n" + count++);
            for (String group : shape.getGroups())
                System.out.println("group " + group);
            for (String member : shape.getMembers())
                System.out.println("    " + member);
        }
    }
    
    public static void main(String[] args) throws ServiceException {
        shapeTest();
    }
}
