/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
/*
 * Created on Aug 31, 2005
 */
package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;

class DirectorySearchParams {

    private SearchDirectoryOptions mSearchOpts;
    private List<NamedEntry> mResult;
    private NamedEntry.CheckRight mRightChecker;
    
    DirectorySearchParams(SearchDirectoryOptions searchOpts, NamedEntry.CheckRight rightChecker) {
        mSearchOpts = searchOpts;
        mRightChecker = rightChecker;
    }
    
    List<NamedEntry> getResult() {
        return mResult;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DirectorySearchParams)) {
            return false;
        }
        
        if (o == this) {
            return true;
        }
        
        DirectorySearchParams other = (DirectorySearchParams) o;
        return mSearchOpts.equals(other.mSearchOpts);
    }
    
    void doSearch() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        mResult = prov.searchDirectory(mSearchOpts);
        
        if (mRightChecker != null) {
            List<NamedEntry> allowed = new ArrayList<NamedEntry>();
            for (int i = 0; i < mResult.size(); i++) {
                NamedEntry entry = (NamedEntry)mResult.get(i);
                if (mRightChecker.allow(entry)) {
                    allowed.add(entry);
                }
            }
            mResult = allowed;
        }
    }
}
