/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.datasource.imap;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.RemoteServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.datasource.MessageContent;
import com.zimbra.cs.datasource.SyncErrorManager;
import com.zimbra.cs.datasource.SyncState;
import com.zimbra.cs.datasource.SyncUtil;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.CopyResult;
import com.zimbra.cs.mailclient.imap.FetchResponseHandler;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.Zimbra;

class ImapFolderSync {
    private final ImapSync imapSync;
    private final ImapConnection connection;
    private final DataSource ds;
    private final Mailbox mailbox;
    private final Statistics stats = new Statistics();
    private ImapFolder tracker;
    private LocalFolder localFolder;
    private RemoteFolder remoteFolder;
    private SyncState syncState;
    private ImapMessageCollection trackedMsgs;
    private Set<Integer> localMsgIds;
    private List<Integer> newMsgIds;                                  
    private List<Long> addedUids;
    private long maxUid;
    private boolean completed;
    private int totalErrors;

    private static final Log LOG = ZimbraLog.datasource;

    private static final int FETCH_SIZE = LC.data_source_fetch_size.intValue();

    // Max number of errors before we generate report and skip item
    private static final int MAX_ITEM_ERRORS = 3;

    // Max number of total per-item failures before we abort sync
    private static final int MAX_TOTAL_ERRORS = 10;

    private static class Statistics {
        int flagsUpdatedLocally;
        int flagsUpdatedRemotely;
        int msgsAddedLocally;
        int msgsAddedRemotely;
        int msgsDeletedLocally;
        int msgsDeletedRemotely;
        int msgsCopiedRemotely;
    }

    public ImapFolderSync(ImapSync imapSync) throws ServiceException {
        this.imapSync = imapSync;
        connection = imapSync.getConnection();
        ds = imapSync.getDataSource();
        mailbox = imapSync.getMailbox();
    }

    /*
     * Synchronizes existing remote IMAP folder. Returns tracker if successful
     * otherwise returns null if local folder deleted or is not eligible for
     * synchronization.
     */
    public ImapFolder syncFolder(ListData ld)
        throws ServiceException, IOException {
        String path = ld.getMailbox();
    	if (ds.isSyncInboxOnly() && !path.equalsIgnoreCase("Inbox"))
    		return null;
        remoteFolder = new RemoteFolder(connection, path);
        tracker = imapSync.getTrackedFolders().getByRemotePath(path);
        if (tracker != null) {
            checkTrackedFolder(ld);
        } else {
            createLocalFolder(ld);
        }
        if (tracker != null) {
            // Check local folder flags for consistency with remote folder
            localFolder.checkFlags(ld);
        }
        return tracker;
    }

    /*
     * Synchronizes existing local folder. Returns tracker if successful
     * otherwise returns null if remote folder deleted, is not eligible
     * for synchronization, or synchronization has been disabled.
     */
    public ImapFolder syncFolder(Folder folder)
        throws ServiceException, IOException {
        DataSourceManager dsm = DataSourceManager.getInstance();
        if (!dsm.isSyncEnabled(ds, folder)) {
            return null;
        }
        localFolder = new LocalFolder(mailbox, folder);
        tracker = imapSync.getTrackedFolders().getByItemId(folder.getId());
        if (tracker != null) { //was in sync
            remoteFolder = new RemoteFolder(connection, tracker.getRemoteId());
            if (!remoteFolder.exists()) {
                remoteFolder.info("folder was deleted");
                if (dsm.isSyncEnabled(ds, folder)) //only delete local if sync enabled
                    localFolder.delete();
                imapSync.deleteFolderTracker(tracker);
                tracker = null;
            } else if (!dsm.isSyncCapable(ds, folder) && !localFolder.getPath().equals(tracker.getLocalPath())) {
            	//we moved local into archive, so delete remote
                if (deleteRemoteFolder(remoteFolder, tracker.getItemId())) {
                    imapSync.deleteFolderTracker(tracker);
                }
            }
        } else if (dsm.isSyncEnabled(ds, folder)) {
            remoteFolder = createRemoteFolder(folder);
            if (remoteFolder == null) return null;
            try {
                remoteFolder.select();
            } catch (CommandFailedException e) {
                syncFolderFailed(folder.getId(), remoteFolder.getPath(),
                                 "Unable to select remote folder", e);
                return null;
            }
            tracker = imapSync.createFolderTracker(
                folder.getId(), folder.getPath(), remoteFolder.getPath(), getUidValidity());
        }
        return tracker;
    }


    private RemoteFolder createRemoteFolder(Folder folder)
        throws ServiceException, IOException {
        String remotePath = imapSync.getRemotePath(folder);
        if (remotePath == null) {
            return null; // not eligible for synchronization
        }
        RemoteFolder newFolder = new RemoteFolder(connection, remotePath);
        try {
            newFolder.create();
        } catch (CommandFailedException e) {
            syncFolderFailed(folder.getId(), remotePath,
                             "Unable to create remote folder", e);
            return null;
        }
        return newFolder;
    }

