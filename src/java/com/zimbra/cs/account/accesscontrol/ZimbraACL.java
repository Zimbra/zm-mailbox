package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;

public class ZimbraACL {
    
    // put ACEs in buckets by grantee type
    Set<ZimbraACE> mUSRGrants;
    Set<ZimbraACE> mGRPGrants;
    Set<ZimbraACE> mPUBGrants;
    
    /**
     * ctor for checking permissions, 
     * @param aces
     * @throws ServiceException
     */
    public ZimbraACL(String[] aces) throws ServiceException {
        for (String ace : aces) {
            ZimbraACE zACE = new ZimbraACE(ace);
            addGrant(zACE);
        }
    }
    
    public ZimbraACL(Set<ZimbraACE> aces) throws ServiceException {
        for (ZimbraACE ace : aces) {
            addGrant(ace);
        }
    }
    
    /*
     * add an ACE to the proper bucket
     */
    private void addGrant(ZimbraACE ace) throws ServiceException {
        switch (ace.getGranteeType()) {
        case GT_USER: 
            if (mUSRGrants == null) mUSRGrants = new HashSet<ZimbraACE>();
            mUSRGrants.add(ace);
            break;
        case GT_GROUP:
            if (mGRPGrants == null) mGRPGrants = new HashSet<ZimbraACE>();
            mGRPGrants.add(ace);
            break;
        case GT_PUBLIC:
            if (mPUBGrants == null) mPUBGrants = new HashSet<ZimbraACE>();
            mPUBGrants.add(ace);
            break;
        default: throw ServiceException.FAILURE("unknown ACL grantee type: " + ace.getGranteeType().name(), null);
        }
    }
    
    private Boolean hasRight(Set<ZimbraACE> grants, Account grantee, Right rightNeeded) throws ServiceException {
        if (grants == null)
            return null;
        
        Boolean result = null;
        for (ZimbraACE ace : grants) {
            if (ace.match(grantee, rightNeeded)) {
                // if denied, return denied
                if (ace.denied())
                    return Boolean.FALSE;
                else
                    result = Boolean.TRUE;  // allowed, don't return yet - still need to check if there are any denied
            }
        }
        
        return result;
    }
    
    private Boolean hasRightGRP(Account grantee, Right rightNeeded) throws ServiceException {
        if (mGRPGrants == null)
            return null;
        
        // Boolean result = null;
        DistributionList mostSpecificMatched = null; // keep tract of the most specific group that matches grantee
        Provisioning prov = Provisioning.getInstance();
        for (ZimbraACE ace : mGRPGrants) {
            if (ace.match(grantee, rightNeeded)) {
                DistributionList dl = prov.get(Provisioning.DistributionListBy.id, ace.getGranteeId());
                if (dl == null) {
                    ZimbraLog.account.warn("cannot find DL " + ace.getGranteeId() +  ", ACE " + ace.toString() + " ignored");
                    continue;
                }
                
                if (mostSpecificMatched == null) {
                    mostSpecificMatched = dl;
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
        
        return (mostSpecificMatched != null);
    }
    
    boolean hasRight(Account grantee, Right rightNeeded) throws ServiceException {
        Boolean result = null;
        
        result = hasRight(mUSRGrants, grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRightGRP(grantee, rightNeeded);
        if (result != null)
            return result;
        
        result = hasRight(mPUBGrants, grantee, rightNeeded);
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
        if (mUSRGrants != null) {
            for (ZimbraACE ace : mUSRGrants)
                if (rights == null || rights.contains(ace.getRight()))
                    aces.add(ace);
        }
        
        if (mGRPGrants != null) {
            for (ZimbraACE ace : mGRPGrants)
                if (rights == null || rights.contains(ace.getRight()))
                    aces.add(ace);
        }
        
        if (mPUBGrants != null) {
            for (ZimbraACE ace : mPUBGrants)
                if (rights == null || rights.contains(ace.getRight()))
                    aces.add(ace);
        }
        
        return aces;
    }
    
    void modifyACEs(Set<ZimbraACE> aces) throws ServiceException {
        // TODO
    }
    
    void revokeACEs(Set<ZimbraACE> acesToRevoke) throws ServiceException {
        Set<ZimbraACE> aces = null;
        for (ZimbraACE ace : acesToRevoke) {
            switch (ace.getGranteeType()) {
            case GT_USER: aces = mUSRGrants; break;
            case GT_GROUP: aces = mGRPGrants; break;
            case GT_PUBLIC: aces = mPUBGrants; break;     
            default: throw ServiceException.FAILURE("unknown ACL grantee type: " + ace.getGranteeType().name(), null);
            }
            
            revoke(aces, ace);
        }
    }
    
    /** Removes the right granted to the specified id.  If the right 
     *  was not previously granted to the target, no error is thrown.
     */
    private void revoke(Set<ZimbraACE> aces, ZimbraACE aceToRevoke) {
        for (ZimbraACE ace : aces) {
            if (ace.isGrantee(aceToRevoke.getGranteeId()) &&
                ace.getRight() == aceToRevoke.getRight() &&
                ace.denied() == aceToRevoke.denied()) {
                aces.remove(ace);
                return;
            }
        }
    }
    
    /**
     * serialize ACL to a set of Strings
     * 
     * @return A set of ACEs in Strin, null if the ACL is empty.
     */
    public Set<String> serialize() {
        Set<String> aces = new HashSet<String>();
        
        if (mUSRGrants != null) {
            for (ZimbraACE ace : mUSRGrants)
                aces.add(ace.serialize());
        }
        
        if (mGRPGrants != null) {
            for (ZimbraACE ace : mGRPGrants)
                aces.add(ace.serialize());
        }
         
        if (mPUBGrants != null) {
            for (ZimbraACE ace : mPUBGrants)
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
            Set<String> serialized = acl.serialize();
            for (String ace : serialized)
                System.out.println(ace);
        } catch (ServiceException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        
    }
    
}
