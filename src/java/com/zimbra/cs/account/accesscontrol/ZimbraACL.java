package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;

public class ZimbraACL {
    
    // non-public, non-authuser aces
    Set<ZimbraACE> mAces;
        
    // authuer aces
    Set<ZimbraACE> mAuthuserAces;
    
    // public aces
    Set<ZimbraACE> mPublicAces;

    
    /**
     * ctor for deserializing from LDAP
     * 
     * It is assumed that the ACL in LDAP is correctly built and saved.
     * For perf reason, this ctor does not check for duplicate/conflict ACEs, 
     * it just loads all ACEs.
     * 
     * @param aces
     * @throws ServiceException
     */
    public ZimbraACL(String[] aces) throws ServiceException {
        for (String aceStr : aces) {
            ZimbraACE ace = new ZimbraACE(aceStr);
            Set<ZimbraACE> aceSet = aceSetByGranteeType(ace.getGranteeType(), true);
            aceSet.add(ace);
        }
    }
    
    /**
     * ctor for modifying ACEs
     * 
     * This ctor DOES check for duplicate/conflict(allow and deny a right to the same grantee).
     * 
     * @param aces
     * @throws ServiceException
     */
    public ZimbraACL(Set<ZimbraACE> aces) throws ServiceException {
        grantAccess(aces);
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
    
    /**
     * @param aceToGrant ace to revoke
     * @return whether aceToGrant was actually added to the ACL. 
     */
    private boolean grant(Set<ZimbraACE> aceSet, ZimbraACE aceToGrant) {
        for (ZimbraACE ace : aceSet) {
            if (ace.isGrantee(aceToGrant.getGrantee()) &&
                ace.getRight() == aceToGrant.getRight()) {
                if (ace.deny() == aceToGrant.deny()) {
                    // already has this ACE, return "not added"
                    return false;
                } else {
                    // the grantee had been granted the right, but was for a different allow/deny,
                    // set it to the new allow/deny and return "added"
                    ace.setDeny(aceToGrant.deny());
                    return true;
                }
            }
        }
        
        // the right had not been granted to the grantee, add it
        aceSet.add(aceToGrant);
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
                aceSet.remove(ace);
                return true;
            }
        }
        return false;
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
    private Boolean hasRightAsUser(Account grantee, Right rightNeeded) throws ServiceException {
        if (mAces == null)
            return null;
        
        Boolean result = null;
        for (ZimbraACE ace : mAces) {
            if (ace.getGranteeType() == GranteeType.GT_USER) {
                if (ace.matches(grantee, rightNeeded)) {
                    if (ace.deny())
                        return Boolean.FALSE;
                    else
                        return Boolean.TRUE;
                }
            }
        }
        return result;
    }
    
    private Boolean hasRightAsGroup(Account grantee, Right rightNeeded) throws ServiceException {
        if (mAces == null)
            return null;
        
        // Boolean result = null;
        DistributionList mostSpecificMatched = null; // keep tract of the most specific group that matches grantee
        Provisioning prov = Provisioning.getInstance();
        for (ZimbraACE ace : mAces) {
            if (ace.matches(grantee, rightNeeded)) {
                DistributionList dl = prov.get(Provisioning.DistributionListBy.id, ace.getGrantee());
                if (dl == null) {
                    ZimbraLog.account.warn("cannot find DL " + ace.getGrantee() +  ", ACE " + ace.toString() + " ignored");
                    continue;
                }
                
                if (mostSpecificMatched == null) {
                    mostSpecificMatched = dl;
                    
                    // TODO
                    if (ace.deny())
                        return Boolean.FALSE;
                    else
                        return Boolean.TRUE;
                    
                } else {
                    /*
                     * TODO: need to take care of matching DLs that are not members of one another,
                     *       and see of any of them are specifically denied.  If so, the grant should 
                     *       be denied.
                     *       
                    if (prov.inDistributionList(dl, mostSpecificMatched.getId()))
                        // found a more specific group
                        mostSpecificMatched = dl;
                    else if (!prov.inDistributionList(mostSpecificMatched, dl.getId())) {
                        // 
                    }
                    */
                }
            }
        }
        
        return null;
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
    
    
    private Boolean hasRightAsGuest(Account grantee, Right rightNeeded) throws ServiceException {
        return null;  //TODO
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
    
    boolean hasRight(Account grantee, Right rightNeeded) throws ServiceException {
        Boolean result = null;
        
        result = hasRightAsUser(grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRightAsGroup(grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRightAsAuthuser(grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRightAsGuest(grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRightAsPublic(grantee, rightNeeded);
        if (result != null)
            return result;
        
        // no match, return denied
        return false;
    }
    
    /**
     * Get ACEs with specified rights
     * 
     * @param rights specified rights.  If null, all ACEs in the ACL will be returned.
     * @return ACEs with right specified in rights
     */
    Set<ZimbraACE> getACEs(Set<Right> rights) {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        
        if (rights == null) {
            if (mAces != null) aces.addAll(mAces);
            if (mAuthuserAces != null) aces.addAll(mAuthuserAces);
            if (mPublicAces != null) aces.addAll(mPublicAces);
        } else {
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
        }
        
        return aces;
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
            ZimbraACL acl = new ZimbraACL(aces);
            List<String> serialized = acl.serialize();
            for (String ace : serialized)
                System.out.println(ace);
        } catch (ServiceException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        
    }
    
}
