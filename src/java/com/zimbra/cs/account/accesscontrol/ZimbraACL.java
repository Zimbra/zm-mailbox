package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;

public class ZimbraACL {
    private static final Log sLog = LogFactory.getLog(ZimbraACL.class);

    
    // usr, grp, gst, key aces
    Set<ZimbraACE> mAces;
        
    // authuer aces
    Set<ZimbraACE> mAuthuserAces;
    
    // public aces
    Set<ZimbraACE> mPublicAces;

    // for the containsRight call
    Set<Right> mContainsRight;
    
    /**
     * ctor for deserializing from LDAP
     * 
     * It is assumed that the ACL in LDAP is correctly built and saved.
     * For perf reason, this ctor does NOT check for duplicate/conflict ACEs, 
     * it just loads all ACEs.
     * 
     * @param aces
     * @throws ServiceException
     */
    public ZimbraACL(String[] aces, RightManager rm) throws ServiceException {
        for (String aceStr : aces) {
            ZimbraACE ace = new ZimbraACE(aceStr, rm);
            Set<ZimbraACE> aceSet = aceSetByGranteeType(ace.getGranteeType(), true);
            addACE(aceSet, ace);
        }
    }
    
    /**
     * ctor for setting initial ACL (when there is currently no ACL for an entry)
     * 
     * This ctor DOES check for duplicate/conflict(allow and deny a right to the same grantee).
     * 
     * @param aces
     * @throws ServiceException
     */
    public ZimbraACL(Set<ZimbraACE> aces) throws ServiceException {
        grantAccess(aces);
    }
    
    /**
     * copy ctor for cloning
     * 
     * @param other
     */
    private ZimbraACL(ZimbraACL other) {
        if (other.mAces != null) {
            mAces = new HashSet<ZimbraACE>();
            for (ZimbraACE ace : other.mAces)
                addACE(mAces, ace.clone());
        }
        
        if (other.mAuthuserAces != null) {
            mAuthuserAces = new HashSet<ZimbraACE>();
            for (ZimbraACE ace : other.mAuthuserAces)
                addACE(mAuthuserAces, ace.clone());
        }
        
        if (other.mPublicAces != null) {
            mPublicAces = new HashSet<ZimbraACE>();
            for (ZimbraACE ace : other.mPublicAces)
                addACE(mPublicAces, ace.clone());
        }
    }
    
    /**
     * returns a deep copy of the ZimbraACL object 
     */
    public ZimbraACL clone() {
        return new ZimbraACL(this);
    }

    private Set<ZimbraACE> aceSetByGranteeType(GranteeType granteeType, boolean createIfNotExist) {
        if (GranteeType.GT_AUTHUSER == granteeType) {
            if (mAuthuserAces == null && createIfNotExist)
                mAuthuserAces = new HashSet<ZimbraACE>();
            return mAuthuserAces;
        } else if (GranteeType.GT_PUBLIC == granteeType) {
            if (mPublicAces == null && createIfNotExist)
                mPublicAces = new HashSet<ZimbraACE>();
            return mPublicAces;
        } else {
            if (mAces == null && createIfNotExist)
                mAces = new HashSet<ZimbraACE>();
            return mAces;
        }
    }
    
    private void addACE(Set<ZimbraACE> aceSet, ZimbraACE aceToGrant) {
        aceSet.add(aceToGrant);
        if (mContainsRight == null)
            mContainsRight = new HashSet<Right>();
        mContainsRight.add(aceToGrant.getRight());
    }
    
    private void removeACE(Set<ZimbraACE> aceSet, ZimbraACE aceToRevoke) {
        aceSet.remove(aceToRevoke);
        // mContainsRight must not be null if we get here
        mContainsRight.remove(aceToRevoke.getRight());
    }
    
