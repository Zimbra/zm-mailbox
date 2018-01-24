/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapHandler.ImapExtension;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.session.PendingLocalModifications;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;

public class ImapSession extends ImapListener {

    protected class PagedLocalFolderData extends ImapListener.PagedFolderData {

        PagedLocalFolderData(String cachekey, ImapFolder i4folder) {
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
                queuedChanges.put(changeId, pns = new PendingLocalModifications());
            }
            return pns;
        }

        private PendingLocalModifications getQueuedLocalNotifications(int changeId) {
            return (PendingLocalModifications) getQueuedNotifications(changeId);
        }

        @Override
        protected synchronized void queueCreate(int changeId, BaseItemInfo item) {
          getQueuedLocalNotifications(changeId).recordCreated(item);
        }

        @Override
        protected synchronized void queueModify(int changeId, Change chg) {
            getQueuedLocalNotifications(changeId).recordModified((MailItem) chg.what, chg.why, (MailItem) chg.preModifyObj);
        }
    }

    ImapSession(ImapMailboxStore imapStore, ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(imapStore, i4folder, handler);
    }

    @Override
    public boolean hasNotifications() {
        return mFolder.hasNotifications();
    }

    /** Returns whether a given SELECT option is active for this folder. */
    boolean isExtensionActivated(ImapExtension ext) {
        switch (ext) {
            case CONDSTORE:
                return !mIsVirtual && handler.sessionActivated(ext);
            default:
                return false;
        }
    }

    synchronized int getFootprint() {
        // FIXME: consider saved search results, in-memory data for paged sessions
        return mFolder instanceof ImapFolder ? ((ImapFolder) mFolder).getSize() : 0;
    }

    /** If the folder is selected READ-WRITE, updates its high-water RECENT
     *  change ID so that subsequent IMAP sessions do not see the loaded
     *  messages as \Recent. */
    @Override
    protected void snapshotRECENT() {
        try {
            Mailbox mbox = (Mailbox) mailbox;
            if (mbox != null && isWritable()) {
                mbox.recordImapSession(folderId.id);
            }
        } catch (MailServiceException.NoSuchItemException nsie) {
            // don't log if the session expires because the folder was deleted out from under it
        } catch (MailServiceException.MailboxInMaintenanceException miMe) {
            if (ZimbraLog.session.isDebugEnabled()) {
                ZimbraLog.session.info(
                        "Mailbox in maintenance detected recording unloaded session's RECENT limit %s", this, miMe);
            } else {
                ZimbraLog.session.info(
                        "Mailbox in maintenance detected recording unloaded session's RECENT limit %s", this);
            }
        } catch (Exception e) {
            ZimbraLog.session.warn("exception recording unloaded session's RECENT limit %s", this, e);
        }
    }

    @Override
    protected boolean isMailboxListener() {
        return true;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return true;
    }

    @Override
    protected void notifyPendingCreates(@SuppressWarnings("rawtypes") PendingModifications pnsIn,
            int changeId, AddedItems added) {
        PendingLocalModifications pns = (PendingLocalModifications) pnsIn;
        if (pns.created != null) {
            for (BaseItemInfo item : pns.created.values()) {
                try {
                    handleCreate(changeId, item, added);
                } catch (ServiceException e) {
                    ZimbraLog.imap.warn("error handling creation of item in changeId %s", changeId);
                }
            }
        }
    }

    private void handleCreate(int changeId, BaseItemInfo item, AddedItems added) throws ServiceException {
        if (item == null || item.getIdInMailbox() <= 0) {
            return;
        } else if (item.getFolderIdInMailbox() == folderId.id && (item instanceof Message || item instanceof Contact)) {
            mFolder.handleItemCreate(changeId, item, added);
        }
    }

    @Override
    protected PagedFolderData createPagedFolderData(boolean active, ImapFolder folder) throws ServiceException {
        return new PagedLocalFolderData(serialize(active), folder);
    }
}

