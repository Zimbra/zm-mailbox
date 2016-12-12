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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Function;
import com.zimbra.client.ZBaseItem;
import com.zimbra.client.ZTag;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.imap.ImapFolder.DirtyMessage;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;

public abstract class ImapListener extends Session {
    protected static final ImapSessionManager MANAGER = ImapSessionManager.getInstance();

    /** Number of queued notifications beyond which we deserialize the session,
     *  apply the changes, and reserialize the session.  This both constrains
     *  the memory footprint of the paged data and helps keep the persisted
     *  cache up to date in case of restart. */
    static final int RESERIALIZATION_THRESHOLD = DebugConfig.imapSerializedSessionNotificationOverloadThreshold;

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
        void handleTagRename(int changeId, ZTag tag, Change chg);
        void handleItemDelete(int changeId, int itemId, Change chg);
        void handleItemCreate(int changeId, MailItem item, AddedItems added);
        void handleItemCreate(int changeId, ZBaseItem item, AddedItems added);
        void handleFolderRename(int changeId, FolderStore folder, Change chg);
        void handleItemUpdate(int changeId, Change chg, AddedItems added);
        void handleAddedMessages(int changeId, AddedItems added);
        void finishNotification(int changeId) throws IOException;
    }


    protected abstract class PagedFolderData implements ImapFolderData {
        private final String cacheKey;
        private final int originalSize;
        private PagedSessionData pagedSessionData; // guarded by PagedFolderData.this
        @SuppressWarnings("rawtypes")
        protected Map<Integer, PendingModifications> queuedChanges;

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

        synchronized boolean notificationsFull() {
            if (queuedChanges == null || queuedChanges.isEmpty()) {
                return false;
            }

            int count = 0;
            for (@SuppressWarnings("rawtypes") PendingModifications pms : queuedChanges.values()) {
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
        protected abstract void queueCreate(int changeId, MailItem item);

        // NOTE: synchronize implementations
        protected abstract void queueCreate(int changeId, ZBaseItem item);

        // NOTE: synchronize implementations
        protected abstract void queueModify(int changeId, Change chg);

        @SuppressWarnings("rawtypes")
        synchronized void replay() {
            // it's an error if we're replaying changes back into this same queuer...
            assert mFolder != this;

            if (queuedChanges == null) {
                return;
            }

            resetRenumber();

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
        public void handleTagRename(int changeId, ZTag tag, Change chg) {
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
        public void handleItemCreate(int changeId, ZBaseItem item, AddedItems added) {
            queueCreate(changeId, item);
        }

        @Override
        public void handleFolderRename(int changeId, FolderStore folder, Change chg) {
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


    final ImapPath mPath;
    final int      mFolderId;
    final boolean  mIsVirtual;
    ImapFolderData mFolder;
    ImapHandler handler;
    private final ImapMailboxStore imapMboxStore;

    private final Map<Integer, Integer> renumberCount = new ConcurrentHashMap<Integer, Integer>();

    protected ImapListener(ImapMailboxStore store, ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(i4folder.getCredentials().getAccountId(), i4folder.getPath().getOwnerAccountId(), Session.Type.IMAP);
        this.imapMboxStore = store;
        mPath      = i4folder.getPath();
        mFolderId  = i4folder.getId();
        mIsVirtual = i4folder.isVirtual();
        mFolder    = i4folder;
        this.handler = handler;

        i4folder.setSession(this);
    }

    ImapMailboxStore getImapMboxStore() {
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

    ImapHandler getHandler() {
        return handler;
    }

    public ImapPath getPath() {
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

    @Override
    protected long getSessionIdleLifetime() {
        return handler.getConfig().getAuthenticatedMaxIdleTime() * 1000;
    }

    int getRenumberCount(ImapMessage msg) {
        Integer count = renumberCount.get(msg.msgId);
        return (count == null ? 0 : count);
    }

    public void incrementRenumber(ImapMessage msg) {
        Integer count = renumberCount.get(msg.msgId);
        count = (count != null ? count + 1 : 1);
        renumberCount.put(msg.msgId, count);
    }

    void resetRenumber() {
        renumberCount.clear();
    }

    boolean hasFailedRenumber() {
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
        if (mbox != null) { // locking order is always Mailbox then Session
            mbox.lock(true);
        }
        try {
            synchronized (this) {
                MANAGER.uncacheSession(this);
                return isRegistered() ? (ImapSession)super.unregister() : this;
            }
        } finally {
            if (mbox != null) {
                mbox.unlock();
            }
        }
    }

    protected abstract void unload(boolean active) throws ServiceException;
    protected abstract boolean requiresReload();

    protected boolean hasExpunges() {
        return mFolder.hasExpunges();
    }

    protected int getEstimatedSize() {
        return mFolder.getSize();
    }

    protected abstract ImapFolder reload() throws ImapSessionClosedException;

    @Override
    protected void cleanup() {
        ImapHandler i4handler = handler;
        if (i4handler != null) {
            ZimbraLog.imap.debug("dropping connection because Session is closing %s", this);
            i4handler.close();
        }
    }

    protected abstract void inactivate();

    protected boolean isSerialized() {
        return mFolder instanceof PagedFolderData;
    }
}
