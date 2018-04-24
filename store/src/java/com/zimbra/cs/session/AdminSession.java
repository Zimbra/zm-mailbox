/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

import java.util.HashMap;
import java.util.List;

/** @author dkarp */
public class AdminSession extends Session {

    private static final long ADMIN_SESSION_TIMEOUT_MSEC = 10 * Constants.MILLIS_PER_MINUTE;

    private DirectorySearchParams mSearchParams;
    private HashMap<String,Object> mData = new HashMap<String,Object>();

    public AdminSession(String accountId) {
        super(accountId, Session.Type.ADMIN);
    }

    @Override
    protected boolean isMailboxListener() {
        return false;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return true;
    }

    @Override
    protected long getSessionIdleLifetime() {
        return ADMIN_SESSION_TIMEOUT_MSEC;
    }

    public Object getData(String key) { return mData.get(key); }
    public void setData(String key, Object data) { mData.put(key, data); }
    public void clearData(String key) { mData.remove(key); }

    @Override public void notifyPendingChanges(PendingModifications pns, int changeId, SourceSessionInfo source) { }

    @Override protected void cleanup() { }

    public List<NamedEntry> searchDirectory(SearchDirectoryOptions searchOpts,
            int offset, NamedEntry.CheckRight rightChecker)
    throws ServiceException {

        DirectorySearchParams params = new DirectorySearchParams(searchOpts, rightChecker);

        boolean needToSearch =  (offset == 0) || (mSearchParams == null) || !mSearchParams.equals(params);
        //ZimbraLog.account.info("this="+this+" mSearchParams="+mSearchParams+" equal="+!params.equals(mSearchParams));
        if (needToSearch) {
            //ZimbraLog.account.info("doing new search: "+query+ " offset="+offset);
            params.doSearch();
            mSearchParams = params;
        } else {
            //ZimbraLog.account.info("cached search: "+query+ " offset="+offset);
        }
        return mSearchParams.getResult();
    }

}
