package com.zimbra.cs.account.accesscontrol;

import java.util.Collection;
import java.util.HashMap;
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
     * returns if grantee 
     * 
     * @param effectiveACEs all grants on the target hierarchy for the requested right
     * @param grantee
     * @return
     * @throws ServiceException
     */
    static boolean canDo(Set<ZimbraACE> effectiveACEs, Account grantee, Right right) throws ServiceException {
        Boolean result = null;
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("canDO: effectiveACEs=" + dump(effectiveACEs));
        }
        
        result = canDoAsIndividual(effectiveACEs, grantee, right);
        if (result != null)
            return result;
        
        result = canDoAsGroup(effectiveACEs, grantee, right);
        if (result != null)
            return result;
        
        if (right.isUserRight()) {
            result = canDoAsAuthuser(effectiveACEs, grantee, right);
            if (result != null)
                return result;
            
            result = canDoAsPublic(effectiveACEs, grantee, right);
            if (result != null)
                return result;
        }
        
        // no match, return denied
        return false;
    }
    
    private static Boolean canDoAsIndividual(Set<ZimbraACE> effectiveACEs, Account grantee, Right right) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : effectiveACEs) {
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
                        sLog.debug("Right " + right.getName() + " denied to " + grantee.getName() + " via grant: " + ace.dump());
                    return Boolean.FALSE;
                } else {
                    if (sLog.isDebugEnabled())
                        sLog.debug("Right " + right.getName() + " allowed to " + grantee.getName() + " via grant: " + ace.dump());
                    return Boolean.TRUE;
                }
            }
        }
        return result;
    }
    
    private static Boolean canDoAsGroup(Set<ZimbraACE> effectiveACEs, Account grantee, Right right) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        // keep track of the most specific group in each unrelated trees that has a matched group
        Map<Integer, ZimbraACE> mostSpecificInMatchedTrees = null;  
        int key = 0;
        for (ZimbraACE ace : effectiveACEs) {
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
                if (prov.inGroup(ace.getGrantee(), t.getValue().getGrantee())) {
                    // encountered a more specific group, replace it 
                    if (sLog.isDebugEnabled())
                        sLog.debug("hasRightAsGroup: replace " + t.getValue().dump() + " with " + ace.dump() + " in tree " + t.getKey());
                    t.setValue(ace);
                    inATree = true;
                } else if (prov.inGroup(t.getValue().getGrantee(), ace.getGrantee())) {
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
    
    private static Boolean canDoAsAuthuser(Set<ZimbraACE> effectiveACEs, Account grantee, Right right) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : effectiveACEs) {
            if (ace.getGranteeType() != GranteeType.GT_AUTHUSER)
                continue;
                
            if (ace.matches(grantee, right)) {
                if (ace.deny())
                    return Boolean.FALSE;
                else
                    return Boolean.TRUE;
            }
        }
        return result;
    }
    
    private static Boolean canDoAsPublic(Set<ZimbraACE> effectiveACEs, Account grantee, Right right) throws ServiceException {
        Boolean result = null;
        for (ZimbraACE ace : effectiveACEs) {
            if (ace.getGranteeType() != GranteeType.GT_PUBLIC)
                continue;
            
            if (ace.matches(grantee, right)) {
                if (ace.deny())
                    return Boolean.FALSE;
                else
                    return Boolean.TRUE;
            }
        }
        return result;
    }
    
    // dump a colection of ZimrbaACE to [...] [...] ...
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
