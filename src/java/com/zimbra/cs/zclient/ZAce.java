package com.zimbra.cs.zclient;

import java.util.Arrays;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

public class ZAce {

    private String mGranteeName;
    private String mGranteeId;
    private ZAce.GranteeType mGranteeType;
    private String mRight;
    private boolean mDeny;
    private String mArgs; // for password

    /** Stolen shamelessly from ACL.java. */
    /** The pseudo-GUID signifying "all authenticated users". */
    public static final String GUID_AUTHUSER = "00000000-0000-0000-0000-000000000000";
    /** The pseudo-GUID signifying "all authenticated and unauthenticated users". */
    public static final String GUID_PUBLIC   = "99999999-9999-9999-9999-999999999999";

    public enum GranteeType {
        /**
         * access is granted to an authenticated user
         */
        usr, 
        /**
         * access is granted to a group of users
         */
        grp,
        /**
         * accesss is granted to public. no authentication needed.
         */
        pub,
        /**
         * access is granted to all authenticated users
         */
        all, 
        /**
         * access is granted to a non-Zimbra email address and a password 
         */
        gst;

        public static GranteeType fromString(String s) throws ServiceException {
            try {
                // GUEST-TODO master control for turning off guest grantee for now
                if (gst.name().equals(s))
                    throw ZClientException.CLIENT_ERROR("guest grantee not yet supported", null);
                return GranteeType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid grantee: "+s+", valid values: "+Arrays.asList(GranteeType.values()), e);
            }
        }
    }

    public ZAce(Element e) throws ServiceException {
        mRight = e.getAttribute(MailConstants.A_RIGHT);
        mDeny = e.getAttributeBool(MailConstants.A_DENY, false);
        mGranteeName = e.getAttribute(MailConstants.A_DISPLAY, null);
        mGranteeId = e.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        mGranteeType = GranteeType.fromString(e.getAttribute(MailConstants.A_GRANT_TYPE));
        mArgs = e.getAttribute(MailConstants.A_PASSWORD, null);
    }
    
    public ZAce(ZAce.GranteeType granteeType, String granteeId, String granteeName, String right, boolean deny, String args) throws ServiceException {
        mRight = right;
        mDeny = deny;
        mGranteeName = granteeName;
        mGranteeId = granteeId;
        mGranteeType = granteeType;
        mArgs = args;
    }

    public void toElement(Element parent) {
        Element ace = parent.addElement(MailConstants.E_ACE);
        ace.addAttribute(MailConstants.A_RIGHT, mRight);
        ace.addAttribute(MailConstants.A_GRANT_TYPE, mGranteeType.name());
        ace.addAttribute(MailConstants.A_ZIMBRA_ID, mGranteeId);
        ace.addAttribute(MailConstants.A_DISPLAY, mGranteeName);
        ace.addAttribute(MailConstants.A_PASSWORD, mArgs);
        if (mDeny)
            ace.addAttribute(MailConstants.A_DENY, mDeny);
    }
    
    public String getRight() {
        return mRight;
    }
    
    public boolean getDeny() {
        return mDeny;
    }
    
    public String getRightDisplay() {
        return (mDeny? "-" : "") + mRight;
    }
    
    /**
     * the type of grantee: 
     * "usr", 
     * "grp",
     * "all" (all authenticated users),
     * "guest" (non-Zimbra email address and password)
     * "pub" (public authenticated and unauthenticated access), 
     */
    public ZAce.GranteeType getGranteeType() {
        return mGranteeType;
    }
    
    /***
     * the display name (*not* the zimbra id) of the principal being granted rights;
     * optional if {grantee-type} is "all"
     */
    public String getGranteeName() {
        if (mGranteeName == null)
            return "";
        else
            return mGranteeName;
    }

    /***
     * the zimbraId of the granteee
     */
    public String getGranteeId() {
        return mGranteeId;                                                                          
    }
    
    public void setGranteeId(String granteeId) {
        mGranteeId = granteeId;
    }
    
    /**
     *  optional argument.  password when {grantee-type} is "guest"
     */
    public String getArgs() {
        return mArgs;
    }

    /**
     * Is this grant a public grant?
     *
     */
    public boolean isPublic() {
        return this.getGranteeType().equals(ZAce.GranteeType.pub);
    }
    
    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("type", mGranteeType.name());
        sb.add("name", mGranteeName);
        sb.add("id", mGranteeId);
        sb.add("right", mRight);
        if (mDeny)
            sb.add("deny", mDeny);
        sb.endStruct();
        return sb.toString();
    }
    

    public String getGranteeTypeDisplay() {
        switch (mGranteeType) {
        case usr: return "account";
        case grp: return "group";
        case pub: return "public";
        case all: return "all";
        case gst: return "guest";
        default: return "unknown";
        }
    }
    
    public int  getGranteeTypeSortOrder() {
        switch (mGranteeType) {
        case usr: return 0;
        case grp: return 1;
        case pub: return 4;
        case all: return 3;
        case gst: return 2;
        default: return 5; // ??
        }
    }
    
    public static ZAce.GranteeType getGranteeTypeFromDisplay(String name) throws ServiceException {
        if (name.equalsIgnoreCase("account")) return GranteeType.usr;
        else if (name.equalsIgnoreCase("group")) return GranteeType.grp; 
        else if (name.equalsIgnoreCase("public")) return GranteeType.pub;
        else if (name.equalsIgnoreCase("all")) return GranteeType.all;
        // else if (name.equalsIgnoreCase("guest")) return GranteeType.gst;  // GUEST-TODO master control for turning off guest grantee for now  
        else throw ZClientException.CLIENT_ERROR("unknown grantee type: "+name, null);
    }
}
