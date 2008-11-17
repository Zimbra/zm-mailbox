package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.account.accesscontrol.Right.RightType;

public class ZimbraACL {
    private static final Log sLog = LogFactory.getLog(ZimbraACL.class);

    // usr, grp, gst, key aces
    List<ZimbraACE> mAces = new ArrayList<ZimbraACE>();

    // for the containsRight call
    Set<Right> mContainsRight = new HashSet<Right>();
    
    /**
     * ctor for loading from LDAP
     * 
     * It is assumed that the ACL in LDAP is correctly built and saved.
     * For efficiency reason, this ctor does NOT check for duplicate/conflict ACEs, 
     * it just loads all ACEs.
     * 
     * @param aces
     * @throws ServiceException
     */
    ZimbraACL(String[] aces, RightManager rm) throws ServiceException {
        for (String aceStr : aces) {
            ZimbraACE ace = new ZimbraACE(aceStr, rm);
            addACE(ace);
        }
    }
    
    /**
     * ctor for granting initial ACL (when there is currently no ACL on an entry)
     * 
     * This ctor DOES check for duplicate/conflict(allow and deny a right to the same grantee).
     * 
     * @param aces
     * @throws ServiceException
     */
    ZimbraACL(Set<ZimbraACE> aces) throws ServiceException {
        grantAccess(aces);
    }
    
    /**
     * copy ctor for cloning
     * 
     * @param other
     */
    private ZimbraACL(ZimbraACL other) {
        if (other.mAces != null) {
            for (ZimbraACE ace : other.mAces)
                addACE(ace.clone());
        }
    }
    
    /**
     * returns a deep copy of the ZimbraACL object 
     */
    public ZimbraACL clone() {
        return new ZimbraACL(this);
    }
    
    /**
     * Negative grants are inserted in the front, positive grants are inserted in the rear. 
     * Rights granted with SOAP/zmprov should not have conflicts (the granting code 
     * verifies that).  But people can do direct LDAP modify.  The arrangement in 
     * order (put negative ones in the front) is a safety net for those situations.
     * The ACL checking code in RightChecker relies on this order to deny conflict grants.
     * 
     * e.g.
     *      ba512b78-6f5b-4192-a160-77ae28896c68 usr invite
     *      ba512b78-6f5b-4192-a160-77ae28896c68 usr -invite
     * 
     * @param aceToGrant
     */
    private void addACE(ZimbraACE aceToGrant) {
        if (aceToGrant.deny())
            mAces.add(0, aceToGrant);
        else
            mAces.add(aceToGrant);
        mContainsRight.add(aceToGrant.getRight());
    }
    
    private void removeACE(ZimbraACE aceToRevoke) {
        mAces.remove(aceToRevoke);
        mContainsRight.remove(aceToRevoke.getRight());
    }
    
