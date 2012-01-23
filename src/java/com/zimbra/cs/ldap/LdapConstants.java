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
    public static final String ATTR_dn = "dn";
    public static final String ATTR_dc = "dc";
    public static final String ATTR_objectClass = "objectClass";
    public static final String ATTR_createTimestamp = "createTimestamp";
    public static final String ATTR_hasSubordinates = "hasSubordinates";
    
    // AD attrs
    public static final String ATTR_memberOf = "memberOf";
    
    // milli seconds to wait for checking LDAP server health
    public static final int CHECK_LDAP_SLEEP_MILLIS = 5000;
    
    public static final String DN_SUBTREE_MATCH_ATTR = "entryDN";
    public static final String DN_SUBTREE_MATCH_MATCHING_RULE = "dnSubtreeMatch";
    public static final String DN_SUBTREE_MATCH_FILTER_TEMPLATE = 
            "(" + DN_SUBTREE_MATCH_ATTR + ":" + DN_SUBTREE_MATCH_MATCHING_RULE + ":=%s)";
    
    
    public static final String FILTER_TYPE_EQUAL = "=";
    public static final String FILTER_TYPE_GREATER_OR_EQUAL = ">=";
    public static final String FILTER_TYPE_LESS_OR_EQUAL = "<=";
    public static final String FILTER_VALUE_ANY = "*";
    
    
    public static final String OC_dcObject = "dcObject";
}
