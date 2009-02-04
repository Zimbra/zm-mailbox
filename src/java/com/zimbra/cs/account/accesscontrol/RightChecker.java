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
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;

public class RightChecker {

    private static final Log sLog = LogFactory.getLog(RightChecker.class);
    
    // public only for unittest
    public static class AllowedAttrs {
        public enum Result {
            ALLOW_ALL,
            DENY_ALL,
            ALLOW_SOME;
        }
        
        private Result mResult;
        private Set<String> mAllowSome;

        
        private AllowedAttrs(Result result, Set<String> allowSome) {
            mResult = result;
            mAllowSome = allowSome;
        }
        
        public Result getResult() {
            return mResult;
        }
        
        public Set<String> getAllowed() {
            return mAllowSome;
        }
        
        public String dump() {
            StringBuilder sb = new StringBuilder();
            sb.append("result = " + mResult + " ");
            
            if (mResult == Result.ALLOW_SOME) {
                sb.append("allowed = (");
                for (String a : mAllowSome)
                    sb.append(a + " ");
                sb.append(")");
            }
            
            return sb.toString();
        }
    }
    
    public static final AllowedAttrs ALLOW_ALL_ATTRS() {
        return new AllowedAttrs(AllowedAttrs.Result.ALLOW_ALL, null);
    }
    
    public static final AllowedAttrs DENY_ALL_ATTRS() {
        return new AllowedAttrs(AllowedAttrs.Result.DENY_ALL, null);
    }
    
    public static AllowedAttrs ALLOW_SOME_ATTRS(Set<String> allowSome) {
        return new AllowedAttrs(AllowedAttrs.Result.ALLOW_SOME, allowSome);
    }
    
    
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
        
        void collectACL(Entry grantedOn) throws ServiceException {
            if (aclsOnGroupTargetsAllowedNotDelegable == null)
                aclsOnGroupTargetsAllowedNotDelegable = new HashSet<ZimbraACE>();
            if (aclsOnGroupTargetsAllowedDelegable == null)
                aclsOnGroupTargetsAllowedDelegable = new HashSet<ZimbraACE>();
            if (aclsOnGroupTargetsDenied == null)
                aclsOnGroupTargetsDenied = new HashSet<ZimbraACE>();
            
            Set<ZimbraACE> allowedNotDelegable = RightUtil.getAllowedNotDelegableACEs(grantedOn);
            Set<ZimbraACE> allowedDelegable = RightUtil.getAllowedDelegableACEs(grantedOn);
            Set<ZimbraACE> denied = RightUtil.getDeniedACEs(grantedOn);
            
            if (allowedNotDelegable != null)
                aclsOnGroupTargetsAllowedNotDelegable.addAll(allowedNotDelegable);
            
            if (allowedDelegable != null)
                aclsOnGroupTargetsAllowedDelegable.addAll(allowedDelegable);
            
            if (denied != null)
                aclsOnGroupTargetsDenied.addAll(denied);
        }
        
