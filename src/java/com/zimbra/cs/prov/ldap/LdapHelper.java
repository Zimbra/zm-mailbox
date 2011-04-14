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
package com.zimbra.cs.prov.ldap;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.ldap.LdapUtil.SearchLdapVisitor;
import com.zimbra.cs.ldap.ILdapContext;

public abstract class LdapHelper {
    
    private LdapProv ldapProv;
    
    protected LdapHelper(LdapProv ldapProv) {
        this.ldapProv = ldapProv;
    }
    
    protected LdapProv getProv() {
        return ldapProv;
    }

    public static class SearchLdapOptions {
        private ILdapContext ldapContext;
        private String searchBase;
        private String query;
        private String[] returnAttrs;
        private Set<String> binaryAttrs;
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
    
    public abstract void searchLdap(SearchLdapOptions searchOptions) throws ServiceException;
}
