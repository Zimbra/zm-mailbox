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

import javax.naming.directory.SearchControls;

import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;

public class JNDISearchControls extends ZSearchControls {

    final private SearchControls searchControls;
    
    JNDISearchControls(ZSearchScope searchScope, int sizeLimit, String[] returnAttrs) {
        searchControls = new SearchControls(((JNDISearchScope) searchScope).getNative(), sizeLimit, 
                TIME_UNLIMITED, returnAttrs, false, false);
    }
    
    SearchControls getNative() {
        return searchControls;
    }

}
