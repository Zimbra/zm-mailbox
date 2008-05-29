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
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.db.DbImapMessage;
import com.zimbra.cs.db.Db;
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
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.sql.SQLException;

class ImapFolderSync {
    private final ImapSync imapSync;
    private final ImapConnection connection;
    private final boolean uidPlus;
    private final DataSource ds;
    private final com.zimbra.cs.mailbox.Mailbox mailbox;
    private final Statistics stats = new Statistics();
    private Folder folder;
    private ImapFolder tracker;
    private ImapMessageCollection trackedMsgs;
    private Set<Integer> existingMsgIds;
    private List<Integer> newMsgIds;
    private MailDateFormat mdf; 

    private static final Log LOG = ZimbraLog.datasource;

    private static final int FETCH_SIZE = LC.data_source_fetch_size.intValue();

    private static class Statistics {
        int matched;
        int updated;
        int addedLocally;
        int addedRemotely;
        int deletedRemotely;
        int deletedLocally;
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
        boolean syncMsgs = true;
        if (tracker != null) {
            syncMsgs = checkTrackedFolder(ld);
        } else {
            createLocalFolder(ld);
        }
        if (syncMsgs && tracker != null) {
            if (tracker.getUidValidity() > 0) {
                syncMessages(fullSync);
            } else {
                info("Synchronization disabled for this folder (IMAP " +
                     "folder is flagged '\\Noselect')");
            }
        }
        return tracker;
    }

