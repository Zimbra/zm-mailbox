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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.BaseFolderInfo;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraTag;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapFolder.DirtyMessage;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.session.Session;

public abstract class ImapListener extends Session {
    protected static final ImapSessionManager MANAGER = ImapSessionManager.getInstance();
    protected final ImapPath mPath;
    protected final ItemIdentifier folderId;
    protected final boolean  mIsVirtual;
    protected ImapFolderData mFolder;
    protected ImapHandler handler;
    private final ImapMailboxStore imapMboxStore;
    private final Map<Integer, Integer> renumberCount = new ConcurrentHashMap<Integer, Integer>();

    /** Number of queued notifications beyond which we deserialize the session,
     *  apply the changes, and reserialize the session.  This both constrains
     *  the memory footprint of the paged data and helps keep the persisted
     *  cache up to date in case of restart. */
    private static final int RESERIALIZATION_THRESHOLD = DebugConfig.imapSerializedSessionNotificationOverloadThreshold;

    protected static class AddedItems {
        protected List<ImapMessage> numbered, unnumbered;

        private boolean isEmpty() {
            return numbered == null && unnumbered == null;
        }

        protected void add(BaseItemInfo item) {
            ImapMessage i4item;
            try {
                i4item = new ImapMessage(item);
            } catch (ServiceException e) {
                ZimbraLog.imap.warn("unable to instantiate ImapMessage", e);
                return;
            }
            if (item.getImapUid() > 0) {
                (numbered == null ? numbered = new ArrayList<ImapMessage>() : numbered).add(i4item);
            } else {
                (unnumbered == null ? unnumbered = new ArrayList<ImapMessage>() : unnumbered).add(i4item);
            }
        }

        protected void sort() {
            if (numbered != null) {
                Collections.sort(numbered);
            }
            if (unnumbered != null) {
                Collections.sort(unnumbered);
            }
        }
    }

    interface ImapFolderData {
        int getId();
        int getSize();
        boolean isWritable();
        boolean hasExpunges();
        boolean hasNotifications();
        void doEncodeState(Element imap);
        void endSelect();

        void handleTagDelete(int changeId, int tagId, Change chg);
        void handleTagRename(int changeId, ZimbraTag tag, Change chg);
        void handleItemDelete(int changeId, int itemId, Change chg);
        void handleItemCreate(int changeId, BaseItemInfo item, AddedItems added);
        void handleFolderRename(int changeId, BaseFolderInfo folder, Change chg);
        void handleItemUpdate(int changeId, Change chg, AddedItems added);
        void handleAddedMessages(int changeId, AddedItems added);
        void finishNotification(int changeId) throws IOException;
    }


    protected abstract class PagedFolderData implements ImapFolderData {
        private final String cacheKey;
        private final int originalSize;
        private PagedSessionData pagedSessionData; // guarded by PagedFolderData.this
        @SuppressWarnings("rawtypes")
        protected TreeMap<Integer, PendingModifications> queuedChanges;

        PagedFolderData(String cachekey, ImapFolder i4folder) {
            cacheKey = cachekey;
            originalSize = i4folder.getSize();
            pagedSessionData = i4folder.getSessionData() == null ? null : new PagedSessionData(i4folder);
        }

