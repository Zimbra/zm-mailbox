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
package com.zimbra.cs.ldap;

public abstract class ZSearchControls {

    public static final int SIZE_UNLIMITED  = 0;
    public static final int TIME_UNLIMITED  = 0;
    public static final String[] RETURN_ALL_ATTRS = null;
    
    /*
     * Note: In additional to the params that can be specified, all ZSearchControls 
     * have the following properties:
     * - no time limit
     * - do not dereference links during search
     */
    
    public static ZSearchControls SEARCH_CTLS_SUBTREE() {
        return createSearchControls(ZSearchScope.SEARCH_SCOPE_SUBTREE,
                SIZE_UNLIMITED, RETURN_ALL_ATTRS);
    }
    
    public static ZSearchControls createSearchControls(ZSearchScope searchScope,
            int sizeLimit, String[] returnAttrs) {
        return LdapClient.getInstance().createSearchControlsImpl(
                searchScope, sizeLimit, returnAttrs);
    }


}

