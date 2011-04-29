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
package com.zimbra.cs.ldap.unboundid;

import java.util.Arrays;
import java.util.List;

import com.unboundid.ldap.sdk.SearchScope;

import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;


public class UBIDSearchControls extends ZSearchControls {

    private SearchScope searchScope;
    private int sizeLimit;
    private List<String> returnAttrs;
    
    UBIDSearchControls(ZSearchScope searchScope, int sizeLimit, String[] returnAttrs) {
        this.searchScope = ((UBIDSearchScope) searchScope).getNative();
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