    /*
     * Synchronizes messages between local and remote folder.
     */
    public void syncMessages(boolean fullSync) throws ServiceException, IOException {
        if (tracker == null) return;
        DataSourceManager dsm = DataSourceManager.getInstance();
        if (tracker.getUidValidity() == 0 || !dsm.isSyncEnabled(ds, localFolder.getFolder())) {
            localFolder.debug("Synchronization disabled for this folder");
            tracker = null;
            return;
        }
        MailboxInfo mb = checkUidValidity();
        syncState = getSyncState(fullSync);
        localFolder.debug("SyncState = " + syncState);
        long uidNext = mb.getUidNext();
        if (uidNext > 0 && uidNext <= syncState.getLastUid()) {
            String msg = String.format(
                "Inconsistent UIDNEXT value from server (got %d but last known uid %d)",
                uidNext, syncState.getLastUid());
            if (isYahoo()) {
                // Bug 31443: Invalid UIDNEXT from Yahoo! IMAP server indicates
                // that the user's mailbox is corrupt. Abort sync and display
                // error message to user indicating that they should try again
                // later (mailbox corruption will be repaired within 24 hours).
                throw RemoteServiceException.YMAIL_INCONSISTENT_STATE();
            }
            ServiceException e = ServiceException.FAILURE(msg, null);
            syncFolderFailed(tracker.getItemId(), tracker.getLocalPath(), msg, e);
            throw e;
        }
        newMsgIds = new ArrayList<Integer>();
        addedUids = new ArrayList<Long>();
        long lastUid = syncState.getLastUid();
        int lastModSeq = syncState.getLastModSeq();
        if (fullSync) {
            // If UIDPLUS supported, use COPY rather than APPEND to remotely
            // move messages that have moved locally between folders.
            if (hasCopyUid()) {
                moveMessages();
            }
            syncFlags(lastUid);
        } else if (lastModSeq > 0) {
            // Push only changes for partial sync
            syncState.setLastModSeq(pushChanges(lastModSeq));
        }
        // Fetch new messages
        List<Long> uidsToDelete = new ArrayList<Long>();
        maxUid = uidNext > 0 ? uidNext - 1 : 0;
        if (maxUid <= 0 || lastUid < maxUid) {
            fetchMessages(lastUid + 1, uidsToDelete);
        }
        if (!addedUids.isEmpty()) {
            Collections.sort(addedUids, Collections.reverseOrder());
            fetchMessages(addedUids, uidsToDelete);
        }
        // Delete and expunge messages
        deleteMessages(uidsToDelete);
        remoteFolder.close();

        // Update sync state for new mailbox status
        syncState.setExists(mb.getExists());
        syncState.setUnseen(mb.getUnseen());

        // Clean up tracked message state no longer in use
        trackedMsgs = null;
        localMsgIds = null;
        completed = true;
    }

    
    private SyncState getSyncState(boolean fullSync) throws ServiceException {
        syncState = ds.removeSyncState(localFolder.getId());
        if (syncState == null || fullSync) {
            int lastModSeq = 0;
            synchronized (mailbox) {
                trackedMsgs = tracker.getMessages();
                if (fullSync) {
                    localMsgIds = localFolder.getMessageIds();
                    lastModSeq = mailbox.getLastChangeID();
                }
            }
            if (syncState == null) {
                syncState = new SyncState();
                syncState.setLastUid(trackedMsgs.getLastUid());
                if (!fullSync) {
                    trackedMsgs = null;
                }
            }
            if (lastModSeq > 0) {
                syncState.setLastModSeq(lastModSeq);
            }
        }
        return syncState;
    }

    private void syncFlags(long lastUid) throws ServiceException, IOException {
        // Fetch flag changes and delete message ids that have been seen
        if (lastUid > 0) {
            fetchFlags(lastUid, localMsgIds);
        }
        // Check for messages deleted remotely or new local messages
        for (int id : localMsgIds) {
            ImapMessage trackedMsg = trackedMsgs.getByItemId(id);
            
            if (trackedMsg != null) {
                localFolder.deleteMessage(id);
                trackedMsg.delete();
                stats.msgsDeletedLocally++;
            } else {
                newMsgIds.add(id);
                stats.msgsAddedRemotely++;
            }
        }
    }

    private int pushChanges(int lastModSeq) throws ServiceException, IOException {
        localFolder.debug("pushChanges: lastModSeq = %d", lastModSeq);
        List<Integer> deletedIds;
        List<Integer> modifiedIds;
        int newModSeq;
        synchronized (mailbox) {
            newModSeq = mailbox.getLastChangeID();
            if (newModSeq <= lastModSeq) {
                return lastModSeq; // No changes
            }
            deletedIds = mailbox.getTombstones(lastModSeq).getIds(MailItem.TYPE_MESSAGE);
            modifiedIds = mailbox.getModifiedItems(null, lastModSeq, MailItem.TYPE_MESSAGE).getFirst();
        }
        if (deletedIds == null) {
            deletedIds = new ArrayList<Integer>();
        }
        if (modifiedIds != null) {
            for (int id : modifiedIds) {
                clearError(id);
                Message msg = localFolder.getMessage(id);
                if (msg != null) {
                    try {
                        pushModification(msg, deletedIds);
                    } catch (Exception e) {
                        pushFailed(id, "Push modification failed", e);
                    }
                }
            }
        }
        if (!deletedIds.isEmpty()) {
            pushDeletes(deletedIds);
        }
        return newModSeq;
    }

