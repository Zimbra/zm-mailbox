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