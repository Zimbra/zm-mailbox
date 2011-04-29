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
package com.zimbra.cs.ldap.jndi;

import javax.naming.directory.SearchResult;

import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZSearchResultEntry;

public class JNDISearchResultEntry extends ZSearchResultEntry {

    SearchResult searchResultEntry;
    JNDIAttributes zAttributes;
    
    JNDISearchResultEntry(SearchResult searchResult) {
        this.searchResultEntry = searchResult;
        this.zAttributes = new JNDIAttributes(searchResultEntry.getAttributes());
    }
    
    @Override
    @TODO
    public void debug() {
        println(searchResultEntry.toString());
    }

    @Override
    public ZAttributes getAttributes() {
        return zAttributes;
    }

    @Override
    public String getDN() {
        return searchResultEntry.getNameInNamespace();
    }

}