    /**
     * @param aceToGrant ace to revoke
     * @return whether aceToGrant was actually added to the ACL. 
     */
    private boolean grant(Set<ZimbraACE> aceSet, ZimbraACE aceToGrant) {
        for (ZimbraACE ace : aceSet) {
            if (ace.isGrantee(aceToGrant.getGrantee()) &&
                ace.getRight() == aceToGrant.getRight()) {
                
                boolean changed = false;
                
                // check if it's got a different allow/deny
                if (ace.deny() != aceToGrant.deny()) {
                    // the grantee had been granted the right, but was for a different allow/deny,
                    // set it to the new allow/deny and return "added"
                    ace.setDeny(aceToGrant.deny());
                    changed = true;
                }
                
                // check if it's got a different password/accesskey
                if (aceToGrant.getGranteeType() == GranteeType.GT_GUEST || aceToGrant.getGranteeType() == GranteeType.GT_KEY) {
                    String newSecret = aceToGrant.getSecret();
                    if (newSecret != null && !newSecret.equals(ace.getSecret())) {
                        ace.setSecret(newSecret);
                        changed = true;
                    }
                }
                
                return changed;
            }
        }
        
        // the right had not been granted to the grantee, add it
        if (aceToGrant.getGranteeType() == GranteeType.GT_KEY && aceToGrant.getSecret() == null)
            aceToGrant.setSecret(ACL.generateAccessKey());
        addACE(aceSet, aceToGrant);
        return true;
    }
    
    /**
     * @param aceToRevoke ace to revoke
     * @return whether aceToRevoke was actually removed from the ACL. 
     */
    private boolean revoke(Set<ZimbraACE> aceSet, ZimbraACE aceToRevoke) {
        if (aceSet == null)
            return false;
        
        for (ZimbraACE ace : aceSet) {
            if (ace.isGrantee(aceToRevoke.getGrantee()) &&
                ace.getRight() == aceToRevoke.getRight() &&
                ace.deny() == aceToRevoke.deny()) {
                removeACE(aceSet, ace);
                return true;
            }
        }
        return false;
    }
    
    boolean containsRight(Right right) {
        if (mContainsRight == null)
            return false;
        else    
            return mContainsRight.contains(right);
    }
    
    
    public Set<ZimbraACE> grantAccess(Set<ZimbraACE> acesToGrant) {
        Set<ZimbraACE> granted = new HashSet<ZimbraACE>();
        for (ZimbraACE ace : acesToGrant) {
            Set<ZimbraACE> aceSet = aceSetByGranteeType(ace.getGranteeType(), true);
            if (grant(aceSet, ace))
                granted.add(ace);
        }
        return granted; 
    }
    
    /**
     * Removes the right granted(allow or deny) to the specified id.  
     * If the right was not previously granted to the target, no error is 
     * thrown.
     * 
     * Note, the deny flag of aceToRevoke has to match the deny flag in the 
     * current grants, or else the ACE won't be removed.
     * 
     * e.g. if the current ACL has: user-A usr -invite
     *      and aceToRevoke has: user-A usr invite
     *      then the aceToRevoke will NOT be revoked, because it does not match.
     * 
     * @param aceToRevoke ace to revoke
     * @return the set of ZimbraACE that are successfully revoked.
     */
    public Set<ZimbraACE> revokeAccess(Set<ZimbraACE> acesToRevoke) {
        Set<ZimbraACE> revoked = new HashSet<ZimbraACE>();
        for (ZimbraACE ace : acesToRevoke) {
            Set<ZimbraACE> aceSet = aceSetByGranteeType(ace.getGranteeType(), false);
            if (revoke(aceSet, ace))
                revoked.add(ace);
        }
        return revoked;    
    }
    
    /**
     * Returns if grantee, as a Zimbra user, is allowed by the set of ACEs in acesByType.
     * Returns null if there isn't a match for grantee as a Zimbra user
     * 
     * @param acesByType
     * @param grantee
     * @param rightNeeded
     * @return
     * @throws ServiceException
     */
    private Boolean hasRightAsIndividual(Account grantee, Right rightNeeded) throws ServiceException {
        if (mAces == null)
            return null;
        
        Boolean result = null;
        for (ZimbraACE ace : mAces) {
            if (ace.getGranteeType() != GranteeType.GT_USER &&
                ace.getGranteeType() != GranteeType.GT_GUEST &&
                ace.getGranteeType() != GranteeType.GT_KEY)
                continue;
            
            if (ace.matches(grantee, rightNeeded)) {
                if (ace.deny())
                    return Boolean.FALSE;
                else
                    return Boolean.TRUE;
            }
        }
        return result;
    }
    