        @Override
        public int getId() {
            return ImapListener.this.getFolderId();
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
            for (@SuppressWarnings("rawtypes") PendingModifications pms : queuedChanges.values()) {
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

        private synchronized boolean notificationsFull() {
            if (queuedChanges == null || queuedChanges.isEmpty()) {
                return false;
            }

            int count = 0;
            for (@SuppressWarnings("rawtypes") PendingModifications pms : queuedChanges.values()) {
                count += pms.getScaledNotificationCount();
            }
            return count > RESERIALIZATION_THRESHOLD;
        }

        private String getCacheKey() {
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

        private synchronized void restore(ImapFolder i4folder) throws ImapSessionClosedException, ServiceException {
            ImapFolder.SessionData sdata = pagedSessionData == null ? null : pagedSessionData.asFolderData(i4folder);
            i4folder.restore(ImapListener.this, sdata);
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

        @SuppressWarnings("rawtypes")
        protected abstract PendingModifications getQueuedNotifications(int changeId);

        private synchronized void queueDelete(int changeId, int itemId, Change chg) {
            getQueuedNotifications(changeId).recordDeleted(
                    getTargetAccountId(), itemId, chg.getFolderId(), (MailItem.Type) chg.what);
        }

        // NOTE: synchronize implementations
        protected abstract void queueCreate(int changeId, BaseItemInfo item);

        // NOTE: synchronize implementations
        protected abstract void queueModify(int changeId, Change chg);

        @SuppressWarnings("rawtypes")
        private synchronized void replay() {
            // it's an error if we're replaying changes back into this same queuer...
            assert mFolder != this;

            if (queuedChanges == null) {
                return;
            }

            resetRenumber();
            lastChangeId = 0;
            SortedSet<Integer> changeIds = new TreeSet<Integer>(queuedChanges.keySet());
            for(Integer changeId : changeIds) {
                PendingModifications mods = queuedChanges.get(changeId);
                notifyPendingChanges(mods, changeId, null);
            }
        }

        @Override
        public void handleTagDelete(int changeId, int tagId, Change chg) {
            queueDelete(changeId, tagId, chg);
        }

        @Override
        public void handleTagRename(int changeId, ZimbraTag tag, Change chg) {
            queueModify(changeId, chg);
        }

        @Override
        public void handleItemDelete(int changeId, int itemId, Change chg) {
            queueDelete(changeId, itemId, chg);
        }

        @Override
        public void handleItemCreate(int changeId, BaseItemInfo item, AddedItems added) {
            queueCreate(changeId, item);
        }

        @Override
        public void handleFolderRename(int changeId, BaseFolderInfo folder, Change chg) {
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
            ImapHandler myHandler = getHandler();
            if (myHandler != null && myHandler.isIdle()) {
                try {
                    reload();
                } catch (ImapSessionClosedException ignore) {
                }
            }
        }

        private class PagedSessionData {
            private ImapFolder.SessionData mOriginalSessionData;
            private int[] mSavedSearchIds;
            private int[] mDirtyChanges;
            private int[] mSessionFlags;

            private PagedSessionData(ImapFolder i4folder) {
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

            private ImapFolder.SessionData asFolderData(ImapFolder i4folder) {
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

            private boolean hasNotifications() {
                if (mOriginalSessionData == null) {
                    return false;
                }
                return mOriginalSessionData.tagsAreDirty || mDirtyChanges != null || mOriginalSessionData.expungedCount > 0;
            }
        }
    }

    protected ImapListener(ImapMailboxStore store, ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(i4folder.getCredentials().getAccountId(), i4folder.getPath().getOwnerAccountId(), Session.Type.IMAP);
        this.imapMboxStore = store;
        mPath      = i4folder.getPath();
        folderId  = i4folder.getItemIdentifier();
        mIsVirtual = i4folder.isVirtual();
        mFolder    = i4folder;
        this.handler = handler;
        final OperationContext octxt = i4folder.getCredentials().getContext();
        if (octxt != null) {
            userAgent = octxt.getUserAgent();
            requestIPAddress = octxt.getRequestIP();
        }

        i4folder.setSession(this);
    }

    protected ImapMailboxStore getImapMboxStore() {
        return imapMboxStore;
    }

    public ImapFolder getImapFolder() throws ImapSessionClosedException {
        MANAGER.recordAccess(this);
        return reload();
    }

    public void closeFolder(boolean isUnregistering) {
        MANAGER.closeFolder(this, false);
    }

    public boolean hasNotifications() {
        return mFolder.hasNotifications();
    }

    protected ImapHandler getHandler() {
        return handler;
    }

    public ImapPath getPath() {
        return mPath;
    }

    protected boolean isInteractive() {
        return handler != null;
    }

    public boolean isWritable() {
        return isInteractive() && mFolder.isWritable();
    }

    protected boolean isVirtual() {
        return mIsVirtual;
    }

    public int getFolderId() {
        return folderId.id;
    }

    public ItemIdentifier getFolderItemIdentifier() {
        return folderId;
    }

    @Override
    protected long getSessionIdleLifetime() {
        return handler.getConfig().getAuthenticatedMaxIdleTime() * 1000;
    }

    public void incrementRenumber(ImapMessage msg) {
        Integer count = renumberCount.get(msg.msgId);
        count = (count != null ? count + 1 : 1);
        renumberCount.put(msg.msgId, count);
    }

    private void resetRenumber() {
        renumberCount.clear();
    }

    private void handleDelete(int changeId, int id, Change chg) {
        ZimbraLog.imap.debug("Handling a delete notification. Change id %d, item id %d", changeId, id);
        MailItem.Type type = (MailItem.Type) chg.what;
        if (id <= 0) {
            return;
        } else if (type == MailItem.Type.TAG) {
            mFolder.handleTagDelete(changeId, id, chg);
        } else if (id == folderId.id && mFolder instanceof ImapFolder) {
            // Once the folder's gone, there's no point in keeping an IMAP Session listening on it around.
            detach();
            //set MailStore to NULL before closing connection to avoid serializing this session
            mailbox = null;

            // notify client that mailbox is deselected due to delete?
            // RFC 2180 3.3: "The server MAY allow the DELETE/RENAME of a multi-accessed
            //                mailbox, but disconnect all other clients who have the
            //                mailbox accessed by sending a untagged BYE response."
            handler.close();
            handler = null;
        } else if (ImapMessage.SUPPORTED_TYPES.contains(type)) {
            mFolder.handleItemDelete(changeId, id, chg);
        }
    }

    protected void handleModify(int changeId, Change chg, AddedItems added) {
        if (chg.what instanceof ZimbraTag && (chg.why & Change.NAME) != 0) {
            mFolder.handleTagRename(changeId, (ZimbraTag) chg.what, chg);
        } else {
            boolean isFolder = (chg.what instanceof BaseItemInfo && ((BaseItemInfo) chg.what).getMailItemType() == MailItemType.FOLDER);
            boolean isMsgOrContact = false;
            BaseItemInfo item = null;
            if (chg.what instanceof BaseItemInfo) {
                item = (BaseItemInfo) chg.what;
                isMsgOrContact = (item.getMailItemType() == MailItemType.MESSAGE || item.getMailItemType() == MailItemType.CONTACT);
            }
            try {
                if (isFolder && ((BaseFolderInfo) chg.what).getFolderIdInOwnerMailbox() == folderId.id) {
                    FolderStore folder = (FolderStore) chg.what;
                    //here we assume that the FolderStore object also implements BaseItemInfo
                    if ((chg.why & Change.FLAGS) != 0 && (((BaseItemInfo) folder).getFlagBitmask() & Flag.BITMASK_DELETED) != 0) {
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
                } else if (isMsgOrContact) {
                    boolean inFolder = mIsVirtual || item.getFolderIdInMailbox() == folderId.id;
                    if (!inFolder && (chg.why & Change.FOLDER) == 0) {
                        return;
                    }
                    mFolder.handleItemUpdate(changeId, chg, added);
                }
            } catch (ServiceException e) {
                ZimbraLog.imap.warn("error handling modified items for changeId %s", changeId, e);
                return;
            }
        }
    }

    private ImapFolder handleRenumberError(String key) {
        resetRenumber();
        ZimbraLog.imap.warn("could not replay due to too many renumbers  key=%s %s", key, this);
        MANAGER.safeRemoveCache(key);
        return null;
    }

    private boolean hasFailedRenumber() {
        //check if any id has been repeatedly renumbered
        for (Integer count : renumberCount.values()) {
            if (count > 5) {
                return true;
            }
        }
        return false;
    }

    public boolean isFailedRenumber(ImapMessage msg) {
        Integer count = renumberCount.get(msg.msgId);
        return (count == null ? false : isFailed(count));
    }

    private boolean isFailed(Integer count) {
        return count > 5;
    }

    public ImapListener detach() {
        MailboxStore mbox = this.getMailbox();
        // locking order is always Mailbox then Session
        try (final MailboxLock l = (mbox != null) ? mbox.getWriteLockAndLockIt() : null) {
            synchronized (this) {
                MANAGER.uncacheSession(this);
                return isRegistered() ? (ImapListener)super.unregister() : this;
            }
        }
    }

    @Override
    public void doEncodeState(Element parent) {
        mFolder.doEncodeState(parent.addNonUniqueElement("imap"));
    }

    protected abstract PagedFolderData createPagedFolderData(boolean active, ImapFolder folder) throws ServiceException;

    /**
     * Unload this session data into cache.
     *
     * @param active true to use active session cache, otherwise use inactive session cache
     */
    protected void unload(boolean active) throws ServiceException {
        //mailbox reference is volatile, so we need to acquire a local reference to avoid an NPE
        MailboxStore mbox = getMailbox();
        if (mbox == null) {
            return;
        }
        // Mailbox.endTransaction() -> ImapSession.notifyPendingChanges() locks in the order of Mailbox -> ImapSession.
        // Need to lock in the same order here, otherwise can result in deadlock.
        try (final MailboxLock l =
                mbox.getWriteLockAndLockIt() /* serialize() locks Mailbox deep inside of it */) {
            synchronized (this) {
                if (mFolder instanceof ImapFolder) { // if the data's already paged out, we can short-circuit
                    mFolder = createPagedFolderData(active, (ImapFolder) mFolder);
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
                                mFolder = createPagedFolderData(active, folder);
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
        }
    }

    // XXX: need to handle the abrupt disconnect case, the LOGOUT case, the timeout case, and the too-many-sessions disconnect case
    @Override
    public Session unregister() {
        MANAGER.closeFolder(this, true);
        return detach();
    }

    protected boolean requiresReload() {
        ImapFolderData fdata = mFolder;
        return fdata instanceof ImapFolder ? false : ((PagedFolderData) fdata).notificationsFull();
    }

    protected boolean hasExpunges() {
        return mFolder.hasExpunges();
    }

    protected int getEstimatedSize() {
        return mFolder.getSize();
    }

    protected void updateLastChangeId(int changeId) {
        ZimbraLog.imap.debug("ImapListener.updateLastChangeId %d->%d %s %s", lastChangeId, changeId, this, hashCode());
        if (changeId > lastChangeId) {
            lastChangeId = changeId;
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void notifyPendingChanges(PendingModifications pnsIn, int changeId, SourceSessionInfo source) {
        if (!pnsIn.hasNotifications()) {
            return;
        }
        if (changeId < lastChangeId) {
            ZimbraLog.imap.debug("ImapListener :: change %d is not higher than last change %d. Ignoring", changeId, lastChangeId);
            return;
        }
        ImapHandler i4handler = handler;
        try {
            synchronized (this) {
                AddedItems added = new AddedItems();
                if (pnsIn.deleted != null) {
                    @SuppressWarnings("unchecked")
                    Map<ModificationKey, Change> deleted = pnsIn.deleted;
                    for (Map.Entry<ModificationKey, Change> entry : deleted.entrySet()) {
                        handleDelete(changeId, entry.getKey().getItemId(), entry.getValue());
                    }
                }
                notifyPendingCreates(pnsIn, changeId, added);
                if (pnsIn.modified != null) {
                    @SuppressWarnings("unchecked")
                    Map<ModificationKey, Change> modified = pnsIn.modified;
                    for (Change chg : modified.values()) {
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
        } finally {
            lastChangeId = changeId;
        }
    }

    protected abstract void notifyPendingCreates(@SuppressWarnings("rawtypes") PendingModifications pns,
            int changeId, AddedItems added);

    protected ImapFolder reload() throws ImapSessionClosedException {
        // ZESC-460, ZCS-4004: Ensure mailbox was not modified by another thread
        MailboxStore mbox = mailbox;
        if (mbox == null) {
            throw new ImapSessionClosedException();
        }
        // Mailbox.endTransaction() -> ImapSession.notifyPendingChanges() locks in the order of Mailbox -> ImapSession.
        // Need to lock in the same order here, otherwise can result in deadlock.
        try (final MailboxLock l = mbox.getWriteLockAndLockIt()
                /* PagedFolderData.replay() locks Mailbox deep inside of it. */) {
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
        }
    }

    @Override
    protected void cleanup() {
        ImapHandler i4handler = handler;
        if (i4handler != null) {
            ZimbraLog.imap.debug("dropping connection because Session is closing %s", this);
            i4handler.close();
        }
    }

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

    protected abstract void snapshotRECENT();

    protected boolean isSerialized() {
        return mFolder instanceof PagedFolderData;
    }

    /**
     * Serializes this {@link ImapSession} to the session manager's current {@link ImapSessionManager.FolderSerializer}
     * if it's not already serialized there.
     *
     * @param active selects active or inactive cache
     * @return the cachekey under which we serialized the folder, or {@code null} if the folder was already serialized
     */
    protected String serialize(boolean active) throws ServiceException {
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

    @Override
    public void updateAccessTime() {
        super.updateAccessTime();
        // ZESC-460, ZCS-4004: Ensure mailbox was not modified by another thread
        MailboxStore mbox = mailbox;
        if (mbox == null) {
            return;
        }
        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
            synchronized (this) {
                PagedFolderData paged = mFolder instanceof PagedFolderData ? (PagedFolderData) mFolder : null;
                if (paged != null) { // if the data's already paged in, we can short-circuit
                    MANAGER.updateAccessTime(paged.getCacheKey());
                }
            }
        }
    }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        helper.add("path", mPath).add("folderId", folderId);
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
