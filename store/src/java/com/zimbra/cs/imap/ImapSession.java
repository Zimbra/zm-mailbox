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

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Objects;
import com.zimbra.client.ZBaseItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapHandler.ImapExtension;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.session.PendingLocalModifications;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.session.Session;

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
        protected synchronized void queueCreate(int changeId, MailItem item) {
          getQueuedLocalNotifications(changeId).recordCreated(item);
        }

        @Override
        protected synchronized void queueCreate(int changeId, ZBaseItem item) {
          ZimbraLog.imap.warn("Unexpected call to queueCreate %s", ZimbraLog.getStackTrace(20));
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

    @Override
    protected boolean requiresReload() {
        ImapFolderData fdata = mFolder;
        return fdata instanceof ImapFolder ? false : ((PagedFolderData) fdata).notificationsFull();
    }

    @Override
    protected void inactivate() {
        if (!isInteractive()) {
            return;
        }

        if (isWritable()) {
            snapshotRECENT();
        }

        mFolder.endSelect();
        // removes this session from the global SessionCache, *not* from ImapSessionManager
        removeFromSessionCache();
        handler = null;
    }

    /** If the folder is selected READ-WRITE, updates its high-water RECENT
     *  change ID so that subsequent IMAP sessions do not see the loaded
     *  messages as \Recent. */
    private void snapshotRECENT() {
        try {
            Mailbox mbox = (Mailbox) mailbox;
            if (mbox != null && isWritable()) {
                mbox.recordImapSession(mFolderId);
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
    public void doEncodeState(Element parent) {
        mFolder.doEncodeState(parent.addNonUniqueElement("imap"));
    }

    // XXX: need to handle the abrupt disconnect case, the LOGOUT case, the timeout case, and the too-many-sessions disconnect case
    @Override
    public Session unregister() {
        MANAGER.closeFolder(this, true);
        return detach();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void notifyPendingChanges(PendingModifications pnsIn, int changeId, Session source) {
        PendingLocalModifications pns = (PendingLocalModifications) pnsIn;
        if (!pns.hasNotifications()) {
            return;
        }

        ImapHandler i4handler = handler;
        try {
            synchronized (this) {
                AddedItems added = new AddedItems();
                if (pns.deleted != null) {
                    for (Map.Entry<ModificationKey, Change> entry : pns.deleted.entrySet()) {
                        handleDelete(changeId, entry.getKey().getItemId(), entry.getValue());
                    }
                }
                if (pns.created != null) {
                    for (MailItem item : pns.created.values()) {
                        handleCreate(changeId, item, added);
                    }
                }
                if (pns.modified != null) {
                    for (Change chg : pns.modified.values()) {
                        handleModify(changeId, chg, added);
                    }
                }

                // add new messages to the currently selected mailbox
                if (!added.isEmpty()) {
                    mFolder.handleAddedMessages(changeId, added);
                }

                mFolder.finishNotification(changeId);
            }

            if (i4handler != null && i4handler.isIdle()) {
                i4handler.sendNotifications(true, true);
            }
        } catch (IOException e) {
            // ImapHandler.dropConnection clears our mHandler and calls SessionCache.clearSession,
            //   which calls Session.doCleanup, which calls Mailbox.removeListener
            if (ZimbraLog.imap.isDebugEnabled()) { // with stack trace
                ZimbraLog.imap.debug("Failed to notify, closing %s", this, e);
            } else { // without stack trace
                ZimbraLog.imap.info("Failed to notify (%s), closing %s", e.toString(), this);
            }
            if (i4handler != null) {
                i4handler.close();
            }
        }
    }

    void handleDelete(int changeId, int id, Change chg) {
        MailItem.Type type = (MailItem.Type) chg.what;
        if (id <= 0) {
            return;
        } else if (type == MailItem.Type.TAG) {
            mFolder.handleTagDelete(changeId, id, chg);
        } else if (id == mFolderId) {
            // Once the folder's gone, there's no point in keeping an IMAP Session listening on it around.
            detach();
            // notify client that mailbox is deselected due to delete?
            // RFC 2180 3.3: "The server MAY allow the DELETE/RENAME of a multi-accessed
            //                mailbox, but disconnect all other clients who have the
            //                mailbox accessed by sending a untagged BYE response."
            if (handler != null) {
                handler.close();
            }
        } else if (ImapMessage.SUPPORTED_TYPES.contains(type)) {
            mFolder.handleItemDelete(changeId, id, chg);
        }
    }

    private void handleCreate(int changeId, MailItem item, AddedItems added) {
        if (item == null || item.getId() <= 0) {
            return;
        } else if (item.getFolderId() == mFolderId && (item instanceof Message || item instanceof Contact)) {
            mFolder.handleItemCreate(changeId, item, added);
        }
    }

    private void handleModify(int changeId, Change chg, AddedItems added) {
        if (chg.what instanceof Tag && (chg.why & Change.NAME) != 0) {
            mFolder.handleTagRename(changeId, (Tag) chg.what, chg);
        } else if (chg.what instanceof Folder && ((Folder) chg.what).getId() == mFolderId) {
            Folder folder = (Folder) chg.what;
            if ((chg.why & Change.FLAGS) != 0 && (folder.getFlagBitmask() & Flag.BITMASK_DELETED) != 0) {
                // notify client that mailbox is deselected due to \Noselect?
                // RFC 2180 3.3: "The server MAY allow the DELETE/RENAME of a multi-accessed
                //                mailbox, but disconnect all other clients who have the
                //                mailbox accessed by sending a untagged BYE response."
                if (handler != null) {
                    handler.close();
                }
            } else if ((chg.why & (Change.FOLDER | Change.NAME)) != 0) {
                mFolder.handleFolderRename(changeId, folder, chg);
            }
        } else if (chg.what instanceof Message || chg.what instanceof Contact) {
            MailItem item = (MailItem) chg.what;
            boolean inFolder = mIsVirtual || item.getFolderId() == mFolderId;
            if (!inFolder && (chg.why & Change.FOLDER) == 0) {
                return;
            }
            mFolder.handleItemUpdate(changeId, chg, added);
        }
    }

    @Override
    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        helper.add("path", mPath).add("folderId", mFolderId);
        if ((mFolder == null) || ((mPath != null) && (!mPath.toString().equals(mFolder.toString())))) {
            helper.add("folder", mFolder);
        }
        if (mIsVirtual) {
            helper.add("isVirtual", mIsVirtual);
        }
        if (handler == null) {
            helper.add("handler", handler);
        }
        return helper;
    }

    @Override
    protected PagedFolderData createPagedFolderData(boolean active, ImapFolder folder) throws ServiceException {
        return new PagedLocalFolderData(serialize(active), folder);
    }
}

