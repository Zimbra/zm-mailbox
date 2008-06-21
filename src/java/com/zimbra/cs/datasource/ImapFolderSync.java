/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.Mailbox;
import com.zimbra.cs.mailclient.imap.ImapData;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.ImapCapabilities;
import com.zimbra.cs.mailclient.imap.FetchResponseHandler;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.db.DbImapMessage;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.Log;
import com.zimbra.common.localconfig.LC;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MailDateFormat;
import java.io.IOException;
import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class ImapFolderSync {
    private final ImapSync imapSync;
    private final ImapConnection connection;
    private final boolean uidPlus;
    private final DataSource ds;
    private final com.zimbra.cs.mailbox.Mailbox mailbox;
    private final Statistics stats = new Statistics();
    private Folder folder;
    private ImapFolder tracker;
    private RemoteFolder remoteFolder;
    private SyncState syncState;
    private ImapMessageCollection trackedMsgs;
    private List<Integer> newMsgIds;
    private boolean expunge;
    private MailDateFormat mdf; 

    private static final Log LOG = ZimbraLog.datasource;

    private static final int FETCH_SIZE = LC.data_source_fetch_size.intValue();

    private static class Statistics {
        int flagsUpdatedLocally;
        int flagsUpdatedRemotely;
        int msgsAddedLocally;
        int msgsAddedRemotely;
        int msgsDeletedLocally;
        int msgsDeletedRemotely;
    }

    public ImapFolderSync(ImapSync imapSync, ImapFolder tracker)
        throws ServiceException {
        this.imapSync = imapSync;
        connection = imapSync.getConnection();
        uidPlus = connection.hasCapability(ImapCapabilities.UIDPLUS);
        ds = imapSync.getDataSource();
        mailbox = ds.getMailbox();
        this.tracker = tracker;
    }

    // Synchronizes existing remote IMAP folder -> local folder
    public ImapFolder syncFolder(ListData ld, boolean fullSync)
        throws ServiceException, IOException {
        remoteFolder = new RemoteFolder(connection, ld.getMailbox());
        boolean syncMsgs;
        if (tracker != null) {
            syncMsgs = checkTrackedFolder(ld);
        } else {
            syncMsgs = createLocalFolder(ld);
        }
        if (syncMsgs) {
            syncMessages(fullSync);
        }
        return tracker;
    }

    // Synchronize local folder with remote IMAP folder
    public void syncFolder(Folder folder, boolean fullSync)
        throws ServiceException, IOException {
        this.folder = folder;
        if (tracker != null) {
            // Remote IMAP folder deleted, empty local folder
            info("Remote IMAP folder '%s' deleted, deleting local folder",
                 tracker.getRemotePath());
            mailbox.delete(null, folder.getId(), folder.getType());
            ds.deleteImapFolder(tracker);
            return;
        }
        // Create and track new remote IMAP folder
        String remotePath = imapSync.getRemotePath(folder);
        if (remotePath == null) {
            // info("Synchronization disabled for this folder");
            return;
        }
        remoteFolder = new RemoteFolder(connection, remotePath);
        remoteFolder.create();
        Mailbox mb = remoteFolder.select();
        tracker = ds.createImapFolder(folder.getId(), folder.getPath(),
            remotePath, mb.getUidValidity());
        syncMessages(fullSync);
    }

    public String getLocalPath() {
        return tracker != null ? tracker.getLocalPath() : null;
    }

    public void finishSync() throws ServiceException, IOException {
        appendNewMessages();
        if (syncState != null) {
            ds.putSyncState(folder.getId(), syncState);
        }
        /*
        if (LOG.isDebugEnabled()) {
            debug("Synchronization completed: " +
                "flags updated locally = %d, flags updated remotely = %d, " +
                "messages added locally = %d, messages added remotely = %d, " +
                "messages deleted locally = %d, messages deleted remotely = %d",
                stats.flagsUpdatedLocally, stats.flagsUpdatedRemotely,
                stats.msgsAddedLocally, stats.msgsAddedRemotely,
                stats.msgsDeletedLocally, stats.msgsDeletedRemotely);
        }
        */
    }

    /*
     * Synchronizes messages between local and remote folder.
     */
    private void syncMessages(boolean fullSync)
        throws ServiceException, IOException {
        if (tracker.getUidValidity() == 0 ||
            !ds.isSyncEnabled(tracker.getLocalPath())) {
            debug("Synchronization disabled for this folder");
            return;
        }
        Mailbox mb = remoteFolder.select();
        if (!checkUidValidity(mb)) {
            mb = remoteFolder.select();
        }
        syncState = getSyncState();
        // debug("SyncState = " + syncState);
        long uidNext = mb.getUidNext();
        if (uidNext > 0 && uidNext <= syncState.getLastUid()) {
            throw new MailException("Invalid UIDNEXT (> last sync uid)");
        }
        boolean fetchMsgs = syncState.hasNewMessages(mb);
        long lastUid = syncState.getLastUid();
        // If full sync requested, fetch flags changes
        if (fullSync) {
            if (trackedMsgs == null) {
                trackedMsgs = DbImapMessage.getImapMessages(mailbox, ds, tracker);
            }
            Set<Integer> msgIds = getLocalMsgIds();
            // Fetch flag changes and delete message ids that have been seen
            if (lastUid > 0) {
                fetchFlags(lastUid, msgIds);
            }
            // Check for messages deleted remotely or new local messages
            newMsgIds = new ArrayList<Integer>();
            for (int id : msgIds) {
                if (trackedMsgs.getByItemId(id) != null) {
                    deleteMessage(id);
                } else {
                    newMsgIds.add(id);
                    stats.msgsAddedRemotely++;
                }
            }
        }
        // Fetch new messages
        if (fetchMsgs) {
            fetchMessages(lastUid + 1, uidNext - 1);
        }
        if (expunge) {
            // Close IMAP folder to automatically expunge deleted messages
            connection.mclose();
        }
        // Update sync state for new mailbox status
        syncState.setExists(mb.getExists());
        syncState.setUnseen(mb.getUnseen());
        // Clean up tracked message state no longer in use
        trackedMsgs = null;
    }

    private SyncState getSyncState() throws ServiceException {
        SyncState ss = ds.getSyncState(folder.getId());
        if (ss == null) {
            ss = new SyncState();
            trackedMsgs = DbImapMessage.getImapMessages(mailbox, ds, tracker);
            ss.setLastUid(trackedMsgs.getLastUid());
        }
        return ss;
    }

    private boolean checkTrackedFolder(ListData ld)
        throws ServiceException, IOException {
        // Check if folder has been deleted locally
        try {
            folder = mailbox.getFolderById(null, tracker.getItemId());
        } catch (MailServiceException.NoSuchItemException e) {
            LOG.info("Local folder '%s' deleted", tracker.getLocalPath());
            remoteFolder.delete();
            imapSync.getDataSource().deleteImapFolder(tracker);
            return false;
        }
        // Check if folder exists but was renamed locally
        if (!folder.getPath().equals(tracker.getLocalPath())) {
            return renameFolder(ld);
        }
        return true;
    }

    private boolean renameFolder(ListData ld)
        throws ServiceException, IOException {
        String localPath = folder.getPath();
        String newRemotePath = imapSync.getRemotePath(folder);
        if (newRemotePath != null) {
            // Folder renamed but still under data source root
            info("Folder was renamed (originally '%s')", tracker.getLocalPath());
            remoteFolder.renameTo(newRemotePath);
            remoteFolder = new RemoteFolder(connection, newRemotePath);
            tracker.setLocalPath(localPath);
            tracker.setRemotePath(newRemotePath);
            ds.updateImapFolder(tracker);
            return true;
        }
        // Folder was moved outside of the data source root, or
        // folder should no longer be synchronized
        info("Folder was renamed (originally '%s') and moved outside the " +
             " data source root", tracker.getLocalPath());
        ds.deleteImapFolder(tracker);          
        // Create new local folder for remote path
        createLocalFolder(ld);
        return true;
    }

    private boolean createLocalFolder(ListData ld)
        throws ServiceException, IOException {
        String remotePath = ld.getMailbox();
        String localPath = imapSync.getLocalPath(ld);
        if (localPath == null) {
            // LOG.info("Remote IMAP folder '%s' is not being synchronized", remotePath);
            return false;
        }
        long uidValidity = 0;
        if (!ld.getFlags().isNoselect()) {
            uidValidity = remoteFolder.select().getUidValidity();
        }
        LOG.info("Found new IMAP folder '%s', creating local folder '%s'",
                 remotePath, localPath);
        // Check if local folder already exists
        try {
            folder = mailbox.getFolderByPath(null, localPath);
        } catch (MailServiceException.NoSuchItemException e) {
            // Local folder does not exist, so create it
            folder = mailbox.createFolder(
                null, localPath, (byte) 0, MailItem.TYPE_UNKNOWN);
        }
        // Handle possible case conversion of INBOX in path
        localPath = folder.getPath();
        ds.initializedLocalFolder(localPath, false);
        tracker = ds.createImapFolder(
            folder.getId(), localPath, remotePath, uidValidity);
        return true;
    }
        
    private void deleteMessage(int id) throws ServiceException {
        debug("Deleting local message with item id %d", id);
        try {
            mailbox.delete(null, id, MailItem.TYPE_UNKNOWN);
        } catch (MailServiceException.NoSuchItemException e) {
            debug("Local message with id %d not found", id);
        }
        DbImapMessage.deleteImapMessage(mailbox, tracker.getItemId(), id);
        stats.msgsDeletedLocally++;
    }

    private void appendNewMessages() throws ServiceException, IOException {
        if (newMsgIds == null || newMsgIds.isEmpty()) {
            return; // No messages to be appended
        }
        info("Appending %d new message(s) to remote IMAP folder", newMsgIds.size());
        try {
            if (!uidPlus) {
                appendMessagesNoUidPlus(newMsgIds);
            } else {
                appendMessages(newMsgIds);
            }
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to append messages", e);
        }
    }

    // Appends new messages. Locally cached message trackers do not exist
    // anymore at this point.
    private void appendMessages(List<Integer> ids)
        throws ServiceException, IOException {
        for (int id : ids) {
            Message msg = getMessageById(id);
            if (msg == null) {
                continue; // Message must have been deleted
            }
            debug("Appending new message with item id %d", id);
            long uid = appendMessage(msg);
            if (uid <= 0) {
                throw ServiceException.FAILURE(
                    "UIDPLUS supported but no UID returned for appended message", null);
            }
            storeImapMessage(uid, id, msg.getFlagBitmask());
        }
    }
    
    private long appendMessage(Message msg)
        throws ServiceException, IOException {
        MimeMessage mm = msg.getMimeMessage(false);
        Flags flags = SyncUtil.zimbraToImapFlags(msg.getFlagBitmask());
        Date date = SyncUtil.getInternalDate(msg, mm);
        return remoteFolder.appendMessage(mm, flags, date);
    }

    private void appendMessagesNoUidPlus(List<Integer> ids)
        throws ServiceException, MessagingException, IOException {
        final int folderId = tracker.getItemId();
        // If server does not support UIDPLUS, append messages, then fetch
        // ENVELOPE information for messages that were just appended in order
        // to obtain the message UIDs.
        long startUid = remoteFolder.select().getUidNext();
        final HashMap<String, AppendInfo> appended = new HashMap<String, AppendInfo>();
        for (int id : ids) {
            Message msg = getMessageById(id);
            if (msg == null) {
                continue; // Message must have just been deleted
            }
            debug("Appending new message with item id %d", id);
            MimeMessage mm = msg.getMimeMessage(false);
            AppendInfo ai = new AppendInfo(msg, mm);
            Flags flags = SyncUtil.zimbraToImapFlags(ai.zflags);
            Date date = SyncUtil.getInternalDate(msg, mm);
            remoteFolder.appendMessage(mm, flags, date);
            appended.put(appendKey(mm.getSentDate(), ai.messageId), ai);
        }
        // Find UIDs for messages just appended
        mdf = new MailDateFormat();
        long endUid = remoteFolder.select().getUidNext() - 1;
        if (startUid <= endUid) {
            String seq = startUid + ":" + endUid;
            connection.uidFetch(seq, "ENVELOPE", new FetchResponseHandler() {
                public void handleFetchResponse(MessageData md) throws Exception {
                    Envelope env = md.getEnvelope();
                    Date date = mdf.parse(env.getDate());
                    String key = appendKey(date, env.getMessageId());
                    AppendInfo ai = appended.remove(key);
                    if (ai != null) {
                        storeImapMessage(md.getUid(), ai.itemId, ai.zflags);
                    }
                }
            });
        }
        // For any appended message whose UID could not be found using the
        // above method, then it is likely that the server has detected a
        // duplicate message and did not return a new UID (this is true for
        // Gmail anyway). In this case, do a slower SEARCH to find the message
        // and check if we already have a tracker for the returned UID. If so,
        // delete the local message that has just been appended since we
        // already have a copy
        for (AppendInfo ai : appended.values()) {
            MessageData md = findMessage(ai);
            if (md != null) {
                long uid = md.getUid();
                // Check for duplicate message
                int id = DbImapMessage.getLocalMessageId(mailbox, folderId, uid);
                if (id > 0) {
                    info("Deleting duplicate message with item id %d, uid %d",
                         ai.itemId, uid);
                    mailbox.delete(null, ai.itemId, MailItem.TYPE_UNKNOWN);
                } else {
                    storeImapMessage(uid, ai.itemId, ai.zflags);
                }
            } else {
                warn("Unable to determine UID for appended message with item id " +
                     ai.itemId, null);
            }
        }
    }

    private MessageData findMessage(final AppendInfo ai) throws IOException {
        List<Long> uids = connection.uidSearch(
            "SENTON", ai.sentDate, "SUBJECT", ai.subject);
        for (long uid : uids) {
            MessageData md = connection.uidFetch(uid, "ENVELOPE");
            if (md != null && ai.messageId.equals(md.getEnvelope().getMessageId())) {
                return md;
            }
        }
        return null;
    }

    private static String appendKey(Date date, String messageId) {
        return String.format("%s|%s", date, messageId);
    }

    private static class AppendInfo {
        final int itemId;
        final String subject;
        final Date sentDate;
        final String messageId;
        final int zflags;

        AppendInfo(Message msg, MimeMessage mm) throws MessagingException {
            itemId = msg.getId();
            subject = mm.getSubject();
            messageId = mm.getMessageID();
            sentDate = mm.getSentDate();
            zflags = msg.getFlagBitmask();
        }
    }

    private void storeImapMessage(long uid, int itemId, int flags)
        throws ServiceException {
        DbImapMessage.storeImapMessage(mailbox, folder.getId(), uid, itemId, flags);
        if (uid > syncState.getLastUid()) {
            syncState.setLastUid(uid);
        }
    }

    /*
     * Checks UIDVALIDITY for currently selected folder. If UIDVALIDITY has
     * changed since the last sync, then append new local messages to the
     * remote folder and empty the local folder so that messages will be
     * fetched again when we synchronize.
     */
    private boolean checkUidValidity(Mailbox mb)
        throws ServiceException, IOException {
        if (mb.getUidValidity() == tracker.getUidValidity()) {
            return true;
        }
        info("Resynchronizing folder because UIDVALIDITY has changed from " +
             "%d to %d", tracker.getUidValidity(), mb.getUidValidity());
        List<Integer> newLocalIds =
            DbImapMessage.getNewLocalMessageIds(mailbox, ds, tracker);
        if (newLocalIds.size() > 0) {
            info("Copying %d messages to remote folder", newLocalIds.size());
            // TODO Handle append failure for individual messages
            for (int id : newLocalIds) {
                Message msg = getMessageById(id);
                if (msg != null) {
                    appendMessage(msg);
                }
            }
        }
        // Empty local folder so that it will be resynced later
        int folderId = folder.getId();
        mailbox.emptyFolder(null, folderId, false);
        tracker.setUidValidity(mb.getUidValidity());
        ds.updateImapFolder(tracker);
        ds.clearSyncState(folderId);
        DbImapMessage.deleteImapMessages(mailbox, folderId);
        return false;
    }

    private Message getMessageById(int itemId) throws ServiceException {
        try {
            return mailbox.getMessageById(null, itemId);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }
    
    private void fetchFlags(long lastUid, Set<Integer> msgIds)
        throws ServiceException, IOException {
        String seq = 1 + ":" + lastUid;
        debug("Fetching flags changes for UID sequence %s", seq);
        List<Long> deletedUids = fetchFlags(seq, msgIds);
        if (!deletedUids.isEmpty()) {
            debug("Deleting %d message(s) from IMAP folder", deletedUids.size());
            remoteFolder.deleteMessages(deletedUids);
            // Remove local messages
            for (long uid : deletedUids) {
                ImapMessage msg = trackedMsgs.getByUid(uid);
                DbImapMessage.deleteImapMessage(
                    mailbox, tracker.getItemId(), msg.getItemId());
                stats.msgsDeletedRemotely++;
            }
            // Mark folder to be expunged if no UIDPLUS
            if (!uidPlus) {
                expunge = true;
            }
        }
    }

    private List<Long> fetchFlags(String seq, Set<Integer> msgIds)
        throws ServiceException, IOException {
        Map<Long, MessageData> mds = connection.uidFetch(seq, "FLAGS");
        List<Long> deletedUids = new ArrayList<Long>();
        for (MessageData md : mds.values()) {
            Long uid = md.getUid();
            ImapMessage trackedMsg = trackedMsgs.getByUid(uid);
            if (trackedMsg == null) {
                info("Ignoring flags for message with uid %d because it " +
                     "is not being tracked", uid);
                continue;
            }
            int msgId = trackedMsg.getItemId();
            if (msgIds.contains(msgId)) {
                msgIds.remove(msgId);
                try {
                    updateFlags(trackedMsg, md.getFlags());
                    continue;
                } catch (MailServiceException.NoSuchItemException e) {
                    // Message was deleted locally
                }
            }
            deletedUids.add(uid);
        }
        return deletedUids;
    }

    // Updates flags for specified message.
    private void updateFlags(ImapMessage msg, Flags flags)
        throws ServiceException, IOException {
        int id = msg.getItemId();
        int localFlags = msg.getFlags();
        int trackedFlags = msg.getTrackedFlags();
        int remoteFlags = SyncUtil.imapToZimbraFlags(flags);
        int newLocalFlags = mergeFlags(localFlags, trackedFlags, remoteFlags);
        int newRemoteFlags = SyncUtil.imapFlagsOnly(newLocalFlags);
        if (LOG.isDebugEnabled() && (newLocalFlags != localFlags ||
            newRemoteFlags != remoteFlags || newRemoteFlags != trackedFlags)) {
            debug("Updated flags for message with item id %d: local=%s, " +
                  "tracked=%s, remote=%s, new_local=%s, new_remote=%s", id,
                Flag.bitmaskToFlags(localFlags),
                Flag.bitmaskToFlags(trackedFlags),
                Flag.bitmaskToFlags(remoteFlags),
                Flag.bitmaskToFlags(newLocalFlags),
                Flag.bitmaskToFlags(newRemoteFlags));
        }
        if (newLocalFlags != localFlags) {
            mailbox.setTags(null, id, MailItem.TYPE_MESSAGE, newLocalFlags,
                            MailItem.TAG_UNCHANGED);
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
            DbImapMessage.setFlags(mailbox, id, newRemoteFlags);
            stats.flagsUpdatedLocally++;
        }
    }
    
    private void fetchMessages(long startUid, long endUid)
        throws ServiceException, IOException {
        String end = endUid > 0 ? String.valueOf(endUid) : "*";
        List<Long> uids = remoteFolder.getUids(startUid + ":" + end);
        int count = uids.size();
        if (count == 0) {
            debug("No new IMAP messages to fetch");
            return;
        }
        List<Long> deletedUids = new ArrayList<Long>();
        debug("Fetching %d new IMAP message(s)", count);
        for (int i = 0; i < count; i += FETCH_SIZE) {
            int j = Math.min(i + FETCH_SIZE - 1, count - 1);
            fetchMessages(uids.get(j) + ":" + uids.get(i), deletedUids);
        }
        if (!deletedUids.isEmpty()) {
            debug("Deleting %d message(s) from IMAP folder that have been " +
                  "filtered locally", deletedUids.size());
            remoteFolder.deleteMessages(deletedUids);
            if (!uidPlus) {
                expunge = true;
            }
        }
    }
    
    private void fetchMessages(String seq, final List<Long> deletedUids)
        throws ServiceException, IOException {
        final Map<Long, MessageData> flagsByUid =
            connection.uidFetch(seq, "(FLAGS INTERNALDATE)");
        connection.uidFetch(seq, "BODY.PEEK[]",
            new FetchResponseHandler() {
                public void handleFetchResponse(MessageData md) throws Exception {
                    handleFetch(md, flagsByUid, deletedUids);
                }
            });
    }

    private void handleFetch(MessageData md,
                             Map<Long, MessageData> flagsByUid,
                             List<Long> deletedUids)
        throws ServiceException, IOException {
        long uid = md.getUid();
        if (uid == -1) {
            throw new MailException("Missing UID in FETCH response");
        }
        ImapData body = getBody(md);
        MessageData flagsData = flagsByUid.get(uid);
        if (flagsData == null) {
            return; // Message must have been deleted remotely
        }
        debug("Found new IMAP message with uid %d", uid);
        ParsedMessage pm = parseMessage(body, flagsData);
        if (pm == null) {
            return; // Parse error
        }
        int zflags = SyncUtil.imapToZimbraFlags(flagsData.getFlags());
        Message msg = imapSync.addMessage(pm, folder.getId(), zflags);
        if (msg != null && msg.getFolderId() == folder.getId()) {
            storeImapMessage(uid, msg.getId(), zflags);
            stats.msgsAddedLocally++;
        } else {
            // Message was filtered and discarded or moved to another folder.
            // This can only happen for messages fetched from INBOX which is
            // always sync'd first. Mark remote message for deletion and do
            // not create a local tracker. If mesage was moved to another
            // folder we will append to the remote folder when when we sync
            // that folder.
            deletedUids.add(uid);
        }
    }

    private ImapData getBody(MessageData md) throws MailException {
        Body[] sections = md.getBodySections();
        if (sections == null || sections.length != 1) {
            throw new MailException(
              "Invalid body section FETCH response for uid " +  md.getUid());
        }
        return sections[0].getData();
    }
    
    private ParsedMessage parseMessage(ImapData body, MessageData md)
        throws ServiceException, IOException {
        Date date = md.getInternalDate();
        Long time = date != null ? (Long) date.getTime() : null;
        try {
            boolean indexAttachments = mailbox.attachmentsIndexingEnabled();
            if (body.isLiteral()) {
                File file = ((Literal) body).getFile();
                if (file != null) {
                    return new ParsedMessage(file, time, indexAttachments);
                }
            }
            return new ParsedMessage(body.getBytes(), time, indexAttachments);
        } catch (MessagingException e) {
            error("Skipping fetched message with uid " + md.getUid() +
                  " due to parse error", e);
            return null;
        }
    }

    private Set<Integer> getLocalMsgIds() throws ServiceException {
        Set<Integer> localIds = new HashSet<Integer>();
        int fid = folder.getId();
        for (int id : mailbox.listItemIds(null, MailItem.TYPE_MESSAGE, fid)) {
            localIds.add(id);
        }
        for (int id : mailbox.listItemIds(null, MailItem.TYPE_CHAT, fid)) {
            localIds.add(id);
        }
        return localIds;
    }

    private static int mergeFlags(int localFlags, int trackedFlags,
                                  int remoteFlags) {
        return trackedFlags & (localFlags & remoteFlags) |
              ~trackedFlags & (localFlags | remoteFlags);
    }

    private void debug(String fmt, Object... args) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("[" + getLocalPath() + "] " + String.format(fmt, args));
        }
    }

    private void info(String fmt, Object... args) {
        LOG.info("[" + getLocalPath() + "] " + String.format(fmt, args));
    }

    private void warn(String msg, Throwable e) {
        LOG.error("[" + getLocalPath() + "] " + msg, e);
    }

    private void error(String msg, Throwable e) {
        LOG.error("[" + getLocalPath() + "] " + msg, e);
    }
}

