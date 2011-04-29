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
    private NamingEnumeration<SearchResult> searchResult;
    
    JNDISearchResultEnumeration(NamingEnumeration<SearchResult> searchResult) {
        this.searchResult = searchResult;
    }
    
    @TODO
    private LdapException mapToLdapException(NamingException namingException) {
        LdapTODO.TODO("go through all callsistes to come up with a mapping that maps NamingException to corresponding NamingException");
        return null;
    }
    
    @Override
    public void close() throws LdapException {
        try {
            searchResult.close();
        } catch (NamingException e) {
            throw mapToLdapException(e);
        }
        
    }

    @Override
    public boolean hasMore() throws LdapException {
        boolean result;
        try {
            result = searchResult.hasMore();
        } catch (NamingException e) {
            throw mapToLdapException(e);
            
        }
        return result;
    }

    @Override
    public ZSearchResultEntry next() throws LdapException {
        JNDISearchResultEntry result;
        try {
            SearchResult sr = searchResult.next();
            result = new JNDISearchResultEntry(sr);
        } catch (NamingException e) {
            throw mapToLdapException(e);
        }
        return result;
    }

}
