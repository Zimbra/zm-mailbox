package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.ACL.GuestAccount;

public class ZimbraACE {
    
    // for serialization
    private static final String S_DELIMITER = " ";
    private static final char S_DENY = '-';
    
    /* 
     * usr: zimbraId of the entry being granted rights
     * grp: zimbraId of the entry being granted rights
     * all: The pseudo-GUID GUID_AUTHUSER signifying "all authenticated users"
     * pub: The pseudo-GUID GUID_PUBLIC signifying "all authenticated and unauthenticated users"
     * gst: email address of the guest being granted rights
     */
    private String mGrantee;
    
    // The type of object the grantee's ID refers to.
    private GranteeType mGranteeType;
    
    // The right being granted.
    private Right mRight;
        
    // if the right is specifically denied
    private boolean mDeny;
    
    // The password for guest accounts.
    private String mPassword;

    
    /*
     * Construct a ZimbraACE from its serialized string form.
     * 
     * ACEs format:
     * <grantee id> [{<encoded grantee password for guest grantee>}] <grantee type> <right>
     */
    ZimbraACE(String ace, RightManager rm) throws ServiceException {
        String[] parts = ace.split(S_DELIMITER);
        if (parts.length != 3 && parts.length != 4)
            throw ServiceException.PARSE_ERROR("bad ACE: " + ace, null);
        
        String right;
        if (parts.length == 3) {
            mGranteeType = GranteeType.fromCode(parts[1]);
            right = parts[2];
        } else {
            mGranteeType = GranteeType.fromCode(parts[2]);
            mPassword = decryptPassword(parts[1]);
            right = parts[3];
        }
        
        mGrantee = parts[0];
        if (mGranteeType != GranteeType.GT_GUEST) {
            if (!Provisioning.isUUID(mGrantee))
                throw ServiceException.PARSE_ERROR("grantee ID" + mGrantee + " is not a UUID", null);
        }
        
        if (right.charAt(0) == S_DENY) {
            mDeny = true;
            mRight = rm.getRight(right.substring(1));
        } else {
            mDeny = false;
            mRight = rm.getRight(right);
        }
    }
    
    /**
     * copy ctor for cloning
     * 
     * @param other
     */
    private ZimbraACE(ZimbraACE other) {
        mGrantee = new String(other.mGrantee);
        mGranteeType = other.mGranteeType;
        mRight = other.mRight;
        mDeny = other.mDeny;
        if (other.mPassword != null)
            mPassword = new String(other.mPassword);
    }
    
    /**
     * returns a deep copy of the ZimbraACE object 
     */
    public ZimbraACE clone() {
        return new ZimbraACE(this);
    }
    
    private String decryptPassword(String encryptedPassword) throws ServiceException {
        throw ServiceException.FAILURE("not yet suported", null);
        // TODO
    }
    
    private String encryptPassword(String clearPassword) {
        // throw ServiceException.FAILURE("not yet suported", null);
        return "{" + clearPassword + "}";   //TODO
    }
    
    public ZimbraACE(String granteeId, GranteeType granteeType, Right right, boolean deny) throws ServiceException {
        mGrantee = granteeId;
        mGranteeType = granteeType;
        mDeny = deny;
        mRight = right;
        mPassword = null;  // TODO
    }
    
    // for unit test and tools to construct a ZimbraACE with non guest grantee
    public ZimbraACE(NamedEntry grantee, Right right, boolean deny) throws ServiceException {
        mGrantee = grantee.getId();
        
        if (ACL.GUID_PUBLIC.equals(mGrantee))
            mGranteeType = GranteeType.GT_PUBLIC;
        else if (ACL.GUID_AUTHUSER.equals(mGrantee))
            mGranteeType = GranteeType.GT_AUTHUSER;
        else {
            if (grantee instanceof Account)
                mGranteeType = GranteeType.GT_USER;
            else if (grantee instanceof DistributionList)
                mGranteeType = GranteeType.GT_GROUP;
            else
                throw ServiceException.FAILURE("invalid grantee type", null);
        }
        mDeny = deny;
        mRight = right;
        mPassword = null;  // TODO
    }
    
