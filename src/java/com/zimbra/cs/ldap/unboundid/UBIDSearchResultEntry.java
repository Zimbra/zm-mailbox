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
