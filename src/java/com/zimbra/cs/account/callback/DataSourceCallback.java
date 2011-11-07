/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.util.Zimbra;

/**
 * Validates <tt>DataSource</tt> attribute values.
 */
public class DataSourceCallback extends AttributeCallback {

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
     * Confirms that polling interval values are not set lower than the minimum.
     */
    @SuppressWarnings("unchecked")
    @Override 
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {
        if (INTERVAL_ATTRS.contains(attrName)) {
            String interval = (String) attrValue;
            if (entry instanceof DataSource) {
                validateDataSource((DataSource) entry, interval);
            } else if (entry instanceof Account) {
                validateAccount((Account) entry, attrName, interval);
            } else if (entry instanceof Cos) {
                validateCos((Cos) entry, attrName, interval);
            }
        }
    }

    /**
     * Updates scheduled tasks for data sources whose polling interval has changed.
     */
    @SuppressWarnings("unchecked")
    @Override 
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // Don't do anything unless inside the server
        if (!Zimbra.started() || !LC.data_source_scheduling_enabled.booleanValue()) {
            return;
        }
        
        // Don't do anything if this postModify is triggered by creating a COS,
        // because no account will be on this COS yet.
        if (context.isCreate() && (entry instanceof Cos))
            return;

        if (INTERVAL_ATTRS.contains(attrName) || Provisioning.A_zimbraDataSourceEnabled.equals(attrName)) {
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
        } else if (entry instanceof DataSource) {
            // Reset error status on any attribute changes (bug 39050).
            DataSourceManager.resetErrorStatus((DataSource) entry);
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
        DataSourceManager.updateSchedule(account, ds);
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
            DataSourceManager.updateSchedule(account, ds);
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

        List<Account> accts;
        Provisioning prov = Provisioning.getInstance();

        // Look up all account id's for this server
        if (prov instanceof LdapProv)
            accts = lookupAccountsFromLDAP(prov, cos.getId());
        else
            accts = lookupAccountsFromDB(prov);

        // Update schedules for all data sources on this server
        for (Account account : accts) {
            if (account != null && Provisioning.ACCOUNT_STATUS_ACTIVE.equals(account.getAccountStatus(prov))) {
                Cos accountCos = prov.getCOS(account);
                if (accountCos != null && cos.getId().equals(accountCos.getId())) {
                    scheduleAccount(account);
                }
            }

        }
    }

    // look up all accounts on this server
    private List<Account> lookupAccountsFromDB(Provisioning prov) throws ServiceException {
        Set<String> accountIds = null;
        List<Account> accts = new ArrayList<Account>();

        DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            accountIds = DbMailbox.listAccountIds(conn);
        } finally {
            DbPool.quietClose(conn);
        }

        for (String accountId : accountIds) {
            Account account = null;
            try {
                account = prov.get(AccountBy.id, accountId);
            } catch (ServiceException e) {
                ZimbraLog.datasource.debug("Unable to look up account for id %s: %s", accountId, e.toString());
            }

            if (account != null) {
                accts.add(account);
            }
        }

        return accts;
    }

    /*
     * look up all accounts on this server with either the specified cos id, or without a cos id set on the account
     * returns:
     *   - all accounts on this server
     *   - and with either the specified cos id, or without a cos id set on the account
     *   - and has at least one sub-entries
     *     (we can't tell whether those sub-entries are data sources, but this is as close as we can be searching for)
     */
    private List<Account> lookupAccountsFromLDAP(Provisioning prov, String cosId)
    throws ServiceException{

        SearchAccountsOptions searchOpts = new SearchAccountsOptions();
        searchOpts.setFilter(ZLdapFilterFactory.getInstance().accountsOnServerAndCosHasSubordinates(
                prov.getLocalServer().getServiceHostname(), cosId));
        List accts = prov.searchDirectory(searchOpts);

        return accts;
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
