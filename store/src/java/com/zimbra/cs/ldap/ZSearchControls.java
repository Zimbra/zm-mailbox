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

