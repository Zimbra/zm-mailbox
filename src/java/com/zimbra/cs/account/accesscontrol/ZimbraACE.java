package com.zimbra.cs.account.accesscontrol;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.ACL.GuestAccount;

public class ZimbraACE {
    
    // for serialization
    
    // delimits {grantee} {grantee type} {right}
    private static final char S_DELIMITER = ' ';
    
    // delimits {external email}:{password} for guest grantee and {external email/name}:{accesskey} for key grantee
    private static final String S_SECRET_DELIMITER = ":";
    
    // delimits -{right}  (negative right)
    private static final char S_DENY = '-';
    
    /* 
     * usr: zimbraId of the entry being granted rights
     * grp: zimbraId of the entry being granted rights
     * gst: email address of the guest being granted rights
     * all: The pseudo-GUID GUID_AUTHUSER signifying "all authenticated users"
     * pub: The pseudo-GUID GUID_PUBLIC signifying "all authenticated and unauthenticated users"
     */
    private String mGrantee;
    
    // The type of object the grantee's ID refers to.
    private GranteeType mGranteeType;
    
    // The right being granted.
    private Right mRight;
        
    // if the right is specifically denied
    private boolean mDeny;
    
    // password for guest grantee, accesskey for key grantee
    private String mSecret;

    
    /*
     * Construct a ZimbraACE from its serialized string form.
     * 
    ACEs format:
    
    {grantee} {grantee-type} [-]{right}

        grantee: 
                grantee type    stored 
                -----------------------------------------------------
                usr             zimbraId of the account
                grp             zimbraId of the distribution list
                gst             {grantee email}:{password}
                key             {grantee email (or just a name)}:{access key}
                all             pseudo id 00000000-0000-0000-0000-000000000000
                pub             pseudo id 99999999-9999-9999-9999-999999999999
                
                grantee name for key grantees, password, and access key(if provided by user) can have 
                spaces, they are enclosed in {}.  {} are not allowed for them.
                 
        grantee-type: usr | grp | gst | key | all | pub
        
        right: one of the supported right.
               if a '-' (minus sign) is prepended to the right, it means the right is 
               specifically denied.
                        
    e.g. fe0e1a88-e6e3-4fe1-b608-3ab6ce50351f grp -viewFreeBusy
         fd6227f2-87e6-4453-9ccc-16853a6f8d27 usr viewFreeBusy
         foo@bar.com:apple tree key viewFreeBusy
         foo bar:8d159aed5fb9431d8ac52db5e20baafb key viewFreeBusy
         foo bar:ocean blue key viewFreeBusy
         00000000-0000-0000-0000-000000000000 all viewFreeBusy
         99999999-9999-9999-9999-999999999999 pub invite     
    */
    private String[] getParts(String ace) throws ServiceException {
        int p3 = ace.lastIndexOf(S_DELIMITER);
        if (p3 == -1)
            throw ServiceException.PARSE_ERROR("bad ACE: " + ace, null);
        
        int p2 = ace.lastIndexOf(S_DELIMITER, p3-1);
        if (p2 == -1)
            throw ServiceException.PARSE_ERROR("bad ACE: " + ace, null);
        
        String[] parts = new String[3];
        parts[0] = ace.substring(0, p2);
        parts[1] = ace.substring(p2+1, p3);
        parts[2] = ace.substring(p3+1);
        return parts;
    }
    
    ZimbraACE(String ace, RightManager rm) throws ServiceException {
        String[] parts = getParts(ace);
        
        String grantee;
        String right;
        
        grantee = parts[0];
        mGranteeType = GranteeType.fromCode(parts[1]);
        right = parts[2];
        
        switch (mGranteeType) {
        case GT_USER:
        case GT_GROUP:
        case GT_AUTHUSER:
        case GT_PUBLIC: 
            if (!Provisioning.isUUID(grantee))
                throw ServiceException.PARSE_ERROR("grantee ID [" + grantee + "] is not a UUID", null);
            mGrantee = grantee;
            break;
        case GT_GUEST:
        case GT_KEY:
            String[] externalParts = grantee.split(S_SECRET_DELIMITER);
            if (externalParts.length != 2)
                throw ServiceException.PARSE_ERROR("bad ACE(gurst/key grantee must have two sub parts): " + ace, null);
            mGrantee = decodeGrantee(externalParts[0]);
            mSecret = decodeSecret(externalParts[1]);
            break;
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
        if (other.mSecret != null)
            mSecret = new String(other.mSecret);
    }
    
    /**
     * return a deep copy of the ZimbraACE object 
     */
    public ZimbraACE clone() {
        return new ZimbraACE(this);
    }
    
    // no encoding for now
    private String encodeGrantee(String granteeName) {
        return granteeName;
    }
    
    // no encoding for now
    private String decodeGrantee(String granteeName) throws ServiceException {
        return granteeName;   
    }
    
    // no encoding for now
    private String encodeSecret(String secret) {
        return secret;
    }
    
    // no encoding for now
    private String decodeSecret(String secret) throws ServiceException {
        return secret;
    }
    
    public ZimbraACE(String granteeId, GranteeType granteeType, Right right, boolean deny, String secret) throws ServiceException {
        mGrantee = granteeId;
        mGranteeType = granteeType;
        mDeny = deny;
        mRight = right;
        mSecret = secret;
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
    
    public String getSecret() {
        return mSecret;
    }
    
    public void setSecret(String secret) {
        mSecret = secret;
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
            case GT_KEY:      return matchesAccessKey(acct);
            default:  throw ServiceException.FAILURE("unknown ACL grantee type: " + mGranteeType, null);
        }
    }

    private boolean matchesGuestAccount(Account acct) {
        if (!(acct instanceof GuestAccount))
            return false;
        return ((GuestAccount) acct).matches(mGrantee, mSecret);
    }
    
    private boolean matchesAccessKey(Account acct) {
        if (!(acct instanceof GuestAccount))
            return false;
        return ((GuestAccount) acct).matchesAccessKey(mGrantee, mSecret);
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
            case GT_KEY:    
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
        
        // grantee
        if (mGranteeType == GranteeType.GT_GUEST || mGranteeType == GranteeType.GT_KEY)
            sb.append(encodeGrantee(mGrantee) + S_SECRET_DELIMITER + encodeSecret(mSecret) + S_DELIMITER);
        else
            sb.append(mGrantee + S_DELIMITER);
        
        // grantee type
        sb.append(getGranteeType().getCode() + S_DELIMITER);
        
        // right
        if (mDeny)
            sb.append(S_DENY);
        sb.append(getRight().getCode());
        
        return sb.toString();
    }
    
    // for logging, debugging
    public String dump() {
        return "[" + serialize() + "]";
    }

    public static void validate(ZimbraACE ace) throws ServiceException {
        if (ace.mGranteeType == GranteeType.GT_GUEST || ace.mGranteeType == GranteeType.GT_KEY) {
            if (ace.getGrantee().contains(S_SECRET_DELIMITER))
                throw ServiceException.INVALID_REQUEST("grantee cannot contain:" + S_SECRET_DELIMITER, null);
            if (ace.getSecret() != null && ace.getSecret().contains(S_SECRET_DELIMITER))
                throw ServiceException.INVALID_REQUEST("password/accesskey cannot contain:" + S_SECRET_DELIMITER, null);
        }
    }

}
