package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;

/*
 * aux class for collecting ACLs on all groups the perspective target entry
 * is a direct/indirect member of. 
 */

public class GroupACLs {
    private NamedEntry target;
    
    // Set of zimbraId of groups the account target is a direct member of
    private Set<String> directGroupsOfAccountTarget;
    
    private Set<ZimbraACE> aclsOnGroupTargetsAllowedNotDelegable = new HashSet<ZimbraACE>();
    private Set<ZimbraACE> aclsOnGroupTargetsAllowedDelegable = new HashSet<ZimbraACE>();
    private Set<ZimbraACE> aclsOnGroupTargetsDenied = new HashSet<ZimbraACE>();
    
    GroupACLs(Entry target) throws ServiceException {
        if (target instanceof Account) {
            Account acctTarget = (Account)target;
            this.target = acctTarget;
            directGroupsOfAccountTarget = Provisioning.getInstance().getDirectDistributionLists(acctTarget);
        } else if (target instanceof  DistributionList) {
            DistributionList groupTarget = (DistributionList)target;
            this.target = groupTarget;
        } else {
            throw ServiceException.FAILURE("internal error", null);
        }
    }
    
    private boolean applies(Group grantedOn, ZimbraACE ace) {
        if (!ace.disinheritSubGroups()) {
            return true;
        }
        
        /*
         * grant does not apply to sub groups
         */
        if (target instanceof Account) {
            return directGroupsOfAccountTarget.contains(grantedOn.getId());
        } else {
            // DistributionList
            return target.getId().equals(grantedOn.getId());
        }
    }
    
    void collectACL(Group grantedOn, boolean skipPositiveGrants) 
    throws ServiceException {
        
        Set<ZimbraACE> allowedNotDelegable = ACLUtil.getAllowedNotDelegableACEs(grantedOn);
        Set<ZimbraACE> allowedDelegable = ACLUtil.getAllowedDelegableACEs(grantedOn);
        Set<ZimbraACE> denied = ACLUtil.getDeniedACEs(grantedOn);
        
        if (allowedNotDelegable != null && !skipPositiveGrants) {
            for (ZimbraACE ace : allowedNotDelegable) {
                if (applies(grantedOn, ace)) {
                    aclsOnGroupTargetsAllowedNotDelegable.add(ace);
                }
            }
        }
        
        if (allowedDelegable != null && !skipPositiveGrants) {
            for (ZimbraACE ace : allowedDelegable) {
                if (applies(grantedOn, ace)) {
                    aclsOnGroupTargetsAllowedDelegable.add(ace);
                }
            }
        }
        
        if (denied != null) {
            for (ZimbraACE ace : denied) {
                if (applies(grantedOn, ace)) {
                    aclsOnGroupTargetsDenied.add(ace);
                }
            }
        }
    }
    
    /*
     * put all denied and allowed grants into one list, as if they are granted 
     * on the same entry.   We put denied in the front, followed by allowed and 
     * delegable, followed by allowed but not delegable, so it is consistent with 
     * ZimbraACL.getAllACEs
     */
    List<ZimbraACE> getAllACLs() {
        if (!aclsOnGroupTargetsAllowedNotDelegable.isEmpty() ||
            !aclsOnGroupTargetsAllowedDelegable.isEmpty() ||   
            !aclsOnGroupTargetsDenied.isEmpty()) {
                
            List<ZimbraACE> aclsOnGroupTargets = new ArrayList<ZimbraACE>();
            aclsOnGroupTargets.addAll(aclsOnGroupTargetsDenied);
            aclsOnGroupTargets.addAll(aclsOnGroupTargetsAllowedDelegable);
            aclsOnGroupTargets.addAll(aclsOnGroupTargetsAllowedNotDelegable);
                
            return aclsOnGroupTargets;
        } else {
            return null;
        }
    }
}
