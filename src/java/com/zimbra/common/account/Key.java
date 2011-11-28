/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.common.account;

import com.zimbra.common.service.ServiceException;

/**
 * Keys used to look up Provisioning objects.
 */
public class Key {

    public static enum AccountBy {

        // case must match protocol
        adminName, appAdminName, id, foreignPrincipal, name, krb5Principal;

        public static AccountBy fromString(String s) throws ServiceException {
            try {
                return AccountBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    // data sources
    public static enum DataSourceBy {
    
        id, name;
    
        public static DataSourceBy fromString(String s) throws ServiceException {
            try {
                return DataSourceBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    // identities
    public static enum IdentityBy {
    
        id, name;
    
        public static IdentityBy fromString(String s) throws ServiceException {
            try {
                return IdentityBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum DomainBy {
    
        // case must match protocol
        id, name, virtualHostname, krb5Realm, foreignName;
    
        public static DomainBy fromString(String s) throws ServiceException {
            try {
                return DomainBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum ServerBy {
    
        // case must match protocol
        id, name, serviceHostname;
    
        public static ServerBy fromString(String s) throws ServiceException {
            try {
                return ServerBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum ZimletBy {
    
        // case must match protocol
        id, name;
    
        public static ZimletBy fromString(String s) throws ServiceException {
            try {
                return ZimletBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    
    }

    // signatures
    public static enum SignatureBy {
    
        id, name;
    
        public static SignatureBy fromString(String s) throws ServiceException {
            try {
                return SignatureBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum CacheEntryBy {
    
        // case must match protocol
        id, name
    }

    public static enum CosBy {
    
        // case must match protocol
        id, name;
    
        public static CosBy fromString(String s) throws ServiceException {
            try {
                return CosBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum GranteeBy {
    
        // case must match protocol
        id, name;
    
        public static GranteeBy fromString(String s) throws ServiceException {
            try {
                return GranteeBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum CalendarResourceBy {
    
        // case must match protocol
        id, foreignPrincipal, name;
    
        public static CalendarResourceBy fromString(String s) throws ServiceException {
            try {
                return CalendarResourceBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum XMPPComponentBy {
    
        // case must match protocol
        id, name, serviceHostname;
    
        public static XMPPComponentBy fromString(String s) throws ServiceException {
            try {
                return XMPPComponentBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum DistributionListBy {
    
        // case must match protocol
        id, name;
    
        public static DistributionListBy fromString(String s) throws ServiceException {
            try {
                return DistributionListBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }
}
