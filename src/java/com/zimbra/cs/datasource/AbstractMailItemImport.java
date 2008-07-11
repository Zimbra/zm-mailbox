/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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

import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.SystemUtil;

import java.io.IOException;

/**
 * Common base class for both ImapImport and Pop3Import.
 */
public abstract class AbstractMailItemImport implements MailItemImport {
    protected final DataSource dataSource;

    protected AbstractMailItemImport(DataSource ds) {
        dataSource = ds;
    }

    protected void validateDataSource() throws ServiceException {
        DataSourceUtil.validateDataSource(getDataSource());
    }
    
    private static final RuleManager RULE_MANAGER = RuleManager.getInstance();

    protected Message addMessage(ParsedMessage pm, int folderId, int flags)
        throws ServiceException, IOException {
        Mailbox mbox = dataSource.getMailbox();
        SharedDeliveryContext context = new SharedDeliveryContext();
        Message msg = null;
        switch (folderId) {
        case Mailbox.ID_FOLDER_INBOX:
            try {
                msg = RULE_MANAGER.applyRules(mbox.getAccount(), mbox, pm,
                    pm.getRawSize(), dataSource.getEmailAddress(), context, Mailbox.ID_FOLDER_INBOX);
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
