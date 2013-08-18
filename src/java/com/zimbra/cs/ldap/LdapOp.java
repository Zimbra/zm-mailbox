/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
/**
 * 
 */
package com.zimbra.cs.ldap;

public enum LdapOp {
    GET_CONN("get a connection from a connection pool"),  
    REL_CONN("release a connection back to the connection pool"),
    OPEN_CONN("open a new connection"),
    CLOSE_CONN("close a connection"),
    CREATE_ENTRY("create an entry"),
    DELETE_ENTRY("delete entry"),
    GET_ENTRY("get entry"),
    GET_SCHEMA("get schema"),
    MODIFY_DN("modify DN"),
    MODIFY_ATTRS("modify attributes"),
    SEARCH("search"),
    TEST_AND_MODIFY_ATTRS("test and modify attributes");
    
    private String desc;
    
    private LdapOp(String desc) {
        this.desc = desc;
    }
}