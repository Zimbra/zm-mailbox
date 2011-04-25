package com.zimbra.cs.ldap.unboundid;

import java.util.Arrays;
import java.util.List;

import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.SearchScope;

import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;


public class UBIDSearchControls extends ZSearchControls {

    private SearchScope searchScope;
    private int sizeLimit;
    private List<String> returnAttrs;
    
    UBIDSearchControls(ZSearchScope searchScope, int sizeLimit, String[] returnAttrs) {
        this.searchScope = ((UBIDSearchScope) searchScope).get();
        this.sizeLimit = sizeLimit;
        
        if (returnAttrs != null) {
            this.returnAttrs = Arrays.asList(returnAttrs);
        }
    }
    
    SearchScope getSearchScope() {
        return searchScope;
    }
    
    int getSizeLimit() {
        return sizeLimit;
    }
    
    int getTimeLimit() {
        return TIME_UNLIMITED;
    }
    
    boolean getTypesOnly() {
        return false;
    }
    
    List<String> getReturnAttrs() {
        return returnAttrs;
    }
    
    /*
    DereferencePolicy getDerefPolicy() {
        return DereferencePolicy.NEVER;
    }
    */
}
