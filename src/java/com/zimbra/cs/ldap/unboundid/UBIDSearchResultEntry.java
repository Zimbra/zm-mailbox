package com.zimbra.cs.ldap.unboundid;

import com.unboundid.ldap.sdk.SearchResultEntry;

import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZSearchResultEntry;

/**
 * Represents one LDAP entry in a search result.
 *
 */
public class UBIDSearchResultEntry extends ZSearchResultEntry  {

    private SearchResultEntry wrapped;
    private UBIDAttributes zAttributes;
    
    UBIDSearchResultEntry(SearchResultEntry searchResultEntry) {
        wrapped = searchResultEntry;
        zAttributes = new UBIDAttributes(wrapped);
    }

    @Override
    public void debug() {
        println(wrapped.toString());
    }
    
    @Override
    public ZAttributes getAttributes() {
        return zAttributes;
    }

    @Override
    public String getDN() {
        return wrapped.getDN();
    }

}
