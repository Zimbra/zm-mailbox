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

import com.unboundid.ldap.sdk.SearchScope;

import com.zimbra.cs.ldap.ZSearchScope;

public class UBIDSearchScope extends ZSearchScope {

    final private SearchScope searchScope;
    
    private UBIDSearchScope(SearchScope searchScope) {
        this.searchScope = searchScope;
    }
    
    SearchScope getNative() {
        return searchScope;
    }

    public static class UBIDSearchScopeFactory extends ZSearchScope.ZSearchScopeFactory {
        @Override
        protected ZSearchScope getObjectSearchScope() {
            return new UBIDSearchScope(SearchScope.BASE);
        }
        
        @Override
        protected ZSearchScope getOnelevelSearchScope() {
            return new UBIDSearchScope(SearchScope.ONE);
        }
        
        @Override
        protected ZSearchScope getSubtreeSearchScope() {
            return new UBIDSearchScope(SearchScope.SUB);
        }
    }

}
