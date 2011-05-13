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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class LdapUtil {
    
    public static String formatMultipleMatchedEntries(ZSearchResultEntry first, ZSearchResultEnumeration rest) 
    throws LdapException {
        StringBuffer dups = new StringBuffer();
        dups.append("[" + first.getDN() + "] ");
        while (rest.hasMore()) {
            ZSearchResultEntry dup = rest.next();
            dups.append("[" + dup.getDN() + "] ");
        }
        
        return new String(dups);
    }
    
    @TODO  // support ZLdapFilter
    public static void searchLdapOnMaster(String base, String query, String[] returnAttrs, 
            SearchLdapVisitor visitor) throws ServiceException {
        searchZimbraLdap(base, query, returnAttrs, true, visitor);
    }

    @TODO // support ZLdapFilter
    public static void searchLdapOnReplica(String base, String query, String[] returnAttrs, 
            SearchLdapVisitor visitor) throws ServiceException {
        searchZimbraLdap(base, query, returnAttrs, false, visitor);
    }
      
    private static void searchZimbraLdap(String base, String query, String[] returnAttrs, 
            boolean useMaster, SearchLdapVisitor visitor) throws ServiceException {
        
        SearchLdapOptions searchOptions = new SearchLdapOptions(base, query, 
                returnAttrs, SearchLdapOptions.SIZE_UNLIMITED, null, 
                ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.get(useMaster));
            zlc.searchPaged(searchOptions);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    


}

