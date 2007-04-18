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
package com.zimbra.cs.datasource;

import java.util.Date;
import java.util.concurrent.Callable;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;

public class DataSourceTask
implements Callable<Void> {
    
    private int mMailboxId;
    private String mDataSourceId;
    private String mAccountId;
    private Date mLastExecTime;
    private Date mNextExecTime;

    public DataSourceTask(int mailboxId, String dataSourceId, String accountId, Date lastExecTime, Date nextExecTime) {
        if (StringUtil.isNullOrEmpty(dataSourceId)) {
            throw new IllegalArgumentException("dataSourceId cannot be null or empty");
        }
        if (StringUtil.isNullOrEmpty(accountId)) {
            throw new IllegalArgumentException("accountId cannot be null or empty");
        }
        if (nextExecTime == null) {
            throw new IllegalArgumentException("nextExecTime cannot be null");
        }
        mMailboxId = mailboxId;
        mDataSourceId = dataSourceId;
        mAccountId = accountId;
        mLastExecTime = lastExecTime;
        mNextExecTime = nextExecTime;
    }

    public int getMailboxId() { return mMailboxId; }
    public String getDataSourceId() { return mDataSourceId; }
    public String getAccountId() { return mAccountId; }
    public Date getLastExecTime() { return mLastExecTime; }
    public Date getNextExecTime() { return mNextExecTime; }

    public Void call()
    throws Exception {
        ZimbraLog.datasource.debug("Executing scheduled import for Account %s, DataSource %s",
            mAccountId, mDataSourceId);
        
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.id, mAccountId);
        if (account == null) {
            ZimbraLog.datasource.info("Account %s was deleted.  Aborting scheduled import.", mAccountId);
            DataSourceManager.updateSchedule(mAccountId, mDataSourceId);
            return null;
        }
        DataSource ds = prov.get(account, DataSourceBy.id, mDataSourceId);
        if (ds == null) {
            ZimbraLog.datasource.info("DataSource %s was deleted.  Aborting scheduled import.", mDataSourceId);
            DataSourceManager.updateSchedule(mAccountId, mDataSourceId);
            return null;
        }
        DataSourceManager.importData(account, ds);
        mLastExecTime = new Date();
        return null;
    }
}
