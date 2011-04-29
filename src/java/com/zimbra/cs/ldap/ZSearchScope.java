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
    public static ZSearchScope SEARCH_SCOPE_OBJECT;
    
    // only entries that are immediate subordinates of the entry specified 
    // by the base DN (but not the base entry itself) should be considered.
    public static ZSearchScope SEARCH_SCOPE_ONELEVEL;
    
    // the base entry itself and any subordinate entries (to any depth) should be considered.
    public static ZSearchScope SEARCH_SCOPE_SUBTREE;
    
    public abstract static class ZSearchScopeFactory {
        protected abstract ZSearchScope getObjectSearchScope();
        protected abstract ZSearchScope getOnelevelSearchScope();
        protected abstract ZSearchScope getSubtreeSearchScope();
    }
    
    public static void init(ZSearchScopeFactory factory) {
        SEARCH_SCOPE_OBJECT = factory.getObjectSearchScope();
        SEARCH_SCOPE_ONELEVEL = factory.getOnelevelSearchScope();
        SEARCH_SCOPE_SUBTREE = factory.getSubtreeSearchScope();
    }

}
