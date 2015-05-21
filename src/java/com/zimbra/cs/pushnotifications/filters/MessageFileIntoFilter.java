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

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;

public class MessageFileIntoFilter implements Filter {

    private Message message;
    private Account account;

    public static final String DATA_SOURCE_INBOX = "Inbox";

    public MessageFileIntoFilter(Account account, Message message) {
        this.message = message;
        this.account = account;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.filters.Filter#apply()
     */
    @Override
    public boolean apply() {
        if (message == null) {
            return false;
        }

        if (!message.isUnread()) {
            ZimbraLog.mailbox.debug("Message is read");
            return false;
        }

        int folderId = message.getFolderId();
        if (Mailbox.ID_FOLDER_INBOX == folderId) {
            return true;
        } else if (isDataSourceInbox(folderId)) {
            return true;
        }
        return false;
    }

    private boolean isDataSourceInbox(int folderId) {
        try {
            Mailbox mbox = message.getMailbox();
            List<DataSource> dataSources = account.getAllDataSources();
            for (DataSource dataSource : dataSources) {
                Folder dataSourceFolder = mbox.getFolderById(null, dataSource.getFolderId());
                List<Folder> subFolders = dataSourceFolder.getSubfolders(null);
                for (Folder folder : subFolders) {
                    if (DATA_SOURCE_INBOX.equals(folder.getName()) && folderId == folder.getId()) {
                        return true;
                    }
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Exception in processing MessageFileIntoFilter");
            return false;
        }
        return false;
    }

}
