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

public abstract class ZSearchScope {
    // only the entry specified by the base DN should be considered.
    public static ZSearchScope SEARCH_SCOPE_BASE;
    
    // only entries that are immediate subordinates of the entry specified 
    // by the base DN (but not the base entry itself) should be considered.
    public static ZSearchScope SEARCH_SCOPE_ONELEVEL;
    
    // the base entry itself and any subordinate entries (to any depth) should be considered.
    public static ZSearchScope SEARCH_SCOPE_SUBTREE;
    
    // any subordinate entries (to any depth) below the entry specified by the base DN should 
    // be considered, but the base entry itself should not be considered.
    public static ZSearchScope SEARCH_SCOPE_CHILDREN;
    
    public abstract static class ZSearchScopeFactory {
        protected abstract ZSearchScope getBaseSearchScope();
        protected abstract ZSearchScope getOnelevelSearchScope();
        protected abstract ZSearchScope getSubtreeSearchScope();
        protected abstract ZSearchScope getChildrenSearchScope();
    }
    
    public static void init(ZSearchScopeFactory factory) {
        SEARCH_SCOPE_BASE = factory.getBaseSearchScope();
        SEARCH_SCOPE_ONELEVEL = factory.getOnelevelSearchScope();
        SEARCH_SCOPE_SUBTREE = factory.getSubtreeSearchScope();
        SEARCH_SCOPE_CHILDREN = factory.getChildrenSearchScope();
    }

}
