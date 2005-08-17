/*
 * Created on Sep 23, 2004
 *
 */
package com.zimbra.cs.account.ldap;

import javax.naming.directory.Attributes;


/**
 * And LdapEntry class that has a getId (liquidId attr) and the concept of a name that is unique within the liquid* objectClass.
 * 
 * @author schemers
 *
 */
public abstract class LdapNamedEntry extends LdapEntry implements Comparable {

    LdapNamedEntry(String dn, Attributes attrs) {
        super(dn, attrs);
    }
    
    public abstract String getId();
    
    public abstract String getName();

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof LdapNamedEntry))
            return 0;
        LdapNamedEntry other = (LdapNamedEntry) obj;
        return getName().compareTo(other.getName());
    }
}