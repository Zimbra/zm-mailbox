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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;

public class DataSourceInitialSyncFilter implements Filter {

    private Message message;
    private Account account;
    private DataSource dataSource;
    private Folder inboxFolder;

    public static final int MESSAGE_COUNT_FOR_INITIAL_SYNC = 20;

    public DataSourceInitialSyncFilter(Account account, Message message, DataSource dataSource) {
        this.message = message;
        this.account = account;
        this.dataSource = dataSource;
    }

    /**
     * @return the inboxFolder
     */
    public Folder getInboxFolder() {
        return inboxFolder;
    }

    /**
     * @param inboxFolder the inboxFolder to set
     */
    public void setInboxFolder(Folder inboxFolder) {
        this.inboxFolder = inboxFolder;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.filters.Filter#apply()
     */
    @Override
    public boolean apply() {
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            if (dataSource == null || mbox == null || message == null) {
                return false;
            }

            if (inboxFolder != null && inboxFolder.getItemCount() == MESSAGE_COUNT_FOR_INITIAL_SYNC) {
                return true;
            }
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn(
                "ZMG: Exception in processing DataSourceInitialSyncFilter mid=%s", account.getId(),
                e);
            return false;
        }
        return false;
    }

}
