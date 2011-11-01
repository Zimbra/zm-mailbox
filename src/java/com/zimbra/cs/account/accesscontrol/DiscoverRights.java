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

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class DiscoverRights {
    Account acct;
    Set<Right> rights;
    
    DiscoverRights(Account credentials, Set<Right> rights) {
        this.acct = credentials;
        this.rights = Sets.newHashSet(rights);
    }
    
    Map<Right, Set<Entry>> handle() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        SearchGrants search = new SearchGrants(prov, EnumSet.of(TargetType.account),
                acct, rights);
        Set<SearchGrants.GrantsOnTarget> searchResults = search.doSearch().getResults();
        
        Map<Right, Set<Entry>> result = Maps.newHashMap();

        for (SearchGrants.GrantsOnTarget grants : searchResults) {
            Entry targetEntry = grants.getTargetEntry();
            ZimbraACL acl = grants.getAcl();
            
            for (ZimbraACE ace : acl.getAllACEs()) {
                Right right = ace.getRight();
                
                if (rights.contains(right)) {
                    Set<Entry> entries = result.get(right);
                    if (entries == null) {
                        entries = Sets.newHashSet();
                        result.put(right, entries);
                    }
                    entries.add(targetEntry);
                }
            }
        }
        return result;
    }
}
