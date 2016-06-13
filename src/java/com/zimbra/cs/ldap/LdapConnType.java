/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.ldap;

public enum LdapConnType {
    PLAIN,
    LDAPS,
    STARTTLS,
    LDAPI;
    
    private static final String LDAPI_SCHEME = "ldapi";
    private static final String LDAPS_SCHEME = "ldaps";
    private static final String LDAPI_URL = LDAPI_SCHEME + "://";
    private static final String LDAPS_URL = LDAPS_SCHEME + "://";
    
    public static LdapConnType getConnType(String urls, boolean wantStartTLS) {
        if (urls.toLowerCase().contains(LDAPI_URL)) {
            return LDAPI;
        } else if (urls.toLowerCase().contains(LDAPS_URL)) {
            return LDAPS;
        } else if (wantStartTLS) {
            return STARTTLS;
        } else {
            return PLAIN;
        }
    }

    /*
     * for external LDAP, only called from legacy external GAL code
     */
    public static boolean requireStartTLS(String[] urls, boolean wantStartTLS) {
        if (wantStartTLS) {
            for (String url : urls) {
                if (url.toLowerCase().contains(LDAPS_URL))
                    return false;
            }
            return true;
        }
        return false;
    }
    
    public static boolean isLDAPI(String scheme) {
        return LDAPI_SCHEME.equalsIgnoreCase(scheme);
    }

}
