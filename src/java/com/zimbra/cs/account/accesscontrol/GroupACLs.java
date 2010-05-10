package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;

/*
 * aux class for collecting ACLs on all groups the perspective target entry
 * is a direct/indirect member of. 
 */

public class GroupACLs {
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
