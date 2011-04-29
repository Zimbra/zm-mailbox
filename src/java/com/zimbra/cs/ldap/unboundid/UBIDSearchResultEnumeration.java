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
package com.zimbra.cs.ldap.unboundid;

import java.util.Iterator;

import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;

public class UBIDSearchResultEnumeration implements ZSearchResultEnumeration {

    SearchResult searchResult;
    Iterator<SearchResultEntry> entriesIter;
    
    UBIDSearchResultEnumeration(SearchResult searchResult) {
        this.searchResult = searchResult;
        this.entriesIter = searchResult.getSearchEntries().iterator();
    }
    
    @Override
    public void close() throws LdapException {
        // DO nothing
    }

    @Override
    public boolean hasMore() throws LdapException {
        return entriesIter.hasNext();
    }

    @Override
    public ZSearchResultEntry next() throws LdapException {
        SearchResultEntry searchResultEntry = entriesIter.next();
        return new UBIDSearchResultEntry(searchResultEntry);
    }

}
