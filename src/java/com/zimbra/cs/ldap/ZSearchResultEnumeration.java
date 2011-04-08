package com.zimbra.cs.ldap;

/*
 * migration path for javax.naming.NamingEnumeration interface
 */
public interface ZSearchResultEnumeration {
    public ZSearchResultEntry next() throws LdapException;
    public boolean hasMore() throws LdapException;
    public void close() throws LdapException;
}

