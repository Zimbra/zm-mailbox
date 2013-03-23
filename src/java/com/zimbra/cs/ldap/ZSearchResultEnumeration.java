/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
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
package com.zimbra.cs.ldap;

import com.zimbra.cs.ldap.LdapTODO.*;

/*
 * migration path for javax.naming.NamingEnumeration interface
 * 
 * TODO: delete this eventually and do everything the pure unboundid way
 * 
 * try to gather all searchDir calls to LdapHelper.searchDir
 */
@TODO
public interface ZSearchResultEnumeration {
    public ZSearchResultEntry next() throws LdapException;
    public boolean hasMore() throws LdapException;
    public void close() throws LdapException;
}

