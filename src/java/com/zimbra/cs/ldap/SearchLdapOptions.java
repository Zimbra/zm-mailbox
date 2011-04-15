package com.zimbra.cs.ldap;

import java.util.Set;

import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil.SearchLdapVisitor;

public class SearchLdapOptions {
    private static final int DEFAULT_RESULT_PAGE_SIZE = 1000;
    
    private ILdapContext ldapContext;
    private String searchBase;
    private String query;
    private String[] returnAttrs;
    private Set<String> binaryAttrs;
    private int resultPageSize  = DEFAULT_RESULT_PAGE_SIZE; // hardcoded for now, add setter API when needed
    private SearchLdapVisitor visitor;
    
    public SearchLdapOptions(ILdapContext ldapContext, String searchbase, String query, 
            String[] returnAttrs, Set<String> binaryAttrs, SearchLdapVisitor visitor) {
        setILdapContext(ldapContext);
        setSearchBase(searchbase);
        setQuery(query);
        setReturnAttrs(returnAttrs);
        setBinaryAttrs(binaryAttrs);
        setVisitor(visitor);
    }
    
    public ILdapContext getILdapContext() {
        return ldapContext;
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
    
    public Set<String> getBinaryAttrs() {
        return binaryAttrs;
    }
    
    public int getResultPageSize() {
        return resultPageSize;
    }

    public SearchLdapVisitor getVisitor() {
        return visitor;
    }
    
    public void setILdapContext(ILdapContext ldapContext) {
        this.ldapContext = ldapContext;
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
    
    public void setBinaryAttrs(Set<String> binaryAttrs) {
        this.binaryAttrs = binaryAttrs;
    }
    
    public void setVisitor(SearchLdapVisitor visitor) {
        this.visitor = visitor;
    }
}
