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
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.MemberOf;

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
        Boolean result = null;
        SeenRight seenRight = new SeenRight();
        
        Provisioning prov = Provisioning.getInstance();
        AclGroups granteeGroups = prov.getAclGroups(grantee);
        
        if (!rightNeeded.isPresetRight())
            throw ServiceException.INVALID_REQUEST("RightChecker.canDo can only check preset right, right " + 
                                                   rightNeeded.getName() + " is a " + rightNeeded.getRightType() + " right",  null);
            
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
            result = checkTargetPresetRight(acl, grantee, granteeGroups, rightNeeded, via, seenRight);
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
                        result = checkTargetPresetRight(aclsOnGroupTargets, grantee, granteeGroups, rightNeeded, via, seenRight);
                    if (result != null) 
                        return result;
                    
                    // set groupACLs to null, we are done with group targets
                    groupACLs = null;
                }
                
                // didn't encounter any group grantedOn, or none of them matches, just check this grantedOn entry
                if (acl == null)
                    continue;
                result = checkTargetPresetRight(acl, grantee, granteeGroups, rightNeeded, via, seenRight);
                if (result != null) 
                    return result;
            }
        }
        
        if (seenRight.seenRight())
            return Boolean.FALSE;
        else
            return null;
    }
    
    private static Boolean checkTargetPresetRight(List<ZimbraACE> acl, Account grantee, AclGroups granteeGroups, Right rightNeeded, ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        
        // if the right is user right, checking for individual match will
        // only check for user grantees, if there are any guest or key grantees
        // (there should *not* be any), they are ignored.
        short adminFlag = (rightNeeded.isUserRight()? 0 : GranteeFlag.F_ADMIN);
        
        // as an individual: user, guest, key
        result = checkPresetRight(acl, grantee, rightNeeded, (short)(GranteeFlag.F_INDIVIDUAL | adminFlag), via, seenRight);
        if (result != null) 
            return result;
        
        // as a group member
        result = checkGroupPresetRight(acl, granteeGroups, grantee, rightNeeded, (short)(GranteeFlag.F_GROUP), via, seenRight);
        if (result != null) 
            return result;
       
        if (rightNeeded.isUserRight()) {
            result = checkPresetRight(acl, grantee, rightNeeded, (short)(GranteeFlag.F_AUTHUSER), via, seenRight);
            if (result != null) 
                return result;
            
            result = checkPresetRight(acl, grantee, rightNeeded, (short)(GranteeFlag.F_PUBLIC), via, seenRight);
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
    private static boolean matchesPresetRight(ZimbraACE ace, Right right, short granteeFlags) throws ServiceException {
        GranteeType granteeType = ace.getGranteeType();
        if (!granteeType.hasFlags(granteeFlags))
            return false;
            
        /*
         * This should not happen. e.g. createAccount is a domain right, but is granted 
         * on an account entry.  We check here just in case the grant got in via unexpected 
         * channel, like ldapmodify.
         */
        if (!ace.getTargetType().isRightApplicable(right))
            return false;
            
        Right rightGranted = ace.getRight();
        if ((rightGranted.isPresetRight() && rightGranted == right) ||
             rightGranted.isComboRight() && ((ComboRight)rightGranted).containsPresetRight(right))
            return true;
        
        return false;
    }
    
    private static Boolean checkPresetRight(List<ZimbraACE> acl, Account grantee, Right right, short granteeFlags, ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : acl) {
            if (!matchesPresetRight(ace, right, granteeFlags))
                continue;
            
            // if we get here, the right matched, mark it in seenRight.  This is so callsite default will not be honored.
            seenRight.setSeenRight();
                
            if (ace.matchesGrantee(grantee))
                return gotResult(ace, grantee, right, via);
        }
       
        return result;
    }
    
    private static Boolean checkGroupPresetRight(List<ZimbraACE> acl, AclGroups granteeGroups, Account grantee, Right right, short granteeFlags, ViaGrant via, SeenRight seenRight) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : acl) {
            if (!matchesPresetRight(ace, right, granteeFlags))
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
        
        // setup grantees ids 
        Set<String> granteeIds = setupGranteeIds(grantee);
        
        Map<String, Integer> allowSome = new HashMap<String, Integer>();
        Map<String, Integer> denySome = new HashMap<String, Integer>();
        Integer relativity = Integer.valueOf(1);
        
        TargetType targetType = TargetType.getTargetType(target);
        
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
            result.setCanSetAttrs(allowSetAttrs.getAllowed());
            result.setCanSetAttrsWithLimit(allowSetAttrs.getAllowedWithLimit());
        }
        
        if (allowGetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_ALL)
            result.setCanGetAllAttrs();
        else if (allowGetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_SOME) {
            result.setCanGetAttrs(allowGetAttrs.getAllowed());
        }
        
        return result;
    }
    
    
    private static Set<Right> getEffectivePresetRights(Account grantee, Entry target) throws ServiceException {
        
        // setup grantees ids 
        Set<String> granteeIds = setupGranteeIds(grantee);
        
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        Integer relativity = Integer.valueOf(1);
        
        TargetType targetType = TargetType.getTargetType(target);
        
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
    
    /////////////////////////////////////////////////
    /////////////////////////////////////////////////
    /////////////     REMOVE BELOW   ////////////////
    /////////////////////////////////////////////////
    /////////////////////////////////////////////////
    
    
    /**
     * Helper class for checking rights.
     * 
     * Wraps:
     *   - List of ACEs on an entry with the needed right, negative grants are in the front
     *   - TargetType on which the right is granted
     *   - Entry on which the right is granted
     *   - Distance to the perspective target
     *   
     *   e.g for this hierarchy:
     *      grantA on globalgrant                                  
     *      grantB on domain test.com                              
     *      grantC on group g1@test.com                            
     *      grantD on group g2@test.com (g2 is a member of g1 and user is a member of g2)     
     *      grantE on group g3@test.com (user is a member of g3)     
     *      grantF on account user@test.com                        
     *      
     *   distance would be:
     *     for canPerform(..., user@test.com, ...), distance for each grant is:
     *         grantF: 0
     *         grantE: 1
     *         grantD: 1
     *         grantC: 2
     *         grantB: 3
     *         grantA: 4
     *         
     *     for canPerform(..., g3@test.com, ...), distance for each grant is:
     *         grantE: 0
     *         grantB: 1
     *         grantA: 2
     *         
     *     for canPerform(..., g2@test.com, ...), distance for each grant is:    
     *         grantD: 0
     *         grantC: 1
     *         grantB: 2
     *         grantA: 3
     *
     *     for canPerform(..., g1@test.com, ...), distance for each grant is:    
     *         grantC: 0
     *         grantB: 1
     *         grantA: 1
     *         
     *     for canPerform(..., test.com, ...), distance for each grant is:    
     *         grantB: 0
     *         grantA: 1
     *         
     *     for canPerform(..., globalgrant, ...), distance for each grant is:    
     *         grantA: 0
     *   
     */
    static class EffectiveACL {
        private TargetType mGrantedOnEntryType; // target type on which the aces are granted
        private Entry mGrantedOnEntry;          // entry object on which the aces are granted
        private int mDistanceToTarget;          // distance to the perspective target
        
        private List<ZimbraACE> mAces;          // grants on the entry with the the right of interest
                
        EffectiveACL(TargetType grantedOnEntryType, Entry grantedOnEntry, int distanceToTarget, List<ZimbraACE> aces) throws ServiceException {
            mGrantedOnEntryType = grantedOnEntryType;
            mGrantedOnEntry = grantedOnEntry;
            mDistanceToTarget = distanceToTarget;
            
            // sanity check, an EffectiveACL must have aces, otherwise it should not be constructed
            if (aces == null)
                throw ServiceException.FAILURE("internal error", null);
            mAces = aces;
        }
        
        List<ZimbraACE> getAces() {
            return mAces;
        }
        
        TargetType getGrantedOnEntryType() {
            return mGrantedOnEntryType;
        }
        
        Entry getGrantedOnEntry() {
            return mGrantedOnEntry;
        }
       
        int getDistanceToTarget() {
            return mDistanceToTarget;
        }
        
        /*
         * ({entry name on which the right is granted}: [grant] [grant] ...)
         */
        String dump() {
            StringBuffer sb = new StringBuffer();
            
            sb.append("(" + mGrantedOnEntry.getLabel() + ": ");
            for (ZimbraACE ace : mAces)
                sb.append(ace.dump() + " ");
            sb.append(")");
            
            return sb.toString();
        }
        
        // dump a List of EffectiveACL as ({entry name on which the right is granted}: [grant] [grant] ...)
        static String dump(List<EffectiveACL> effectiveACLs) {
            StringBuffer sb = new StringBuffer();
            for (EffectiveACL acl : effectiveACLs) {
                sb.append(acl.dump() + " ");
            }
            return sb.toString();
        }
    }
    

    /*
     * canDo(for preset rights) and canAccessAttrs(for attr rights) methods traverse a EffectiveACL list, 
     * which was expanded on the target.
     * 
     * The traversing order is:
     * 
     *     (loop 1 - grantees) For each identity(user or group, to which a right can granted) the perspective grantee(the account) can be matched 
     *     {
     *         (loop 2 - targets) walk up the target chain (the List<EffectiveACL> returned by expanding the perspective target) 
     *         {
     *                 (loop 3 - grants) iterate each grant on the target entry 
     *                 {
     *                     examine the grant
     *                 }
     *         }
     *     }
     *     
     *     1 - grantee: is sorted in the most -> least relative order
     *                  - i.e. the perspective account and all groups the account is a direct/indirect member of
     *                  - e.g  account foo@test.com -> group password-admins@test.com -> group domain-admins@test.com
     *               
     *     2 - target : - is sorted in the most -> least relative order
     *                  - i.e. the perspective target and all other targets from which the right can be inherited
     *                  - e.g. 1  account bar@test.com -> group server-team@test.com -> domain test.com -> global grant
     *                    e.g. 2  cos -> global grant
     *     
     *     3 - grants : - is sorted in the deny -> allow 
     *                  - i.e. all negative grants appear before positive grants
     *                  
     * 
     *  For example, for this target hierarchy:
     *      domain D
     *          group G1 (allow right R to group GC)
     *              group G2 (deny right R to group GB)
     *                  group G3 (deny right R to group GA)
     *                      user account U   
     *                  
     *  And this grantee hierarchy:
     *      group GA
     *          group GB
     *              group GC
     *                  (admin) account A
     *              
     *  Then A is *allowed* for right R on target account U, because GC is more specific to A than GA and GB.
     *  Even if on the target side, grant on G3(grant to GA) and G2(grant to GB) is more specific than the 
     *  grant on G1(grant to GC).                          
     *                  
     */
        
        
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
    static Boolean canDo_XXX(Account grantee, Entry target, Right rightNeeded, ViaGrant via) throws ServiceException {
        
        if (!rightNeeded.isPresetRight())
            throw ServiceException.INVALID_REQUEST("RightChecker.canDo can only check preset right, right " + 
                                                   rightNeeded.getName() + " is a " + rightNeeded.getRightType() + " right",  null);
            
        Provisioning prov = Provisioning.getInstance();
        
        List<EffectiveACL> effectiveACLs = TargetType.expandTargetByRight(prov, target, rightNeeded);
        if (effectiveACLs != null && effectiveACLs.size() > 0)
            return checkPresetRight_XXX(effectiveACLs, grantee, rightNeeded, via);
        else
            return null;
    }
    
    private static boolean checkPresetRight_XXX(List<EffectiveACL> effectiveACLs, Account grantee, Right right, ViaGrant via) throws ServiceException {
        Boolean result = null;
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("RightChecker.canDo: effectiveACLs=" + EffectiveACL.dump(effectiveACLs));
        }
        
        short adminFlag = (right.isUserRight()? 0 : GranteeFlag.F_ADMIN);
        
        // as an individual
        result = check_XXX(effectiveACLs, grantee, right, via, (short)(GranteeFlag.F_INDIVIDUAL | adminFlag));
        if (result != null)
            return result;
        
        // as a group member
        List<MemberOf> groups = Provisioning.getInstance().getAclGroups((Account)grantee).memberOf();
        Boolean tempResult = null;
        int tempResultAtDist= 0;
        for (MemberOf group : groups) {
            if (tempResult != null) {
                if (group.getDistance() > tempResultAtDist)
                    return tempResult;
                // else this group is at the same distance as the group
                // on which the temp positive result was found, continue
                // to check this group
            }
            result = checkGroup_XXX(effectiveACLs, group.getId(), grantee, right, via);
            if (result != null) {
                if (result == Boolean.FALSE)
                    return result;
                else {
                    // got an allow, continue checking all groups with the same distance to 
                    // the perspective account, see if there i a denied one.  If som should 
                    // deny, if not, result the allowed result.
                    tempResult = result;
                    tempResultAtDist = group.getDistance();
                }
            }
        }
        if (tempResult != null)
            return tempResult;
        
        if (right.isUserRight()) {
            // as an authed user
            result = check_XXX(effectiveACLs, grantee, right, via, GranteeFlag.F_AUTHUSER);
            if (result != null)
                return result;
            
            // as public 
            result = check_XXX(effectiveACLs, grantee, right, via, GranteeFlag.F_PUBLIC);
            if (result != null)
                return result;
        }
        
        // no match, return denied
        return false;
    }
    
    /*
     * Iterate through the effective ACLs to determine if the right is allowed or denied to the grantee.
     * 
     * The effectiveACLs list contains all grants on all entries with the specified right that could 
     * influence the result.   Each EffectiveACL contains grants with the specified right on one target entry.
     * 
     * The List is sorted as follows:
     * - from the most specific target entry to the least specific target entry
     * - on the same target entry, negative grants are in the front, positive grants are in the rear
     * 
     * @param effectiveACLs
     * @param grantee
     * @param right
     * @param via
     * @param granteeFlags
     * @return
     * @throws ServiceException
     */
    private static Boolean check_XXX(List<EffectiveACL> effectiveACLs, Account grantee, Right right, ViaGrant via, short granteeFlags) throws ServiceException {
        Boolean result = null;
        for (EffectiveACL acl : effectiveACLs) {
            for (ZimbraACE ace : acl.getAces()) {
                GranteeType granteeType = ace.getGranteeType();
                if (!granteeType.hasFlags(granteeFlags))
                    continue;
                
                if (ace.matchesGrantee(grantee))
                    return gotResult_XXX(acl, ace, grantee, right, via);
            }
        }
        return result;
    }
    

    /*
     * @param effectiveACLs
     * @param groupId
     * @param grantee not used in this method, just for passing to gotResult for logging purpose.
     * @param right
     * @param via
     * @return
     * @throws ServiceException
     */
    private static Boolean checkGroup_XXX(List<EffectiveACL> effectiveACLs, String groupId, Account grantee, Right right, ViaGrant via) throws ServiceException {
        Boolean result = null;
        for (EffectiveACL acl : effectiveACLs) {
            for (ZimbraACE ace : acl.getAces()) {
                GranteeType granteeType = ace.getGranteeType();
                if (ace.getGrantee().equals(groupId))   
                    return gotResult_XXX(acl, ace, grantee, right, via);
            }
        }
        return result;
    }

    
    private static Boolean gotResult_XXX(EffectiveACL acl, ZimbraACE ace, Account grantee, Right right, ViaGrant via) {
        if (ace.deny()) {
            if (sLog.isDebugEnabled())
                sLog.debug("Right " + "[" + right.getName() + "]" + " DENIED to " + grantee.getName() + 
                           " via grant: " + ace.dump() + " on: " + acl.getGrantedOnEntry());
            if (via != null)
                via.setImpl(new ViaGrantImpl(acl.getGrantedOnEntryType(),
                                             acl.mGrantedOnEntry.getLabel(),
                                             ace.getGranteeType(),
                                             ace.getGranteeDisplayName(),
                                             ace.getRight(),
                                             ace.deny()));
            return Boolean.FALSE;
        } else {
            if (sLog.isDebugEnabled())
                sLog.debug("Right " + "[" + right.getName() + "]" + " ALLOWED to " + grantee.getName() + 
                           " via grant: " + ace.dump() + " on: " + acl.getGrantedOnEntry());
            if (via != null)
                via.setImpl(new ViaGrantImpl(acl.getGrantedOnEntryType(),
                                             acl.mGrantedOnEntry.getLabel(),
                                             ace.getGranteeType(),
                                             ace.getGranteeDisplayName(),
                                             ace.getRight(),
                                             ace.deny()));
            return Boolean.TRUE;
        }
    }

    
    static AllowedAttrs canAccessAttrs_XXX(Account grantee, Entry target, AdminRight rightNeeded, Map<String, Object> attrs) throws ServiceException {
        if (rightNeeded != AdminRight.R_PSEUDO_GET_ATTRS && rightNeeded != AdminRight.R_PSEUDO_SET_ATTRS)
            throw ServiceException.FAILURE("internal error", null); 
        
        Provisioning prov = Provisioning.getInstance();
        
        List<EffectiveACL> effectiveACLs = TargetType.expandTargetByRight(prov, target, rightNeeded);
        AllowedAttrs result;
        if (effectiveACLs != null && effectiveACLs.size() > 0)
            result = canAccessAttrs_XXX(effectiveACLs, grantee, rightNeeded, TargetType.getAttributeClass(target));
        else     
            result = AccessManager.DENY_ALL_ATTRS();
        
        computeConDo(result, target, rightNeeded, attrs);
        return result;
    }
    
    /*
     * if we set canDo to false, we must give it an attribute name if there is one in attrs
     */
    private static void computeConDo(AllowedAttrs result, Entry target, AdminRight rightNeeded, Map<String, Object> attrs) {
        
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
    
    
    /**
     * ======================================================================
     * 
     *  Given a list of EffectiveACL, returns the set of attributes the 
     *  perspective grantee can get/set(specified by the right arg).
     *  
     *  Called from AccessManager.canAccessAttrs()
     *  
     * ======================================================================
     * 
     * @param effectiveACLs
     * @param grantee perpective grantee
     * @param right   right needed, either R_PSEUDO_GET_ATTRS or R_PSEUDO_SET_ATTRS
     * @param klass   AttributeClass object, need for obtaining the set of all attributes
     *                that can appear on the perspective target type.
     *
     * @return
     * @throws ServiceException
     */
    private static AllowedAttrs canAccessAttrs_XXX(List<EffectiveACL> effectiveACLs, Account grantee, Right right, AttributeClass klass) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        AllowedAttrs result = null;
        
        Map<String, AttrDist> allowSome = new HashMap<String, AttrDist>();
        Map<String, AttrDist> denySome = new HashMap<String, AttrDist>();
        
        /*
         * Collecting phase.
         * 
         * traverse the effective ACLs, which is sorted in the order of most to least specific targets.
         * first pass visits the user grantees, second pass visits the group grantees
         */
        
        int baseDistance = 0;
        CollectAttrsResult_XXX car;
        Right.RightType rightTypeNeeded = right.getRightType();
        
        /*
         * 1. collect attrs in grants that are granted directly to the authed account
         *    begin with base distance 0
         */
        car = collectAttrs_XXX(effectiveACLs, grantee.getId(), 0, rightTypeNeeded, allowSome, denySome);
        
        if (car.mResult == CollectAttrsResult_XXX.Result.SOME) {
            /*
             * 2. walk up the group hierarchy of the authed account, collect attrs that 
             *    are granted to the group use the distance from previous call to collectAttrs 
             *    as the base instance
             */
            List<MemberOf> groups = prov.getAclGroups((Account)grantee).memberOf();
            for (MemberOf group : groups) {
                car = collectAttrs_XXX(effectiveACLs, group.getId(), car.mDistance, rightTypeNeeded, allowSome, denySome);
                if (car.mResult != CollectAttrsResult_XXX.Result.SOME)
                    break; // stop if allow/deny all was encountered
            }
        }
        
        if (sLog.isDebugEnabled()) {
            if (car.mResult == CollectAttrsResult_XXX.Result.ALLOW_ALL)
                sLog.debug("allow all at: " + car.mDistance);
            else if (car.mResult == CollectAttrsResult_XXX.Result.DENY_ALL)
                sLog.debug("deny all at: " + car.mDistance);
            
            sLog.debug("allowSome: " + AttrDist.dump(allowSome));
            sLog.debug("denySome: " + AttrDist.dump(denySome));
        }
        
        /*
         * Computing phase
         * 
         * computes results from the collecting phase 
         * 
         */
        
        if (car.mResult == CollectAttrsResult_XXX.Result.ALLOW_ALL)
            result = processAllowAll_XXX(allowSome, denySome, klass);
        else if (car.mResult == CollectAttrsResult_XXX.Result.DENY_ALL)
            result = processDenyAll_XXX(allowSome, denySome, klass);
        else {
            // now allowSome and denySome contain attrs allowed/denied and their shortest distance 
            // to the target, remove denied ones from allowed if they've got a shorter distance
            Set<String> conflicts = SetUtil.intersect(allowSome.keySet(), denySome.keySet());
            if (!conflicts.isEmpty()) {
                for (String attr : conflicts) {
                    if (denySome.get(attr).mDistance <= allowSome.get(attr).mDistance)
                        allowSome.remove(attr);
                }
            }
            Set<String> allowSomeWithLimit = buildAllowedWithLimit(allowSome);
            result = AccessManager.ALLOW_SOME_ATTRS(allowSome.keySet(), allowSomeWithLimit);
        }
        
        return result;
    }
    
    /*
     * an allowed attribute with two properties:
     *     - "distance" to the perspective grantee, i.e., how relevant the grant is. 
     *       This is used when deciding whether an attr is allowed where is the conflict (some grants
     *       allow it, some deny it)
     *         
     *     - for setting, whether allows settng to any value ot setting withing inheritec limit
     *     
     * The distance is calculated as follows:
     * 
     *     ***** distance = 0 *****
     *     (1 - grantees) For each identity(user or group, to which a right can granted) the perspective 
     *     grantee(the account) can assume the role of 
     *     {
     *         (2 - targets) walk up the target chain (the List<EffectiveACL> returned by expanding the perspective target) 
     *         {
     *                 (3 - grants) iterate each grant on the target entry 
     *                 {
     *                     collect attrs on this grant
     *                     ***** distance++ *****
     *                 }
     *         }
     *     }
     *     
     *     1 - grantee: is sorted in the most -> least relative order
     *                  - i.e. the perspective account and all groups the account is a direct/indirect member of
     *                  - e.g  account foo@test.com -> group password-admins@test.com -> group domain-admins@test.com
     *               
     *     2 - target : - is sorted in the most -> least relative order
     *                  - i.e. the perspective target and all other targets from which the right can be inherited
     *                  - e.g. 1  account bar@test.com -> group server-team@test.com -> domain test.com -> global grant
     *                    e.g. 2  cos -> global grant
     *     
     *     3 - grants : - is sorted in the deny -> allow 
     *                  - i.e. all negative grants appear before positive grants
     *                  - grants in an EffectiveACL are those granted with AttrRight and the right is applicable 
     *                    to the perspective target. (e.g. if the perspective target is a cos, the EffectiveACL 
     *                    contains only those grants with AttrRight that are applicable to cos TargetType).
     *     
     *     The canAccessAttrs method run the above loops to collect.  It stops when an "all attrs" grant, either 
     *     negative or positive.  It then computes the net result with the collected lists(one allowed list, one 
     *     denied list) of AttrDist.
     */
    private static class AttrDist {
        int mDistance;
        boolean mLimit;
        
        AttrDist(AttrRight.Attr attr, int distance) {
            mLimit = attr.getLimit();
            mDistance = distance;
        }
        
        static String dump(Map<String, AttrDist> attrs) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, AttrDist> a : attrs.entrySet())
                sb.append("(" + a.getKey() + ", " + a.getValue().mDistance +  ", " + a.getValue().mLimit + ")");
            return sb.toString();
        }
    }
    
    private static class CollectAttrsResult_XXX {
        enum Result {
            SOME,
            ALLOW_ALL,
            DENY_ALL;
        }
        
        CollectAttrsResult_XXX(Result result, int distance) {
            mResult = result;
            mDistance = distance;
        }
        
        Result mResult;
        int mDistance;  // distance for next rank of grants
    }
    
    
    private static Set<String> buildAllowedWithLimit(Map<String, AttrDist> allowSome) {
        // build the limited set
        Set<String> allowedWithlimit = new HashSet<String>();
        for (Map.Entry<String, AttrDist> a : allowSome.entrySet()) {
            if (a.getValue().mLimit) 
                allowedWithlimit.add(a.getKey());
        }
        return allowedWithlimit;
    }
    
    private static AllowedAttrs processDenyAll_XXX(Map<String, AttrDist> allowSome, Map<String, AttrDist> denySome, AttributeClass klass) throws ServiceException {
        if (allowSome.isEmpty())
            return AccessManager.DENY_ALL_ATTRS();
        else {
            Set<String> allowed = allowSome.keySet();
            Set<String> allowedWithlimit = buildAllowedWithLimit(allowSome);
            return AccessManager.ALLOW_SOME_ATTRS(allowed, allowedWithlimit);
        }
    }

    private static AllowedAttrs processAllowAll_XXX(Map<String, AttrDist> allowSome, Map<String, AttrDist> denySome, AttributeClass klass) throws ServiceException {
        
        if (denySome.isEmpty()) {
            if (allowSome.isEmpty())
                return AccessManager.ALLOW_ALL_ATTRS();
            else {
                // there could be some attrs with limit in the allowSome list,
                // remove those from allowed and put them in the allowed with limit set
             
                Set<String> allowed = new HashSet<String>();
                allowed.addAll(AttributeManager.getInstance().getAttrsInClass(klass));
                
                // build the limited set
                Set<String> allowedWithlimit = new HashSet<String>();
                for (Map.Entry<String, AttrDist> a : allowSome.entrySet()) {
                    if (a.getValue().mLimit && allowed.contains(a.getKey())) {
                        allowedWithlimit.add(a.getKey());
                        allowed.remove(a.getKey());
                    }
                }
                
                return allowedWithlimit.isEmpty()? 
                           AccessManager.ALLOW_ALL_ATTRS():  // didn't find any with limit attrs in allowSome, so return allow all
                           AccessManager.ALLOW_SOME_ATTRS(allowed, allowedWithlimit);
            }

        } else {
            // get all attrs that can appear on the target entry
            Set<String> allowed = new HashSet<String>();
            allowed.addAll(AttributeManager.getInstance().getAttrsInClass(klass));
            
            // remove denied from all
            for (String d : denySome.keySet())
                allowed.remove(d);
            
            // build the limited set
            Set<String> allowedWithlimit = new HashSet<String>();
            for (Map.Entry<String, AttrDist> a : allowSome.entrySet()) {
                if (a.getValue().mLimit && allowed.contains(a.getKey())) 
                    allowedWithlimit.add(a.getKey());
            }
            
            return AccessManager.ALLOW_SOME_ATTRS(allowed, allowedWithlimit);
        }
    }

    private static CollectAttrsResult_XXX collectAttrs_XXX(
            List<EffectiveACL> effectiveACLs, String granteeId, int baseDistance, Right.RightType rightTypeNeeded, 
            Map<String, AttrDist> allowSome, Map<String, AttrDist> denySome) throws ServiceException {
        
        int dist = baseDistance;
        
        CollectAttrsResult_XXX seenAllowAll = null;
        
        for (EffectiveACL acl : effectiveACLs) {
            // next farther target
            dist++;
            
            for (ZimbraACE ace : acl.getAces()) {
                /*
                 * only look at AttrRight's.  AttrRight's contained in ComboRight's are expanded
                 * into multiple AttrRights.
                 * 
                 * need to check because this can be called from two routes:
                 * - from canAccessAttrs(), the EffectiveACL list only contains AttrRight's
                 * but,
                 * - from getEffectiveRights, the EffectiveACL list can contain preset, combo, and attr rights.
                 */
                if (!ace.getRight().isAttrRight())
                    continue;
                
                // must be an AttrRight by now
                AttrRight right = (AttrRight)ace.getRight();
                
                /*
                 * check if the grant is applicable to the right (get or set) needed
                 * 
                 * need to check because this can be called from two routes:
                 * - from canAccessAttrs(), the EffectiveACL list only contains applicable grants
                 * but,
                 * - from getEffectiveRights, the EffectiveACL list can contain both get/setAttrs rights
                 */
                if (!right.applicableToRightType(rightTypeNeeded))
                    continue;
                
                dist++;
                Integer distance = Integer.valueOf(dist);
                
                if (ace.getGrantee().equals(granteeId)) {
                    
                    
                    if (right.allAttrs()) {
                        // all attrs, return if we hit a grant with all attrs
                        if (ace.deny())
                            return new CollectAttrsResult_XXX(CollectAttrsResult_XXX.Result.DENY_ALL, dist);
                        else {
                            /*
                             * allow all, still need to visit all grants on the same target
                             * this is needed because for allow-all and allow-some-with-limit on the 
                             * same distance, those in allow-some-with limit should be allowed with 
                             * limit.  Grants on the same target are sorted so denied are in the 
                             * front, but allowed(with/wo limit) and allow-all can appear in random 
                             * order.   Remember the allow-all grant, and return it after we've gone 
                             * through all grants on this target to this grantee.
                             */
                            seenAllowAll = new CollectAttrsResult_XXX(CollectAttrsResult_XXX.Result.ALLOW_ALL, dist);
                        }
                    } else {
                        // some attrs
                        for (AttrRight.Attr attr : right.getAttrs()) {
                            if (ace.deny())
                                denySome.put(attr.getAttrName(), new AttrDist(attr, distance));
                            else
                                allowSome.put(attr.getAttrName(), new AttrDist(attr, distance));
                        }
                    }
                }
            }
            
            if (seenAllowAll != null)
                return seenAllowAll;
            
        }
        
        return new CollectAttrsResult_XXX(CollectAttrsResult_XXX.Result.SOME, dist);
    }


    /**
     * ======================================================================
     * 
     *  Given a list of EffectiveACL, returns all preset rights and all attrs
     *  that can be get/set by the perspective grantee.
     *  
     *  Called from RightCommand.getEffectiveRights, for admin console only.
     *  Not used for ACL checking.
     * 
     * ======================================================================
     * 
     * @param effectiveACLs
     * @param grantee
     * @return
     * @throws ServiceException
     */
    static RightCommand.EffectiveRights getEffectiveRights_XXX(List<EffectiveACL> effectiveACLs, Account grantee, AttributeClass klass,
                                                           RightCommand.EffectiveRights result) throws ServiceException {
        
        // get effective preset rights
        Set<Right> presetRights = getEffectivePresetRights_XXX(effectiveACLs, grantee);
        
        // get effective setAttrs rights
        AllowedAttrs allowSetAttrs = canAccessAttrs_XXX(effectiveACLs, grantee, AdminRight.R_PSEUDO_SET_ATTRS, klass);
        
        // get effective getAttrs rights
        AllowedAttrs allowGetAttrs = canAccessAttrs_XXX(effectiveACLs, grantee, AdminRight.R_PSEUDO_GET_ATTRS, klass);
        
        // finally, populate our result 
        for (Right r : presetRights)
            result.addPresetRight(r.getName());
        
        if (allowSetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_ALL)
            result.setCanSetAllAttrs();
        else if (allowSetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_SOME) {
            result.setCanSetAttrs(allowSetAttrs.getAllowed());
            result.setCanSetAttrsWithLimit(allowSetAttrs.getAllowedWithLimit());
        }
        
        if (allowGetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_ALL)
            result.setCanGetAllAttrs();
        else if (allowGetAttrs.getResult() == AccessManager.AllowedAttrs.Result.ALLOW_SOME) {
            result.setCanGetAttrs(allowGetAttrs.getAllowed());
        }
        
        return result;
    }
    
    private static Set<Right> getEffectivePresetRights_XXX(List<EffectiveACL> effectiveACLs, Account grantee) throws ServiceException {
        
        Provisioning prov = Provisioning.getInstance();
        
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        
        /*
         * Collecting phase.
         * 
         * traverse the effective ACLs, which is sorted in the order of most to least specific targets.
         * first pass visits the user grantees, second pass visits the group grantees
         */
        
        int baseDistance = 0;
        CollectAttrsResult_XXX car;
        /*
         * 1. collect attrs in grants that are granted directly to the authed account
         *    begin with base distance 0
         */
        baseDistance = collectPresetRights_XXX(effectiveACLs, grantee.getId(), 0, allowed, denied);
        
        /*
         * 2. walk up the group hierarchy of the authed account, collect preset rights that 
         *    are granted to the group use the distance from previous call to collectAttrs 
         *    as the base instance
         */
        List<MemberOf> groups = prov.getAclGroups((Account)grantee).memberOf();
        for (MemberOf group : groups)
            baseDistance = collectPresetRights_XXX(effectiveACLs, group.getId(), baseDistance, allowed, denied);
        
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

    private static int collectPresetRights_XXX(List<EffectiveACL> effectiveACLs, String granteeId, int baseDistance,
                                           Map<Right, Integer> allowed, Map<Right, Integer> denied) throws ServiceException {

        int dist = baseDistance;
        
        for (EffectiveACL acl : effectiveACLs) {
            // next farther target
            dist++;
        
            for (ZimbraACE ace : acl.getAces()) {
                dist++;
                Integer distance = Integer.valueOf(dist);
                
                if (ace.getGrantee().equals(granteeId)) {
                    Right right = ace.getRight();
                    
                    if (right.isComboRight()) {
                        ComboRight comboRight = (ComboRight)right;
                        for (Right r : comboRight.getPresetRights()) {
                            if (ace.deny())
                                denied.put(r, distance);
                            else
                                allowed.put(r, distance);
                        }
                    } else if (right.isPresetRight()) {
                        if (ace.deny())
                            denied.put(right, distance);
                        else
                            allowed.put(right, distance);
                    } 
                }
            }
        }
        
        return dist;  // for next rank of grantee
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