    private void pushDeletes(List<Integer> deletedIds)
        throws ServiceException, IOException {
        int folderId = localFolder.getId();
        for (int id : deletedIds) {
            clearError(id);
            ImapMessage msgTracker = getMsgTracker(id);
            if (msgTracker != null && msgTracker.getFolderId() == folderId) {
                deleteMessage(msgTracker.getUid());
            }
        }
    }

    /*
     * There are four ways in which a message could have been modified:
     *
     * 1. Message flags changed
     * 2. New message added to this folder
     * 3. Message moved from this folder to another
     * 4. Message moved to this folder from another
     *
     * We can handle cases 1-3 here. Case 4 is the mirror of case 3 so just
     * let it be handled when the originating folder is processed.
     */
    private void pushModification(Message msg, List<Integer> deletedIds)
        throws ServiceException, IOException {
        int folderId = localFolder.getId();
        int msgId = msg.getId();
        int msgFolderId = msg.getFolderId();

        ImapMessage msgTracker = getMsgTracker(msgId);
        if (msgTracker != null) {
            int trackedFolderId = tracker.getFolderId();
            if (msgFolderId == trackedFolderId) {
                if (trackedFolderId == folderId) {
                    // Case 1: Message flags changed. Update remote flags.
                    int flags = msgTracker.getFlags();
                    updateFlags(msgTracker, SyncUtil.zimbraToImapFlags(flags));
                } else {
                    // Case 4: Message moved from another folder. Let move be
                    // handled when source folder is processed.
                }
            } else if (trackedFolderId == folderId) {
                // Case 3: Message moved to another folder. Try to use COPY
                // if UIDPLUS available, otherwise just delete the message
                // remotely and let the new local message be appended when
                // the destination folder is processed.
                if (!moveMessage(msgTracker)) {
                    deletedIds.add(msgId);
                }
            }
        } else if (msgFolderId == folderId) {
            // Case 2: New message has been added to this folder
            newMsgIds.add(msgId);
        }
    }

    public void finishSync() throws ServiceException, IOException {
        if (!completed) return;
        if (newMsgIds != null && !newMsgIds.isEmpty()) {
            appendMsgs(newMsgIds);
        }
        if (syncState != null) {
            ds.putSyncState(localFolder.getId(), syncState);
        }
        if (LOG.isDebugEnabled()) {
            if (stats.flagsUpdatedLocally > 0) {
                localFolder.debug("Updated %d flags", stats.flagsUpdatedLocally);
            }
            if (stats.flagsUpdatedRemotely > 0) {
                remoteFolder.debug("Updated %d flags", stats.flagsUpdatedRemotely);
            }
            if (stats.msgsAddedLocally > 0) {
                localFolder.debug("Added %d new messages", stats.msgsAddedLocally);
            }
            if (stats.msgsAddedRemotely > 0) {
                remoteFolder.debug("Added %d new messages", stats.msgsAddedRemotely);
            }
            if (stats.msgsDeletedLocally > 0) {
                localFolder.debug("Deleted %d messages", stats.msgsDeletedLocally);
            }
            if (stats.msgsDeletedRemotely > 0) {
                remoteFolder.debug("Deleted %d messages", stats.msgsDeletedRemotely);
            }
            if (stats.msgsCopiedRemotely > 0) {
                remoteFolder.debug("Copied %d messages", stats.msgsCopiedRemotely);
            }
            // localFolder.debug("Synchronization completed");
        }
    }

    public LocalFolder getLocalFolder() {
        return localFolder;
    }

    private void checkTrackedFolder(ListData ld)
        throws ServiceException, IOException {
        // Check if local folder was deleted
        localFolder = LocalFolder.fromId(mailbox, tracker.getItemId());
        DataSourceManager dsm = DataSourceManager.getInstance();
        if (localFolder == null || (!dsm.isSyncCapable(ds, localFolder.getFolder()) &&
        		                    !localFolder.getPath().equals(tracker.getLocalPath()))) {
            LOG.debug("Local folder '%s' was deleted", tracker.getLocalPath());
            if (deleteRemoteFolder(remoteFolder, tracker.getItemId())) {
                imapSync.deleteFolderTracker(tracker);
            }
            tracker = null;
            return;
        }
        // Check if local folder was renamed
        if (!localFolder.getPath().equals(tracker.getLocalPath())) {
            renameFolder(ld, localFolder.getId());
        }
    }

    private boolean deleteRemoteFolder(RemoteFolder folder, int itemId)
        throws ServiceException, IOException {
        try {
            folder.delete();
        } catch (CommandFailedException e) {
            syncFolderFailed(itemId, folder.getPath(), "Unable to delete remote folder", e);
            return false;
        }
        return true;
    }
    