    // Synchronize local folder with remote IMAP folder
    public void syncFolder(Folder folder, boolean fullSync)
        throws ServiceException, IOException {
        this.folder = folder;
        if (tracker != null) {
            // Remote IMAP folder deleted, empty local folder
            String remotePath = tracker.getRemotePath();
            info("Remote IMAP folder '%s' was deleted, deleting local folder",
                 remotePath);
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
        createImapFolder(remotePath);
        Mailbox mb = connection.select(remotePath);
        tracker = ds.createImapFolder(folder.getId(), folder.getPath(),
                                      remotePath, mb.getUidValidity());
        syncMessages(fullSync);
    }

    public Folder getFolder() {
        return folder;
    }

    private void createImapFolder(String name) throws IOException {
        debug("Creating IMAP folder '%s'", name);
        try {
            connection.create(name);
        } catch (CommandFailedException e) {
            // Create failed, so check if folder already exists
            info("Attemping to create IMAP folder '%s' which already exists", name);
            if (connection.list("", name).isEmpty()) {
                throw e;
            }
        }
    }

    /*
     * Synchronizes messages between local and remote folder.
     * Get list of all local ids:
     *  - Includes ids for a) existing tracked messages, b) new local messages
     *  - If tracked but no local id then deleted
     */
    private void syncMessages(boolean fullSync)
        throws ServiceException, IOException {
        if (!ds.isSyncEnabled(tracker.getLocalPath())) {
            info("Synchronization disabled for this folder");
            return;
        }
        int folderId = tracker.getItemId();
        String remotePath = tracker.getRemotePath();
        Mailbox mb = connection.select(remotePath);
        checkUidValidity(mb);
        long lastSyncUid = ds.getLastSyncUid(folderId);
        if (lastSyncUid <= 0) {
            trackedMsgs = DbImapMessage.getImapMessages(mailbox, ds, tracker);
            lastSyncUid = trackedMsgs.getMaxUid();
        }
        long uidNext = mb.getUidNext();
        if (uidNext < lastSyncUid) {
            // TODO Should we treat this same as change in UIDVALIDITY?
            uidNext = 0; // In case server sent a bogus UIDNEXT
        }
        List<Long> uids = fetchUids(lastSyncUid + 1, uidNext - 1);
        if (!fullSync && uids.isEmpty()) {
            return; // No new IMAP messages - do not sync flags
        }
        existingMsgIds = getLocalMsgIds();
        if (trackedMsgs == null) {
            trackedMsgs = DbImapMessage.getImapMessages(mailbox, ds, tracker);
        }
        fetchMessages(uids);
        syncFlags(1, lastSyncUid);
        // Remaining message ids with trackers were deleted remotely
        for (int id : existingMsgIds) {
            if (trackedMsgs.getByItemId(id) != null) {
                deleteMessage(id);
            }
        }
        // Message ids without trackers are for new local messages
        newMsgIds = new ArrayList<Integer>();
        for (int id : existingMsgIds) {
            if (trackedMsgs.getByItemId(id) == null) {
                newMsgIds.add(id);
                stats.addedRemotely++;
            }
        }
    }

    private boolean checkTrackedFolder(ListData ld)
        throws ServiceException, IOException {
        // Check if folder has been deleted locally
        try {
            folder = mailbox.getFolderById(null, tracker.getItemId());
        } catch (MailServiceException.NoSuchItemException e) {
            LOG.info("Local folder '%s' was deleted, so deleting IMAP folder '%s'",
                     tracker.getLocalPath(), tracker.getRemotePath());
            connection.delete(tracker.getRemotePath());
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
        String remotePath = tracker.getRemotePath();
        String newRemotePath = imapSync.getRemotePath(folder);
        if (newRemotePath != null) {
            // Folder renamed but still under data source root
            info("Folder was renamed (originally '%s')", tracker.getLocalPath());
            info("Renaming IMAP folder from '%s' to '%s'", remotePath, newRemotePath);
            connection.rename(remotePath, newRemotePath);
            tracker.setLocalPath(localPath);
            tracker.setRemotePath(newRemotePath);
            ds.updateImapFolder(tracker);
            return true;
        }
        // Folder was moved outside of the data source root, or
        // folder should no longer be synchronized
        info("Folder was renamed (originally '%s') and moved outside the data source root",
             tracker.getLocalPath());
        ds.deleteImapFolder(tracker);          
        // Create new local folder for remote path
        createLocalFolder(ld);
        return true;
    }

    private void createLocalFolder(ListData ld)
        throws ServiceException, IOException {
        String remotePath = ld.getMailbox();
        String localPath = imapSync.getLocalPath(ld);
        if (localPath == null) {
            // LOG.info("Remote IMAP folder '%s' is not being synchronized", remotePath);
            return;
        }
        long uidValidity = 0;
        if (!ld.getFlags().isNoselect()) {
            uidValidity = connection.select(remotePath).getUidValidity();
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
    }
        
    private void deleteMessage(int id) throws ServiceException {
        debug("Deleting local message with item id %d", id);
        try {
            mailbox.delete(null, id, MailItem.TYPE_UNKNOWN);
        } catch (MailServiceException.NoSuchItemException e) {
            debug("Local message with id %d not found", id);
        }
        DbImapMessage.deleteImapMessage(mailbox, tracker.getItemId(), id);
        stats.deletedLocally++;
    }

    public void appendNewMessages() throws ServiceException, IOException {
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
        int folderId = tracker.getItemId();
        String remotePath = tracker.getRemotePath();
        for (int id : ids) {
            Message msg = getMessageById(id);
            if (msg == null) {
                continue; // Message must have been deleted
            }
            debug("Appending new message with item id %d", id);
            long uid = appendMessage(remotePath, msg);
            if (uid <= 0) {
                throw ServiceException.FAILURE(
                    "UIDPLUS supported but no UID returned for appended message", null);
            }
            storeImapMessage(uid, id, msg.getFlagBitmask());
            ds.updateLastSyncUid(folderId, uid);
        }
    }
    
    private long appendMessage(String mailbox, Message msg)
        throws ServiceException, IOException {
        MimeMessage mm = msg.getMimeMessage(false);
        Flags flags = FlagsUtil.zimbraToImapFlags(msg.getFlagBitmask());
        Date date = getInternalDate(msg, mm);
        return ImapUtil.append(connection, mailbox, mm, flags, date);
    }

    private void appendMessagesNoUidPlus(List<Integer> ids)
        throws ServiceException, MessagingException, IOException {
        final int folderId = tracker.getItemId();
        String remotePath = tracker.getRemotePath();
        // If server does not support UIDPLUS, append messages, then fetch
        // ENVELOPE information for messages that were just appended in order
        // to obtain the message UIDs.
        long startUid = connection.select(remotePath).getUidNext();
        final HashMap<String, AppendInfo> appended = new HashMap<String, AppendInfo>();
        for (int id : ids) {
            Message msg = getMessageById(id);
            if (msg == null) {
                continue; // Message must have just been deleted
            }
            debug("Appending new message with item id %d", id);
            MimeMessage mm = msg.getMimeMessage(false);
            AppendInfo ai = new AppendInfo(msg, mm);
            Flags flags = FlagsUtil.zimbraToImapFlags(ai.flags);
            Date date = getInternalDate(msg, mm);
            ImapUtil.append(connection, remotePath, mm, flags, date);
            appended.put(appendKey(mm.getSentDate(), ai.messageId), ai);
        }
        // Find UIDs for messages just appended
        mdf = new MailDateFormat();
        long endUid = connection.select(remotePath).getUidNext() - 1;
        if (startUid <= endUid) {
            String seq = startUid + ":" + endUid;
            connection.uidFetch(seq, "ENVELOPE", new FetchResponseHandler() {
                public void handleFetchResponse(MessageData md) throws Exception {
                    Envelope env = md.getEnvelope();
                    Date date = mdf.parse(env.getDate());
                    String key = appendKey(date, env.getMessageId());
                    AppendInfo ai = appended.remove(key);
                    if (ai != null) {
                        long uid = md.getUid();
                        storeImapMessage(uid, ai.itemId, ai.flags);
                        ds.updateLastSyncUid(folderId, uid);
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
                    storeImapMessage(uid, ai.itemId, ai.flags);
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
        final int flags;

        AppendInfo(Message msg, MimeMessage mm) throws MessagingException {
            itemId = msg.getId();
            subject = mm.getSubject();
            messageId = mm.getMessageID();
            sentDate = mm.getSentDate();
            flags = msg.getFlagBitmask();
        }
    }

    private void storeImapMessage(long uid, int itemId, int flags) {
        try {
            DbImapMessage.storeImapMessage(
                mailbox, folder.getId(), uid, itemId, flags);
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof SQLException &&
                Db.errorMatches((SQLException) cause, Db.Error.DUPLICATE_ROW)) {
                warn("Tracker already exists for message with id " + itemId, null);
            }
        }
    }

    private static Date getInternalDate(Message msg, MimeMessage mm) {
        Date date = null;
        try {
            date = mm.getReceivedDate();
        } catch (MessagingException e) {
            // Fall through
        }
        return date != null ? date : new Date(msg.getDate());
    }

    /*
     * Checks UIDVALIDITY for currently selected folder. If UIDVALIDITY has
     * changed since the last sync, then append new local messages to the
     * remote folder and empty the local folder so that messages will be
     * fetched again when we synchronize.
     */
    private void checkUidValidity(Mailbox mb) throws ServiceException, IOException {
        if (mb.getUidValidity() == tracker.getUidValidity()) {
            return;
        }
        String remotePath = tracker.getRemotePath();
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
                    appendMessage(remotePath, msg);
                }
            }
        }
        // Empty local folder so that it will be resynced later
        mailbox.emptyFolder(null, folder.getId(), false);
        tracker.setUidValidity(mb.getUidValidity());
        ds.updateImapFolder(tracker);
        DbImapMessage.deleteImapMessages(mailbox, tracker.getItemId());
    }

    private Message getMessageById(int itemId) throws ServiceException {
        try {
            return mailbox.getMessageById(null, itemId);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }
    
    private void syncFlags(long startUid, long endUid)
        throws ServiceException, IOException {
        if (startUid > endUid) return;
        debug("Syncing message flags for UID sequence %d:%d", startUid, endUid);
        List<MessageData> mds =
            ImapUtil.fetch(connection, startUid + ":" + endUid, "FLAGS");
        List<Long> deletedUids = new ArrayList<Long>();
        for (MessageData md : mds) {
            Long uid = md.getUid();
            ImapMessage trackedMsg = trackedMsgs.getByUid(uid);
            if (trackedMsg == null) {
                info("Ignoring flags for message with uid %d because it is " +
                     "not being tracked", uid);
                continue;
            }
            int msgId = trackedMsg.getItemId();
            if (existingMsgIds.contains(msgId)) {
                existingMsgIds.remove(msgId);
                try {
                    updateFlags(trackedMsg, md.getFlags());
                    continue;
                } catch (MailServiceException.NoSuchItemException e) {
                    // Message was deleted locally
                }
            }
            deletedUids.add(uid);
        }
        if (!deletedUids.isEmpty()) {
            deleteMessages(deletedUids);
        }
    }

    private void deleteMessages(List<Long> uids)
        throws IOException, ServiceException {
        int size = uids.size();
        info("Deleting and expunging %d IMAP message(s)", size);
        for (int i = 0; i < size; i += 16) {
            deleteMessages(uids.subList(i, Math.min(size - i, 16)), uidPlus);
        }
        // If UIDPLUS not supported, then all we can do is expunge all messages
        // which unfortunately may end up removing some messages we ourselves
        // did not flag for deletion.
        if (!uidPlus) {
            connection.expunge();
        }
    }

    private void deleteMessages(List<Long> uids, boolean expunge)
        throws IOException, ServiceException {
        String seq = ImapData.asSequenceSet(uids);
        debug("Deleting IMAP messages for sequence set: " + seq);
        // Mark remote messages for deletion
        connection.uidStore(seq, "+FLAGS.SILENT", "(\\Deleted)");
        // If UIDPLUS supported, then expunge deleted messages
        if (expunge) {
            connection.uidExpunge(seq);
        }
        // Finally, remove local messages
        for (long uid : uids) {
            ImapMessage msg = trackedMsgs.getByUid(uid);
            DbImapMessage.deleteImapMessage(
                mailbox, tracker.getItemId(), msg.getItemId());
            stats.deletedRemotely++;
        }
    }

    // Updates flags for specified message.
    private void updateFlags(ImapMessage msg, Flags flags)
        throws ServiceException, IOException {
        int id = msg.getItemId();
        int localFlags = msg.getFlags();
        int trackedFlags = msg.getTrackedFlags();
        int remoteFlags = FlagsUtil.imapToZimbraFlags(flags);
        int newLocalFlags = mergeFlags(localFlags, trackedFlags, remoteFlags);
        int newRemoteFlags = FlagsUtil.imapFlagsOnly(newLocalFlags);
        boolean updated = false;
        if (newLocalFlags != localFlags) {
            mailbox.setTags(null, id, MailItem.TYPE_MESSAGE, newLocalFlags,
                            MailItem.TAG_UNCHANGED);
            updated = true;
        }
        if (newRemoteFlags != remoteFlags) {
            String uids = String.valueOf(msg.getUid());
            Flags toAdd = FlagsUtil.getFlagsToAdd(flags, newRemoteFlags);
            Flags toRemove = FlagsUtil.getFlagsToRemove(flags, newRemoteFlags);
            // UID STORE never fails even if message was deleted
            if (!toAdd.isEmpty()) {
                connection.uidStore(uids, "+FLAGS.SILENT", toAdd);
            }
            if (!toRemove.isEmpty()) {
                connection.uidStore(uids, "-FLAGS.SILENT", toRemove);
            }
            updated = true;
        }
        if (newRemoteFlags != trackedFlags) {
            DbImapMessage.setFlags(mailbox, id, newRemoteFlags);
            updated = true;
        }
        if (updated) {
            stats.updated++;
            if (LOG.isDebugEnabled()) {
                debug("Updated flags for message with item id %d: local=%s, " +
                      "tracked=%s, remote=%s, new_local=%s, new_remote=%s", id,
                      Flag.bitmaskToFlags(localFlags),
                      Flag.bitmaskToFlags(trackedFlags),
                      Flag.bitmaskToFlags(remoteFlags),
                      Flag.bitmaskToFlags(newLocalFlags),
                      Flag.bitmaskToFlags(newRemoteFlags));
            }
        } else {
            stats.matched++;
        }
    }

    private List<Long> fetchUids(long startUid, long endUid) throws IOException {
        if (endUid > 0 && startUid > endUid) {
            return Collections.emptyList();
        }
        String end = endUid > 0 ? String.valueOf(endUid) : "*";
        return connection.getUids(startUid + ":" + end);
    }

    private void fetchMessages(List<Long> uids)
        throws ServiceException, IOException {
        if (uids.isEmpty()) return;
        // Sort UIDs in reverse order so we download latest messages first
        Collections.sort(uids, Collections.reverseOrder());
        int count = uids.size();
        debug("Fetching %d new IMAP message(s)", count);
        for (int i = 0; i < count; i += FETCH_SIZE) {
            int j = Math.min(i + FETCH_SIZE - 1, count - 1);
            fetchMessages(uids.get(j) + ":" + uids.get(i));
        }
    }

    private void fetchMessages(String seq) throws ServiceException, IOException {
        final Map<Long, MessageData> mds =
            connection.uidFetch(seq, "(FLAGS INTERNALDATE)");
        connection.uidFetch(seq, "BODY.PEEK[]",
            new FetchResponseHandler() {
                public void handleFetchResponse(MessageData md) throws Exception {
                    handleFetch(md, mds);
                }
            });
    }
    
    private void handleFetch(MessageData md, Map<Long, MessageData> mds)
        throws ServiceException, IOException {
        long uid = md.getUid();
        if (uid == -1) {
            throw new MailException("Missing UID in FETCH response");
        }
        ImapData body = getBody(md);
        MessageData flagsData = mds.get(uid);
        if (flagsData == null) {
            return; // Message must have been deleted
        }
        if (trackedMsgs.getByUid(uid) != null) {
            debug("Skipped fetched message with uid %d because it already exists locally", uid);
            return;
        }
        debug("Found new IMAP message with uid %d", uid);
        ParsedMessage pm = parseMessage(body, flagsData);
        if (pm == null) {
            return; // Parse error
        }

        int flags = FlagsUtil.imapToZimbraFlags(flagsData.getFlags());
        int msgId = imapSync.addMessage(pm, folder.getId(), flags);
        DbImapMessage.storeImapMessage(
            mailbox, tracker.getItemId(), uid, msgId, flags);
        stats.addedLocally++;
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
            LOG.debug("[" + folder.getPath() + "] " + String.format(fmt, args));
        }
    }

    private void info(String fmt, Object... args) {
        LOG.info("[" + folder.getPath() + "] " + String.format(fmt, args));
    }

    private void warn(String msg, Throwable e) {
        LOG.error("[" + folder.getPath() + "] " + msg, e);
    }

    private void error(String msg, Throwable e) {
        LOG.error("[" + folder.getPath() + "] " + msg, e);
    }
}

