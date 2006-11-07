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
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;


/**
 * Represents an external data source, such as a POP3 mail server, from which ZCS
 * can import <code>MailItem</code>s.
 * 
 * @author bburtin
 */
public class DataSourceManager {

    private static Map<DataSource.Type, MailItemImport> sImports =
        Collections.synchronizedMap(new HashMap<DataSource.Type, MailItemImport>());
    
    // accountId -> dataSourceId -> ImportStatus
    private static Map<String, Map<String, ImportStatus>> sImportStatus =
        new HashMap<String, Map<String, ImportStatus>>();
    
    static {
        registerImport(DataSource.Type.pop3, new Pop3Import());
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
        ZimbraLog.mailbox.info("Testing " + ds);
        MailItemImport mii = sImports.get(ds.getType());
        String error = mii.test(ds);
        if (error == null) {
            ZimbraLog.mailbox.info("Test of " + ds + " succeeded");
        } else {
            ZimbraLog.mailbox.info("Test of " + ds + " failed: " + error);
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
                ZimbraLog.mailbox.info(ds + ": attempted to start import while " +
                    " an import process was already running.  Ignoring the second request.");
                return;
            }
            importStatus.mHasRun = true;
            importStatus.mIsRunning = true;
            importStatus.mSuccess = false;
            importStatus.mError = null;
        }
        
        boolean success = false;
        String error = null;

        try {
            MailItemImport mii = sImports.get(ds.getType());
            mii.importData(account, ds);
            success = true;
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Import from " + ds + " failed", e);
            error = e.getMessage();
        } finally {
            synchronized (importStatus) {
                importStatus.mSuccess = success;
                importStatus.mError = error;
                importStatus.mIsRunning = false;
            }
        }
    }
}
