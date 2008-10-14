/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.account.callback;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;

/**
 * Validates <tt>DataSource</tt> attribute values.
 * 
 * @author bburtin
 */
public class DataSourceCallback extends AttributeCallback {

    private static final String KEY_INTERVAL_CHANGED = "IntervalChanged";
    
    /**
      * Confirms that the polling interval set on the data source is at least as long
      * as the minimum set for the account.
      * 
      * @param entry a {@link DataSource} or {@link Account}
      */
    @SuppressWarnings("unchecked")
    public void preModify(Map context, String attrName, Object attrValue, Map attrsToModify,
                          Entry entry, boolean isCreate)
    throws ServiceException {
        if (!Provisioning.A_zimbraDataSourcePollingInterval.equals(attrName)) {
            return;
        }
        
        String newInterval = (String) attrValue;
        String oldInterval = "";
        if (entry != null) {
            oldInterval = entry.getAttr(Provisioning.A_zimbraDataSourcePollingInterval);
        }
        
        if (entry instanceof DataSource) {
            validateDataSource((DataSource) entry, newInterval);
        } else if (entry instanceof Account) {
            validateAccount((Account) entry, newInterval);
        } else if (entry instanceof Cos) {
            validateCos((Cos) entry, newInterval);
        }

        // Determine if the interval has changed
        long lNewInterval = DateUtil.getTimeInterval(newInterval, 0);
        long lOldInterval = DateUtil.getTimeInterval(oldInterval, 0);
        context.put(KEY_INTERVAL_CHANGED, (lNewInterval != lOldInterval));
    }

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
        if (!Provisioning.A_zimbraDataSourcePollingInterval.equals(attrName)) {
            return;
        }
        
        // Don't do anything if the interval didn't change
        Boolean intervalChanged = (Boolean) context.get(KEY_INTERVAL_CHANGED);
        if (intervalChanged == null) {
            ZimbraLog.datasource.warn("%s: unable to determine if polling interval changed.",
                DataSourceCallback.class.getSimpleName());
            return;
        }
        if (!intervalChanged) {
            return;
        }
        
        // Update schedules for any affected data sources
        try {
            if (entry instanceof DataSource) {
                scheduleDataSource((DataSource) entry);
            } else if (entry instanceof Account) {
                scheduleAccount((Account) entry);
            } else if (entry instanceof Cos) {
                scheduleCos((Cos) entry);
            }
        } catch (ServiceException e) {
            ZimbraLog.datasource.warn("Unable to update schedule for %s", entry, e);
        }
    }
    
    private void validateDataSource(DataSource ds, String newInterval)
    throws ServiceException {
        Account account = ds.getAccount();
        if (account == null) {
            ZimbraLog.datasource.warn("Could not determine account for %s", ds);
            return;
        }
        validateInterval(newInterval, account.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
    }
    
    private void scheduleDataSource(DataSource ds)
    throws ServiceException {
        Account account = ds.getAccount();
        if (account == null) {
            ZimbraLog.datasource.warn("Could not determine account for %s", ds);
            return;
        }
        DataSourceManager.updateSchedule(account.getId(), ds.getId());
    }
    
    private void validateAccount(Account account, String newInterval)
    throws ServiceException {
        validateInterval(newInterval, account.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
    }
    
    private void scheduleAccount(Account account)
    throws ServiceException {
        List<DataSource> dataSources = Provisioning.getInstance().getAllDataSources(account);
        for (DataSource ds : dataSources) {
            DataSourceManager.updateSchedule(account.getId(), ds.getId());
        }
    }
    
    private void validateCos(Cos cos, String newInterval)
    throws ServiceException {
        validateInterval(newInterval, cos.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
    }
    
    /**
     * Updates data source schedules for all accounts that are on the current server
     * and in the given COS. 
     */
    private void scheduleCos(Cos cos)
    throws ServiceException {
        // Look up all account id's for this server
        Connection conn = null;
        Set<String> accountIds = null;
        
        try {
            conn = DbPool.getConnection();
            accountIds = DbMailbox.getAccountIds(conn);
        } finally {
            DbPool.quietClose(conn);
        }
        
        // Update schedules for all data sources on this server
        Provisioning prov = Provisioning.getInstance();
        for (String accountId : accountIds) {
            Account account = null;
            try {
                account = prov.get(AccountBy.id, accountId);
            } catch (ServiceException e) {
                ZimbraLog.datasource.debug("Unable to look up account for id %s: %s", accountId, e.toString());
            }
            if (account != null && Provisioning.ACCOUNT_STATUS_ACTIVE.equals(account.getAccountStatus(prov))) {
                Cos accountCos = prov.getCOS(account);
                if (accountCos != null && cos.getId().equals(accountCos.getId())) {
                    scheduleAccount(account);
                }
            }
            
        }
    }
    
    private void validateInterval(String newInterval, String minInterval)
    throws ServiceException {
        long interval = DateUtil.getTimeInterval(newInterval, 0);
        if (interval == 0) {
            return;
        }
        long lMinInterval = DateUtil.getTimeInterval(minInterval, 0);
        if (interval < lMinInterval) {
            String msg = String.format(
                "Polling interval value %s is shorter than the allowed minimum of %s.",
                newInterval, minInterval);
            throw ServiceException.INVALID_REQUEST(msg, null);
        }
    }
}