    private Boolean hasRightAsGroup(Account grantee, Right rightNeeded) throws ServiceException {
        if (mAces == null)
            return null;
        
        Provisioning prov = Provisioning.getInstance();
        
        // keep track of the most specific group in each unrelated trees that has a matched group
        Map<Integer, ZimbraACE> mostSpecificInMatchedTrees = null;  
        int key = 0;
        for (ZimbraACE ace : mAces) {
            if (ace.getGranteeType() != GranteeType.GT_GROUP)
                continue;
            
            if (!ace.matches(grantee, rightNeeded)) 
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
                    sLog.debug("hasRightAsGroup: grantee "+ grantee.getName() + " denied for right " + rightNeeded.getName() + " via ACE: " + a.dump());
                return Boolean.FALSE;
            }
        }
        
        // Okay, every group says yes, allow it.
        if (sLog.isDebugEnabled())
            sLog.debug("hasRightAsGroup: grantee "+ grantee.getName() + " allowed for right " + rightNeeded.getName() + " via ACE: " + dump(mostSpecificInMatchedTrees.values()));
        return Boolean.TRUE;
    }
    
    private Boolean hasRightAsAuthuser(Account grantee, Right rightNeeded) throws ServiceException {
        if (mAuthuserAces == null)
            return null;
        
        Boolean result = null;
        for (ZimbraACE ace : mAuthuserAces) {
            if (ace.matches(grantee, rightNeeded)) {
                if (ace.deny())
                    return Boolean.FALSE;
                else
                    return Boolean.TRUE;
            }
        }
        return result;
    }
    
    private Boolean hasRightAsPublic(Account grantee, Right rightNeeded) throws ServiceException {
        if (mPublicAces == null)
            return null;
        
        Boolean result = null;
        for (ZimbraACE ace : mPublicAces) {
            if (ace.matches(grantee, rightNeeded)) {
                if (ace.deny())
                    return Boolean.FALSE;
                else
                    return Boolean.TRUE;
            }
        }
        return result;
    }
    
    /**
     * Conflict resolution rules for multiple matches:
     * 
     * Rule 1. If multiple ACEs for a target apply to a grantee, the most specific ACE takes precedence.
     *         e.g. - ACL has: allow user A and deny group G
     *              - user A is in group G
     *              => user A will be allowed
     * 
     * Rule 2. If the grantee is in multiple matching groups in the ACL, the ACE for the most specific group takes precedence.
     *         e.g. - ALC has: allow group G1 and deny group G2
     *              - user A is in both group G1 and G2
     *              - group G1 is in Group G2
     *              => user A will be allowed
     *   
     * Rule 3. If multiple unrelated group ACEs conflict for a target, then denied take precedence over allowed and not granted.
     *         e.g. - ACL says: allow group G1 and deny group G2
     *              - user A is in both group G1 and G2; G1 is not a member of G2 and G2 is not a member of G1
     *              => user A will be denied.
     *                      
     * Rule 4. If multiple ACEs conflict for a target, this is a wrong setting and should not happen using 
     *         the supported granting/revoking interface: SOAP and zmmailbox.
     *         The result is unexpectedIf an ACL does contain such ACEs.  The result would be whichever ACE is seen first, and the 
     *         order is not guaranteed. 
     *         e.g. - ACL has: allow user A and deny user A
     *              => result is unexpected.
     * 
     * 
     * A more complete example - not realistic, but an example:
     * 
     * For ACL:
     *      acct-A             usr  rightR
     *      group-G1           grp -rightR
     *      group-G2           grp  rightR
     *      all-authed(000...) all -rightR
     *      the-public(999...) pub  rightR
     *      
     * and group membership:
     *      group-G1 has members acct-A and acct-B and acct-C
     *      group-G2 has members acct-A and acct-B and account-D and account-E
     *      group-G3 has members group G2 and account-E
     *      
     *                     G1(-R)                G3
     *                   / | \                  /  \
     *               A(R)  B  C                E   G2(R)
     *                                            / | | \
     *                                           A  B D  E
     *                                                 
     * then for rightR:
     *                         acct-A is allowed (via "acct-A             usr  rightR"  (rule 1)
     *                         acct-B is denied  (via "group-G1           grp -rightR"  (rule 3)
     *                         acct-C is denied  (via "group-G1           grp -rightR"  (single match)
     *                         acct-D is allowed (via "group-G2           grp  rightR"  (single match)
     *                         acct-E is allowed (via "group-G2           grp  rightR"  (rule 2)
     *                         acct-F is denied  (via "all-authed(000...) all -rightR"  (single match)
     *     external email foo@bar.com is allowed (via "the-public(999...) pub  rightR"  (single match)
     *       
     *
     * @param grantee
     * @param rightNeeded
     * @return
     * @throws ServiceException
     */
    boolean hasRight(Account grantee, Right rightNeeded) throws ServiceException {
        Boolean result = null;
        
        result = hasRightAsIndividual(grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRightAsGroup(grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRightAsAuthuser(grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRightAsPublic(grantee, rightNeeded);
        if (result != null)
            return result;
        
        // no match, return denied
        return false;
    }
    
    /**
     * Get all ACEs
     * @return all ACEs
     */
    Set<ZimbraACE> getAllACEs() {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        
        if (mAces != null) aces.addAll(mAces);
        if (mAuthuserAces != null) aces.addAll(mAuthuserAces);
        if (mPublicAces != null) aces.addAll(mPublicAces);
        
        return aces;
    }
    
    /**
     * Get ACEs with specified rights
     * 
     * @param rights specified rights.
     * @return ACEs with right specified in rights
     */
    Set<ZimbraACE> getACEs(Set<Right> rights) {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        
        if (mAces != null) {
            for (ZimbraACE ace : mAces) {
                if (rights.contains(ace.getRight()))
                    aces.add(ace);
            }
        }
        if (mAuthuserAces != null) {
            for (ZimbraACE ace : mAuthuserAces) {
                if (rights.contains(ace.getRight()))
                    aces.add(ace);
            }
        }
        if (mPublicAces != null) {
            for (ZimbraACE ace : mPublicAces) {
                if (rights.contains(ace.getRight()))
                    aces.add(ace);
            }
        }
        
        return aces;
    }
    
    /**
     * Get ACEs with the specified right into the result Set
     * 
     * @param right
     * @param result
     */
    void getACEs(Right right, Set<ZimbraACE> result) {
        if (mAces != null) {
            for (ZimbraACE ace : mAces) {
                if (right == ace.getRight())
                    result.add(ace);
            }
        }
        if (mAuthuserAces != null) {
            for (ZimbraACE ace : mAuthuserAces) {
                if (right == ace.getRight())
                    result.add(ace);
            }
        }
        if (mPublicAces != null) {
            for (ZimbraACE ace : mPublicAces) {
                if (right == ace.getRight())
                    result.add(ace);
            }
        }
    }
    
    public List<String> serialize() {
        List<String> aces = new ArrayList<String>();
        
        if (mAces != null) {
            for (ZimbraACE ace : mAces)
                aces.add(ace.serialize());
        }
        if (mAuthuserAces != null) {
            for (ZimbraACE ace : mAuthuserAces)
                aces.add(ace.serialize());
        }
        if (mPublicAces != null) {
            for (ZimbraACE ace : mPublicAces)
                aces.add(ace.serialize());
        }
        return aces;
    }
    
    /*
        debugging/logging methods
    */
    
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
        String[] aces = new String[] {
        "99999999-9999-9999-9999-999999999999 pub -invite",
        "f36fb465-c54b-4d1e-b2d5-5786078bf1d3 grp viewFreeBusy",
        "44d2b6b8-8001-4305-a9c0-419d04a44a9a usr -invite",
        "3b110e75-4003-4634-a3ec-fea456ad7d84 grp -invite"};
        // aces.add("00000000-0000-0000-0000-000000000000 ALL -invite");
        
        try {
            ZimbraACL acl = new ZimbraACL(aces, RightManager.getInstance());
            List<String> serialized = acl.serialize();
            for (String ace : serialized)
                System.out.println(ace);
        } catch (ServiceException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        
    }
    
}
