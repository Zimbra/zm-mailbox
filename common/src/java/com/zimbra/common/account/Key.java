/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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

    public static enum UCServiceBy {

        // case must match protocol
        id, name;

        public static UCServiceBy fromString(String s) throws ServiceException {
            try {
                return UCServiceBy.valueOf(s);
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
        id, name;

        public static CacheEntryBy fromString(String s) throws ServiceException {
            try {
                return CacheEntryBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
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

    public static enum ShareLocatorBy {

        id;

        public static ShareLocatorBy fromString(String s) throws ServiceException {
            try {
                return ShareLocatorBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum AlwaysOnClusterBy {

        id, name;

        public static AlwaysOnClusterBy fromString(String s) throws ServiceException {
            try {
                return AlwaysOnClusterBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }
}
