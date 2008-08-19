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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mime.ParsedMessage;

import java.io.IOException;
import java.util.List;

public abstract class MailItemImport implements DataSource.DataImport {
    protected final DataSource dataSource;

    private static final RuleManager RULE_MANAGER = RuleManager.getInstance();

    protected MailItemImport(DataSource ds) {
        dataSource = ds;
    }

    public abstract String test() throws ServiceException;

    public abstract void importData(List<Integer> folderIds, boolean fullSync)
        throws ServiceException;

    protected void validateDataSource() throws ServiceException {
        DataSource ds = getDataSource();
        if (ds.getHost() == null) {
            throw ServiceException.FAILURE(ds + ": host not set", null);
        }
        if (ds.getPort() == null) {
            throw ServiceException.FAILURE(ds + ": port not set", null);
        }
        if (ds.getConnectionType() == null) {
            throw ServiceException.FAILURE(ds + ": connectionType not set", null);
        }
        if (ds.getUsername() == null) {
            throw ServiceException.FAILURE(ds + ": username not set", null);
        }
    }

    protected boolean isOffline() {
        return getDataSource().isOffline();
    }
    
    protected Message offlineAddMessage(ParsedMessage pm, int folderId, int flags)
        throws ServiceException, IOException {
        Mailbox mbox = dataSource.getMailbox();
        SharedDeliveryContext context = new SharedDeliveryContext();
        Message msg = null;
        switch (folderId) {
        case Mailbox.ID_FOLDER_INBOX:
            try {
                msg = RULE_MANAGER.applyRules(mbox.getAccount(), mbox, pm, pm.getRawSize(),
                    dataSource.getEmailAddress(), context, Mailbox.ID_FOLDER_INBOX);
                if (msg == null) {
                    return null; // Message was discarded
                }
                // Set flags (setting of BITMASK_UNREAD is implicit)
                if (flags != Flag.BITMASK_UNREAD) {
                    // Bug 28275: Cannot set DRAFT flag after message has been created
                    flags &= ~Flag.BITMASK_DRAFT;
                    mbox.setTags(null, msg.getId(), MailItem.TYPE_MESSAGE,
                                 flags, MailItem.TAG_UNCHANGED);
                }
            } catch (Exception e) {
                ZimbraLog.datasource.warn("Error applying filter rules", e);
            }
            break;
        case Mailbox.ID_FOLDER_DRAFTS:
        case Mailbox.ID_FOLDER_SENT:
            flags |= Flag.BITMASK_FROM_ME;
            break;
        }
        if (msg == null) {
            msg = mbox.addMessage(null, pm, folderId, false, flags, null);
        }
        return msg;
    }

    public boolean isSslEnabled() {
        return dataSource.getConnectionType() == DataSource.ConnectionType.ssl;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
