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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTask;

public class DataSourceTask
extends ScheduledTask {

    private static final String KEY_DATA_SOURCE_ID = "dsid";
    
    /**
     * Constructor with no arguments required for task instantiation.
     */
    public DataSourceTask() {
    }
    
    public DataSourceTask(int mailboxId, String accountId, String dataSourceId, long intervalMillis) {
        if (StringUtil.isNullOrEmpty(accountId)) {
            throw new IllegalArgumentException("accountId cannot be null or empty");
        }
        if (StringUtil.isNullOrEmpty(dataSourceId)) {
            throw new IllegalArgumentException("dataSourceId cannot be null or empty");
        }
        
        setMailboxId(mailboxId);
        setProperty(KEY_DATA_SOURCE_ID, dataSourceId);
        setIntervalMillis(intervalMillis);
    }

    public String getName() { return getDataSourceId(); }
    
    public String getDataSourceId() {
        return getProperty(KEY_DATA_SOURCE_ID);
    }
    
    public Void call()
    throws Exception {
        ZimbraLog.addMboxToContext(getMailboxId());
        ZimbraLog.datasource.debug("Running scheduled import for DataSource %s",
            getDataSourceId());
        Mailbox mbox = null;
        
        try {
            // Look up mailbox, account and data source
            mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
            Account account = mbox.getAccount();
            Provisioning prov = Provisioning.getInstance();
            DataSource ds = prov.get(account, DataSourceBy.id, getDataSourceId());
            if (ds != null) {
                // Do the work
                ZimbraLog.addDataSourceNameToContext(ds.getName());
                DataSourceManager.importData(account, ds);
            } else {
                ZimbraLog.datasource.info("DataSource %s was deleted.  Cancelling future tasks.",
                    getDataSourceId());
                DataSourceManager.cancelTask(mbox, getDataSourceId());
            }
        } catch (ServiceException e) {
            ZimbraLog.datasource.warn("Scheduled DataSource import failed.  Cancelling future tasks.", e);
            DataSourceManager.cancelTask(mbox, getDataSourceId());
            return null;
        }
        
        ZimbraLog.removeDataSourceNameFromContext();
        ZimbraLog.removeMboxFromContext(getMailboxId());

        return null;
    }
}
