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
package com.zimbra.cs.ldap;

public enum LdapConnType {
    PLAIN,
    LDAPS,
    STARTTLS;
    
    public static LdapConnType getConnType(String urls, boolean wantStartTLS) {
        if (urls.toLowerCase().contains("ldaps://")) {
            return LDAPS;
        } else if (wantStartTLS) {
            return STARTTLS;
        } else {
            return PLAIN;
        }
    }

    /*
     * for external LDAP, only called from legacy code
     */
    public static boolean requireStartTLS(String[] urls, boolean wantStartTLS) {
        if (wantStartTLS) {
            for (String url : urls) {
                if (url.toLowerCase().contains("ldaps://"))
                    return false;
            }
            return true;
        }
        return false;
    }

}
