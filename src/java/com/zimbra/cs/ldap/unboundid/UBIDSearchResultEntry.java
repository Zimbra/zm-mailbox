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

import com.unboundid.ldap.sdk.SearchResultEntry;

import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZSearchResultEntry;

/**
 * Represents one LDAP entry in a search result.
 *
 */
public class UBIDSearchResultEntry extends ZSearchResultEntry  {

    private SearchResultEntry searchResultEntry;
    private UBIDAttributes zAttributes;
    
    UBIDSearchResultEntry(SearchResultEntry searchResultEntry) {
        this.searchResultEntry = searchResultEntry;
        this.zAttributes = new UBIDAttributes(searchResultEntry);
    }

    @Override
    public void debug() {
        println(searchResultEntry.toString());
    }
    
    @Override
    public ZAttributes getAttributes() {
        return zAttributes;
    }

    @Override
    public String getDN() {
        return searchResultEntry.getDN();
    }

}
