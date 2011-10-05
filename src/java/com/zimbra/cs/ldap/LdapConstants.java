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

public class LdapConstants {

    public static final String LDAP_TRUE  = "TRUE";
    public static final String LDAP_FALSE = "FALSE";
    public static final String EARLIEST_SYNC_TOKEN = "19700101000000Z";

    public static final String DN_ROOT_DSE = "";
    public static final String ATTR_DN = "dn";
    public static final String ATTR_OBJECTCLASS = "objectClass";
    public static final String ATTR_CREATE_TIMESTAMP = "createTimestamp";
    
    // AD attrs
    public static final String ATTR_MEMBER_OF = "memberOf";
    
    // milli seconds to wait for checking LDAP server health
    public static final int CHECK_LDAP_SLEEP_MILLIS = 5000;
    
}
