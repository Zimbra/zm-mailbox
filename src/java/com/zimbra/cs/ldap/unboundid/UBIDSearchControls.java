/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
