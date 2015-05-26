/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.pushnotifications.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.util.AccountUtil;

/**
 * Create and execute {@link FilterChain}
 */
public class FilterManager {

    /**
     * Default filter chain that can be executed before all push notifications
     * @param account
     * @return TRUE if filter chain passes else FALSE
     */
    public static boolean executeDefaultFilters(Account account) {
        FilterChain filterChain = new DefaultFilterChain(account);
        return filterChain.execute();
    }

    /**
     * Executes a filter chain before a new message push notification
     * 
     * @param account
     * @param message
     * @param datasource
     * @return TRUE if filter chain passes else FALSE
     */
    public static boolean executeNewMessageFilters(Account account, Message message,
        DataSource dataSource) {
        FilterChain filterChain = new NewMessageFilterChain(account, message, dataSource);
        return filterChain.execute();
    }

    /**
     * Executes a filter chain before a new message push notification
     * 
     * @param account
     * @param message
     * @return TRUE if filter chain passes else FALSE
     */
    public static boolean executeNewMessageFilters(Account account, Message message) {
        FilterChain filterChain = new NewMessageFilterChain(account, message, getDataSource(
            account, message));
        return filterChain.execute();
    }

    public static DataSource getDataSource(Account account, Message msg) {
        Mailbox mbox;
        try {
            if (account == null || msg == null) {
                return null;
            }
            mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            Map<Integer, DataSource> dataSourceMap = new HashMap<Integer, DataSource>();
            List<DataSource> dataSources = account.getAllDataSources();
            if (dataSources != null) {
                for (DataSource dataSource : dataSources) {
                    int dataSourceFolderId = dataSource.getFolderId();
                    if ((dataSourceFolderId != -1) && (dataSource.getEmailAddress() != null)) {
                        dataSourceMap.put(dataSourceFolderId, dataSource);
                    }
                }
                return dataSourceMap.get(AccountUtil.getRootFolderIdForItem(msg, mbox,
                    dataSourceMap.keySet()));
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.debug("Exception in retriving data source", e);
            return null;
        }
        return null;
    }

}
