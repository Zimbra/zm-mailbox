package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccessManager.AllowedAttrs;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCardinality;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.MemberOf;
import com.zimbra.cs.account.Server;

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
        private Set<ZimbraACE> aclsOnGroupTargetsAllowed = null;
        private Set<ZimbraACE> aclsOnGroupTargetsDenied = null;
        
        void collectACL(Entry grantedOn) throws ServiceException {
            if (aclsOnGroupTargetsAllowed == null)
                aclsOnGroupTargetsAllowed = new HashSet<ZimbraACE>();
            if (aclsOnGroupTargetsDenied == null)
                aclsOnGroupTargetsDenied = new HashSet<ZimbraACE>();
            
            Set<ZimbraACE> allowed = RightUtil.getAllowedACEs(grantedOn);
            Set<ZimbraACE> denied = RightUtil.getDeniedACEs(grantedOn);
            
            if (allowed != null)
                aclsOnGroupTargetsAllowed.addAll(allowed);
            
            if (denied != null)
                aclsOnGroupTargetsDenied.addAll(denied);
        }
        
        /*
         * end of group targets, put all denied and allowed grants into one list, as if 
         * they are granted on the same entry.   We put denied in the front, so it is 
         *  consistent with ZimbraACL.getAllACEs
         */
        List<ZimbraACE> getAllACLs() {
            if ((aclsOnGroupTargetsAllowed != null && !aclsOnGroupTargetsAllowed.isEmpty()) || 
                (aclsOnGroupTargetsDenied != null && !aclsOnGroupTargetsDenied.isEmpty())) {
                    
                List<ZimbraACE> aclsOnGroupTargets = new ArrayList<ZimbraACE>();
                aclsOnGroupTargets.addAll(aclsOnGroupTargetsDenied);
                aclsOnGroupTargets.addAll(aclsOnGroupTargetsAllowed);
                    
                return aclsOnGroupTargets;
            } else
                return null;
        }
    }

    
    /**
     * Check if grantee is allowed for rightNeeded on target entry.
     * 
     * @param grantee
     * @param target
     * @param rightNeeded
     * @param via if not null, will be populated with the grant info via which the result was decided.
     * @return Boolean.TRUE if allowed, Boolean.FALSE if denied, null if there is no grant applicable to the rightNeeded.
     * @throws ServiceException
     */
    static Boolean canDo(Account grantee, Entry target, Right rightNeeded, ViaGrant via) throws ServiceException {
        if (!rightNeeded.isPresetRight())
            throw ServiceException.INVALID_REQUEST("RightChecker.canDo can only check preset right, right " + 
                                                   rightNeeded.getName() + " is a " + rightNeeded.getRightType() + " right",  null);
        
        Boolean result = null;
        SeenRight seenRight = new SeenRight();
        
        Provisioning prov = Provisioning.getInstance();
        AclGroups granteeGroups = prov.getAclGroups(grantee);
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
        List<ZimbraACE> acl = RightUtil.getAllACEs(target);
        if (acl != null) {
            result = checkTargetPresetRight(acl, targetType, grantee, granteeGroups, rightNeeded, via, seenRight);
            if (result != null) 
                return result;
        }
        
        // check grants granted on entries from which the target entry can inherit from
        TargetIterator iter = TargetIterator.getTargetIeterator(Provisioning.getInstance(), target);
        Entry grantedOn;
        
        GroupACLs groupACLs = null;
        
        while ((grantedOn = iter.next()) != null) {
            acl = RightUtil.getAllACEs(grantedOn);
            
            if (grantedOn instanceof DistributionList) {
                if (acl == null)
                    continue;
                
                // don't check yet, collect all acls on all target groups
                if (groupACLs == null)
                    groupACLs = new GroupACLs();
                groupACLs.collectACL(grantedOn);
                
            } else {
                // end of group targets, put all collected denied and allowed grants into one list, as if 
                // they are granted on the same entry, then check.  We put denied in the front, so it is 
                // consistent with ZimbraACL.getAllACEs
                if (groupACLs != null) {
                    List<ZimbraACE> aclsOnGroupTargets = groupACLs.getAllACLs();
                    if (aclsOnGroupTargets != null)
                        result = checkTargetPresetRight(aclsOnGroupTargets, targetType, grantee, granteeGroups, rightNeeded, via, seenRight);
                    if (result != null) 
                        return result;
                    
                    // set groupACLs to null, we are done with group targets
                    groupACLs = null;
                }
                
                // didn't encounter any group grantedOn, or none of them matches, just check this grantedOn entry
                if (acl == null)
                    continue;
                result = checkTargetPresetRight(acl, targetType, grantee, granteeGroups, rightNeeded, via, seenRight);
                if (result != null) 
                    return result;
            }
        }
        
        if (seenRight.seenRight())
            return Boolean.FALSE;
        else
            return null;
    }
    
    private static Boolean checkTargetPresetRight(List<ZimbraACE> acl, TargetType targetType, Account grantee, AclGroups granteeGroups, Right rightNeeded, ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        
        // if the right is user right, checking for individual match will
        // only check for user grantees, if there are any guest or key grantees
        // (there should *not* be any), they are ignored.
        short adminFlag = (rightNeeded.isUserRight()? 0 : GranteeFlag.F_ADMIN);
        
        // as an individual: user, guest, key
        result = checkPresetRight(acl, targetType, grantee, rightNeeded, (short)(GranteeFlag.F_INDIVIDUAL | adminFlag), via, seenRight);
        if (result != null) 
            return result;
        
        // as a group member
        result = checkGroupPresetRight(acl, targetType, granteeGroups, grantee, rightNeeded, (short)(GranteeFlag.F_GROUP), via, seenRight);
        if (result != null) 
            return result;
       
        if (rightNeeded.isUserRight()) {
            result = checkPresetRight(acl, targetType, grantee, rightNeeded, (short)(GranteeFlag.F_AUTHUSER), via, seenRight);
            if (result != null) 
                return result;
            
            result = checkPresetRight(acl, targetType, grantee, rightNeeded, (short)(GranteeFlag.F_PUBLIC), via, seenRight);
            if (result != null) 
                return result;
        }
        
        return null;
    }
    
    /*
     * checks 
     *     - if the grant matches required granteeFlags
     *     - if the granted right is applicable to the entry on which it is granted
     *     - if the granted right matches the requested right
     */
    private static boolean matchesPresetRight(ZimbraACE ace, TargetType targetType, Right right, short granteeFlags) throws ServiceException {
        GranteeType granteeType = ace.getGranteeType();
        if (!granteeType.hasFlags(granteeFlags))
            return false;
            
        if (!right.applicableOnTargetType(targetType))
            return false;
        
            
        Right rightGranted = ace.getRight();
        if ((rightGranted.isPresetRight() && rightGranted == right) ||
             rightGranted.isComboRight() && ((ComboRight)rightGranted).containsPresetRight(right))
            return true;
        
        return false;
    }
    
    private static Boolean checkPresetRight(List<ZimbraACE> acl, TargetType targetType, Account grantee, Right right, short granteeFlags, ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : acl) {
            if (!matchesPresetRight(ace, targetType, right, granteeFlags))
                continue;
            
            // if we get here, the right matched, mark it in seenRight.  This is so callsite default will not be honored.
            seenRight.setSeenRight();
                
            if (ace.matchesGrantee(grantee))
                return gotResult(ace, grantee, right, via);
        }
       
        return result;
    }
    
    private static Boolean checkGroupPresetRight(List<ZimbraACE> acl, TargetType targetType, AclGroups granteeGroups, Account grantee, Right right, short granteeFlags, ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : acl) {
            if (!matchesPresetRight(ace, targetType, right, granteeFlags))
                continue;
            
            // if we get here, the right matched, mark it in seenRight.  This is so callsite default will not be honored.
            seenRight.setSeenRight();
            
            if (granteeGroups.groupIds().contains(ace.getGrantee()))   
                return gotResult(ace, grantee, right, via);
        }
        return result;
    }
    
    private static Boolean gotResult(ZimbraACE ace, Account grantee, Right right, ViaGrant via) throws ServiceException {
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
    static AllowedAttrs canAccessAttrs(Account grantee, Entry target, AdminRight rightNeeded, Map<String, Object> attrs) throws ServiceException {
        if (rightNeeded != AdminRight.R_PSEUDO_GET_ATTRS && rightNeeded != AdminRight.R_PSEUDO_SET_ATTRS)
            throw ServiceException.FAILURE("internal error", null); 
        
        Set<String> granteeIds = setupGranteeIds(grantee);
        TargetType targetType = TargetType.getTargetType(target);
        
        Map<String, Integer> allowSome = new HashMap<String, Integer>();
        Map<String, Integer> denySome = new HashMap<String, Integer>();
        Integer relativity = Integer.valueOf(1);
        
        //
        // collecting phase
        //
        CollectAttrsResult car = CollectAttrsResult.SOME;
        
        // check the target entry itself
        List<ZimbraACE> acl = RightUtil.getAllACEs(target);
        if (acl != null) {
            car = checkTargetAttrsRight(acl, targetType, granteeIds, rightNeeded, relativity, allowSome, denySome);
            relativity += 2;
        }
        
        if (!car.isAll()) {
            // check grants granted on entries from which the target entry can inherit from
            TargetIterator iter = TargetIterator.getTargetIeterator(Provisioning.getInstance(), target);
            Entry grantedOn;
            
            GroupACLs groupACLs = null;
            
            while ((grantedOn = iter.next()) != null && (!car.isAll())) {
                acl = RightUtil.getAllACEs(grantedOn);
                
                if (grantedOn instanceof DistributionList) {
                    if (acl == null)
                        continue;
                    
                    // don't check yet, collect all acls on all target groups
                    if (groupACLs == null)
                        groupACLs = new GroupACLs();
                    groupACLs.collectACL(grantedOn);
                    
                } else {
                    // end of group targets, put all collected denied and allowed grants into one list, as if 
                    // they are granted on the same entry, then check.  We put denied in the front, so it is 
                    // consistent with ZimbraACL.getAllACEs
                    if (groupACLs != null) {
                        List<ZimbraACE> aclsOnGroupTargets = groupACLs.getAllACLs();
                        if (aclsOnGroupTargets != null) {
                            car = checkTargetAttrsRight(aclsOnGroupTargets, targetType, granteeIds, rightNeeded, relativity, allowSome, denySome);
                            relativity += 2;
                            if (car.isAll()) 
                                break;
                            // else continue with the next target 
                        }
                        
                        // set groupACLs to null, we are done with group targets
                        groupACLs = null;
                    }
                    
                    if (acl == null)
                        continue;
                    car = checkTargetAttrsRight(acl, targetType, granteeIds, rightNeeded, relativity, allowSome, denySome);
                    relativity += 2;
                }
            }
        }
        
        // todo, log result from the collectin phase
        
        //
        // computing phase
        //
        
        AllowedAttrs result;
        
        AttributeClass klass = TargetType.getAttributeClass(target);
        
        if (car== CollectAttrsResult.ALLOW_ALL)
            result = processAllowAll(allowSome, denySome, klass);
        else if (car== CollectAttrsResult.DENY_ALL)
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
            result = AccessManager.ALLOW_SOME_ATTRS(allowSome.keySet());
        }
        
        // computeConDo(result, target, rightNeeded, attrs);
        return result;

    }
    
    private static AllowedAttrs processDenyAll(Map<String, Integer> allowSome, Map<String, Integer> denySome, AttributeClass klass) throws ServiceException {
        if (allowSome.isEmpty())
            return AccessManager.DENY_ALL_ATTRS();
        else {
            Set<String> allowed = allowSome.keySet();
            return AccessManager.ALLOW_SOME_ATTRS(allowed);
        }
    }

    private static AllowedAttrs processAllowAll(Map<String, Integer> allowSome, Map<String, Integer> denySome, AttributeClass klass) throws ServiceException {
        
        if (denySome.isEmpty()) {
            return AccessManager.ALLOW_ALL_ATTRS();
        } else {
            // get all attrs that can appear on the target entry
            Set<String> allowed = new HashSet<String>();
            allowed.addAll(AttributeManager.getInstance().getAttrsInClass(klass));
            
            // remove denied from all
            for (String d : denySome.keySet())
                allowed.remove(d);
            return AccessManager.ALLOW_SOME_ATTRS(allowed);
        }
    }
    
    private static CollectAttrsResult checkTargetAttrsRight(List<ZimbraACE> acl, TargetType targetType,
                                                            Set<String> granteeIds, Right rightNeeded,
                                                            Integer relativity,
                                                            Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
        CollectAttrsResult result = null;
        
        // as an individual: user
        short granteeFlags = (short)(GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);
        result = checkAttrsRight(acl, targetType, granteeIds, rightNeeded.getRightType(), granteeFlags, relativity, allowSome, denySome);
        if (result.isAll()) 
            return result;
        
        // as a group member, bump up the relativity
        granteeFlags = (short)(GranteeFlag.F_GROUP);
        result = checkAttrsRight(acl, targetType, granteeIds, rightNeeded.getRightType(), granteeFlags, relativity+1, allowSome, denySome);

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


    private static CollectAttrsResult processAttrsGrant(ZimbraACE ace, TargetType targetType, AttrRight attrRight, Right.RightType rightTypeNeeded, Integer relativity,
                                                        Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
        
        // note: do NOT call ace.getRight in this method, ace is passed in for deny() and getTargetType() calls only.  
        //
        // Because if the granted right is a combo right, ace.getRight will 
        // return the combo right, not the attr rights in the combo right.  If the grant contains a combo right, attr rights 
        // of the combo right will be passed in as the attrRight arg, or each attr right in the combo right.
        // ace is passed in for deny() and getTargetType() calls only.   
        
        if (!attrRight.applicableToRightType(rightTypeNeeded))
            return null;
        
        if (!attrRight.applicableOnTargetType(targetType))
            return null;
        
        if (attrRight.allAttrs()) {
            // all attrs, return if we hit a grant with all attrs
            if (ace.deny()) {
                /*
                 * if the grant is setAttrs and the needed right is getAttrs:
                 *    - negative setAttrs grant does *not* negate any attrs explicitly allowed for getAttrs grants
                 *    - positive setAttrs grant *does* automatically give getAttrs right
                 */
                if (attrRight.getRightType() == rightTypeNeeded)
                    return CollectAttrsResult.DENY_ALL;  // right type granted === right type needed
                // else just ignore the grant
            } else
                return CollectAttrsResult.ALLOW_ALL;
        } else {
            // some attrs
            for (AttrRight.Attr attr : attrRight.getAttrs()) {
                if (ace.deny()) {
                    if (attrRight.getRightType() == rightTypeNeeded)
                        denySome.put(attr.getAttrName(), relativity);  // right type granted === right type needed
                    // else just ignore the grant
                } else
                    allowSome.put(attr.getAttrName(), relativity);
            }
        }
        return CollectAttrsResult.SOME;
    }
    
    private static CollectAttrsResult checkAttrsRight(List<ZimbraACE> acl, TargetType targetType,
                                                      Set<String> granteeIds, Right.RightType rightTypeNeeded, short granteeFlags, 
                                                      Integer relativity,
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
            
            if (rightGranted.isAttrRight()) {
                AttrRight attrRight = (AttrRight)rightGranted;
                result = processAttrsGrant(ace, targetType, attrRight, rightTypeNeeded, relativity, allowSome, denySome);
                
                /*
                 * denied grants appear before allowed grants
                 * - if we see a deny all, return, because on the same target/grantee-relativity deny take precedent allowed.
                 * - if we see an allow all, return, because all deny-some grants have been processed, and put in the denySome map,
                 *   those will be remove from the allowed ones.
                 */
                if (result != null && result.isAll())
                    return result;
                
            } else if (rightGranted.isComboRight()) {
                ComboRight comboRight = (ComboRight)rightGranted;
                for (AttrRight attrRight : comboRight.getAttrRights()) {
                    result = processAttrsGrant(ace, targetType, attrRight, rightTypeNeeded, relativity, allowSome, denySome);
                    // return if we see an allow-all/deny-all, same reasom as above.
                    if (result != null && result.isAll())
                        return result;
                }
            }
        }
        
        return CollectAttrsResult.SOME;
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
    static RightCommand.EffectiveRights getEffectiveRights(Account grantee, Entry target,
            RightCommand.EffectiveRights result) throws ServiceException {

        // get effective preset rights
        Set<Right> presetRights = getEffectivePresetRights(grantee, target);
        
        // get effective setAttrs rights
        AllowedAttrs allowSetAttrs = canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_SET_ATTRS, null);
        
        // get effective getAttrs rights
        AllowedAttrs allowGetAttrs = canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_GET_ATTRS, null);
        
        // finally, populate our result 
        for (Right r : presetRights)
            result.addPresetRight(r.getName());
        
        if (allowSetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_ALL)
            result.setCanSetAllAttrs();
        else if (allowSetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_SOME) {
            result.setCanSetAttrs(fillDefault(target, allowSetAttrs));
        }
        
        if (allowGetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_ALL)
            result.setCanGetAllAttrs();
        else if (allowGetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_SOME) {
            result.setCanGetAttrs(allowGetAttrs.getAllowed());
        }
        
        return result;
    }
    
    private static Map<String, RightCommand.EffectiveAttr> fillDefault(Entry target, AllowedAttrs allowSetAttrs) throws ServiceException {
        Entry inheritFrom = null;
        Provisioning prov = Provisioning.getInstance();
        AttributeManager am = AttributeManager.getInstance();
        
        if (target instanceof Server || target instanceof Domain)
            inheritFrom = prov.getConfig();
        else if (target instanceof Account)
            inheritFrom = prov.getCOS((Account)target);
        
        Map<String, RightCommand.EffectiveAttr> effAttrs = new HashMap<String, RightCommand.EffectiveAttr>();
        for (String attrName : allowSetAttrs.getAllowed()) {
            Set<String> defaultValues = null;
            if (inheritFrom != null) {
                AttributeCardinality ac = am.getAttributeCardinality(attrName);
                if (ac == AttributeCardinality.single) {
                    String defaultValue = inheritFrom.getAttr(attrName);
                    if (defaultValue != null) {
                        defaultValues = new HashSet<String>();
                        defaultValues.add(defaultValue);
                    }
                } else {
                    defaultValues = inheritFrom.getMultiAttrSet(attrName);
                }
            }
            effAttrs.put(attrName, new RightCommand.EffectiveAttr(attrName, defaultValues));
        }
        return effAttrs;
    }
    
    private static Set<Right> getEffectivePresetRights(Account grantee, Entry target) throws ServiceException {
        
        Set<String> granteeIds = setupGranteeIds(grantee);
        TargetType targetType = TargetType.getTargetType(target);
        
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        Integer relativity = Integer.valueOf(1);
        
        //
        // collecting phase
        //
        CollectAttrsResult car = CollectAttrsResult.SOME;
        
        // check the target entry itself
        List<ZimbraACE> acl = RightUtil.getAllACEs(target);
        if (acl != null) {
            collectPresetRightOnTarget(acl, targetType, granteeIds, relativity, allowed, denied);
            relativity += 2;
        }
        
        // check grants granted on entries from which the target entry can inherit from
        TargetIterator iter = TargetIterator.getTargetIeterator(Provisioning.getInstance(), target);
        Entry grantedOn;
            
        GroupACLs groupACLs = null;
            
        while ((grantedOn = iter.next()) != null && (!car.isAll())) {
            acl = RightUtil.getAllACEs(grantedOn);
                
            if (grantedOn instanceof DistributionList) {
                if (acl == null)
                    continue;
                    
                // don't check yet, collect all acls on all target groups
                if (groupACLs == null)
                    groupACLs = new GroupACLs();
                groupACLs.collectACL(grantedOn);
                    
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
                    if (r.applicableOnTargetType(targetType)) {
                        if (ace.deny())
                            denied.put(r, relativity);
                        else
                            allowed.put(r, relativity);
                    }
                }
            } else if (right.isPresetRight()) {
                if (right.applicableOnTargetType(targetType)) {
                    if (ace.deny())
                        denied.put(right, relativity);
                    else
                        allowed.put(right, relativity);
                }
            } 
        }
    }
    
    //
    // util methods
    //
    private static Set<String> setupGranteeIds(Account grantee) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        AclGroups granteeGroups = prov.getAclGroups(grantee);
        
        // setup grantees ids 
        Set<String> granteeIds = new HashSet<String>();
        granteeIds.add(grantee.getId());
        granteeIds.addAll(granteeGroups.groupIds());
        
        return granteeIds;
    }
    

    /*
     * if we set canDo to false, we must give it an attribute name if there is one in attrs
     */
    /*
    private static void computeCanDo(AllowedAttrs result, Entry target, AdminRight rightNeeded, Map<String, Object> attrs) {
        
        if (attrs == null) {
            // caller just wants to get the allowed attr set, doesn't really care about the canDo result, so we set it to 
            // null and don't bother computing it, even when the attr result is allow/deny all.
            result.setCanDo(null);
            
        } else if (result.getResult() == AllowedAttrs.Result.ALLOW_ALL) {
            result.setCanDo(Boolean.TRUE);
            
        } else if (result.getResult() == AllowedAttrs.Result.DENY_ALL) {
            // pick any attr from the requested attrs
            String deniedAttr = "";
            for (String a : attrs.keySet()) {
                deniedAttr = a;
                break;  // got one, break
            }
            result.setCanDo(Boolean.FALSE, deniedAttr);
            
        } else {
            // allow some, check if result can accommodate rightNeeded/attrs
            Set<String> allowed = result.getAllowed();
            if (rightNeeded.getRightType() == Right.RightType.getAttrs) {
                // get
                for (String a : attrs.keySet()) {
                    if (!allowed.contains(a)) {
                        result.setCanDo(Boolean.FALSE, a);
                        return;
                    }
                }
                result.setCanDo(Boolean.TRUE);
                
            } else {
                // set
                Set<String> allowedWithLimit = result.getAllowedWithLimit();
                for (String a : attrs.keySet()) {
                    if (!allowed.contains(a)) {
                        result.setCanDo(Boolean.FALSE, a);
                        return;
                    } 
                    // allowed, see if the value it's setting to is within the inherited limit
                    if (allowedWithLimit.contains(a)) {
                        if (!valueWithinLimit(target, a, attrs.get(a))) {
                            result.setCanDo(Boolean.FALSE, a);
                            return;
                        }
                    }
                }
                result.setCanDo(Boolean.TRUE);
            }
        }
    }
    
    private static boolean valueWithinLimit(Entry target, String attrName, Object value) {
        return true; // TODO 
    }
    */
    

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