    private boolean renameFolder(ListData ld, int itemId)
        throws ServiceException, IOException {
        String localPath = localFolder.getPath();
        String newRemotePath = imapSync.getRemotePath(localFolder.getFolder());
        localFolder.info("folder was renamed (originally '%s')", tracker.getLocalPath());
        if (newRemotePath != null) {
            // Folder renamed but still under data source root
            try {
                if (!newRemotePath.equals(remoteFolder.getPath()))
                    remoteFolder = remoteFolder.renameTo(newRemotePath);
            } catch (CommandFailedException e) {
                syncFolderFailed(itemId, localPath,
                    "Unable to rename remote folder to " + newRemotePath, e);
                return false;
            }
            tracker.setLocalPath(localPath);
            tracker.setRemoteId(newRemotePath);
            tracker.update();
            ds.clearSyncState(tracker.getFolderId());
        } else {
            // Folder was moved outside of the data source root, or
            // folder should no longer be synchronized
            localFolder.info("folder was moved outside data source root");
            imapSync.deleteFolderTracker(tracker);
            // Create new local folder for remote path
            createLocalFolder(ld);
        }
        return true;
    }

    private void createLocalFolder(ListData ld)
        throws ServiceException, IOException {
        String remotePath = ld.getMailbox();
        String localPath = imapSync.getLocalPath(ld);
        if (localPath == null) {
            // LOG.info("Remote IMAP folder '%s' is not being synchronized", remotePath);
            tracker = null;
            return;
        }
        Flags flags = ld.getFlags();
        long uidValidity = 0;
        if (!flags.isNoselect()) {
            remoteFolder.select();
            uidValidity = getUidValidity();
        }
        // Create local folder
        localFolder = new LocalFolder(mailbox, localPath);
        if (!localFolder.exists()) {
            localFolder.create();
        }
        // Handle possible case conversion of INBOX in path
        localPath = localFolder.getPath();
        tracker = imapSync.createFolderTracker(
            localFolder.getId(), localPath, remotePath, uidValidity);
    }                                                        

    private void appendMsgs(List<Integer> itemIds) throws ServiceException, IOException {
        remoteFolder.info("Appending %d message(s) to remote IMAP folder", itemIds.size());
        ImapAppender appender = newImapAppender(remoteFolder.getPath());
        for (int id : itemIds) {
            if (skipItem(id)) {
                LOG.warn("Skipping append of item %d due to previous errors", id);
                continue;
            }
            Message msg = localFolder.getMessage(id);
            if (msg == null) {
                clearError(id);
                continue; // Message must have been deleted
            }
            remoteFolder.debug("Appending new message with item id %d", id);
            // Bug 27924: delete tracker from source folder if it exists
            ImapMessage msgTracker = getMsgTracker(id);
            if (msgTracker != null) {
                msgTracker.delete();
            }
            try {
                long uid = appender.appendMessage(msg);
                try {
                    msgTracker = tracker.getMessage(uid);
                    if (msgTracker.getItemId() != id) {
                        // Another track associated with same remote message
                        // because of deduping. Delete deplicate local message
                        localFolder.deleteMessage(id);
                    }
                } catch (MailServiceException.NoSuchItemException e) {
                    storeImapMessage(uid, id, msg.getFlagBitmask());
                }
            } catch (Exception e) {
                syncMessageFailed(id, "Append message failed", e);
            }
        }
    }

