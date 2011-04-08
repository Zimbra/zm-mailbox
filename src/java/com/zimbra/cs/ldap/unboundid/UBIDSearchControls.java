package com.zimbra.cs.ldap.unboundid;

import java.util.List;

import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.SearchScope;

import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;


public class UBIDSearchControls extends ZSearchControls {

    final private SearchScope searchScope;
    
    private UBIDSearchControls(UBIDSearchScope searchScope) {
        this.searchScope = searchScope.get();
    }
    
    SearchScope getSearchScope() {
        return searchScope;
    }
    
    int getSizeLimit() {
        return 0;
    }
    
    int getTimeLimit() {
        return 0;
    }
    
    boolean getTypesOnly() {
        return false;
    }
    
    List<String> getReturnAttrs() {
        return null;
    }
    
    DereferencePolicy getDerefPolicy() {
        return DereferencePolicy.NEVER;
    }
    
    public static class UBIDSearchControlsFactory extends ZSearchControlsFactory {
        protected ZSearchControls getSearchControl(ZSearchScope searchScope) {
            return new UBIDSearchControls((UBIDSearchScope)searchScope);
        }
    }
}
