package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.account.accesscontrol.SearchGrants.GrantsOnTarget;

public class ParticallyDenied {

    private static final Log sLog = LogFactory.getLog(ParticallyDenied.class);
    
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
    private static void checkDenied(Provisioning prov, Entry targetToGrant, Right rightToGrant,
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
    
    private static void getAllGrantableTargetTypes(Right right, Set<TargetType> result) throws ServiceException {

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
    
}
