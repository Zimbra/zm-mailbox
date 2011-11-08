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
package com.zimbra.cs.account.accesscontrol;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.entry.LdapEntry;

public class DiscoverRights {
    Account acct;
    Set<Right> rights;
    
    DiscoverRights(Account credentials, Set<Right> rights) {
        this.acct = credentials;
        this.rights = Sets.newHashSet(rights);
    }
    
    /*
     * Discover grants that are granted on the designated target type for the 
     * specified rights.  Note: grants granted on other targets are not searched/returned.
     * 
     * e.g. for an account right, returns grants that are granted on account entries that 
     *      are applicable to the account.  Grants granted on DL, group, domain, and global 
     *      are NOT returned.
     */
    Map<Right, Set<Entry>> handle() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        // collect target types for requested rights
        Set<TargetType> targetTypesToSearch = Sets.newHashSet();
        for (Right right : rights) {
            targetTypesToSearch.add(right.getTargetType());
        }
        
        SearchGrants search = new SearchGrants(prov, targetTypesToSearch, acct, rights);
        Set<SearchGrants.GrantsOnTarget> searchResults = search.doSearch().getResults();
        
        Map<Right, Set<Entry>> result = Maps.newHashMap();

        for (SearchGrants.GrantsOnTarget grants : searchResults) {
            Entry targetEntry = grants.getTargetEntry();
            ZimbraACL acl = grants.getAcl();
            
            for (ZimbraACE ace : acl.getAllACEs()) {
                Right right = ace.getRight();
                
                if (rights.contains(right) && !isSameEntry(targetEntry, acct)) {
                    // include the entry only if it is the designated target type for the right
                    TargetType targetTypeForRight = right.getTargetType();
                    TargetType taregtTypeOfEntry = TargetType.getTargetType(targetEntry);
                    
                    if (targetTypeForRight.equals(taregtTypeOfEntry)) {
                        Set<Entry> entries = result.get(right);
                        if (entries == null) {
                            entries = Sets.newHashSet();
                            result.put(right, entries);
                        }
                        entries.add(targetEntry);
                    }
                }
            }
        }
        return result;
    }
    
    private boolean isSameEntry(Entry entry1, Entry entry2) throws ServiceException {
        if ((entry1 instanceof LdapEntry) && (entry2 instanceof LdapEntry)) {
            String entry1DN = ((LdapEntry) entry1).getDN();
            String entry2DN = ((LdapEntry) entry2).getDN();
            return (entry1DN != null && entry2DN != null && entry1DN.equals(entry2DN));
        } else {
            throw ServiceException.FAILURE("internal server error - not LdapEntry", null);
        }
    }
}
