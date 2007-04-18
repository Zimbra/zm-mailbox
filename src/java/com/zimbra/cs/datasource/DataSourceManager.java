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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.TaskScheduler;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.db.DbDataSourceTask;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.util.Zimbra;


/**
 * Represents an external data source, such as a POP3 or IMAP mail server,
 * from which ZCS can import <code>MailItem</code>s.
 * 
 * @author bburtin
 */
public class DataSourceManager {

    private static Map<DataSource.Type, MailItemImport> sImports =
        Collections.synchronizedMap(new HashMap<DataSource.Type, MailItemImport>());
    
    // accountId -> dataSourceId -> ImportStatus
    private static Map<String, Map<String, ImportStatus>> sImportStatus =
        new HashMap<String, Map<String, ImportStatus>>();

    // We don't need many threads, since currently the scheduled task just kicks off
    // another thread that runs the actual import.
    private static TaskScheduler<Void> sScheduledPolls =
        new TaskScheduler<Void>("DataSource", 1, 3);
    
    static {
        registerImport(DataSource.Type.pop3, new Pop3Import());
        registerImport(DataSource.Type.imap, new ImapImport());
    }
    
    public static void startup()
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        long earliest = 0;
        
        for (DataSourceTask task : DbDataSourceTask.getAllDataSourceTasks()) {
            // Look up account and data source
            Account account = prov.get(AccountBy.id, task.getAccountId());
            if (account == null) {
                // Account was deleted
                DbDataSourceTask.deleteDataSourceTask(task.getDataSourceId());
                continue;
            }
            DataSource ds = prov.get(account, DataSourceBy.id, task.getDataSourceId());
            if (ds == null) {
                // Data source was deleted
                DbDataSourceTask.deleteDataSourceTask(task.getDataSourceId());
                continue;
            }
            
            // Calculate the scheduling offset.  The earliest task gets executed immediately.  All
            // subsequent tasks get delayed, relative to the earliest.
            if (earliest == 0) {
                earliest = task.getNextExecTime().getTime();
            }
            sScheduledPolls.schedule(task.getDataSourceId(), task, task.getNextExecTime().getTime() - earliest);
        }
    }

    /**
     * Tests connecting to a data source.  Do not actually create the
     * data source.
     * 
     * @return <code>null</code> if the test succeeded, or the error message
     * if it didn't.
     */
    public static String test(DataSource ds)
    throws ServiceException {
        ZimbraLog.datasource.info("Testing " + ds);
        MailItemImport mii = sImports.get(ds.getType());
        String error = mii.test(ds);
        if (error == null) {
            ZimbraLog.datasource.info("Test of " + ds + " succeeded");
        } else {
            ZimbraLog.datasource.info("Test of " + ds + " failed: " + error);
        }
        
        return error;
    }

    /**
     * Associate the specified type with the <code>MailItemImport</code>
     * implementation that will perform import of data of that type.
     * 
     * @param type the data source type
     * @param mii import implementation
     */
    public static void registerImport(DataSource.Type type, MailItemImport mii) {
        sImports.put(type, mii);
    }

    public static List<ImportStatus> getImportStatus(Account account)
    throws ServiceException {
        List<DataSource> dsList = Provisioning.getInstance().getAllDataSources(account);
        List<ImportStatus> allStatus = new ArrayList<ImportStatus>();
        for (DataSource ds : dsList) {
            allStatus.add(getImportStatus(account, ds));
        }
        
        return allStatus;
    }
    
    private static ImportStatus getImportStatus(Account account, DataSource ds) {
        ImportStatus importStatus = null;
        
        synchronized (sImportStatus) {
            Map<String, ImportStatus> isMap = sImportStatus.get(account.getId());
            if (isMap == null) {
                isMap = new HashMap<String, ImportStatus>();
                sImportStatus.put(account.getId(), isMap);
            }
            importStatus = isMap.get(ds.getId());
            if (importStatus == null) {
                importStatus = new ImportStatus(ds.getId());
                isMap.put(ds.getId(), importStatus);
            }
        }
        
        return importStatus;
    }
    
    /**
     * Execute this data source's <code>MailItemImport</code> implementation
     * to import data. 
     */
    public static void importData(Account account, DataSource ds) {
        ImportStatus importStatus = getImportStatus(account, ds);
        
        synchronized (importStatus) {
            if (importStatus.isRunning()) {
                ZimbraLog.datasource.info(ds + ": attempted to start import while " +
                    " an import process was already running.  Ignoring the second request.");
                return;
            }
            importStatus.mHasRun = true;
            importStatus.mIsRunning = true;
            importStatus.mSuccess = false;
            importStatus.mError = null;
        }
        
        Thread thread = new Thread(new ImportDataThread(account, ds));
        thread.setName("ImportDataThread");
        thread.start();
    }

    /**
     * Updates scheduling data for this datasource both in memory and in the
     * <tt>data_source_task</tt> database table.
     */
    public static void updateSchedule(String accountId, String dataSourceId)
    throws ServiceException {
        DataSourceTask task = (DataSourceTask) sScheduledPolls.cancel(dataSourceId, true);
        DbDataSourceTask.deleteDataSourceTask(dataSourceId);

        // Look up account and data source
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.id, accountId);
        if (account == null) {
            // Account was deleted
            return;
        }
        DataSource ds = prov.get(account, DataSourceBy.id, dataSourceId);
        if (ds == null) {
            // Data source was deleted
            return;
        }
        
        // Note: currently this algorithm isn't too smart about updating data
        // in the database.  To make the code simpler, it deletes the row, then
        // inserts a new one when rescheduling.  We can optimize later if this
        // becomes a performance problem.
        
        
        if (ds.isScheduled()) {
            // Calculate delay based on last exec time and current time
            long delay = ds.getPollingInterval();
            if (task != null && task.getLastExecTime() != null) {
                // Take current time into consideration, in case the poll interval is
                // changing or we got an explicit <ImportDataRequest> between two
                // scheduled polls
                delay = task.getLastExecTime().getTime() + delay - System.currentTimeMillis();
                if (delay < 0) {
                    delay = 0;
                }
            }
            
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            Date scheduledTime = new Date(System.currentTimeMillis() + delay);
            
            if (task == null) {
                // Create a new task if this is the first time
                task = new DataSourceTask(mbox.getId(), ds.getId(), account.getId(), null, scheduledTime);
            }
            ZimbraLog.datasource.debug("Scheduling automated poll for %s at %s",
                ds, scheduledTime);
            sScheduledPolls.schedule(ds.getId(), task, delay);
            DbDataSourceTask.createDataSourceTask(mbox, ds.getId(), account.getId(), task.getLastExecTime(), scheduledTime);
        }
    }
    
    private static class ImportDataThread implements Runnable {
        Account mAccount;
        DataSource mDataSource;
        
        public ImportDataThread(Account account, DataSource ds) {
            if (account == null) {
                throw new IllegalArgumentException("account cannot be null");
            }
            if (ds == null) {
                throw new IllegalArgumentException("DataSource cannot be null");
            }
            mAccount = account;
            mDataSource = ds;
        }
        
        public void run() {
            ZimbraLog.addAccountNameToContext(mAccount.getName());
            ZimbraLog.addDataSourceNameToContext(mDataSource.getName());
            try {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
                ZimbraLog.addMboxToContext(mbox.getId());
            } catch (ServiceException e) {
                ZimbraLog.datasource.warn("Unable to look up mailbox", e);
            }
            
            MailItemImport mii = sImports.get(mDataSource.getType());
            boolean success = false;
            String error = null;

            try {
                ZimbraLog.datasource.info("Importing data from %s", mDataSource);
                mii.importData(mAccount, mDataSource);
                ZimbraLog.datasource.info("Import completed");
                success = true;
            } catch (Throwable t) {
                // Catch Throwable, so that we don't lose track of runtime exceptions 
                if (t instanceof OutOfMemoryError) {
                    Zimbra.halt("DataSourceManager.run()", t);
                }
                ZimbraLog.datasource.warn("Import from %s failed", mDataSource, t);
                error = t.getMessage();
                if (error == null) {
                    error = t.toString();
                }
            } finally {
                ImportStatus importStatus = getImportStatus(mAccount, mDataSource);
                synchronized (importStatus) {
                    importStatus.mSuccess = success;
                    importStatus.mError = error;
                    importStatus.mIsRunning = false;
                }
                try {
                    updateSchedule(mAccount.getId(), mDataSource.getId());
                } catch (ServiceException e) {
                    ZimbraLog.datasource.warn("Unable to reschedule DataSourceTask", e);
                }
            }
        }
    }
}
