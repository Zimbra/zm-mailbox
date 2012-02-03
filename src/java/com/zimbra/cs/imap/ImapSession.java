/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Function;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapFolder.DirtyMessage;
import com.zimbra.cs.imap.ImapHandler.ImapExtension;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.session.Session;

public class ImapSession extends Session {
    private static final ImapSessionManager MANAGER = ImapSessionManager.getInstance();

    interface ImapFolderData {
        int getId();
        int getSize();
        boolean isWritable();
        boolean hasExpunges();
        boolean hasNotifications();
        void doEncodeState(Element imap);
        void endSelect();

        void handleTagDelete(int changeId, int tagId, Change chg);
        void handleTagRename(int changeId, Tag tag, Change chg);
        void handleItemDelete(int changeId, int itemId, Change chg);
        void handleItemCreate(int changeId, MailItem item, AddedItems added);
        void handleFolderRename(int changeId, Folder folder, Change chg);
        void handleItemUpdate(int changeId, Change chg, AddedItems added);
        void handleAddedMessages(int changeId, AddedItems added);
        void finishNotification(int changeId) throws IOException;
    }

    private final ImapPath mPath;
    private final int      mFolderId;
    private final boolean  mIsVirtual;
    private ImapFolderData mFolder;
    private ImapHandler handler;

    ImapSession(ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(i4folder.getCredentials().getAccountId(), i4folder.getPath().getOwnerAccountId(), Session.Type.IMAP);
        mPath      = i4folder.getPath();
        mFolderId  = i4folder.getId();
        mIsVirtual = i4folder.isVirtual();
        mFolder    = i4folder;
        this.handler = handler;

        i4folder.setSession(this);
    }

    ImapHandler getHandler() {
        return handler;
    }

    ImapFolder getImapFolder() throws ImapSessionClosedException {
        MANAGER.recordAccess(this);
        return reload();
    }

    ImapPath getPath() {
        return mPath;
    }

    boolean isInteractive() {
        return handler != null;
    }

    public boolean isWritable() {
        return isInteractive() && mFolder.isWritable();
    }

    boolean isVirtual() {
        return mIsVirtual;
    }

    public int getFolderId() {
        return mFolderId;
    }

    boolean hasNotifications() {
        return mFolder.hasNotifications();
    }

