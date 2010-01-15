/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;

public enum GranteeType {

    GT_USER("usr",     (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_INDIVIDUAL)),
    GT_GROUP("grp",    (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_GROUP)),
    GT_AUTHUSER("all", GranteeFlag.F_AUTHUSER),
    GT_DOMAIN("dom",   GranteeFlag.F_ADMIN),  // only for the crossDomainAdmin right
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
        return ((flags & mFlags) == flags);
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
    public static NamedEntry lookupGrantee(Provisioning prov, GranteeType granteeType, 
            GranteeBy granteeBy, String grantee) throws ServiceException {
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
        case GT_DOMAIN:
            granteeEntry = prov.get(DomainBy.fromString(granteeBy.name()), grantee);
            if (granteeEntry == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(grantee); 
            break;
        default:
            ServiceException.INVALID_REQUEST("invallid grantee type for lookupGrantee:" + 
                    granteeType.getCode(), null);
        }
    
        return granteeEntry;
    }

}