        /*
         * put all denied and allowed grants into one list, as if they are granted 
         * on the same entry.   We put denied in the front, followed by allowed but 
         * not delegable, followed by allowed and delegable, so it is consistent with 
         * ZimbraACL.getAllACEs
         */
        List<ZimbraACE> getAllACLs() {
            if ((aclsOnGroupTargetsAllowedNotDelegable != null && !aclsOnGroupTargetsAllowedNotDelegable.isEmpty()) ||
                (aclsOnGroupTargetsAllowedDelegable != null && !aclsOnGroupTargetsAllowedDelegable.isEmpty()) ||   
                (aclsOnGroupTargetsDenied != null && !aclsOnGroupTargetsDenied.isEmpty())) {
                    
                List<ZimbraACE> aclsOnGroupTargets = new ArrayList<ZimbraACE>();
                aclsOnGroupTargets.addAll(aclsOnGroupTargetsDenied);
                aclsOnGroupTargets.addAll(aclsOnGroupTargetsAllowedNotDelegable);
                aclsOnGroupTargets.addAll(aclsOnGroupTargetsAllowedDelegable);
                    
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
     * @param canDelegateNeeded if we are checking for "can delegate" the right
     * @param via if not null, will be populated with the grant info via which the result was decided.
     * @return Boolean.TRUE if allowed, Boolean.FALSE if denied, null if there is no grant applicable to the rightNeeded.
     * @throws ServiceException
     */
    static Boolean checkPresetRight(Account grantee, Entry target, 
                                    Right rightNeeded, boolean canDelegateNeeded, 
                                    ViaGrant via) throws ServiceException {
        if (!rightNeeded.isPresetRight())
            throw ServiceException.INVALID_REQUEST("RightChecker.canDo can only check preset right, right " + 
                                                   rightNeeded.getName() + " is a " + rightNeeded.getRightType() + " right",  null);
        
        boolean granteeMustBeAdmin = !rightNeeded.isUserRight();
        
        if (granteeMustBeAdmin) {
            // if the grantee is no longer legitimate, e.g. not an admin any more, ignore all his grants
            if (!isValidGranteeForAdminRights(GranteeType.GT_USER, grantee))
                return null;
        }
        
        Boolean result = null;
        SeenRight seenRight = new SeenRight();
        
        Provisioning prov = Provisioning.getInstance();
        
        // only get admin groups 
        AclGroups granteeGroups = prov.getAclGroups(grantee, granteeMustBeAdmin);
        
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
            result = checkTargetPresetRight(acl, targetType, grantee, granteeGroups, rightNeeded, canDelegateNeeded, via, seenRight);
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
                        result = checkTargetPresetRight(aclsOnGroupTargets, targetType, grantee, granteeGroups, rightNeeded, canDelegateNeeded, via, seenRight);
                    if (result != null) 
                        return result;
                    
                    // set groupACLs to null, we are done with group targets
                    groupACLs = null;
                }
                
                // didn't encounter any group grantedOn, or none of them matches, just check this grantedOn entry
                if (acl == null)
                    continue;
                result = checkTargetPresetRight(acl, targetType, grantee, granteeGroups, rightNeeded, canDelegateNeeded, via, seenRight);
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
                                                  ViaGrant via, SeenRight seenRight) 
        throws ServiceException {
        Boolean result = null;
        
        // if the right is user right, checking for individual match will
        // only check for user grantees, if there are any guest or key grantees
        // (there should *not* be any), they are ignored.
        short adminFlag = (rightNeeded.isUserRight()? 0 : GranteeFlag.F_ADMIN);
        
        // as an individual: user, guest, key
        result = checkPresetRight(acl, targetType, grantee, rightNeeded, canDelegateNeeded, (short)(GranteeFlag.F_INDIVIDUAL | adminFlag), via, seenRight);
        if (result != null) 
            return result;
        
        // as a group member
        result = checkGroupPresetRight(acl, targetType, granteeGroups, grantee, rightNeeded, canDelegateNeeded, (short)(GranteeFlag.F_GROUP), via, seenRight);
        if (result != null) 
            return result;
       
        // if right is an user right, check authed users and public
        if (rightNeeded.isUserRight()) {
            // all authed zimbra user
            result = checkPresetRight(acl, targetType, grantee, rightNeeded, canDelegateNeeded, (short)(GranteeFlag.F_AUTHUSER), via, seenRight);
            if (result != null) 
                return result;
            
            // public
            result = checkPresetRight(acl, targetType, grantee, rightNeeded, canDelegateNeeded, (short)(GranteeFlag.F_PUBLIC), via, seenRight);
            if (result != null) 
                return result;
        }
        
        return null;
    }
    
    /*
     * check if rightNeeded is applicable on target type 
     */
    static boolean rightApplicableOnTargetType(TargetType targetType, 
                                               Right rightNeeded, 
                                               boolean canDelegateNeeded) {
        if (canDelegateNeeded) {
            // check if the right is grantable on the target
            if (!rightNeeded.grantableOnTargetType(targetType))
                return false;
        } else {
            // check if the right is applicable(executable) on the target
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
     *       Note: ace.canDelegate() is not checked in this method, even if 
     *             canDelegateNeeded is true.   We return matched as long as the 
     *             right is seen.  If canDelegateNeeded is true but the ace is not
     *             delegable, it will be denied in getResult.  
     *             
     *             Doing so will deny the case when a "super right" is delegable 
     *             but a "sub-right" of the "super-right" is only executable:
     *                 {id} usr +superRight
     *                 {id} usr subRight
     *             For such grants:
     *                 delegating superRight is denied
     *                 delegating subRight is denied
     *                 delegating another sub right of the super right is allowed
     *                 
     */
    private static boolean matchesPresetRight(ZimbraACE ace, TargetType targetType, 
                                              Right rightNeeded, boolean canDelegateNeeded,
                                              short granteeFlags) throws ServiceException {
        GranteeType granteeType = ace.getGranteeType();
        if (!granteeType.hasFlags(granteeFlags))
            return false;
            
        if (!rightApplicableOnTargetType(targetType, rightNeeded, canDelegateNeeded))
            return false;
            
        Right rightGranted = ace.getRight();
        if ((rightGranted.isPresetRight() && rightGranted == rightNeeded) ||
             rightGranted.isComboRight() && ((ComboRight)rightGranted).containsPresetRight(rightNeeded)) {
            return true;
        }
        
        return false;
    }
    
    private static Boolean checkPresetRight(List<ZimbraACE> acl, TargetType targetType, 
                                            Account grantee, 
                                            Right rightNeeded, boolean canDelegateNeeded,
                                            short granteeFlags, ViaGrant via, SeenRight seenRight) 
        throws ServiceException {
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
    
    private static Boolean checkGroupPresetRight(List<ZimbraACE> acl, TargetType targetType, 
                                                 AclGroups granteeGroups, Account grantee, 
                                                 Right rightNeeded, boolean canDelegateNeeded,
                                                 short granteeFlags, ViaGrant via, SeenRight seenRight) 
        throws ServiceException {
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
    
    private static Boolean gotResult(ZimbraACE ace, Account grantee, Right right, boolean canDelegateNeeded, ViaGrant via) throws ServiceException {
        if (shouldDeny(ace, canDelegateNeeded)) {
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
    public static AllowedAttrs canAccessAttrs(Account grantee, Entry target, AdminRight rightNeeded, boolean canDelegateNeeded) throws ServiceException {
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
            car = checkTargetAttrsRight(acl, targetType, granteeIds, rightNeeded, canDelegateNeeded, relativity, allowSome, denySome);
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
                            car = checkTargetAttrsRight(aclsOnGroupTargets, targetType, granteeIds, rightNeeded, canDelegateNeeded, relativity, allowSome, denySome);
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
                    car = checkTargetAttrsRight(acl, targetType, granteeIds, rightNeeded, canDelegateNeeded, relativity, allowSome, denySome);
                    relativity += 2;
                }
            }
        }
        
        // todo, log result from the collecting phase
        
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
            result = ALLOW_SOME_ATTRS(allowSome.keySet());
        }
        
        // computeCanDo(result, target, rightNeeded, attrs);
        return result;

    }
    
    private static AllowedAttrs processDenyAll(Map<String, Integer> allowSome, Map<String, Integer> denySome, AttributeClass klass) throws ServiceException {
        if (allowSome.isEmpty())
            return DENY_ALL_ATTRS();
        else {
            Set<String> allowed = allowSome.keySet();
            return ALLOW_SOME_ATTRS(allowed);
        }
    }

    private static AllowedAttrs processAllowAll(Map<String, Integer> allowSome, Map<String, Integer> denySome, AttributeClass klass) throws ServiceException {
        
        if (denySome.isEmpty()) {
            return ALLOW_ALL_ATTRS();
        } else {
            // get all attrs that can appear on the target entry
            Set<String> allowed = new HashSet<String>();
            allowed.addAll(AttributeManager.getInstance().getAllAttrsInClass(klass));
            
            // remove denied from all
            for (String d : denySome.keySet())
                allowed.remove(d);
            return ALLOW_SOME_ATTRS(allowed);
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
            Right rightNeeded, boolean canDelegateNeeded,
            Integer relativity,
            Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
        
        CollectAttrsResult result = null;
        
        // as an individual: user
        short granteeFlags = (short)(GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);
        result = expandACLToAttrs(acl, targetType, granteeIds, rightNeeded.getRightType(), canDelegateNeeded, granteeFlags, relativity, allowSome, denySome);
        if (result.isAll()) 
            return result;
        
        // as a group member, bump up the relativity
        granteeFlags = (short)(GranteeFlag.F_GROUP);
        result = expandACLToAttrs(acl, targetType, granteeIds, rightNeeded.getRightType(), canDelegateNeeded, granteeFlags, relativity+1, allowSome, denySome);

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
            Right.RightType rightTypeNeeded, boolean canDelegateNeeded,
            Integer relativity,
            Map<String, Integer> allowSome, Map<String, Integer> denySome) throws ServiceException {
        
        //
        // note: do NOT call ace.getRight in this method, ace is passed in for deny() and getTargetType() 
        // calls only.  
        //
        // Because if the granted right is a combo right, ace.getRight will return the combo right, 
        // not the attr rights in the combo right.  If the grant contains a combo right, each attr 
        // rights of the combo right will be passed in as the attrRight arg.
        //
        
        // check if get/set matches
        if (!attrRightGranted.suitableFor(rightTypeNeeded))
            return null;
        
        //
        // check if the granted attrs right is indeed applicable on the target type
        // this is a sanity check mainly for the case when checking for execution
        // permission (i.e. canDelegateNeeded == false), in case someone somehow 
        // sneak in a zimbraACE on the wrong target.  e.g. put a setAttrs server
        // right on a cos entry.  This should not happen if the grant is done via
        // RightCommand.grantRight.
        //
        if (!rightApplicableOnTargetType(targetType, attrRightGranted, canDelegateNeeded))
            return null;
        
        if (attrRightGranted.allAttrs()) {
            // all attrs, return if we hit a grant with all attrs
            if (shouldDeny(ace, canDelegateNeeded)) {
                /*
                 * if the grant is setAttrs and the needed right is getAttrs:
                 *    - negative setAttrs grant does *not* negate any attrs explicitly allowed for getAttrs grants
                 *    - positive setAttrs grant *does* automatically give getAttrs right
                 */
                if (attrRightGranted.getRightType() == rightTypeNeeded)
                    return CollectAttrsResult.DENY_ALL;  // right type granted === right type needed
                // else just ignore the grant
            } else {
                return CollectAttrsResult.ALLOW_ALL;
            }
        } else {
            // some attrs
            for (String attrName : attrRightGranted.getAttrs()) {
                if (shouldDeny(ace, canDelegateNeeded)) {
                    if (attrRightGranted.getRightType() == rightTypeNeeded)
                        denySome.put(attrName, relativity);  // right type granted === right type needed
                    // else just ignore the grant
                } else {
                    allowSome.put(attrName, relativity);
                }
            }
        }
        return CollectAttrsResult.SOME;
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
            Right.RightType rightTypeNeeded, boolean canDelegateNeeded,
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
            
            if (rightGranted.isAttrRight()) {
                AttrRight attrRight = (AttrRight)rightGranted;
                result = expandAttrsGrantToAttrs(ace, targetType, attrRight, rightTypeNeeded, canDelegateNeeded, relativity, allowSome, denySome);
                
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
                    result = expandAttrsGrantToAttrs(ace, targetType, attrRight, rightTypeNeeded, canDelegateNeeded, relativity, allowSome, denySome);
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
    static RightCommand.EffectiveRights getEffectiveRights(
            Account grantee, Entry target,
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException {

        Set<Right> presetRights;
        AllowedAttrs allowSetAttrs;
        AllowedAttrs allowGetAttrs;
        
        if (isSystemAdmin(grantee, true)) {
            // all preset rights on the target type
            TargetType targetType = TargetType.getTargetType(target);
            presetRights = getAllExecutablePresetRights(targetType);
            
            // all attrs on the target type
            allowSetAttrs = ALLOW_ALL_ATTRS();
            
            // all attrs on the target type
            allowGetAttrs = ALLOW_ALL_ATTRS();
            
        } else {    
            // get effective preset rights
            presetRights = getEffectivePresetRights(grantee, target);
            
            // get effective setAttrs rights
            allowSetAttrs = canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_SET_ATTRS, false);
            
            // get effective getAttrs rights
            allowGetAttrs = canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_GET_ATTRS, false);
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
        
        return result;
    }
    
    static RightCommand.EffectiveRights getEffectiveAttrs(Account grantee, Entry target, 
                                                          RightCommand.EffectiveRights result) throws ServiceException {
        
        // get effective setAttrs rights 
        AllowedAttrs allowSetAttrs = canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_SET_ATTRS, false);
        
        // setAttrs
        if (allowSetAttrs.getResult() == AllowedAttrs.Result.ALLOW_ALL) {
            result.setCanSetAllAttrs();
            result.setCanSetAttrs(expandAttrs(target));
        } else if (allowSetAttrs.getResult() == AllowedAttrs.Result.ALLOW_SOME) {
            result.setCanSetAttrs(fillDefault(target, allowSetAttrs));
        }
        
        return result;
    }
    
    private static List<String> setToSortedList(Set<String> set) {
        List<String> list = new ArrayList<String>(set);
        Collections.sort(list);
        return list;
    }
    
    private static SortedMap<String, RightCommand.EffectiveAttr> fillDefault(Entry target, AllowedAttrs allowSetAttrs) throws ServiceException {
        return fillDefaultAndConstratint(target, allowSetAttrs.getAllowed());
    }
    
    private static SortedMap<String, RightCommand.EffectiveAttr> expandAttrs(Entry target) throws ServiceException {
        return fillDefaultAndConstratint(target, TargetType.getAttrsInClass(target));
    }
    
    private static SortedMap<String, RightCommand.EffectiveAttr> fillDefaultAndConstratint(Entry target, Set<String> attrs) throws ServiceException {
        SortedMap<String, RightCommand.EffectiveAttr> effAttrs = new TreeMap<String, RightCommand.EffectiveAttr>();
        
        Entry constraintEntry = AttributeConstraint.getConstraintEntry(target);
        Map<String, AttributeConstraint> constraints = (constraintEntry==null)?null:AttributeConstraint.getConstraint(constraintEntry);
        boolean hasConstraints = (constraints != null && !constraints.isEmpty());

        for (String attrName : attrs) {
            Set<String> defaultValues = null;
            
            Object defaultValue = target.getAttrDefault(attrName);
            if (defaultValue instanceof String) {
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
    private static Set<String> setupGranteeIds(Account grantee) throws ServiceException {
        Set<String> granteeIds = new HashSet<String>();
        Provisioning prov = Provisioning.getInstance();
        
        if (!isValidGranteeForAdminRights(GranteeType.GT_USER, grantee))
            return granteeIds;  // return empty Set
        
        // get only admin groups
        AclGroups granteeGroups = prov.getAclGroups(grantee, true);
        
        // setup grantees ids 
        granteeIds.add(grantee.getId());
        granteeIds.addAll(granteeGroups.groupIds());
        
        return granteeIds;
    }
    
    
    static boolean canAccessAttrs(AllowedAttrs attrsAllowed, Set<String> attrsNeeded) throws ServiceException {
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("canAccessAttrs attrsAllowed: " + attrsAllowed.dump());
            
            StringBuilder sb = new StringBuilder();
            if (attrsNeeded == null)
                sb.append("<all attributes>");
            else {
                for (String a : attrsNeeded)
                    sb.append(a + " ");
            }
            sLog.debug("canAccessAttrs attrsNeeded: " + sb.toString());
        }
        
        // regardless what attrs say, allow all attrs
        if (attrsAllowed.getResult() == AllowedAttrs.Result.ALLOW_ALL)
            return true;
        else if (attrsAllowed.getResult() == AllowedAttrs.Result.DENY_ALL)
            return false;
        
        //
        // allow some
        //
        
        // need all, nope
        if (attrsNeeded == null)
            return false;
        
        // see if all needed are allowed
        Set<String> allowed = attrsAllowed.getAllowed();
        for (String attr : attrsNeeded) {
            if (!allowed.contains(attr))
                return false;
        }
        return true;
    }

    
    /**
     * Given a result from canAccessAttrsfrom, returns if setting attrs to values is allowed.
     * 
     * This method DOES check for constraints.
     * 
     * @param attrsAllowed result from canAccessAttrs
     * @param attrsNeeded attrs needed to be set.  Cannot be null, must specify which attrs/values to set
     * @return
     */
    static boolean canSetAttrs(AllowedAttrs attrsAllowed, Account grantee, Entry target, Map<String, Object> attrsNeeded) throws ServiceException {
        
        if (attrsNeeded == null)
            throw ServiceException.FAILURE("internal error", null);
        
        if (attrsAllowed.getResult() == AllowedAttrs.Result.DENY_ALL)
            return false;
        
        Entry constraintEntry = AttributeConstraint.getConstraintEntry(target);
        Map<String, AttributeConstraint> constraints = (constraintEntry==null)?null:AttributeConstraint.getConstraint(constraintEntry);
        boolean hasConstraints = (constraints != null && !constraints.isEmpty());
        
        if (hasConstraints) {
            // see if the grantee can set zimbraConstraint on the constraint entry
            // if so, the grantee can set attrs to any value (not restricted by the constraints)
            AllowedAttrs allowedAttrsOnConstraintEntry = canAccessAttrs(grantee, constraintEntry, AdminRight.R_PSEUDO_SET_ATTRS, false);
            if (allowedAttrsOnConstraintEntry.getResult() == AllowedAttrs.Result.ALLOW_ALL ||
                (allowedAttrsOnConstraintEntry.getResult() == AllowedAttrs.Result.ALLOW_SOME &&
                 allowedAttrsOnConstraintEntry.getAllowed().contains(Provisioning.A_zimbraConstraint)))
                hasConstraints = false;
        }
        
        boolean allowAll = (attrsAllowed.getResult() == AllowedAttrs.Result.ALLOW_ALL);
        Set<String> allowed = attrsAllowed.getAllowed();
        
        for (Map.Entry<String, Object> attr : attrsNeeded.entrySet()) {
            if (!allowAll && !allowed.contains(attr.getKey()))
                return false;
                  
            if (hasConstraints) {
                if (AttributeConstraint.violateConstraint(constraints, attr.getKey(), attr.getValue()))
                    return false;
            }
        }
        return true;
    }

    /*
     * construct a pseudo target
     */
    static Entry createPseudoTarget(Provisioning prov,
                                    TargetType targetType, 
                                    DomainBy domainBy, String domainStr,
                                    CosBy cosBy, String cosStr) throws ServiceException {
        
        Entry targetEntry = null;
        Config config = prov.getConfig();
        
        String zimbraId = PseudoZimbraId.getPseudoZimbraId();
        Map<String, Object> attrMap = new HashMap<String, Object>();
        attrMap.put(Provisioning.A_zimbraId, zimbraId);
        
        Domain domain = null;
        if (targetType == TargetType.account ||
            targetType == TargetType.resource ||
            targetType == TargetType.distributionlist) {
            
            // need a domain
            
            if (domainBy == null || domainStr == null)
                throw ServiceException.INVALID_REQUEST("domainBy and domain identifier is required", null);
    
            domain = prov.get(domainBy, domainStr);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domainStr);
        }
        
        switch (targetType) {
        case account:
        case resource:
            Cos cos = null;
            if (cosBy != null && cosStr != null) {
                cos = prov.get(cosBy, cosStr);
                if (cos == null)
                    throw AccountServiceException.NO_SUCH_COS(cosStr);
            } else {
                String domainCosId = domain != null ? domain.getAttr(Provisioning.A_zimbraDomainDefaultCOSId, null) : null;
                if (domainCosId != null) cos = prov.get(CosBy.id, domainCosId);
                if (cos == null) cos = prov.get(CosBy.name, Provisioning.DEFAULT_COS_NAME);
            }
            targetEntry = new Account("pseudo@"+domain.getName(),
                                       zimbraId,
                                       attrMap,
                                       cos.getAccountDefaults(),
                                       prov);
            break;
            
        case cos:  
            targetEntry = new Cos("pseudocos", zimbraId, attrMap, prov);
            break;
        case distributionlist:
            targetEntry = new DistributionList("pseudo@"+domain.getName(), zimbraId, attrMap, prov);
            DistributionList dl = (DistributionList)targetEntry;
            dl.turnToAclGroup();
            break;
        case domain:
            targetEntry = new Domain("pseudo.pseudo", zimbraId, attrMap, config.getDomainDefaults(), prov);
            break;
        case server:  
            targetEntry = new Server("pseudo.pseudo", zimbraId, attrMap, config.getServerDefaults(), prov);
            break;
        case xmppcomponent:
            targetEntry = new XMPPComponent("pseudo", zimbraId, attrMap, prov);
            break;
        case zimlet:
            targetEntry = new Zimlet("pseudo", zimbraId, attrMap, prov);
            break;
        default: 
            throw ServiceException.INVALID_REQUEST("unsupported target for createPseudoTarget", null);
        }
        
        return targetEntry;
    }

    /**
     * returns if grantee is an admin account or admin group
     * 
     * @param gt
     * @param grantee
     * @return
     */
    static boolean isValidGranteeForAdminRights(GranteeType gt, NamedEntry grantee) {
        if (gt == GranteeType.GT_USER) {
            return grantee.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
        } else if (gt == GranteeType.GT_GROUP) {
            return grantee.getBooleanAttr(Provisioning.A_zimbraIsAdminGroup, false);
        } else
            return false;
    }

    static boolean isSystemAdmin(Account acct, boolean asAdmin) {
        return (asAdmin && acct != null && acct.getBooleanAttr(Provisioning.A_zimbraIsSystemAdminAccount, false));
    }
    
    private static boolean shouldDeny(ZimbraACE ace, boolean canDelegateNeeded) {
        return (ace.deny() ||
                (canDelegateNeeded && !ace.canDelegate()));
    }
    
    static class PseudoZimbraId {
        private static final String PSEUDO_ZIMBRA_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        
        static String getPseudoZimbraId() {
            return PSEUDO_ZIMBRA_ID;
        }
        
        static boolean isPseudoZimrbaId(String zid) {
            return (PSEUDO_ZIMBRA_ID.equals(zid));
        }
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
