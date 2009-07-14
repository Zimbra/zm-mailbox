/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.StringUtil;
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
import com.zimbra.cs.util.Zimbra;

/**
 * Validates <tt>DataSource</tt> attribute values.
 */
public class DataSourceCallback extends AttributeCallback {

    private static final String KEY_INTERVAL_CHANGED = "IntervalChanged";
    private static final Set<String> INTERVAL_ATTRS = new HashSet<String>();
    
    static {
        INTERVAL_ATTRS.add(Provisioning.A_zimbraDataSourcePollingInterval);
        INTERVAL_ATTRS.add(Provisioning.A_zimbraDataSourcePop3PollingInterval);
        INTERVAL_ATTRS.add(Provisioning.A_zimbraDataSourceImapPollingInterval);
        INTERVAL_ATTRS.add(Provisioning.A_zimbraDataSourceLivePollingInterval);
        INTERVAL_ATTRS.add(Provisioning.A_zimbraDataSourceRssPollingInterval);
        INTERVAL_ATTRS.add(Provisioning.A_zimbraDataSourceCaldavPollingInterval);
        INTERVAL_ATTRS.add(Provisioning.A_zimbraDataSourceYabPollingInterval);
        INTERVAL_ATTRS.add(Provisioning.A_zimbraDataSourceCalendarPollingInterval);
    }
    
    /**
      * Confirms that the polling interval set on the data source is at least as long
      * as the minimum set for the account.
      * 
      * @param entry a {@link DataSource} or {@link Account}
      */
    @SuppressWarnings("unchecked")
    @Override public void preModify(Map context, String attrName, Object attrValue,
                                    Map attrsToModify, Entry entry, boolean isCreate)
    throws ServiceException {
        if (isCreate) {
            // No old value, so nothing to do.  Creation is handled in postModify().
            return;
        }
        if (!LC.data_source_scheduling_enabled.booleanValue()) {
            return;
        }
        
        if (INTERVAL_ATTRS.contains(attrName)) {
            context.put(KEY_INTERVAL_CHANGED, willIntervalChange(attrName, attrValue, entry));
        } else if (attrName.equals(Provisioning.A_zimbraDataSourceEnabled)) {
            String oldValue = entry.getAttr(Provisioning.A_zimbraDataSourceEnabled);
            context.put(KEY_INTERVAL_CHANGED, StringUtil.equal(oldValue, (String) attrValue));
        } else {
            context.put(KEY_INTERVAL_CHANGED, false);
        }
    }
    
    private boolean willIntervalChange(String attrName, Object attrValue, Entry entry)
    throws ServiceException {
        String newInterval = (String) attrValue;
        String oldInterval = "";
        if (entry != null) {
            oldInterval = entry.getAttr(attrName);
        }
        
        if (entry instanceof DataSource) {
            validateDataSource((DataSource) entry, newInterval);
        } else if (entry instanceof Account) {
            validateAccount((Account) entry, attrName, newInterval);
        } else if (entry instanceof Cos) {
            validateCos((Cos) entry, attrName, newInterval);
        }

        // Determine if the interval has changed
        long lNewInterval = DateUtil.getTimeInterval(newInterval, 0);
        long lOldInterval = DateUtil.getTimeInterval(oldInterval, 0);
        return (lNewInterval != lOldInterval);
    }
    
    @SuppressWarnings("unchecked")
    @Override public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
        // Don't do anything unless inside the server
        if (!Zimbra.started())
            return;
        if (!LC.data_source_scheduling_enabled.booleanValue()) {
            return;
        }
        
        // Don't do anything if the interval didn't change
        Boolean intervalChanged = isCreate || (Boolean) context.get(KEY_INTERVAL_CHANGED);
        if (intervalChanged == null || !intervalChanged) {
            ZimbraLog.datasource.debug("Polling interval did not change.  Not updating schedule.");
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
        validateInterval(Provisioning.A_zimbraDataSourcePollingInterval,
            newInterval, account.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
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
    
    private void validateAccount(Account account, String attrName, String newInterval)
    throws ServiceException {
        validateInterval(attrName, newInterval, account.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
    }
    
    private void scheduleAccount(Account account)
    throws ServiceException {
        ZimbraLog.datasource.info("Updating schedule for all DataSources for account %s.", account.getName());
        List<DataSource> dataSources = Provisioning.getInstance().getAllDataSources(account);
        for (DataSource ds : dataSources) {
            DataSourceManager.updateSchedule(account.getId(), ds.getId());
        }
    }
    
    private void validateCos(Cos cos, String attrName, String newInterval)
    throws ServiceException {
        validateInterval(newInterval, attrName, cos.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
    }
    
    /**
     * Updates data source schedules for all accounts that are on the current server
     * and in the given COS. 
     */
    private void scheduleCos(Cos cos)
    throws ServiceException {
        ZimbraLog.datasource.info("Updating schedule for all DataSources for all accounts in COS %s.", cos.getName());
        
        // Look up all account id's for this server
        Set<String> accountIds = null;

        synchronized (DbMailbox.getSynchronizer()) {
            Connection conn = null;
            try {
                conn = DbPool.getConnection();
                accountIds = DbMailbox.listAccountIds(conn);
            } finally {
                DbPool.quietClose(conn);
            }
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
    
    private void validateInterval(String attrName, String newInterval, String minInterval)
    throws ServiceException {
        long interval = DateUtil.getTimeInterval(newInterval, 0);
        if (interval == 0) {
            return;
        }
        long lMinInterval = DateUtil.getTimeInterval(minInterval, 0);
        if (interval < lMinInterval) {
            String msg = String.format(
                "Polling interval %s for %s is shorter than the allowed minimum of %s.",
                newInterval, attrName, minInterval);
            throw ServiceException.INVALID_REQUEST(msg, null);
        }
    }
}
