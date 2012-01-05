/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
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
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;

public enum GranteeType {

    // Need to keep JAXB class com.zimbra.soap.type.GranteeType in sync with this class
    GT_USER("usr",      com.zimbra.soap.type.GranteeType.usr, (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_IS_ZIMBRA_ENTRY)),
    GT_GROUP("grp",     com.zimbra.soap.type.GranteeType.grp, (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_GROUP      | GranteeFlag.F_IS_ZIMBRA_ENTRY)),
    GT_EXT_GROUP("egp", com.zimbra.soap.type.GranteeType.egp, (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_GROUP)),
    GT_AUTHUSER("all",  com.zimbra.soap.type.GranteeType.all, (short)(                      GranteeFlag.F_AUTHUSER)),
    GT_DOMAIN("dom",    com.zimbra.soap.type.GranteeType.dom, (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_DOMAIN     | GranteeFlag.F_IS_ZIMBRA_ENTRY)),  // only for the admin crossDomainAdmin right and user rights
    GT_GUEST("gst",     com.zimbra.soap.type.GranteeType.gst, (short)(                      GranteeFlag.F_INDIVIDUAL                                  | GranteeFlag.F_HAS_SECRET)),
    GT_KEY("key",       com.zimbra.soap.type.GranteeType.key, (short)(                      GranteeFlag.F_INDIVIDUAL                                  | GranteeFlag.F_HAS_SECRET)),
    GT_PUBLIC("pub",    com.zimbra.soap.type.GranteeType.pub, (short)(                      GranteeFlag.F_PUBLIC));

    private static class GT {
        static Map<String, GranteeType> sCodeMap = new HashMap<String, GranteeType>();
    }
    
    private String mCode;
    private short mFlags;
    private com.zimbra.soap.type.GranteeType jaxbGranteeType;
        
    GranteeType(String code, com.zimbra.soap.type.GranteeType jaxbGT, short flags) {
        mCode = code;
        GT.sCodeMap.put(code, this);
        mFlags = flags;
        jaxbGranteeType = jaxbGT;
    }
    
    public static GranteeType fromCode(String granteeType) throws ServiceException {
        GranteeType gt = GT.sCodeMap.get(granteeType);
        if (gt == null)
            throw ServiceException.PARSE_ERROR("invalid grantee type: " + granteeType, null);
        
        return gt;
    }

    /* return equivalent JAXB enum */
    public com.zimbra.soap.type.GranteeType toJaxb() {
        return jaxbGranteeType;
    }

    public static GranteeType fromJaxb(com.zimbra.soap.type.GranteeType jaxbGT) {
        for (GranteeType gt :GranteeType.values()) {
            if (gt.toJaxb() == jaxbGT) {
                return gt;
            }
        }
        throw new IllegalArgumentException("Unrecognised GranteeType:" + jaxbGT);
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

    /*
     * whether the grantee type is allowed for amin rights
     */
    public boolean allowedForAdminRights() {
        return hasFlags(GranteeFlag.F_ADMIN);
    }
    
    /*
     * whether this grantee type can take a secret
     */
    public boolean allowSecret() {
        return hasFlags(GranteeFlag.F_HAS_SECRET);
    }
    
    public boolean isZimbraEntry() {
        return hasFlags(GranteeFlag.F_IS_ZIMBRA_ENTRY);
    }
    
    public boolean needsGranteeIdentity() {
        return !(GT_AUTHUSER == this || GT_PUBLIC == this);
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
            Key.GranteeBy granteeBy, String grantee) throws ServiceException {
        NamedEntry granteeEntry = null;
        
        switch (granteeType) {
        case GT_USER:
            granteeEntry = prov.get(AccountBy.fromString(granteeBy.name()), grantee);
            if (granteeEntry == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(grantee); 
            }
            break;
        case GT_GROUP:
            granteeEntry = prov.getGroupBasic(Key.DistributionListBy.fromString(granteeBy.name()), grantee);
            if (granteeEntry == null) {
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(grantee); 
            }
            break;
        case GT_DOMAIN:
            granteeEntry = prov.get(Key.DomainBy.fromString(granteeBy.name()), grantee);
            if (granteeEntry == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(grantee);
            }
            break;
        default:
            throw ServiceException.INVALID_REQUEST("invallid grantee type for lookupGrantee:" + 
                    granteeType.getCode(), null);
        }
    
        return granteeEntry;
    }

}
