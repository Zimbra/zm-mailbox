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

    public final static String LDAP_TRUE  = "TRUE";
    public final static String LDAP_FALSE = "FALSE";
    public final static String EARLIEST_SYNC_TOKEN = "19700101000000Z";

    // 
    // object classes and attributes used in the com.zimbra.cs.ldap package
    //
    // We don't want to use Provisioning.A_** constants in the 
    // in the com.zimbra.cs.ldap package because it should not have any 
    // dependency on the account package.
    public final static String DN_ROOT_DSE = "";
    public final static String ATTR_DN = "dn";
    public final static String ATTR_OBJECTCLASS = "objectClass";
    
}
