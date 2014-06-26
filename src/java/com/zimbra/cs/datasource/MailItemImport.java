/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.type.DataSource.ConnectionType;

import java.io.IOException;
import java.util.List;

public abstract class MailItemImport implements DataSource.DataImport {
    protected final DataSource dataSource;
    protected final Mailbox mbox;

    public MailItemImport(DataSource ds) throws ServiceException {
        dataSource = ds;
        mbox = DataSourceManager.getInstance().getMailbox(ds);
    }

    public void validateDataSource() throws ServiceException {
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

    public boolean isOffline() {
        return getDataSource().isOffline();
    }


    public Message addMessage(OperationContext octxt, ParsedMessage pm, int size,
                                 int folderId, int flags, DeliveryContext dc)
        throws ServiceException, IOException {
        Message msg = null;
        switch (folderId) {
        case Mailbox.ID_FOLDER_INBOX:
            try {
                List<ItemId> addedMessageIds = RuleManager.applyRulesToIncomingMessage(octxt, mbox, pm, size,
                    dataSource.getEmailAddress(), dc, Mailbox.ID_FOLDER_INBOX, true);
                Integer newMessageId = getFirstLocalId(addedMessageIds);
                if (newMessageId == null) {
                    return null; // Message was discarded or filed remotely
                } else {
                    msg = mbox.getMessageById(null, newMessageId);
                }
                // Set flags (setting of BITMASK_UNREAD is implicit)
                if (flags != Flag.BITMASK_UNREAD) {
                    // Bug 28275: Cannot set DRAFT flag after message has been created
                    flags &= ~Flag.BITMASK_DRAFT;
                    mbox.setTags(octxt, newMessageId, MailItem.Type.MESSAGE, flags, MailItem.TAG_UNCHANGED);
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
            msg = mbox.addMessage(octxt, pm, new DeliveryOptions().setFolderId(folderId).setFlags(flags), null);
        }
        return msg;
    }

    public boolean isSslEnabled() {
        return dataSource.getConnectionType() == ConnectionType.ssl;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Mailbox getMailbox() {
        return mbox;
    }

    public Integer getFirstLocalId(List<ItemId> idList) {
        if (idList == null) {
            return null;
        }
        for (ItemId id : idList) {
            if (id.belongsTo(mbox)) {
                return id.getId();
            }
        }
        return null;
    }

    public void checkIsEnabled() throws ServiceException {
        if (!getDataSource().isManaged()) {
            throw ServiceException.FAILURE(
                "Import aborted because data source has been deleted or disabled", null);
        }
    }
}
