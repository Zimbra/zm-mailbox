package com.zimbra.cs.gal;

public interface GalSearchQueryCallback {

    /**
     * 
     * @return extra query to be ANDed with the query for GAL sync account search
     */
    public String getMailboxSearchQuery();
    
    /**
     * 
     * @return extra query to be ANDed with the query for Zimbra GAL LDAP search
     */
    public String getZimbraLdapSearchQuery();
}
