package com.zimbra.cs.ldap.jndi;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;

public class JNDISearchResultEnumeration implements ZSearchResultEnumeration {

    // wrapped JNDI NamingEnumeration<SearchResult>
    private NamingEnumeration<SearchResult> wrapped;
    
    JNDISearchResultEnumeration(NamingEnumeration<SearchResult> searchResult) {
        wrapped = searchResult;
    }
    
    @TODO
    private LdapException mapToLdapException(NamingException namingException) {
        LdapTODO.TODO("go through all callsistes to come up with a mapping that maps NamingException to corresponding NamingException");
        return null;
    }
    
    @Override
    public void close() throws LdapException {
        try {
            wrapped.close();
        } catch (NamingException e) {
            throw mapToLdapException(e);
        }
        
    }

    @Override
    public boolean hasMore() throws LdapException {
        boolean result;
        try {
            result = wrapped.hasMore();
        } catch (NamingException e) {
            throw mapToLdapException(e);
            
        }
        return result;
    }

    @Override
    public ZSearchResultEntry next() throws LdapException {
        JNDISearchResultEntry result;
        try {
            SearchResult searchResult = wrapped.next();
            result = new JNDISearchResultEntry(searchResult);
        } catch (NamingException e) {
            throw mapToLdapException(e);
        }
        return result;
    }

}
