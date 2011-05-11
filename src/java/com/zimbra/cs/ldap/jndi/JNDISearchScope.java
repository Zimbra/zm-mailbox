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

import com.zimbra.cs.ldap.ZSearchScope;

public class JNDISearchScope extends ZSearchScope {

    final private int searchScope;
    
    private JNDISearchScope(int searchScope) {
        this.searchScope = searchScope;
    }
    
    public int getNative() {
        return searchScope;
    }
    
    public static class JNDISearchScopeFactory extends ZSearchScope.ZSearchScopeFactory {
        @Override
        protected ZSearchScope getObjectSearchScope() {
            return new JNDISearchScope(SearchControls.OBJECT_SCOPE);
        }
        
        @Override
        protected ZSearchScope getOnelevelSearchScope() {
            return new JNDISearchScope(SearchControls.ONELEVEL_SCOPE);
        }
        
        @Override
        protected ZSearchScope getSubtreeSearchScope() {
            return new JNDISearchScope(SearchControls.SUBTREE_SCOPE);
        }
    }


}
