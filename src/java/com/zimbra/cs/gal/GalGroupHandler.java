/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.gal;

import java.util.HashMap;

import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public interface GalGroupHandler {

    public boolean isGroup(SearchResult sr);
    
    public String[] getMembers(ZimbraLdapContext zlc, SearchResult sr);
}
