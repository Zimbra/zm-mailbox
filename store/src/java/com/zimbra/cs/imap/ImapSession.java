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

import com.google.common.base.Objects;
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

    @Override
    protected boolean isSerialized() {
        return mFolder instanceof PagedFolderData;
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

    /**
     * Serializes this {@link ImapSession} to the session manager's current {@link ImapSessionManager.FolderSerializer}
     * if it's not already serialized there.
     *
     * @param mem true to use memcached if available, otherwise false
     * @return the cachekey under which we serialized the folder, or {@code null} if the folder was already serialized
     */
    private String serialize(boolean active) throws ServiceException {
        // if the data's already paged out, we can short-circuit
        ImapFolder i4folder = mFolder instanceof ImapFolder ? (ImapFolder) mFolder : null;
        if (i4folder == null) {
            return null;
        }

        if (!isRegistered()) {
            throw ServiceException.FAILURE("cannot serialize unregistered session", null);
        }

        // if it's a disconnected session, no need to track expunges
        if (!isInteractive()) {
            i4folder.collapseExpunged(false);
        }

        String cachekey = MANAGER.cacheKey(this, active);
        MANAGER.serialize(cachekey, i4folder);
        return cachekey;
    }

    /**
     * Unload this session data into cache.
     *
     * @param active true to use active session cache, otherwise use inactive session cache
     */
    @Override
    protected void unload(boolean active) throws ServiceException {
        Mailbox mbox = (Mailbox) mailbox;
        if (mbox == null) {
            return;
        }
        // Mailbox.endTransaction() -> ImapSession.notifyPendingChanges() locks in the order of Mailbox -> ImapSession.
        // Need to lock in the same order here, otherwise can result in deadlock.
        mbox.lock.lock(); // serialize() locks Mailbox deep inside of it
        try {
            synchronized (this) {
                if (mFolder instanceof ImapFolder) { // if the data's already paged out, we can short-circuit
                    mFolder = new PagedFolderData(serialize(active), (ImapFolder) mFolder);
                } else if (mFolder instanceof PagedFolderData) {
                    PagedFolderData paged = (PagedFolderData) mFolder;
                    if (paged.getCacheKey() == null || !paged.getCacheKey().equals(MANAGER.cacheKey(this, active))) {
                        //currently cached to wrong cache need to move it so it doesn't get expired or LRU'd
                        ZimbraLog.imap.trace("relocating cached item to %s already unloaded but cache key mismatched %s",
                                (active ? "active" : "inactive"), this);
                        ImapFolder folder = null;
                        try {
                            folder = reload();
                            if (folder != null) {
                                mFolder = new PagedFolderData(serialize(active), folder);
                            } else {
                                ZimbraLog.imap.debug(
                                        "folder not found while reloading for relocate. probably already evicted. %s",
                                        this);
                            }
                        } catch (ImapSessionClosedException e) {
                            throw ServiceException.FAILURE("Session closed while relocating paged item", e);
                        }
                    }
                }
            }
        } finally {
            mbox.lock.release();
        }
    }

    @Override
    protected ImapFolder reload() throws ImapSessionClosedException {
        Mailbox mbox = (Mailbox) mailbox;
        if (mbox == null) {
            throw new ImapSessionClosedException();
        }
        // Mailbox.endTransaction() -> ImapSession.notifyPendingChanges() locks in the order of Mailbox -> ImapSession.
        // Need to lock in the same order here, otherwise can result in deadlock.
        mbox.lock.lock(); // PagedFolderData.replay() locks Mailbox deep inside of it.
        try {
            synchronized (this) {
                // if the data's already paged in, we can short-circuit
                if (mFolder instanceof PagedFolderData) {
                    PagedFolderData paged = (PagedFolderData) mFolder;
                    ImapFolder i4folder = MANAGER.deserialize(paged.getCacheKey());
                    if (i4folder == null) { // cache miss
                        if (ImapSessionManager.isActiveKey(paged.getCacheKey())) {
                            ZimbraLog.imap.debug("cache miss in active cache with key %s. %s",paged.getCacheKey(), this);
                        }
                        return null;
                    }
                    try {
                        paged.restore(i4folder);
                    } catch (ServiceException e) {
                        ZimbraLog.imap.warn("Failed to restore folder %s for session %s", paged.getCacheKey(), this, e);
                        return null;
                    }
                    // need to switch target before replay (yes, this is inelegant)
                    mFolder = i4folder;
                    // replay all queued events into the restored folder
                    try {
                        paged.replay(); //catch some error and return null so we drop cache and reload from db
                        if (hasFailedRenumber()) {
                            return handleRenumberError(paged.getCacheKey());
                        }
                    } catch (ImapRenumberException e) {
                        return handleRenumberError(paged.getCacheKey());
                    }
                    // if it's a disconnected session, no need to track expunges
                    if (!isInteractive()) {
                        i4folder.collapseExpunged(false);
                    }
                }
                return (ImapFolder) mFolder;
            }
        } finally {
            mbox.lock.release();
        }
    }

    ImapFolder handleRenumberError(String key) {
        resetRenumber();
        ZimbraLog.imap.warn("could not replay due to too many renumbers  key=%s %s", key, this);
        MANAGER.safeRemoveCache(key);
        return null;
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
    public void updateAccessTime() {
        super.updateAccessTime();
        Mailbox mbox = this.getMailboxOrNull();
        if (mbox == null) {
            return;
        }
        mbox.lock.lock();
        try {
            synchronized (this) {
                PagedFolderData paged = mFolder instanceof PagedFolderData ? (PagedFolderData) mFolder : null;
                if (paged != null) { // if the data's already paged in, we can short-circuit
                    MANAGER.updateAccessTime(paged.getCacheKey());
                }
            }
        } finally {
            mbox.lock.release();
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
}
