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

    GT_USER("usr",     (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_INDIVIDUAL)),
    GT_GROUP("grp",    (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_GROUP)),
    GT_AUTHUSER("all", GranteeFlag.F_AUTHUSER),
    GT_GUEST("gst",    GranteeFlag.F_INDIVIDUAL),
    GT_KEY("key",      GranteeFlag.F_INDIVIDUAL),
    GT_PUBLIC("pub",   GranteeFlag.F_PUBLIC);

    private static class GT {
        static Map<String, GranteeType> sCodeMap = new HashMap<String, GranteeType>();
    }
    
    private String mCode;
    private short mFlags;
        
    GranteeType(String code, short flags) {
        mCode = code;
        GT.sCodeMap.put(code, this);
        mFlags = flags;
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
        return hasFlags(GranteeFlag.F_ADMIN);
    }
    
    public boolean hasFlags(short flags) {
        return ((flags & mFlags) != 0);
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
