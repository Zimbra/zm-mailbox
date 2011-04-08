package com.zimbra.cs.ldap.unboundid;

import java.util.Iterator;
import java.util.List;

import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;

public class UBIDSearchResultEnumeration implements ZSearchResultEnumeration {

    SearchResult wrapped;
    Iterator<SearchResultEntry> entriesIter;
    
    UBIDSearchResultEnumeration(SearchResult searchResult) {
        wrapped = searchResult;
        entriesIter = wrapped.getSearchEntries().iterator();
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