    /** Returns whether the principal id exactly matches the grantee.
     *  <tt>principalId</tt> must be {@link ACL#GUID_PUBLIC} (<tt>null</tt>
     *  is also OK) if the actual grantee is {@link ACL#GRANTEE_PUBLIC}.
     *  <tt>principalId</tt> must be {@link ACL#GUID_AUTHUSER} if the actual
     *  grantee is {@link ACL#GRANTEE_AUTHUSER}.
     *  
     * @param zimbraId  The zimbraId of the principal. */
    // orig: ACL.Grant.isGrantee
    public boolean isGrantee(String principalId) {
        if (principalId == null || principalId.equals(ACL.GUID_PUBLIC))
            return (mGranteeType == GranteeType.GT_PUBLIC);
        else if (principalId.equals(ACL.GUID_AUTHUSER))
            return (mGranteeType == GranteeType.GT_AUTHUSER);
        return principalId.equals(mGrantee);
    }
    
    public String getGrantee() {
        return mGrantee;
    }
    
    public GranteeType getGranteeType() {
        return mGranteeType;
    }
    
    public Right getRight() {
        return mRight;
    }
    
    public boolean deny() {
        return mDeny;
    }
    
    public String getPassword() {
        return mPassword;
    }
    
    void setDeny(boolean deny) {
        mDeny = deny;
    }
    
    /** Returns whether this grant applies to the given {@link Account}.
     *  If <tt>acct</tt> is <tt>null</tt>, only return
     *  <tt>true</tt> if the grantee is {@link ACL#GRANTEE_PUBLIC}. */
    // orig: ACL.Grant.matches
    private boolean matches(Account acct) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (acct == null)
            return mGranteeType == GranteeType.GT_PUBLIC;
        switch (mGranteeType) {
            case GT_PUBLIC:   return true;
            case GT_AUTHUSER: return !(acct instanceof ACL.GuestAccount); // return !acct.equals(ACL.ANONYMOUS_ACCT);
            case GT_GROUP:    return prov.inDistributionList(acct, mGrantee);
            case GT_USER:     return mGrantee.equals(acct.getId());
            case GT_GUEST:    return matchesGuestAccount(acct);
            default:  throw ServiceException.FAILURE("unknown ACL grantee type: " + mGranteeType, null);
        }
    }

    // orig: ACL.Grant.matchesGuestAccount
    private boolean matchesGuestAccount(Account acct) {
        if (!(acct instanceof GuestAccount))
            return false;
        return ((GuestAccount) acct).matches(mGrantee, mPassword);
    }
    
    boolean matches(Account grantee, Right rightNeeded) throws ServiceException {
        if (rightNeeded == mRight)
            return matches(grantee);
        return false;
    }
    
    public String getGranteeDisplayName() {
        try {
            switch (mGranteeType) {
            case GT_USER: 
                Account acct = Provisioning.getInstance().get(AccountBy.id, mGrantee);
                if (acct != null)
                    return acct.getName();
            case GT_GROUP:
                DistributionList group = Provisioning.getInstance().get(DistributionListBy.id, mGrantee);
                if (group != null)
                    return group.getName();
            case GT_GUEST:
                return mGrantee;
            case GT_AUTHUSER:
            case GT_PUBLIC:
            default:
                return null;
            }
        } catch (ServiceException e) {
            return null;
        }
    }
    
    // serialize to the format for storing in LDAP
    public String serialize() {
        StringBuffer sb = new StringBuffer();
        sb.append(mGrantee + S_DELIMITER);
        if (mGranteeType == GranteeType.GT_GUEST)
            sb.append(encryptPassword(mPassword) + S_DELIMITER);
        sb.append(getGranteeType().getCode() + S_DELIMITER);
        if (mDeny)
            sb.append(S_DENY);
        sb.append(getRight().getCode());
        return sb.toString();
    }
    
    // for logging, debugging
    public String dump() {
        return "[" + serialize() + "]";
    }


}
