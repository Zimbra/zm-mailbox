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

public class LdapConstants {

    public static final String LDAP_TRUE  = "TRUE";
    public static final String LDAP_FALSE = "FALSE";
    public static final String EARLIEST_SYNC_TOKEN = "19700101000000Z";

    public static final String DN_ROOT_DSE = "";
    public static final String ATTR_dn = "dn";
    public static final String ATTR_dc = "dc";
    public static final String ATTR_uid = "uid";
    public static final String ATTR_ou = "ou";
    public static final String ATTR_cn = "cn";
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
    public static final String PEOPLE = "people";
    public static final String TOP = "top";
    public static final String PERSON = "person";
    public static final String ORGANIZATIONAL_PERSON = "organizationalPerson";
}
