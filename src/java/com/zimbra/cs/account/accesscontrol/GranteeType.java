package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;

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
    
    /**
     * central place where a grantee should be loaded
     * 
     * @param prov
     * @param granteeType
     * @param granteeBy
     * @param grantee
     * @return
     * @throws ServiceException
     */
    public static NamedEntry lookupGrantee(Provisioning prov, GranteeType granteeType, GranteeBy granteeBy, String grantee) throws ServiceException {
        NamedEntry granteeEntry = null;
        
        switch (granteeType) {
        case GT_USER:
            granteeEntry = prov.get(AccountBy.fromString(granteeBy.name()), grantee);
            if (granteeEntry == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(grantee); 
            break;
        case GT_GROUP:
            granteeEntry = prov.getAclGroup(DistributionListBy.fromString(granteeBy.name()), grantee);
            if (granteeEntry == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(grantee); 
            break;
        default:
            ServiceException.INVALID_REQUEST("invallid grantee type for lookupGrantee:" + granteeType.getCode(), null);
        }
    
        return granteeEntry;
    }

}
