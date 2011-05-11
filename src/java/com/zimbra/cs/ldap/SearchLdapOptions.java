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

import java.util.Map;
import java.util.Set;


public class SearchLdapOptions {
    
    public static final int SIZE_UNLIMITED  = 0;
    public static final String[] RETURN_ALL_ATTRS = null;
    
    public static interface SearchLdapVisitor {
          public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs);
    }

    private static final int DEFAULT_RESULT_PAGE_SIZE = 1000;
    
    private String searchBase;
    private String query;
    private String[] returnAttrs = RETURN_ALL_ATTRS;
    private int maxResults = SIZE_UNLIMITED;
    private Set<String> binaryAttrs;
    private int resultPageSize  = DEFAULT_RESULT_PAGE_SIZE; // hardcoded for now, add setter API when needed
    private ZSearchScope searchScope;
    private SearchLdapOptions.SearchLdapVisitor visitor;
    
    public SearchLdapOptions(String searchbase, String query, 
            String[] returnAttrs, int maxResults, Set<String> binaryAttrs, 
            ZSearchScope searchScope, SearchLdapOptions.SearchLdapVisitor visitor) {
        setSearchBase(searchbase);
        setQuery(query);
        setReturnAttrs(returnAttrs);
        setMaxResults(maxResults);
        setBinaryAttrs(binaryAttrs);
        setSearchScope(searchScope);
        setVisitor(visitor);
    }
    
    public String getSearchBase() {
        return searchBase;
    }
    public String getQuery() {
        return query;
    }
    
    public String[] getReturnAttrs() {
        return returnAttrs;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    public Set<String> getBinaryAttrs() {
        return binaryAttrs;
    }
    
    public int getResultPageSize() {
        return resultPageSize;
    }
    
    public ZSearchScope getSearchScope() {
        return searchScope;
    }

    public SearchLdapOptions.SearchLdapVisitor getVisitor() {
        return visitor;
    }
    
    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public void setReturnAttrs(String[] returnAttrs) {
        this.returnAttrs = returnAttrs;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
    
    public void setBinaryAttrs(Set<String> binaryAttrs) {
        this.binaryAttrs = binaryAttrs;
    }
    
    public void setSearchScope(ZSearchScope searchScope) {
        this.searchScope = searchScope;
    }
    
    public void setVisitor(SearchLdapOptions.SearchLdapVisitor visitor) {
        this.visitor = visitor;
    }
}