    boolean hasExpunges() {
        return mFolder.hasExpunges();
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

    boolean isSerialized() {
        return mFolder instanceof PagedFolderData;
    }

    synchronized int getFootprint() {
        // FIXME: consider saved search results, in-memory data for paged sessions
        return mFolder instanceof ImapFolder ? ((ImapFolder) mFolder).getSize() : 0;
    }

    int getEstimatedSize() {
        return mFolder.getSize();
    }

    boolean requiresReload() {
        ImapFolderData fdata = mFolder;
        return fdata instanceof ImapFolder ? false : ((PagedFolderData) fdata).notificationsFull();
    }

    void inactivate() {
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
            Mailbox mbox = mailbox;
            if (mbox != null && isWritable()) {
                mbox.recordImapSession(mFolderId);
            }
        } catch (MailServiceException.NoSuchItemException nsie) {
            // don't log if the session expires because the folder was deleted out from under it
        } catch (Exception e) {
            ZimbraLog.session.warn("exception recording unloaded session's RECENT limit", e);
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
        mFolder.doEncodeState(parent.addElement("imap"));
    }

    @Override
    protected long getSessionIdleLifetime() {
        return handler.getConfig().getAuthenticatedMaxIdleTime() * 1000;
    }

    // XXX: need to handle the abrupt disconnect case, the LOGOUT case, the timeout case, and the too-many-sessions disconnect case
    @Override
    public Session unregister() {
        MANAGER.closeFolder(this, true);
        return detach();
    }

    Session detach() {
        Mailbox mbox = mailbox;
        if (mbox != null) { // locking order is always Mailbox then Session
            mbox.lock.lock();
        }
        try {
            synchronized (this) {
                MANAGER.uncacheSession(this);
                return isRegistered() ? super.unregister() : this;
            }
        } finally {
            if (mbox != null) {
                mbox.lock.release();
            }
        }
    }

    @Override
    protected void cleanup() {
        ImapHandler i4handler = handler;
        if (i4handler != null) {
            ZimbraLog.imap.debug("dropping connection because Session is closing");
            i4handler.close();
        }
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
    void unload(boolean active) throws ServiceException {
        Mailbox mbox = mailbox;
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
                        ZimbraLog.imap.trace("relocating cached item to %s already unloaded but cache key mismatched", (active ? "active" : "inactive"));
                        ImapFolder folder = null;
                        try {
                            folder = reload();
                            if (folder != null) {
                                mFolder = new PagedFolderData(serialize(active), folder);
                            } else {
                                ZimbraLog.imap.warn("folder not found while reloading for relocate. probably already evicted");
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

    ImapFolder reload() throws ImapSessionClosedException {
        Mailbox mbox = mailbox;
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
                            ZimbraLog.imap.debug("cache miss in active cache with key %s",paged.getCacheKey());
                        }
                        return null;
                    }
                    try {
                        paged.restore(i4folder);
                    } catch (ServiceException e) {
                        ZimbraLog.imap.warn("Failed to restore folder %s", paged.getCacheKey(), e);
                        return null;
                    }
                    // need to switch target before replay (yes, this is inelegant)
                    mFolder = i4folder;
                    // replay all queued events into the restored folder
                    paged.replay();
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

    static class AddedItems {
        List<ImapMessage> numbered, unnumbered;

        boolean isEmpty() {
            return numbered == null && unnumbered == null;
        }

        void add(MailItem item) {
            if (item.getImapUid() > 0) {
                (numbered == null ? numbered = new ArrayList<ImapMessage>() : numbered).add(new ImapMessage(item));
            } else {
                (unnumbered == null ? unnumbered = new ArrayList<ImapMessage>() : unnumbered).add(new ImapMessage(item));
            }
        }

        void sort() {
            if (numbered != null) {
                Collections.sort(numbered);
            }
            if (unnumbered != null) {
                Collections.sort(unnumbered);
            }
        }
    }

    @Override
    public void notifyPendingChanges(PendingModifications pns, int changeId, Session source) {
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

    /** Number of queued notifications beyond which we deserialize the session,
     *  apply the changes, and reserialize the session.  This both constrains
     *  the memory footprint of the paged data and helps keep the persisted
     *  cache up to date in case of restart. */
    static final int RESERIALIZATION_THRESHOLD = DebugConfig.imapSerializedSessionNotificationOverloadThreshold;

    private final class PagedFolderData implements ImapFolderData {
        private final String cacheKey;
        private final int originalSize;
        private PagedSessionData pagedSessionData; // guarded by PagedFolderData.this
        private Map<Integer, PendingModifications> queuedChanges;

        PagedFolderData(String cachekey, ImapFolder i4folder) {
            cacheKey = cachekey;
            originalSize = i4folder.getSize();
            pagedSessionData = i4folder.getSessionData() == null ? null : new PagedSessionData(i4folder);
        }

        @Override
        public int getId() {
            return ImapSession.this.getFolderId();
        }

        @Override
        public int getSize() {
            return originalSize;
        }

        @Override
        public synchronized boolean isWritable() {
            return pagedSessionData == null ? false : pagedSessionData.mOriginalSessionData.writable;
        }

        @Override
        public synchronized boolean hasExpunges() {
            // hugely overbroad, but this should never be called in the first place...
            if (pagedSessionData != null && pagedSessionData.mOriginalSessionData.expungedCount > 0) {
                return true;
            }
            if (queuedChanges == null || queuedChanges.isEmpty()) {
                return false;
            }
            for (PendingModifications pms : queuedChanges.values()) {
                if (pms.deleted != null && !pms.deleted.isEmpty()) {
                    return true;
                }
                if (pms.modified != null && !pms.modified.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized boolean hasNotifications() {
            if (pagedSessionData != null && pagedSessionData.hasNotifications()) {
                return true;
            }
            return queuedChanges != null && !queuedChanges.isEmpty();
        }

        synchronized boolean notificationsFull() {
            if (queuedChanges == null || queuedChanges.isEmpty()) {
                return false;
            }

            int count = 0;
            for (PendingModifications pms : queuedChanges.values()) {
                count += pms.getScaledNotificationCount();
            }
            return count > RESERIALIZATION_THRESHOLD;
        }

        String getCacheKey() {
            return cacheKey;
        }

        @Override
        public void doEncodeState(Element imap) {
            imap.addAttribute("paged", true);
        }

        @Override
        public synchronized void endSelect() {
            pagedSessionData = null;
        }

        synchronized void restore(ImapFolder i4folder) throws ImapSessionClosedException, ServiceException {
            ImapFolder.SessionData sdata = pagedSessionData == null ? null : pagedSessionData.asFolderData(i4folder);
            i4folder.restore(ImapSession.this, sdata);
            if (pagedSessionData != null && pagedSessionData.mSessionFlags != null) {
                int[] sflags = pagedSessionData.mSessionFlags;
                for (int i = 0; i < sflags.length; i += 2) {
                    ImapMessage i4msg = i4folder.getByImapId(sflags[i]);
                    if (i4msg != null) {
                        i4msg.sflags = (short) sflags[i + 1];
                    }
                }
            }
        }

        private PendingModifications getQueuedNotifications(int changeId) {
            if (queuedChanges == null) {
                queuedChanges = new TreeMap<Integer, PendingModifications>();
            }
            PendingModifications pns = queuedChanges.get(changeId);
            if (pns == null) {
                queuedChanges.put(changeId, pns = new PendingModifications());
            }
            return pns;
        }

        private synchronized void queueDelete(int changeId, int itemId, Change chg) {
            getQueuedNotifications(changeId).recordDeleted(
                    getTargetAccountId(), itemId, (MailItem.Type) chg.what, chg.op, chg.when);
        }

        private synchronized void queueCreate(int changeId, MailItem item) {
            getQueuedNotifications(changeId).recordCreated(item);
        }

        private synchronized void queueModify(int changeId, Change chg) {
            getQueuedNotifications(changeId).recordModified(
                    chg.op, (MailItem) chg.what, chg.why, chg.when, (MailItem) chg.preModifyObj);
        }

        synchronized void replay() {
            // it's an error if we're replaying changes back into this same queuer...
            assert mFolder != this;

            if (queuedChanges == null) {
                return;
            }

            for (Iterator<Map.Entry<Integer, PendingModifications>> it = queuedChanges.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, PendingModifications> entry = it.next();
                notifyPendingChanges(entry.getValue(), entry.getKey(), null);
                it.remove();
            }
        }

        @Override
        public void handleTagDelete(int changeId, int tagId, Change chg) {
            queueDelete(changeId, tagId, chg);
        }

        @Override
        public void handleTagRename(int changeId, Tag tag, Change chg) {
            queueModify(changeId, chg);
        }

        @Override
        public void handleItemDelete(int changeId, int itemId, Change chg) {
            queueDelete(changeId, itemId, chg);
        }

        @Override
        public void handleItemCreate(int changeId, MailItem item, AddedItems added) {
            queueCreate(changeId, item);
        }

        @Override
        public void handleFolderRename(int changeId, Folder folder, Change chg) {
            queueModify(changeId, chg);
        }

        @Override
        public void handleItemUpdate(int changeId, Change chg, AddedItems added) {
            queueModify(changeId, chg);
        }

        @Override
        public void handleAddedMessages(int changeId, AddedItems added) {}

        @Override
        public void finishNotification(int changeId) throws IOException {
            // idle sessions need to be notified immediately
            ImapHandler handler = getHandler();
            if (handler != null && handler.isIdle()) {
                try {
                    reload();
                } catch (ImapSessionClosedException ignore) {
                }
            }
        }

        class PagedSessionData {
            ImapFolder.SessionData mOriginalSessionData;
            int[] mSavedSearchIds;
            int[] mDirtyChanges;
            int[] mSessionFlags;

            PagedSessionData(ImapFolder i4folder) {
                mOriginalSessionData = i4folder.getSessionData();
                if (mOriginalSessionData == null) {
                    return;
                }

                // save the session data in a simple form
                if (mOriginalSessionData.savedSearchResults != null) {
                    mSavedSearchIds = new int[mOriginalSessionData.savedSearchResults.size()];
                    int pos = 0;
                    for (ImapMessage i4msg : mOriginalSessionData.savedSearchResults) {
                        mSavedSearchIds[pos++] = i4msg.imapUid;
                    }
                }

                if (!mOriginalSessionData.dirtyMessages.isEmpty()) {
                    mDirtyChanges = new int[mOriginalSessionData.dirtyMessages.size() * 2];
                    int pos = 0;
                    for (Map.Entry<Integer, DirtyMessage> dentry : mOriginalSessionData.dirtyMessages.entrySet()) {
                        mDirtyChanges[pos++] = dentry.getKey();
                        mDirtyChanges[pos++] = dentry.getValue().modseq;
                    }
                }

                final short defaultFlags = (short) (getFolderId() != Mailbox.ID_FOLDER_SPAM ? 0 :
                    ImapMessage.FLAG_SPAM | ImapMessage.FLAG_JUNKRECORDED);
                final List<Integer> sflags = new ArrayList<Integer>();
                i4folder.traverse(new Function<ImapMessage, Void>() {
                    @Override
                    public Void apply(ImapMessage i4msg) {
                        if ((i4msg.sflags & ~ImapMessage.FLAG_IS_CONTACT) != defaultFlags) {
                            sflags.add(i4msg.imapUid);
                            sflags.add((int) i4msg.sflags);
                        }
                        return null;
                    }
                });

                if (!sflags.isEmpty()) {
                    mSessionFlags = ArrayUtil.toIntArray(sflags);
                }

                // kill references to ImapMessage objects, since they'll change after the restore
                mOriginalSessionData.savedSearchResults = null;
                mOriginalSessionData.dirtyMessages.clear();
            }

            ImapFolder.SessionData asFolderData(ImapFolder i4folder) {
                if (mOriginalSessionData != null) {
                    if (mSavedSearchIds != null) {
                        mOriginalSessionData.savedSearchResults = new ImapMessageSet();
                        for (int uid : mSavedSearchIds) {
                            mOriginalSessionData.savedSearchResults.add(i4folder.getByImapId(uid));
                        }
                        mOriginalSessionData.savedSearchResults.remove(null);
                    }

                    mOriginalSessionData.dirtyMessages.clear();
                    if (mDirtyChanges != null) {
                        for (int i = 0; i < mDirtyChanges.length; i += 2) {
                            ImapMessage i4msg = i4folder.getByImapId(mDirtyChanges[i]);
                            if (i4msg != null) {
                                mOriginalSessionData.dirtyMessages.put(mDirtyChanges[i],
                                        new DirtyMessage(i4msg, mDirtyChanges[i + 1]));
                            }
                        }
                    }
                }

                return mOriginalSessionData;
            }

            boolean hasNotifications() {
                if (mOriginalSessionData == null) {
                    return false;
                }
                return mOriginalSessionData.tagsAreDirty || mDirtyChanges != null || mOriginalSessionData.expungedCount > 0;
            }
        }
    }

}
