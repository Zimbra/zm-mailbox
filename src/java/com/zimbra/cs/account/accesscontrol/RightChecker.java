package com.zimbra.cs.account.accesscontrol;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class RightChecker {

    private static final Log sLog = LogFactory.getLog(RightChecker.class);
    
    /**
     * Helper class for checking rights.
     * 
     * Wraps aces on a entry with:
     *    - aces on a entry with the needed right
     *    - entry name on which the right is granted
     * 
     * // todo remove the following crap
     * 
     * e.g for this hierarchy:
     *      grantA on globalgrant                                  
     *      grantB on domain test.com                              
     *      grantC on group g1@test.com                            
     *      grantD on group g2@test.com (g2 is a member of g1 and user is a member of g2)     
     *      grantE on group g3@test.com (user is a member of g3)     
     *      grantF on account user@test.com                        
     *      
     * for the access manager call:
     *     canPerform(..., user@test.com, ...), distance for each grant is:
     *         grantF: 0
     *         grantE: 1
     *         grantD: 1
     *         grantC: 2
     *         grantB: 3
     *         grantA: 4
     *         
     *     canPerform(..., g3@test.com, ...), distance for each grant is:
     *         grantE: 0
     *         grantB: 1
     *         grantA: 2
     *         
     *     canPerform(..., g2@test.com, ...), distance for each grant is:    
     *         grantD: 0
     *         grantC: 1
     *         grantB: 2
     *         grantA: 3
     *
     *     canPerform(..., g1@test.com, ...), distance for each grant is:    
     *         grantC: 0
     *         grantB: 1
     *         grantA: 1
     *         
     *     canPerform(..., test.com, ...), distance for each grant is:    
     *         grantB: 0
     *         grantA: 1
     *         
     *     canPerform(..., globalgrant, ...), distance for each grant is:    
     *         grantA: 0
     *                          
     *         
     *
     */
    static class EffectiveACL {
        private Set<ZimbraACE> mAces;    // grants on the entry with the the right of interest
        private String mGrantedOnEntry;  // name of the entry on which this ace is granted, for debugging logging purpose only
                
        EffectiveACL(String grantedOnEntry, Set<ZimbraACE> aces) throws ServiceException {
            mGrantedOnEntry = grantedOnEntry;
            
            // sanity check, an EffectiveACL must have aces, otherwise it should not be constructed
            if (aces == null)
                throw ServiceException.FAILURE("internal error", null);
            mAces = aces;
        }

        void setACEs(Set<ZimbraACE> aces) {
            mAces = aces;
        }
        
        Set<ZimbraACE> getAces() {
            return mAces;
        }
        
        String getGrantedOnEntry() {
            return mGrantedOnEntry;
        }
        
        /*
         * ({entry name on which the right is granted}: [grant] [grant] ...)
         */
        String dump() {
            StringBuffer sb = new StringBuffer();
            
            sb.append("(" + mGrantedOnEntry + ": ");
            for (ZimbraACE ace : mAces)
                sb.append(ace.dump() + " ");
            sb.append(")");
            
            return sb.toString();
        }
    }
    
    /**
     * returns if grantee 
     * 
     * @param effectiveACEs all grants on the target hierarchy for the requested right
     * @param grantee
     * @return
     * @throws ServiceException
     */
    static boolean canDo(List<EffectiveACL> effectiveACLs, Account grantee, Right right) throws ServiceException {
        Boolean result = null;
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("RightChecker.canDo: effectiveACLs=" + dump(effectiveACLs));
        }
        
        result = canDoAsIndividual(effectiveACLs, grantee, right);
        if (result != null)
            return result;
        
        result = canDoAsGroup(effectiveACLs, grantee, right);
        if (result != null)
            return result;
        
        if (right.isUserRight()) {
            result = canDoAsAuthuser(effectiveACLs, grantee, right);
            if (result != null)
                return result;
            
            result = canDoAsPublic(effectiveACLs, grantee, right);
            if (result != null)
                return result;
        }
        
        // no match, return denied
        return false;
    }
    
    private static Boolean canDoAsIndividual(List<EffectiveACL> effectiveACLs, Account grantee, Right right) throws ServiceException {
        Boolean result = null;
        for (EffectiveACL acl : effectiveACLs) {
            for (ZimbraACE ace : acl.getAces()) {
                if (ace.getGranteeType() != GranteeType.GT_USER &&
                    ace.getGranteeType() != GranteeType.GT_GUEST &&
                    ace.getGranteeType() != GranteeType.GT_KEY)
                    continue;
                
                // ignores the grant if it is not allowed for admin rights
                if (!right.isUserRight() && !ace.getGranteeType().allowedForAdminRights())
                    continue;
                
                if (ace.matches(grantee, right)) {
                    if (ace.deny()) {
                        if (sLog.isDebugEnabled())
                            sLog.debug("Right " + "[" + right.getName() + "]" + " DENIED to " + grantee.getName() + " via grant: " + ace.dump() + " on: " + acl.getGrantedOnEntry());
                        return Boolean.FALSE;
                    } else {
                        if (sLog.isDebugEnabled())
                            sLog.debug("Right " + "[" + right.getName() + "]" + " ALLOWED to " + grantee.getName() + " via grant: " + ace.dump() + " on: " + acl.getGrantedOnEntry());
                        return Boolean.TRUE;
                    }
                }
            }
        }
        return result;
    }
    
    private static Boolean canDoAsGroup(List<EffectiveACL> effectiveACLs, Account grantee, Right right) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        // keep track of the most specific group in each unrelated trees that has a matched group
        Map<Integer, ZimbraACE> mostSpecificInMatchedTrees = null;  
        int key = 0;
        for (EffectiveACL acl : effectiveACLs) {
            for (ZimbraACE ace : acl.getAces()) {
                if (ace.getGranteeType() != GranteeType.GT_GROUP)
                    continue;
                
                if (!ace.matches(grantee, right)) 
                    continue;
                
                // we now have a matched group, go through the current matched trees and remember only the 
                // most specific group in each tree
                if (mostSpecificInMatchedTrees == null)
                    mostSpecificInMatchedTrees = new HashMap<Integer, ZimbraACE>();
                boolean inATree = false;
                for (Map.Entry<Integer, ZimbraACE> t : mostSpecificInMatchedTrees.entrySet()) {
                    if (prov.inAclGroup(ace.getGrantee(), t.getValue().getGrantee())) {
                        // encountered a more specific group, replace it 
                        if (sLog.isDebugEnabled())
                            sLog.debug("hasRightAsGroup: replace " + t.getValue().dump() + " with " + ace.dump() + " in tree " + t.getKey());
                        t.setValue(ace);
                        inATree = true;
                    } else if (prov.inAclGroup(t.getValue().getGrantee(), ace.getGrantee())) {
                        // encountered a less specific group, ignore it
                        if (sLog.isDebugEnabled())
                            sLog.debug("hasRightAsGroup: ignore " + ace.dump() + " for tree " + t.getKey());
                        inATree = true;
                    }
                }
                
                // not in any tree, put it in a new tree
                if (!inATree) {
                    if (sLog.isDebugEnabled())
                        sLog.debug("hasRightAsGroup: " + "put " + ace.dump() + " in tree " + key);
                    mostSpecificInMatchedTrees.put(key++, ace);
                }
            }
        }
        
        // no match found
        if (mostSpecificInMatchedTrees == null)
            return null;
        
        // we now have the most specific group of each unrelated trees that matched this grantee/right
        // if they all agree on the allow/deny, good.  If they don't, honor the deny.
        for (ZimbraACE a : mostSpecificInMatchedTrees.values()) {
            if (a.deny()) {
                if (sLog.isDebugEnabled())
                    sLog.debug("hasRightAsGroup: grantee "+ grantee.getName() + " denied for right " + right.getName() + " via ACE: " + a.dump());
                return Boolean.FALSE;
            }
        }
        
        // Okay, every group says yes, allow it.
        if (sLog.isDebugEnabled())
            sLog.debug("hasRightAsGroup: grantee "+ grantee.getName() + " allowed for right " + right.getName() + " via ACE: " + dump(mostSpecificInMatchedTrees.values()));
        return Boolean.TRUE;
    }
    
    private static Boolean canDoAsAuthuser(List<EffectiveACL> effectiveACLs, Account grantee, Right right) throws ServiceException {
        Boolean result = null;
        for (EffectiveACL acl : effectiveACLs) {
            for (ZimbraACE ace : acl.getAces()) {
                if (ace.getGranteeType() != GranteeType.GT_AUTHUSER)
                    continue;
                    
                if (ace.matches(grantee, right)) {
                    if (ace.deny())
                        return Boolean.FALSE;
                    else
                        return Boolean.TRUE;
                }
            }
        }
        return result;
    }
    
    private static Boolean canDoAsPublic(List<EffectiveACL> effectiveACLs, Account grantee, Right right) throws ServiceException {
        Boolean result = null;
        for (EffectiveACL acl : effectiveACLs) {
            for (ZimbraACE ace : acl.getAces()) {
                if (ace.getGranteeType() != GranteeType.GT_PUBLIC)
                    continue;
                
                if (ace.matches(grantee, right)) {
                    if (ace.deny())
                        return Boolean.FALSE;
                    else
                        return Boolean.TRUE;
                }
            }
        }
        return result;
    }
    
    // dump a List of EffectiveACL as ({entry name on which the right is granted}: [grant] [grant] ...)
    private static String dump(List<EffectiveACL> effectiveACLs) {
        StringBuffer sb = new StringBuffer();
        for (EffectiveACL acl : effectiveACLs) {
            sb.append(acl.dump() + " ");
        }
        return sb.toString();
    }
    
    // dump a List of ZimrbaACE as [...] [...] ...
    private static String dump(Collection<ZimbraACE> aces) {
        StringBuffer sb = new StringBuffer();
        for (ZimbraACE ace : aces)
            sb.append(ace.dump() + " ");
        
        return sb.toString();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
