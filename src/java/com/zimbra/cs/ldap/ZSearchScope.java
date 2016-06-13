/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