    /**
     * @param aceToGrant ace to revoke
     * @return whether aceToGrant was actually added to the ACL. 
     */
    private boolean grant(ZimbraACE aceToGrant) {
        for (ZimbraACE ace : mAces) {
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
        addACE(aceToGrant);
        return true;
    }
    
    /**
     * @param aceToRevoke ace to revoke
     * @return whether aceToRevoke was actually removed from the ACL. 
     */
    private boolean revoke(ZimbraACE aceToRevoke) {
        for (ZimbraACE ace : mAces) {
            if (ace.isGrantee(aceToRevoke.getGrantee()) &&
                ace.getRight() == aceToRevoke.getRight() &&
                ace.deny() == aceToRevoke.deny()) {
                removeACE(ace);
                return true;
            }
        }
        return false;
    }
    
    boolean containsRight(Right right) {
        return mContainsRight.contains(right);
    }
    
    
    Set<ZimbraACE> grantAccess(Set<ZimbraACE> acesToGrant) {
        Set<ZimbraACE> granted = new HashSet<ZimbraACE>();
        for (ZimbraACE ace : acesToGrant) {
            if (grant(ace))
                granted.add(ace);
        }
        return granted; 
    }
    
    /**
     * Removes the specified grant(allow or deny).  
     * If the specified grant was not previously granted on the target, 
     * no error is thrown.
     * 
     * Note, the deny flag of aceToRevoke has to match the deny flag in the 
     * current grants, or else the ACE won't be matched/removed.
     * 
     * e.g. if the current ACL has: user-A usr -invite
     *      and aceToRevoke has: user-A usr invite
     *      then the aceToRevoke will NOT be revoked, because it does not match.
     * 
     * @param aceToRevoke ace to revoke
     * @return the set of ZimbraACE that are successfully revoked.
     */
    Set<ZimbraACE> revokeAccess(Set<ZimbraACE> acesToRevoke) {
        Set<ZimbraACE> revoked = new HashSet<ZimbraACE>();
        for (ZimbraACE ace : acesToRevoke) {
            if (revoke(ace))
                revoked.add(ace);
        }
        return revoked;    
    }
    
    /**
     * Get all ACEs
     * @return all ACEs
     */
    Set<ZimbraACE> getAllACEs() {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.addAll(mAces);
        return aces;
    }
    
    /**
     * Get ACEs with specified rights
     * 
     * @param rights specified rights.
     * @return ACEs with right specified in rights
     */
    Set<ZimbraACE> getACEs(Set<Right> rights) {
        Set<ZimbraACE> result = new HashSet<ZimbraACE>();
        
        for (ZimbraACE ace : mAces) {
            if (rights.contains(ace.getRight()))
                result.add(ace);
        }

        return result;
    }
    
    List<ZimbraACE> getACEsByGranteeId(Set<String> granteeIds, TargetType targetType) {
        List<ZimbraACE> result = new ArrayList<ZimbraACE>();
        
        for (ZimbraACE ace : mAces) {
            if (granteeIds.contains(ace.getGrantee())) {
                Right rightGranted = ace.getRight();
                
                if (rightGranted.isComboRight()) {
                    // always pass in needing get, because get will get us both get and set
                    expandComboRightForAttrRights(ace, RightType.getAttrs, targetType, result);
                    
                    // also, add the combo right itself
                    result.add(ace);
                    
                } else if (rightGranted.isAttrRight()) {
                    AttrRight attrRight = (AttrRight)rightGranted; 
                    
                    // setAttrs imply getAttrs on the same set of attrs
                    // unlike getACEsByAttrRight, we don't check get/set here.
                    // here we need both get/setAttrs rights
                    /*
                    if (!attrRight.applicableToRightType(rightTypeNeeded))
                        continue;
                    */
                    
                    if (!rightGranted.applicableOnTargetType(targetType))
                        continue;
                    
                    result.add(ace);
                } else {
                    // preset right
                    result.add(ace);
                }
            }
        }

        return result;
    }
    
    /**
     * Returns a List of ACEs with the specified preset right.
     * 
     * Negative grants are in the front, positive grants are in the rear of the 
     * returned List. 
     * 
     * @param right
     * @param result 
     */
    List<ZimbraACE> getACEsByPresetRight(Right right) {
        List<ZimbraACE> result = new ArrayList<ZimbraACE>();
        
        for (ZimbraACE ace : mAces) {
            if (ace.getRight().isComboRight()) {
                ComboRight comboRight = (ComboRight)ace.getRight();
                if (comboRight.containsPresetRight(right))
                    result.add(ace);
            } else if (right == ace.getRight()) {
                result.add(ace);
            }
        }

        return result;
    }
    
    private void expandComboRightForAttrRights(ZimbraACE ace, RightType rightTypeNeeded, TargetType targetType, List<ZimbraACE> result) {
        Right rightGranted = ace.getRight();
        
        ComboRight comboRight = (ComboRight)rightGranted;
        Set<AttrRight> attrRights = comboRight.getAttrRights();
        
        for (AttrRight attrRight : attrRights) {
            // setAttrs imply getAttrs on the same set of attrs
            if (!attrRight.applicableToRightType(rightTypeNeeded))
                continue;
            
            if (!rightGranted.applicableOnTargetType(targetType))
                continue;
            
            // create a pseudo ZimbraACE that represent this expanded right
            ZimbraACE pseudoACE = ace.clone();
            pseudoACE.setRight(attrRight);
            result.add(pseudoACE);
        }
    }
    
    /**
     * 
     * For get/setAttrs rights
     * 
     * @param rightNeeded  AdminRight.RT_PSEUDO_GET_ATTRS or AdminRight.RT_PSEUDO_SET_ATTRS
     * @param targetType   target type of the actual enrty this operation is to be performed
     * @param attrs
     * @return
     */
    List<ZimbraACE> getACEsByAttrRight(Right rightNeeded, TargetType targetType) {
        List<ZimbraACE> result = new ArrayList<ZimbraACE>();
        
        RightType rightTypeNeeded = rightNeeded.getRightType();
        
        for (ZimbraACE ace : mAces) {
            Right rightGranted = ace.getRight();
            
            if (rightGranted.isComboRight()) {
                expandComboRightForAttrRights(ace, rightTypeNeeded, targetType, result);
                
            } else if (rightGranted.isAttrRight()) {
                AttrRight attrRight = (AttrRight)rightGranted; 
                
                // setAttrs imply getAttrs on the same set of attrs
                if (!attrRight.applicableToRightType(rightTypeNeeded))
                    continue;
                
                if (!rightGranted.applicableOnTargetType(targetType))
                    continue;

                result.add(ace);
                
            } else
                continue;

        }

        return result;
    }
    
    List<String> serialize() {
        List<String> aces = new ArrayList<String>();
        
        for (ZimbraACE ace : mAces)
            aces.add(ace.serialize());
        
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