    private ImapMessage getMsgTracker(int itemId) throws ServiceException {
        try {
            return tracker.getMessage(itemId);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }

    private void storeImapMessage(long uid, int msgId, int flags)
        throws ServiceException {
        ImapMessage msgTracker = new ImapMessage(ds, localFolder.getId(), msgId, flags, uid);
        
        msgTracker.add();
        if (uid > syncState.getLastUid())
            syncState.setLastUid(uid);
    }

    /*
     * Select and check UIDVALIDITY for remote folder. If UIDVALIDITY has
     * changed since the last sync, then append new local messages to the
     * remote folder and empty the local folder so that messages will be
     * fetched again when we synchronize.
     */
    private MailboxInfo checkUidValidity()
        throws ServiceException, IOException {
        MailboxInfo mb = remoteFolder.select();
        long uidValidity = getUidValidity();
        if (uidValidity == tracker.getUidValidity()) {
            return mb;
        }
        remoteFolder.info("Resynchronizing folder because UIDVALIDITY has " +
                          "changed from %d to %d", tracker.getUidValidity(), uidValidity);
        List<Integer> newLocalIds = tracker.getNewMessageIds();
        if (newLocalIds.size() > 0) {
            remoteFolder.info("Copying %d messages to remote folder", newLocalIds.size());
            ImapAppender appender = newImapAppender(remoteFolder.getPath());
            for (int id : newLocalIds) {
                clearError(id);
                Message msg = localFolder.getMessage(id);
                if (msg != null) {
                    try {
                        appender.appendMessage(msg);
                    } catch (Exception e) {
                        syncMessageFailed(id, "Append message failed", e);
                    }
                }
            }
        }
        // Empty local folder so that it will be resynced later
        int folderId = localFolder.getId();
        mailbox.emptyFolder(null, tracker.getItemId(), false);
        localFolder.emptyFolder();
        tracker.deleteMappings();
        tracker.setUidValidity(uidValidity);
        tracker.update();
        ds.clearSyncState(folderId);
        return remoteFolder.select();
    }

    private ImapAppender newImapAppender(String path) {
        return new ImapAppender(connection, path).setHasAppendUid(hasAppendUid());
    }
    
    private void fetchFlags(long lastUid, Set<Integer> msgIds)
        throws ServiceException, IOException {
        String seq = 1 + ":" + lastUid;
        remoteFolder.debug("Fetching flags for UID sequence %s", seq);
        List<Long> uidsToDelete = fetchFlags(seq, msgIds);
        deleteMessages(uidsToDelete);
    }

    private List<Long> fetchFlags(String seq, Set<Integer> existingMsgIds)
        throws ServiceException, IOException {
        Map<Long, MessageData> mds = connection.uidFetch(seq, "FLAGS");
        // Remove messages that have been flagged \Deleted
        removeDeleted(mds);
        List<Long> uidsToDelete = new ArrayList<Long>();
        for (MessageData md : mds.values()) {
            long uid = md.getUid();
            ImapMessage trackedMsg = trackedMsgs.getByUid(uid);
            if (trackedMsg != null) {
                int msgId = trackedMsg.getItemId();
                if (existingMsgIds.contains(msgId)) {
                    existingMsgIds.remove(msgId);
                    try {
                        updateFlags(trackedMsg, md.getFlags());
                        clearError(msgId);
                    } catch (MailServiceException.NoSuchItemException e) {
                        // Message was deleted locally
                        uidsToDelete.add(uid);
                        clearError(msgId);
                    } catch (Exception e) {
                        syncMessageFailed(msgId, "Unable to update message flags", e);
                    }
                } else {
                    uidsToDelete.add(uid);
                    clearError(msgId);
                }
            } else {
                remoteFolder.debug(
                    "Adding new message with UID %d detected while syncing flags", uid);
                addedUids.add(uid);
            }
        }
        return uidsToDelete;
    }

    // Updates flags for specified message.
    private void updateFlags(ImapMessage msg, Flags flags)
        throws ServiceException, IOException {
        int id = msg.getItemId();
        int localFlags = msg.getItemFlags();
        int trackedFlags = msg.getFlags();
        int remoteFlags = SyncUtil.imapToZimbraFlags(flags);
        int newLocalFlags = mergeFlags(localFlags, trackedFlags, remoteFlags);
        int newRemoteFlags = SyncUtil.imapFlagsOnly(newLocalFlags);
        if (LOG.isDebugEnabled() && (newLocalFlags != localFlags ||
            newRemoteFlags != remoteFlags || newRemoteFlags != trackedFlags)) {
            localFolder.debug("Updating flags for message with item id %d: " +
                "local=%s, tracked=%s, remote=%s, new_local=%s, new_remote=%s",
                id, Flag.bitmaskToFlags(localFlags),
                Flag.bitmaskToFlags(trackedFlags),
                Flag.bitmaskToFlags(remoteFlags),
                Flag.bitmaskToFlags(newLocalFlags),
                Flag.bitmaskToFlags(newRemoteFlags));
        }
        if (newLocalFlags != localFlags) {
            localFolder.setMessageFlags(id, newLocalFlags);
            stats.flagsUpdatedLocally++;
        }
        if (newRemoteFlags != remoteFlags) {
            String uids = String.valueOf(msg.getUid());
            Flags toAdd = SyncUtil.getFlagsToAdd(flags, newRemoteFlags);
            Flags toRemove = SyncUtil.getFlagsToRemove(flags, newRemoteFlags);
            // UID STORE never fails even if message was deleted
            if (!toAdd.isEmpty()) {
                connection.uidStore(uids, "+FLAGS.SILENT", toAdd);
            }
            if (!toRemove.isEmpty()) {
                connection.uidStore(uids, "-FLAGS.SILENT", toRemove);
            }
            stats.flagsUpdatedRemotely++;
        }
        if (newRemoteFlags != trackedFlags) {
            msg.setFlags(newRemoteFlags);
            msg.update();
            stats.flagsUpdatedLocally++;
        }
    }

    private void fetchMessages(long startUid, List<Long> uidsToDelete)
        throws ServiceException, IOException {
        List<Long> uids = remoteFolder.getUids(startUid, maxUid);
        if (uids.size() > 0) {
            fetchMessages(uids, uidsToDelete);
        }
    }

    private void fetchMessages(List<Long> uids, List<Long> uidsToDelete)
        throws ServiceException, IOException {
        remoteFolder.debug("Fetching %d new IMAP message(s)", uids.size());
        long lastCheckTime = System.currentTimeMillis();
        removeSkippedUids(uids);
        Iterator<Long> ui = uids.iterator();
        while (ui.hasNext()) {
            imapSync.checkIsEnabled();
            fetchMessages(nextFetchSeq(ui), uidsToDelete);
            // Send pending messages if any...
            ds.checkPendingMessages();
            long time = System.currentTimeMillis();
            long freq = ds.getSyncFrequency();
            if (maxUid > 0 && freq > 0 && time - lastCheckTime > freq) {
                // Check for newly arrived messages...
                lastCheckTime = time;
                List<Long> newUids = remoteFolder.getUids(maxUid + 1, 0);
                if (!newUids.isEmpty()) {
                    remoteFolder.debug("Fetching %d newly arrived IMAP message(s)", newUids.size());
                    Iterator<Long> nui = newUids.iterator();
                    do {
                        fetchMessages(nextFetchSeq(nui), uidsToDelete);
                    } while (nui.hasNext());
                    maxUid = newUids.get(0);
                }
            }
        }
    }

    // Remove UIDs which should be skipped due to errors
    private void removeSkippedUids(List<Long> uids) {
        Iterator<Long> it = uids.iterator();
        while (it.hasNext()) {
            long uid = it.next();
            if (skipUid(uid)) {
                LOG.warn("Skipping fetch of uid %d due to previous errors", uid);
                it.remove();
            }
        }
    }
    
    private String nextFetchSeq(Iterator<Long> uids) {
        StringBuilder sb = new StringBuilder();
        sb.append(uids.next());
        for (int count = FETCH_SIZE; uids.hasNext() && --count > 0; ) {
            sb.append(',').append(uids.next());
        }
        return sb.toString();
    }
    
    private void fetchMessages(String seq, final List<Long> uidsToDelete)
        throws ServiceException, IOException {
        final Map<Long, MessageData> flagsByUid =
            connection.uidFetch(seq, "(FLAGS INTERNALDATE)");
        removeDeleted(flagsByUid);
        final Set<Long> uidSet = flagsByUid.keySet();
        if (uidSet.isEmpty()) return;
        FetchResponseHandler handler = new FetchResponseHandler() {
            public void handleFetchResponse(MessageData md) throws Exception {
                long uid = md.getUid();
                try {
                    handleFetch(md, flagsByUid, uidsToDelete);
                    clearError(uid);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("Out of memory");
                } catch (Exception e) {
                    syncFailed("Fetch failed for uid " + uid, e);
                    SyncErrorManager.incrementErrorCount(ds, remoteId(uid));
                }
                uidSet.remove(uid);
            }
        };
        // Try fetching group of messages first
        LOG.debug("Fetching messages for sequence: " + seq);
        try {
            connection.uidFetch(getSequence(uidSet), "BODY.PEEK[]", handler);
        } catch (CommandFailedException e) {
            String msg = "UID FETCH failed: " + e.toString();
            checkCanContinue(msg, e);
            LOG.warn(msg, e);
        }
        if (uidSet.isEmpty()) return;
        LOG.info("Fetching remaining messages one at a time for UIDs: " + uidSet);
        for (long uid : getOrderedUids(uidSet)) {
            try {
                LOG.info("Fetching message for uid: " + uid);
                MessageData md = connection.uidFetch(uid, "BODY.PEEK[]");
                handler.handleFetchResponse(md);
            } catch (Exception e) {
                String msg = "Error while fetching message for UID " + uid;
                checkCanContinue(msg, e);
                LOG.warn(msg, e);
            }
        }
        if (!uidSet.isEmpty()) {
            LOG.error("Unable to fetch messages for uids: " + uidSet);
        }
    }

    // Discard messages that have been flagged \Deleted
    private void removeDeleted(Map<Long, MessageData> mds) {
        Iterator<MessageData> it = mds.values().iterator();
        while (it.hasNext()) {
            MessageData md = it.next();
            Flags flags = md.getFlags();
            if (flags != null && flags.isDeleted()) {
                remoteFolder.debug(
                    "Remote message with uid %d is flagged \\Deleted", md.getUid());
                it.remove();
            }
        }
    }

    private static String getSequence(Set<Long> uidSet) {
        StringBuilder sb = new StringBuilder();
        Iterator<Long> it = getOrderedUids(uidSet).iterator();
        sb.append(it.next());
        while (it.hasNext()) {
            sb.append(',').append(it.next());
        }
        return sb.toString();
    }

    private static Collection<Long> getOrderedUids(Collection<Long> uidSet) {
        List<Long> uids = new ArrayList<Long>(uidSet);
        Collections.sort(uids, Collections.reverseOrder());
        return uids;
    }

    private void handleFetch(MessageData md,
                             Map<Long, MessageData> flagsByUid,
                             List<Long> uidsToDelete)
        throws ServiceException, IOException {
        long uid = md.getUid();
        if (uid == -1) {
            throw new MailException("Missing UID in FETCH response");
        }
        MessageData flagsData = flagsByUid.get(uid);
        remoteFolder.debug("Found new IMAP message with uid %d", uid);
        // Parse the message data
        Date date = flagsData.getInternalDate();
        Long receivedDate = date != null ? date.getTime() : null;
        int zflags = SyncUtil.imapToZimbraFlags(flagsData.getFlags());
        int folderId = localFolder.getId();
        MessageContent mc = getContent(md);
        Message msg;
        try {
            ParsedMessage pm = mc.getParsedMessage(receivedDate, mailbox.attachmentsIndexingEnabled());
            msg = imapSync.addMessage(null, pm, folderId, zflags, mc.getDeliveryContext());
        } finally {
            mc.cleanup();
        }
        if (msg != null && msg.getFolderId() == folderId) {
            storeImapMessage(uid, msg.getId(), zflags);
            stats.msgsAddedLocally++;
        } else {
            // Message was filtered and discarded or moved to another folder.
            // This can only happen for messages fetched from INBOX which is
            // always imported first. Mark remote message for deletion and
            // do not create a local tracker. If message was moved to another
            // folder we will append it to the remote folder when we sync
            // that folder.
            uidsToDelete.add(uid);
        }
    }

    private static MessageContent getContent(MessageData md) throws MailException {
        Body[] sections = md.getBodySections();
        if (sections == null || sections.length != 1) {
            throw new MailException(
              "Invalid body section FETCH response for uid " +  md.getUid());
        }
        return (MessageContent) sections[0].getData();
    }
    
    private void deleteMessages(List<Long> uids) throws ServiceException, IOException {
        for (long uid : uids) {
            deleteMessage(uid);
        }
    }

    private boolean deleteMessage(long uid) throws ServiceException, IOException {
        if (skipUid(uid)) {
            LOG.warn("Skipping remote delete of uid %d due to previous errors", uid);
            return false;
        }
        try {
            remoteFolder.deleteMessage(uid);
            stats.msgsDeletedRemotely += 1;
            clearError(uid);
        } catch (CommandFailedException e) {
            syncMessageFailed(uid, "Cannot delete message with uid " + uid, e);
            return false;
        }
        try {
            tracker.getMessage(uid).delete();
        } catch (MailServiceException.NoSuchItemException e) {
            // Fall through...
        }
        return true;
    }

    private void moveMessages() throws IOException, ServiceException {
        Collection<DataSourceItem> mappings = tracker.getMappings();
        List<Integer> allIds = mailbox.listItemIds(mailbox.getOperationContext(),
            MailItem.TYPE_MESSAGE, tracker.getItemId());
        Integer sortedIds[] = allIds.toArray(new Integer[allIds.size()]);

        Arrays.sort(sortedIds);
        for (DataSourceItem mapping : mappings) {
            if (Arrays.binarySearch(sortedIds, mapping.itemId) < 0)
                moveMessage(new ImapMessage(ds, mapping));
        }
    }

    private boolean moveMessage(ImapMessage msgTracker)
        throws ServiceException, IOException {
        Message msg;
        Folder folder;

        if (!hasCopyUid())
            return false;
        try {
            msg = mailbox.getMessageById(null, msgTracker.getItemId());
            folder = mailbox.getFolderById(null, msg.getFolderId());
        } catch (MailServiceException.NoSuchItemException e) {
            return false;
        }
        if (!DataSourceManager.getInstance().isSyncEnabled(ds, folder)) return false;
        int fid = folder.getId();
        ImapFolderCollection trackedFolders = imapSync.getTrackedFolders();
        ImapFolder folderTracker = trackedFolders.getByItemId(fid);
        String remotePath;
        if (folderTracker != null) {
            remotePath = folderTracker.getRemoteId();
        } else {
            // If remote folder does not exist, then create it on demand
            RemoteFolder newFolder = createRemoteFolder(folder);
            if (newFolder == null) return false;
            remotePath = newFolder.getPath();
        }
        CopyResult cr;
        try {
            cr = remoteFolder.copyMessage(msgTracker.getUid(), remotePath);
        } catch (IOException e) {
            syncMessageFailed(msgTracker.getUid(), "COPY failed", e);
            return false;
        }
        if (cr == null) {
            return false; // Message not found
        }
        stats.msgsCopiedRemotely++;
        // If remote folder created on demand, then create folder tracker
        if (folderTracker == null) {
            imapSync.createFolderTracker(
                fid, folder.getPath(), remotePath, cr.getUidValidity());
        } else {
            // If target folder was already sync'd, then make sure we remove
            // msg id from folder's list of new messages to be appended
            ImapFolderSync syncedFolder = imapSync.getSyncedFolder(fid);
            if (syncedFolder != null && syncedFolder.newMsgIds != null) {
                syncedFolder.newMsgIds.remove(Integer.valueOf(msg.getId()));
            }
        }
        if (!deleteMessage(msgTracker.getUid())) {
            LOG.warn("Unable to delete message with uid " + msgTracker.getUid());
            return false;
        }
        // Delete original message tracker and create new one
        msgTracker.delete();
        long uid = cr.getToUids()[0];
        msgTracker = new ImapMessage(ds, fid, msg.getId(), msgTracker.getFlags(), uid);
        msgTracker.add();
        // This bit of ugliness is to make sure we update target folder sync state
        // to reflect UID that was just added
        ImapFolderSync syncedFolder = imapSync.getSyncedFolder(fid);
        if (syncedFolder != null && syncedFolder.syncState != null) {
            syncedFolder.syncState.updateLastUid(uid);
        } else {
            SyncState ss = ds.getSyncState(fid);
            if (ss != null) {
                ss.updateLastUid(uid);
                ds.putSyncState(fid, ss);
            }
        }
        return true;
    }

    /*
     * Merges local flags, tracked flags, and remote flag and returns new
     * local flags bitmask.
     */
    private static int mergeFlags(int localFlags, int trackedFlags,
                                  int remoteFlags) {
        return trackedFlags & (localFlags & remoteFlags) |
              ~trackedFlags & (localFlags | remoteFlags);
    }
    
    private boolean hasCopyUid() {
        return connection.hasUidPlus() || isYahoo();
    }

    private boolean hasAppendUid() {
        return connection.hasUidPlus() || isYahoo();
    }
    
    private boolean isYahoo() {
        return ImapUtil.isYahoo(connection);
    }
    
    private long getUidValidity() {
        // Bug 35554: If server does not provide UIDVALIDITY, then assume a value of 1
        long uidValidity = connection.getMailbox().getUidValidity();
        return uidValidity > 0 ? uidValidity : 1;
    }
    
    private boolean skipItem(int itemId) {
        return SyncErrorManager.getErrorCount(ds, itemId) >= MAX_ITEM_ERRORS;
    }

    private boolean skipUid(long uid) {
        return SyncErrorManager.getErrorCount(ds, remoteId(uid)) >= MAX_ITEM_ERRORS;
    }

    private String remoteId(long uid) {
        return getUidValidity() + ":" + uid;
    }

    private void clearError(int itemId) {
        SyncErrorManager.clearError(ds, itemId);
    }

    private void clearError(long uid) {
        SyncErrorManager.clearError(ds, remoteId(uid));
    }

    private void pushFailed(int itemId, String msg, Exception e)
        throws ServiceException {
        checkCanContinue(msg, e);
        LOG.error(msg, e);
        ds.reportError(itemId, msg, e);
    }
    
    private void syncFailed(String msg, Exception e)
        throws ServiceException {
        checkCanContinue(msg, e);
        LOG.error(msg, e);
        ds.reportError(-1, msg, e);
    }

    private void syncFolderFailed(int itemId, String path, String msg, Exception e)
        throws ServiceException {
        checkCanContinue(msg, e);
        int count = SyncErrorManager.incrementErrorCount(ds, itemId);
        if (count <= MAX_ITEM_ERRORS) {
            LOG.error(msg, e);
            if (count == MAX_ITEM_ERRORS) {
                String error = String.format(
                    "Synchronization of folder '%s' disabled due to error: %s",
                    path, msg);
                ds.reportError(itemId, error, e);
                try {
                    if (ds.isOffline()) {
                        // Disable sync on folder
                        SyncUtil.setSyncEnabled(DataSourceManager.getInstance().getMailbox(ds), itemId, false);
                    }
                } catch (MailServiceException.NoSuchItemException ex) {
                    // Ignore if local folder has been deleted
                }
                // Clear error state in case folder sync reenabled
                clearError(itemId);
            }
        }
    }

    private void syncMessageFailed(int itemId, String msg, Exception e)
        throws ServiceException {
        // For command failures, increment error count before checking if
        // we can continue with other requests
        if (!(e instanceof CommandFailedException)) {
            checkCanContinue(msg, e);
        }
        int count = SyncErrorManager.incrementErrorCount(ds, itemId);
        if (count <= MAX_ITEM_ERRORS) {
            LOG.error(msg, e);
            if (count == MAX_ITEM_ERRORS) {
                ds.reportError(itemId, msg, e);
            }
        }
        incrementTotalErrors();
        if (e instanceof CommandFailedException &&
            !((CommandFailedException) e).canContinue()) {
            throw ServiceException.FAILURE(msg, e);
        }
    }

    private void syncMessageFailed(long uid, String msg, Exception e)
        throws ServiceException {
        checkCanContinue(msg, e);
        int count = SyncErrorManager.incrementErrorCount(ds, remoteId(uid));
        if (count <= MAX_ITEM_ERRORS) {
            LOG.error(msg, e);
            if (count == MAX_ITEM_ERRORS) {
                ds.reportError(-1, msg, e);
            }
        }
        incrementTotalErrors();
    }

    private void incrementTotalErrors() throws ServiceException {
        totalErrors++;
        if (totalErrors > MAX_TOTAL_ERRORS) {
            String error = String.format(
                "Synchronization of folder '%s' disabled due to maximum number of per-item errors exceeded",
                localFolder.getPath());
            ds.reportError(localFolder.getId(), error, ServiceException.FAILURE(error, null));
            try {
                // Disable sync on folder
                SyncUtil.setSyncEnabled(DataSourceManager.getInstance().getMailbox(ds), localFolder.getId(), false);
            } catch (MailServiceException.NoSuchItemException e) {
                // Ignore if local folder has been deleted
            }
        }
    }
    
    // Log error and abort sync if we can't continue
    private void checkCanContinue(String msg, Exception e) throws ServiceException {
        if (!canContinue(e)) {
            LOG.error(msg, e);
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.FAILURE(msg, e);
            }
        }
    }

    private boolean canContinue(Throwable e) {
        if (!ds.isOffline()) {
            return false;
        } else if (e instanceof ServiceException) {
            Throwable cause = e.getCause();
            return cause == null || canContinue(cause);
        } else if (e instanceof SQLException) {
            return Db.errorMatches((SQLException) e, Db.Error.DUPLICATE_ROW);
        } else if (e instanceof CommandFailedException) {
            return ((CommandFailedException) e).canContinue();
        } else {
            return false;
        }
    }
}

