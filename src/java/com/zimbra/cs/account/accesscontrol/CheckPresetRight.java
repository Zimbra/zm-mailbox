package com.zimbra.cs.account.accesscontrol;

import java.util.List;

import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;

/**
 * Check if grantee is allowed for rightNeeded on target entry.
 */ 

public class PresetRightChecker {
    private static final Log sLog = LogFactory.getLog(RightChecker.class);
    
    
    
    // input to the class
    private Account mGrantee;
    private Entry mTarget;
    private Right mRightNeeded;
    private boolean mCanDelegateNeeded;
    private ViaGrant mVia;
    
    // derived from input or aux vars
    private Provisioning mProv; 
    private AclGroups mGranteeGroups;
    private TargetType mTargetType;
    private SeenRight mSeenRight;
    
    private static class SeenRight {
        private boolean mSeen;
        
        void setSeenRight() {
            mSeen = true;
        }
        
        boolean seenRight() {
            return mSeen;
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
     * @return Boolean.TRUE if allowed, 
     *         Boolean.FALSE if denied, 
     *         null if there is no grant applicable to the rightNeeded.
     * @throws ServiceException
     */
    public static Boolean check(Account grantee, Entry target, 
            Right rightNeeded, boolean canDelegateNeeded, ViaGrant via) throws ServiceException {
        
        PresetRightChecker checker = new PresetRightChecker(grantee, target, rightNeeded, canDelegateNeeded, via);
        return checker.checkRight();
    }
    

    private PresetRightChecker(Account grantee, Entry target, 
            Right rightNeeded, boolean canDelegateNeeded, ViaGrant via) throws ServiceException {
        mProv = Provisioning.getInstance();
        
        mGrantee = grantee;
        mRightNeeded = rightNeeded;
        mCanDelegateNeeded = canDelegateNeeded;
        mVia = via;
        
        mTarget = target;
        
        // This path is called from AccessManager.canDo, the target object can be a 
        // DistributionList obtained from prov.get(DistributionListBy).  
        // We require one from prov.getAclGroup(DistributionListBy) here, call getAclGroup to be sure.
        if (mTarget instanceof DistributionList)
            mTarget = mProv.getAclGroup(DistributionListBy.id, ((DistributionList)target).getId());
        
        mTargetType = TargetType.getTargetType(mTarget);
        
        mSeenRight = new SeenRight();
    }
    
    private AclGroups getGranteeGroups() throws ServiceException {
        if (mGranteeGroups == null) {
            // get all groups the grantee belongs if we haven't done so (Prov.getAclGroups never returns null)
            // get only admin groups if the right is an admin right
            boolean adminGroupsOnly = !mRightNeeded.isUserRight();
            mGranteeGroups = mProv.getAclGroups(mGrantee, adminGroupsOnly);
        }
        return mGranteeGroups;
    }
            
    private Boolean checkRight() throws ServiceException {
        
        if (!mRightNeeded.isPresetRight())
            throw ServiceException.INVALID_REQUEST("RightChecker.canDo can only check preset right, right " + 
                    mRightNeeded.getName() + " is a " + mRightNeeded.getRightType() + " right",  null);
        
        boolean adminRight = !mRightNeeded.isUserRight();
        
        
        Domain granteeDomain = null; 
        
        if (adminRight) {
            // if the grantee is no longer legitimate, e.g. not an admin any more, ignore all his grants
            if (!RightChecker.isValidGranteeForAdminRights(GranteeType.GT_USER, mGrantee))
                return null;
            
            granteeDomain = mProv.getDomain(mGrantee);
            // if we ever get here, the grantee must have a domain
            if (granteeDomain == null)
                throw ServiceException.FAILURE("internal error, cannot find domain for " + mGrantee.getName(), null);
                 
            // should only come from granting/revoking check
            if (mRightNeeded == Admin.R_crossDomainAdmin)
                return CrossDomain.checkCrossDomainAdminRight(mProv, granteeDomain, mTarget, mCanDelegateNeeded);
        }

        
        Boolean result = null;
        
        // check grants explicitly granted on the target entry
        // we don't return the target entry itself in TargetIterator because if 
        // target is a dl, we need to know if the dl returned from TargetIterator 
        // is the target itself or one of the groups the target is in.  So we check 
        // the actual target separately
        List<ZimbraACE> acl = ACLUtil.getAllACEs(mTarget);
        if (acl != null) {
            result = checkTargetPresetRight(acl);
            if (result != null) 
                return result;
        }
        
        //
        // if the target is a domain-ed entry, get the domain of the target.
        // It is needed for checking the cross domain right.
        //
        Domain targetDomain = TargetType.getTargetDomain(mProv, mTarget);
        
        // group target is only supported for admin rights
        boolean expandTargetGroups = RightChecker.allowGroupTarget(mRightNeeded);
        
        // check grants granted on entries from which the target entry can inherit from
        TargetIterator iter = TargetIterator.getTargetIeterator(mProv, mTarget, expandTargetGroups);
        Entry grantedOn;
        
        GroupACLs groupACLs = null;
        
        while ((grantedOn = iter.next()) != null) {
            acl = ACLUtil.getAllACEs(grantedOn);
            
            if (grantedOn instanceof DistributionList) {
                if (acl == null)
                    continue;
                
                boolean skipPositiveGrants = false;
                if (adminRight)
                    skipPositiveGrants = !CrossDomain.crossDomainOK(mProv, mGrantee, granteeDomain, 
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
                        result = checkTargetPresetRight(aclsOnGroupTargets);
                    if (result != null) 
                        return result;
                    
                    // set groupACLs to null, we are done with group targets
                    groupACLs = null;
                }
                
                // didn't encounter any group grantedOn, or none of them matches, just check this grantedOn entry
                if (acl == null)
                    continue;
                result = checkTargetPresetRight(acl);
                if (result != null) 
                    return result;
            }
        }
        
        if (mSeenRight.seenRight())
            return Boolean.FALSE;
        else
            return null;
    }
    
    private Boolean checkTargetPresetRight(List<ZimbraACE> acl) throws ServiceException {
        Boolean result = null;
        
        // if the right is user right, checking for individual match will
        // only check for user grantees, if there are any guest or key grantees
        // (there should *not* be any), they are ignored.
        short adminFlag = (mRightNeeded.isUserRight()? 0 : GranteeFlag.F_ADMIN);
        
        // as an individual: user, guest, key
        result = checkPresetRight(acl, (short)(GranteeFlag.F_INDIVIDUAL | adminFlag));
        if (result != null) 
            return result;
        
        // as a group member
        result = checkGroupPresetRight(acl, (short)(GranteeFlag.F_GROUP));
        if (result != null) 
            return result;
       
        // if right is an user right, check domain, authed users and public grantees
        if (mRightNeeded.isUserRight()) {
            // as an zimbra user in the same domain
            result = checkPresetRight(acl, (short)(GranteeFlag.F_DOMAIN));
            if (result != null) 
                return result;
            
            // all authed zimbra user
            result = checkPresetRight(acl, (short)(GranteeFlag.F_AUTHUSER));
            if (result != null) 
                return result;
            
            // public
            result = checkPresetRight(acl, (short)(GranteeFlag.F_PUBLIC));
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
     *     
     *     Note: if canDelegateNeeded is true but the granted right is not delegable,
     *           we skip the grant (i.e. by returning false) and let the flow continue 
     *           to check other grants, instead of denying the granting attempt.
     *            
     *           That is, a more relevant executable only grant will *not* take away
     *           the grantable property of a less relevant grantable grant.
     *                 
     */
    private boolean matchesPresetRight(ZimbraACE ace, short granteeFlags) throws ServiceException {
        GranteeType granteeType = ace.getGranteeType();
        if (!granteeType.hasFlags(granteeFlags))
            return false;
            
        if (!RightChecker.rightApplicableOnTargetType(mTargetType, mRightNeeded, mCanDelegateNeeded))
            return false;
        
        if (mCanDelegateNeeded && ace.canExecuteOnly())
            return false;
            
        Right rightGranted = ace.getRight();
        if ((rightGranted.isPresetRight() && rightGranted == mRightNeeded) ||
             rightGranted.isComboRight() && ((ComboRight)rightGranted).containsPresetRight(mRightNeeded)) {
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
     * @return
     * @throws ServiceException
     */
    private Boolean checkPresetRight(List<ZimbraACE> acl, short granteeFlags) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : acl) {
            if (!matchesPresetRight(ace, granteeFlags))
                continue;
            
            // if we get here, the right matched, mark it in seenRight.  
            // This is so callsite default will not be honored.
            mSeenRight.setSeenRight();
                
            if (ace.matchesGrantee(mGrantee))
                return gotResult(ace);
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
     *   - for admin rights granted to a group, the grant is effective only if the group has
     *     zimbraIsAdminGroup=TRUE.  The the group's zimbraIsAdminGroup is set to false after 
     *     if grant is made, the grant is still there on the target entry, but becomes useless.
     */
    private Boolean checkGroupPresetRight(List<ZimbraACE> acl, short granteeFlags) throws ServiceException {
        Boolean result = null;
        
        for (ZimbraACE ace : acl) {
            if (!matchesPresetRight(ace, granteeFlags))
                continue;
            
            // if we get here, the right matched, mark it in seenRight.  
            // This is so callsite default will not be honored.
            mSeenRight.setSeenRight();
            
            if (getGranteeGroups().groupIds().contains(ace.getGrantee()))   
                return gotResult(ace);
        }
        return result;
    }
    
    private Boolean gotResult(ZimbraACE ace) throws ServiceException {
        if (ace.deny()) {
            if (sLog.isDebugEnabled())
                sLog.debug("Right " + "[" + mRightNeeded.getName() + "]" + " DENIED to " + mGrantee.getName() + 
                           " via grant: " + ace.dump() + " on: " + ace.getTargetType().getCode() + ace.getTargetName());
            if (mVia != null)
                mVia.setImpl(new ViaGrantImpl(ace.getTargetType(),
                                              ace.getTargetName(),
                                              ace.getGranteeType(),
                                              ace.getGranteeDisplayName(),
                                              ace.getRight(),
                                              ace.deny()));
            return Boolean.FALSE;
        } else {
            if (sLog.isDebugEnabled())
                sLog.debug("Right " + "[" + mRightNeeded.getName() + "]" + " ALLOWED to " + mGrantee.getName() + 
                           " via grant: " + ace.dump() + " on: " + ace.getTargetType().getCode() + ace.getTargetName());
            if (mVia != null)
                mVia.setImpl(new ViaGrantImpl(ace.getTargetType(),
                                              ace.getTargetName(),
                                              ace.getGranteeType(),
                                              ace.getGranteeDisplayName(),
                                              ace.getRight(),
                                              ace.deny()));
            return Boolean.TRUE;
        }
    }
}
