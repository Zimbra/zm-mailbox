/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbScheduledTask;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTaskManager;


/**
 * Represents an external data source, such as a POP3 or IMAP mail server,
 * from which ZCS can import <code>MailItem</code>s.
 * 
 * @author bburtin
 */
public class DataSourceManager {
    // accountId -> dataSourceId -> ImportStatus
    private static final Map<String, Map<String, ImportStatus>> sImportStatus =
        new HashMap<String, Map<String, ImportStatus>>();

    /**
     * Tests connecting to a data source.  Do not actually create the
     * data source.
     * 
     * @return <code>null</code> if the test succeeded, or the error message
     * if it didn't.
     */
    public static String test(DataSource ds)
    throws ServiceException {
        ZimbraLog.datasource.info("Testing %s", ds);
        MailItemImport mii = newMailItemImport(ds);
        String error = mii.test();
        if (error == null) {
            ZimbraLog.datasource.info("Test succeeded");
        } else {
            ZimbraLog.datasource.info("Test failed: %s", error);
        }
        
        return error;
    }

    private static MailItemImport newMailItemImport(DataSource ds)
            throws ServiceException {
        switch (ds.getType()) {
        case imap:
            return new ImapImport(ds);
        case pop3:
            return new Pop3Import(ds);
        default:
            throw new IllegalArgumentException(
                "Unknown data import type: " + ds.getType());
        }
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

    public static void importData(DataSource ds) throws ServiceException {
        importData(ds, true);
    }
    
    /**
     * Executes the data source's <code>MailItemImport</code> implementation
     * to import data in the current thread.
     */
    public static void importData(DataSource ds, boolean fullSync) throws ServiceException {
        ImportStatus importStatus = getImportStatus(ds.getAccount(), ds);
        
        synchronized (importStatus) {
            if (importStatus.isRunning()) {
                ZimbraLog.datasource.info("Attempted to start import while " +
                    " an import process was already running.  Ignoring the second request.");
                return;
            }
            importStatus.mHasRun = true;
            importStatus.mIsRunning = true;
            importStatus.mSuccess = false;
            importStatus.mError = null;
        }
        
        
        MailItemImport mii = newMailItemImport(ds);
        boolean success = false;
        String error = null;

        try {
            ZimbraLog.datasource.info("Importing data.");
            mii.importData(fullSync);
            ZimbraLog.datasource.info("Import completed.");
            success = true;
        } catch (ServiceException x) {
            error = x.getMessage();
            if (error == null) {
                error = x.toString();
            }
            throw x;
        } finally {
            synchronized (importStatus) {
                importStatus.mSuccess = success;
                importStatus.mError = error;
                importStatus.mIsRunning = false;
            }
        }
    }

    static void cancelTask(Mailbox mbox, String dsId)
    throws ServiceException {
        ScheduledTaskManager.cancel(DataSourceTask.class.getName(), dsId, mbox.getId(), false);
        DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
    }
    
    /**
     * Updates scheduling data for this <tt>DataSource</tt> both in memory and in the
     * <tt>data_source_task</tt> database table.
     */
    public static void updateSchedule(String accountId, String dsId)
    throws ServiceException {
        ZimbraLog.datasource.debug("Updating schedule for account %s, data source %s", accountId, dsId);
        
        // Look up account and data source
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.id, accountId);
        if (account == null) {
            ZimbraLog.datasource.info(
                "Account %s was deleted for data source %s.  Deleting scheduled task.",
                accountId, dsId);
            DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
            // Don't have mailbox ID, so we'll have to wait for the task to run and clean itself up.
            return;
        }
        DataSource ds = prov.get(account, DataSourceBy.id, dsId);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        if (ds == null) {
            ZimbraLog.datasource.info(
                "Data source %s was deleted.  Deleting scheduled task.", dsId);
            ScheduledTaskManager.cancel(DataSourceTask.class.getName(), dsId, mbox.getId(), false);
            DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
            return;
        }
        Connection conn = null;
        
        ZimbraLog.datasource.info("Updating schedule for data source %s", ds.getName());
        try {
            conn = DbPool.getConnection();
            ScheduledTaskManager.cancel(conn, DataSourceTask.class.getName(), ds.getId(), mbox.getId(), false);
            if (ds.isScheduled()) {
                DataSourceTask task = new DataSourceTask(mbox.getId(), accountId, dsId, ds.getPollingInterval());
                ZimbraLog.datasource.debug("Scheduling %s", task);
                ScheduledTaskManager.schedule(conn, task);
            }
            conn.commit();
        } catch (ServiceException e) {
            ZimbraLog.datasource.warn("Unable to schedule data source %s", ds.getName());
            DbPool.quietRollback(conn);
        } finally {
            DbPool.quietClose(conn);
        }
    }
}
