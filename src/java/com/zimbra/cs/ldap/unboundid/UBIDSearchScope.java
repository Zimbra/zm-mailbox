package com.zimbra.cs.ldap.unboundid;

import com.unboundid.ldap.sdk.SearchScope;

import com.zimbra.cs.ldap.ZSearchScope;

public class UBIDSearchScope extends ZSearchScope {

    // the wrapped unboundid SearchScope
    final private SearchScope searchScope;
    
    private UBIDSearchScope(SearchScope searchScope) {
        this.searchScope = searchScope;
    }
    
    // get the wrapped object
    SearchScope get() {
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
