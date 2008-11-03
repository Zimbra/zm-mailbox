package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public enum GranteeType {
    
    GT_USER("usr", true),   // compare grantee ID with Account's zimbraId
    GT_GROUP("grp", true),  // compare grantee ID with Account's zimbraMemberOf values
    GT_AUTHUSER("all"),     // the caller needs to present a valid Zimbra credential
    GT_GUEST("gst"),        // the caller needs to present a non-Zimbra email address and password
    GT_KEY("key"),          // the caller needs to present an access key
    GT_PUBLIC("pub");       // always succeeds

    private static class GT {
        static Map<String, GranteeType> sCodeMap = new HashMap<String, GranteeType>();
    }
    
    private String mCode;
    private boolean mAllowedForAdminRights;
    
    GranteeType(String code) {
        mCode = code;
        GT.sCodeMap.put(code, this);
    }
    
    GranteeType(String code, boolean allowedForAdminRights) {
        mCode = code;
        GT.sCodeMap.put(code, this);
        mAllowedForAdminRights = allowedForAdminRights;
    }
    
    public static GranteeType fromCode(String granteeType) throws ServiceException {
        // GUEST-TODO master control for turning off guest grantee for now
        if (granteeType.equals(GT_GUEST.getCode()))
            throw ServiceException.FAILURE("guest grantee not yet supported", null);
        
        GranteeType gt = GT.sCodeMap.get(granteeType);
        if (gt == null)
            throw ServiceException.PARSE_ERROR("invalid grantee type: " + granteeType, null);
        
        return gt;
    }
    
    /**
     * - code stored in the ACE.
     * - code appear in XML
     * - code displayed by CLI
     * 
     * @return 
     */
    public String getCode() {
        return mCode;    
    }

    public boolean allowedForAdminRights() {
        return mAllowedForAdminRights;
    }

}
