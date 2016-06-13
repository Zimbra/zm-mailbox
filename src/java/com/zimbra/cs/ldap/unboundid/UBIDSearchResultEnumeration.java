/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
