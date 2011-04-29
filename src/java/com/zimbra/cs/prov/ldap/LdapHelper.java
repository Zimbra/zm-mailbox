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
package com.zimbra.cs.prov.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.LdapException.LdapMultipleEntriesMatchedException;

public abstract class LdapHelper {
    
    private LdapProv ldapProv;
    
    protected LdapHelper(LdapProv ldapProv) {
        this.ldapProv = ldapProv;
    }
    
    protected LdapProv getProv() {
        return ldapProv;
    }
    
    public abstract void searchLdap(ILdapContext ldapContext, SearchLdapOptions searchOptions) 
    throws ServiceException;
    
    /**
     * Processes a search operation with the provided information. 
     * It is expected that at most one entry will be returned from the search
     *
     * @param base            search base
     * @param query           search query
     * @param initZlc         initial ZLdapContext
     *                        - if null, a new one will be created to be used for the search, 
     *                          and then closed
     *                        - if not null, it will be used for the search, this API will 
     *                          *not* close it, it is the responsibility of callsite to close 
     *                          it when it i no longer needed.
     * @param loadFromMaster  if initZlc is null, whether to do the search on LDAP master.
     * @return                a ZSearchResultEnumeration is an entry is found
     *                        null if the search does not find any matching entry.
     *
     * @throws                LdapMultipleEntriesMatchedException if more than one entries is 
     *                        matched.
     *                        ServiceException for other error.
     *                        
     */
    public abstract ZSearchResultEntry searchForEntry(String base, String query, 
            ZLdapContext initZlc, boolean useMaster) 
    throws LdapMultipleEntriesMatchedException, ServiceException;
    
}
