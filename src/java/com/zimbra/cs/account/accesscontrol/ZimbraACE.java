package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.mailbox.ACL;

public class ZimbraACE {
    
    private static final String DELIMITER = " ";
    private static final char DENY = '-';
    
    String mGranteeId;
    GranteeType mGranteeType;
    Right mRight;
    boolean mDenied;
    
    ZimbraACE(String ace) throws ServiceException {
        String[] parts = ace.split(DELIMITER);
        if (parts.length != 3)
            throw ServiceException.PARSE_ERROR("ACE must contain 3 parts", null);
        
        mGranteeId = parts[0];
        if (!Provisioning.isUUID(mGranteeId))
            throw ServiceException.PARSE_ERROR(mGranteeId + " is not a UUID", null);
        
        mGranteeType = GranteeType.fromCode(parts[1]);
        
        if (parts[2].charAt(0) == DENY) {
            mDenied = true;
            mRight = Right.fromCode(parts[2].substring(1));
        } else {
            mDenied = false;
            mRight = Right.fromCode(parts[2]);
        }
    }
    
    public ZimbraACE(String granteeId, GranteeType granteeType, Right right, boolean deny) throws ServiceException {
        mGranteeId = granteeId;
        mGranteeType = granteeType;
        mDenied = deny;
        mRight = right;
    }
    
    // for unit test and tools to construct an ACE with USR grantee
    public ZimbraACE(Account grantee, Right right, boolean deny) throws ServiceException {
        mGranteeId = grantee.getId();
        if (ACL.GUID_PUBLIC.equals(mGranteeId))
            mGranteeType = GranteeType.GT_PUBLIC;
        else
            mGranteeType = GranteeType.GT_USER;
        mDenied = deny;
        mRight = right;
    }
    
    public ZimbraACE(DistributionList grantee, Right right, boolean deny) throws ServiceException {
        mGranteeId = grantee.getId();
        if (ACL.GUID_PUBLIC.equals(mGranteeId))
            throw ServiceException.FAILURE("bad GUID for distribution list", null);
        else
            mGranteeType = GranteeType.GT_GROUP;
        mDenied = deny;
        mRight = right;
    }
    
    /** Returns whether the principal id exactly matches the grantee.
     *  <tt>zimbraId</tt> must be {@link ACL#GUID_PUBLIC} (<tt>null</tt>
     *  is also OK) if the actual grantee is {@link ACL#GRANTEE_PUBLIC}.
     *  <tt>zimbraId</tt> must be {@link ACL#GUID_AUTHUSER} if the actual
     *  grantee is {@link ACL#GRANTEE_AUTHUSER}.
     * 
     * @param zimbraId  The zimbraId of the principal. 
     */
    boolean isGrantee(String zimbraId) {
        if (zimbraId == null || zimbraId.equals(ACL.GUID_PUBLIC))
            return (mGranteeType == GranteeType.GT_PUBLIC);
        // else if (zimbraId.equals(GUID_AUTHUSER))
        //     return (mType == GRANTEE_AUTHUSER);
        return zimbraId.equals(mGranteeId);
    }
    
    public String getGranteeId() {
        return mGranteeId;
    }
    
    public GranteeType getGranteeType() {
        return mGranteeType;
    }
    
    public Right getRight() {
        return mRight;
    }
    
    public boolean denied() {
        return mDenied;
    }
    
    private boolean match(Account grantee) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        switch (mGranteeType) {
        case GT_USER: return mGranteeId.equals(grantee.getId());
        case GT_GROUP: return prov.inDistributionList(grantee, mGranteeId);
        case GT_PUBLIC: return true;
        default: throw ServiceException.FAILURE("unknown ACL grantee type: " + mGranteeType.name(), null);
        }
    }
    
    boolean match(Account grantee, Right rightNeeded) throws ServiceException {
        if (rightNeeded == mRight)
            return match(grantee);
        return false;
    }
    
    public String getGranteeDisplayName() {
        try {
            switch (mGranteeType) {
            case GT_USER: 
                Account acct = Provisioning.getInstance().get(AccountBy.id, mGranteeId);
                if (acct != null)
                    return acct.getName();
            case GT_GROUP:
                DistributionList group = Provisioning.getInstance().get(DistributionListBy.id, mGranteeId);
                if (group != null)
                    return group.getName();
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
        sb.append(getGranteeId() + DELIMITER);
        sb.append(getGranteeType().getCode() + DELIMITER);
        if (denied())
            sb.append(DENY);
        sb.append(getRight().getCode());
        return sb.toString();
    }
    
    // for logging, debugging
    public String toString() {
        return serialize();
    }


}
