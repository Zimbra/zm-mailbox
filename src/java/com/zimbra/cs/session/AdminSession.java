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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter;
import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Constants;

/** @author dkarp */
public class AdminSession extends Session {

    private static final long ADMIN_SESSION_TIMEOUT_MSEC = 10 * Constants.MILLIS_PER_MINUTE;
  
    private AccountSearchParams mSearchParams;
    private HashMap<String,Object> mData = new HashMap<String,Object>();

    AdminSession(String accountId, String sessionId) throws ServiceException {
        super(accountId, sessionId, SessionCache.SESSION_ADMIN);
    }

    protected long getSessionIdleLifetime() {
        return ADMIN_SESSION_TIMEOUT_MSEC;
    }
    
    public void dumpState(Writer w) {
    	try {
    		w.write("AdminSession - ");
    	} catch(IOException e) { e.printStackTrace(); }
    	super.dumpState(w);
    }

    public Object getData(String key) { return mData.get(key); }
    public void setData(String key, Object data) { mData.put(key, data); }
    
    public void notifyPendingChanges(PendingModifications pns) { }
    
    public void notifyIM(IMNotification imn) { }
    protected boolean shouldRegisterWithIM() { return false; }

    protected void cleanup() {
    }

    public List searchAcounts(Domain d, String query, String[] attrs, String sortBy,
            boolean sortAscending, int flags, int offset) throws ServiceException {
        AccountSearchParams params = new AccountSearchParams(d, query, attrs, sortBy, sortAscending, flags);
        boolean needToSearch = (mSearchParams == null) || !mSearchParams.equals(params);
        //ZimbraLog.account.info("this="+this+" mSearchParams="+mSearchParams+" equal="+!params.equals(mSearchParams));
        if (needToSearch) {
            //ZimbraLog.account.info("doing new search: "+query+ " offset="+offset);
            params.doSearch();
            mSearchParams = params;
        } else {
            //ZimbraLog.account.info("cached search: "+query+ " offset="+offset);
        }
        return mSearchParams.mResult;
    }

    public List searchCalendarResources(
            Domain d, EntrySearchFilter filter, String[] attrs, String sortBy,
            boolean sortAscending, int offset)
    throws ServiceException {
        String query = LdapEntrySearchFilter.toLdapCalendarResourcesFilter(filter);
        return searchAcounts(
                d, query, attrs, sortBy, sortAscending,
                Provisioning.SA_CALENDAR_RESOURCE_FLAG,
                offset);
    }
}
