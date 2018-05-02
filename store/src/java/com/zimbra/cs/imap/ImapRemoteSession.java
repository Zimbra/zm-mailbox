/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.util.TreeMap;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingRemoteModifications;

public class ImapRemoteSession extends ImapListener {
    protected class PagedRemoteFolderData extends ImapListener.PagedFolderData {

        PagedRemoteFolderData(String cachekey, ImapFolder i4folder) {
            super(cachekey, i4folder);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected PendingModifications getQueuedNotifications(int changeId) {
            if (queuedChanges == null) {
                queuedChanges = new TreeMap<Integer, PendingModifications>();
            }
            PendingModifications pns = queuedChanges.get(changeId);
            if (pns == null) {
                queuedChanges.put(changeId, pns = new PendingRemoteModifications());
            }
            return pns;
        }

        private PendingRemoteModifications getQueuedRemoteNotifications(int changeId) {
            return (PendingRemoteModifications) getQueuedNotifications(changeId);
        }

        @Override
        protected synchronized void queueCreate(int changeId, BaseItemInfo item) {
            getQueuedRemoteNotifications(changeId).recordCreated(item);
        }

        @Override
        protected synchronized void queueModify(int changeId, Change chg) {
            getQueuedRemoteNotifications(changeId).recordModified((BaseItemInfo) chg.what, chg.why, null);
        }
    }

    private void handleCreate(int changeId, BaseItemInfo item, AddedItems added) {
        try {
            if (item == null || item.getIdInMailbox() <= 0) {
                return;
            } else if (item.getFolderIdInMailbox() == folderId.id &&
                (item.getMailItemType() == MailItemType.MESSAGE || item.getMailItemType() == MailItemType.CONTACT)) {
                    mFolder.handleItemCreate(changeId, item, added);
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("Error retrieving ID of item or folder", e);
        }
    }

    protected ImapRemoteSession(ImapMailboxStore imapStore, ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(imapStore, i4folder, handler);
        mailbox = imapStore.getMailboxStore();
    }

    @Override
    protected boolean isMailboxListener() {
        return false;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return true;
    }

    private void unregisterFromRemoteServer() {
        try {
            MailboxStore mbs = mailbox;
            if (mbs == null) {
                ZimbraLog.imap.info(
                        "ImapRemoteSession.unregisterFromRemoteServer called but mailbox=null - %s\n%s",
                        this, ZimbraLog.getStackTrace(5));
                return;
            }
            ImapServerListener listener =
                    ImapServerListenerPool.getInstance().getForAccountId(mbs.getAccountId());
            if (listener == null) {
                ZimbraLog.imap.info(
                        "ImapRemoteSession.unregisterFromRemoteServer called but listener=null - %s\n%s",
                        this, ZimbraLog.getStackTrace(5));
                return;
            }
            listener.removeListener(this);
        } catch (ServiceException e) {
            ZimbraLog.imap.error(e);
        }
    }

    @Override
    public ImapListener detach() {
        unregisterFromRemoteServer();
        return super.detach();
    }

    @Override
    protected void notifyPendingCreates(@SuppressWarnings("rawtypes") PendingModifications pnsIn,
            int changeId, AddedItems added) {
        PendingRemoteModifications pns = (PendingRemoteModifications) pnsIn;
        if (pns.created != null) {
            for (BaseItemInfo item : pns.created.values()) {
                handleCreate(changeId, item, added);
            }
        }
    }

    @Override
    protected PagedFolderData createPagedFolderData(boolean active, ImapFolder folder) throws ServiceException {
        return new PagedRemoteFolderData(serialize(active), folder);
    }

    /**
     * Conditionally update the folder's high-water RECENT change ID.
     * @see com.zimbra.cs.imap.ImapSession#snapshotRECENT()
     */
    @Override
    protected void snapshotRECENT() {
        try {
            ZMailbox mbox = (ZMailbox) mailbox;
            if (mbox != null && isWritable()) {
                mbox.recordImapSession(folderId);
            }
        } catch(ServiceException e) {
            ZimbraLog.session.warn("exception recording unloaded session's RECENT limit %s", this, e);
        }
    }

    protected int getLastKnownChangeId() {
        return super.lastChangeId;
    }
}
