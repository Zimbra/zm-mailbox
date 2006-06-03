/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
/*
 * Created on Aug 31, 2005
 */
package com.zimbra.cs.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.service.ServiceException;

class AccountSearchParams {
    String mDomainId;
    String mQuery;
    String[] mAttrs;
    HashSet mAttrsSet;
    String mSortBy;
    boolean mSortAscending;
    int mFlags;
    List mResult;
    
    AccountSearchParams(Domain d, String query, String[] attrs, String sortBy, boolean sortAscending, int flags) {
        mDomainId = (d == null) ? "" : d.getId();
        mQuery = (query == null) ? "" : query;
        mAttrs = new String[attrs == null ? 0 : attrs.length];
        for (int i=0; i < mAttrs.length; i++) mAttrs[i] = attrs[i];
        mAttrsSet = new HashSet(Arrays.asList(mAttrs));
        mSortBy = (sortBy == null) ? "" : sortBy;
        mSortAscending = sortAscending;
        mFlags = flags;
    }

    public boolean equals(Object o) {
        if (!(o instanceof AccountSearchParams)) return false;
        if (o == this) return true;
        
        AccountSearchParams other = (AccountSearchParams) o; 
        return 
            mDomainId.equals(other.mDomainId) &&
            mQuery.equals(other.mQuery) &&
            mAttrsSet.equals(other.mAttrsSet) &&
            mSortBy.equals(other.mSortBy) &&
            mSortAscending == other.mSortAscending &&
            mFlags == other.mFlags;
    }
    
    void doSearch() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (!mDomainId.equals("")) {
            mResult = prov.searchAccounts(prov.get(DomainBy.ID, mDomainId), mQuery, mAttrs, mSortBy, mSortAscending, mFlags);
        } else {
            mResult = prov.getInstance().searchAccounts(mQuery, mAttrs, mSortBy, mSortAscending, mFlags);
        }
    }
    
}
