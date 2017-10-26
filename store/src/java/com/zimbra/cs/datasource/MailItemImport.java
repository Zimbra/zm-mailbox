/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.MessageCallbackContext;
import com.zimbra.cs.mailbox.Mailbox.MessageCallback.Type;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.purge.DataSourcePurge;
import com.zimbra.cs.purge.DataSourcePurge.ConversationPurgeQueue;
import com.zimbra.cs.purge.PurgeFromAllDataSources;
import com.zimbra.cs.purge.PurgeFromIncomingDataSource;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.type.DataSource.ConnectionType;

public abstract class MailItemImport implements DataSource.DataImport {
    protected final DataSource dataSource;
    protected final Mailbox mbox;
    protected int usage;

    private static final Map<String, PurgeLock> purgeLocks = new ConcurrentHashMap<String, PurgeLock>();
    private int MAX_PURGE_ATTEMPTS = 10;

    public MailItemImport(DataSource ds) throws ServiceException {
        this(ds, false);
    }

    public MailItemImport(DataSource ds, boolean test) throws ServiceException {
        dataSource = ds;
        mbox = ds.getAccount() == null && test ? null : DataSourceManager.getInstance().getMailbox(ds);
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
        CurrentUsage usage = new CurrentUsage(size);
        switch (folderId) {
        case Mailbox.ID_FOLDER_INBOX:
            try {
                List<ItemId> addedMessageIds = RuleManager.applyRulesToIncomingMessage(octxt, mbox, pm, size,
                    dataSource.getEmailAddress(), dc, Mailbox.ID_FOLDER_INBOX, true);
                Integer newMessageId = getFirstLocalId(addedMessageIds);
                if (newMessageId == null) {
                    return null; // Message was discarded or filed remotely
                } else {
//                    purgeIfNecessary(octxt, usage, pm);
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
            DeliveryOptions dopts = new DeliveryOptions().setFolderId(folderId).setFlags(flags);
            Folder folder = mbox.getFolderById(octxt, folderId);
            String folderName = folder.getName();
            if (folderName.startsWith("/")) {
                folderName = folderName.substring(1);
            }
            MessageCallbackContext ctxt = null;
            if (folderName.equalsIgnoreCase("sent")) {
                ctxt = new MessageCallbackContext(Type.sent);
            } else if (!folderName.equalsIgnoreCase("drafts") && !folderName.equalsIgnoreCase("trash")) {
                ctxt = new MessageCallbackContext(Type.received);
                ctxt.setRecipient(dataSource.getEmailAddress());
            }
            if (ctxt != null) {
                ctxt.setDataSourceId(dataSource.getId());
                dopts.setCallbackContext(ctxt);
            }
            msg = mbox.addMessage(octxt, pm, dopts, null);
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

    private static synchronized PurgeLock getPurgeLock(Account account) {
        PurgeLock lock;
        lock = purgeLocks.get(account.getId());
        if (lock == null) {
            lock = new PurgeLock();
            purgeLocks.put(account.getId(), lock);
        }
        return lock;
    }

    protected void purgeIfNecessary(OperationContext octxt) throws ServiceException {
        purgeIfNecessary(octxt, null, null);
    }

    protected void purgeIfNecessary(OperationContext octxt, CurrentUsage usage, ParsedMessage pm) throws ServiceException {
        if (!mbox.getAccount().isFeatureDataSourcePurgingEnabled()) {
            return;
        }
        if (usage == null) {
            usage = new CurrentUsage();
        }
        // first, see if the incoming message is part of an existing conversation thread
        Integer thisConvId = null;
        if (pm != null) {
            List<Conversation> matchingConvs = mbox.lookupConversation(pm);
            if (matchingConvs.size() > 0) {
                Collections.sort(matchingConvs, new MailItem.SortSizeDescending());
                thisConvId = matchingConvs.remove(0).getId();
            }
            // regardless of whether we have to purge, remove PurgeableConv instances representing the current conversation
            // from the purge queue
            if (thisConvId != null) {
                if (thisConvId < 0) {
                    // the incoming message is the second message in a conversation, so the PurgeableConv entry in the queue
                    // will be under the ID of the first message
                    ConversationPurgeQueue.removeAllNodesById(-1 * thisConvId);
                } else {
                    // the incoming message is part of a conversation that already has a dedicated ID, so the PurgeableConv
                    // entry will be under the ID of the conversation
                    ConversationPurgeQueue.removeAllNodesById(thisConvId);
                }
            }
        }
        if (usage.spaceToFreeUp > 0 && !usage.sizeOverQuota) {
            // see if a purge process is already running
            PurgeLock purgeLock = getPurgeLock(mbox.getAccount());
            try {
                synchronized (purgeLock) {
                    if (purgeLock.isLocked()) {
                        purgeLock.lock();; //wait and acquire purge lock
                        usage.calculate(); //could have changed since previous purge
                        if (usage.spaceToFreeUp <= 0) {
                            return;
                        }
                    } else {
                        purgeLock.lock();
                    }
                }
                DataSourcePurge purge;
                if (usage.overTotalQuota) {
                    purge = new PurgeFromAllDataSources(mbox);
                } else {
                    purge = new PurgeFromIncomingDataSource(mbox);
                }
                purge.purgeConversations(octxt, dataSource, usage.spaceToFreeUp, thisConvId);
                usage.calculate();
                int attempt = 0;
                while (usage.spaceToFreeUp > 0 && !usage.sizeOverQuota && attempt < MAX_PURGE_ATTEMPTS) {
                    attempt++;
                    // This is possible in some edge cases, such as when some purged conversations
                    // spanned multiple data sources, which would make the actual purged size less than
                    // the conversation size.
                    ZimbraLog.datasource.warn(String.format("still need to free up %d bytes!", usage.spaceToFreeUp));
                    purge.purgeConversations(octxt, dataSource, usage.spaceToFreeUp, thisConvId);
                    usage.calculate();
                }
            } finally {
                purgeLock.unlock();
            }
        }
    }

    public class CurrentUsage {
        private int size;
        protected long spaceToFreeUp;
        protected boolean overDsQuota;
        protected boolean willBeOverDsQuota;
        protected boolean overTotalQuota;
        protected boolean willBeOverTotalQuota;
        protected boolean sizeOverQuota;

        public CurrentUsage() throws ServiceException {
            this(0);
        }

        public CurrentUsage(int incomingMessageSize) throws ServiceException {
            this.size = incomingMessageSize;
            calculate();
        }

        private void calculate() throws ServiceException {
            long quota = dataSource.getQuota(mbox.getAccount());
            long totalQuota = mbox.getAccount().getDataSourceTotalQuota();
            long thisDsUsage = dataSource.getUsage();
            long totalDsUsage = mbox.getTotalDataSourceUsage();
            if ((quota > 0 && size > quota) || (totalQuota > 0 && size > totalQuota)) {
                sizeOverQuota = true;
            } else {
                sizeOverQuota = false;
            }
            long thisDsSpaceToFreeUp = Math.max(0, thisDsUsage + size - quota);
            long totalDsSpaceToFreeUp = totalQuota == 0L ? 0 : Math.max(0, totalDsUsage + size - totalQuota);
            spaceToFreeUp = Math.max(thisDsSpaceToFreeUp, totalDsSpaceToFreeUp);
            overDsQuota = thisDsUsage > quota;
            willBeOverDsQuota = thisDsUsage + size > quota;
            overTotalQuota = totalQuota == 0L ? false : totalDsUsage > totalQuota;
            willBeOverTotalQuota = totalQuota == 0L ? false : totalDsUsage + size > totalQuota;
        }
    }

    public static class PurgeLock {

        private boolean locked;
        private ReentrantLock lock = new ReentrantLock(true);

        public PurgeLock() {
            locked = false;
        }

        public void lock() {
            lock.lock();
            locked = true;
        }

        public void unlock() {
            lock.unlock();
            locked = false;
        }

        public boolean isLocked() {
            return locked;
        }
    }

}
