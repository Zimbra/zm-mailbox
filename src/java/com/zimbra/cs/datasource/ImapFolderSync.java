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
import java.io.IOException;
import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Map;
import java.util.Comparator;
import java.sql.SQLException;

class ImapFolderSync {
    private final ImapSync imapSync;
    private final ImapConnection connection;
    private final DataSource ds;
    private final com.zimbra.cs.mailbox.Mailbox mailbox;
    private final Statistics stats = new Statistics();
    private Folder folder;
    private ImapFolder tracker;
    private ImapMessageCollection trackedMsgs;
    private Set<Integer> msgIds;
    private List<Integer> newMsgIds;

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
        Mailbox mb = selectImapFolder(remotePath);
        tracker = ds.createImapFolder(folder.getId(), folder.getPath(),
                                      remotePath, mb.getUidValidity());
        syncMessages(fullSync);
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
        selectImapFolder(tracker.getRemotePath());
        checkUidValidity();
        trackedMsgs = DbImapMessage.getImapMessages(mailbox, ds, tracker);
        msgIds = getLocalMsgIds();
        long lastUid = trackedMsgs.getMaxUid();
        boolean newMsgs = hasNewRemoteMessages(lastUid);
        // Sync flags if there are changes or fullsync requested
        if (lastUid > 0 && (fullSync || newMsgs || hasLocalChanges())) {
            syncFlags(lastUid);
        }
        // Fetch new messages
        if (newMsgs) {
            fetchMsgs(lastUid);
        }
        newMsgIds = new ArrayList<Integer>();
        // Remaining local ids are for new or deleted messages
        for (int id : msgIds) {
            ImapMessage msg = trackedMsgs.getByItemId(id);
            if (msg != null) {
                deleteMessage(id); // Message deleted remotely
            } else {
                newMsgIds.add(id);
                stats.addedRemotely++;
            }
        }
    }

    private boolean hasLocalChanges() {
        for (int id : msgIds) {
            if (!trackedMsgs.containsItemId(id)) {
                return true; // Has new local message
            }
        }
        for (ImapMessage msg : trackedMsgs) {
            if (!msgIds.contains(msg.getItemId())) {
                return true; // Has deleted local message
            }
        }
        return false;
    }

    private boolean hasNewRemoteMessages(long lastUid) {
        Mailbox mb = getMailbox();
        long uidNext = mb.getUidNext();
        return mb.getExists() > 0 && (uidNext <= 0 || lastUid + 1 < uidNext);
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

    Mailbox selectImapFolder(String path) throws IOException {
        Mailbox mb = connection.getMailbox();
        if (mb != null && mb.getName().equals(path)) {
            return mb;
        }
        LOG.info("Selecting IMAP folder '%s'", path);
        return connection.select(path);
    }
    
    private void createLocalFolder(ListData ld)
        throws ServiceException, IOException {
        String remotePath = ld.getMailbox();
        String localPath = imapSync.getLocalPath(remotePath, ld.getDelimiter());
        if (localPath == null) {
            // LOG.info("Remote IMAP folder '%s' is not being synchronized", remotePath);
            return;
        }
        long uidValidity = 0;
        if (!ld.getFlags().isNoselect()) {
            uidValidity = selectImapFolder(remotePath).getUidValidity();
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
        Iterator<Integer> it = newMsgIds.iterator();
        while (it.hasNext()) {
            appendMessage(it.next());
            it.remove();
        }
    }
    
    private void appendMessage(int id) throws ServiceException, IOException {
        debug("Appending new local message with item id %d", id);
        Message msg = mailbox.getMessageById(null, id);
        if (msg == null) {
            return; // Must have been deleted while syncing
        }
        String error = null;
        long uid = ImapUtil.appendUid(connection, tracker.getRemotePath(), msg);
        if (uid <= 0) {
            error = "Cannot determine UID for message";
        } else {
            try {
                DbImapMessage.storeImapMessage(
                    mailbox, folder.getId(), uid, id, msg.getFlagBitmask());
            } catch (ServiceException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof SQLException &&
                    Db.errorMatches((SQLException) cause, Db.Error.DUPLICATE_ROW)) {
                    error = "A tracker already exists for the message";
                } else {
                    throw e;
                }
            }
        }
        if (error != null) {
            warn("Unable to append message with id " + id + ": " + error, null);
        }
    }

    /*
     * Checks UIDVALIDITY for currently selected folder. If UIDVALIDITY has
     * changed since the last sync, then append new local messages to the
     * remote folder and empty the local folder so that messages will be
     * fetched again when we synchronize.
     */
    private void checkUidValidity() throws ServiceException, IOException {
        Mailbox mb = getMailbox();
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
                Message msg = mailbox.getMessageById(null, id);
                ImapUtil.append(connection, remotePath, msg);
            }
        }
        // Empty local folder so that it will be resynced later
        mailbox.emptyFolder(null, folder.getId(), false);
        tracker.setUidValidity(mb.getUidValidity());
        ds.updateImapFolder(tracker);
        DbImapMessage.deleteImapMessages(mailbox, tracker.getItemId());
    }

    private void syncFlags(long lastUid) throws ServiceException, IOException {
        debug("Syncing message flags");
        List<MessageData> mds =
            ImapUtil.fetch(connection, "1:" + lastUid, "FLAGS");
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
        if (!deletedUids.isEmpty()) {
            deleteMsgs(deletedUids);
        }
    }

    private void deleteMsgs(List<Long> uids)
        throws IOException, ServiceException {
        int size = uids.size();
        debug("Deleting and expunging %d IMAP message(s)", size);
        boolean uidPlus = connection.hasCapability(ImapCapabilities.UIDPLUS);
        for (int i = 0; i < size; i += 16) {
            deleteMsgs(uids.subList(i, Math.min(size - i, 16)), uidPlus);
        }
        // If UIDPLUS not supported, then all we can do is expunge all messages
        // which unfortunately may end up removing some messages that we
        // ourselves did not flag for deletion.
        if (!uidPlus) {
            connection.expunge();
        }
    }

    private void deleteMsgs(List<Long> uids, boolean uidPlus)
        throws IOException, ServiceException {
        String seq = ImapData.asSequenceSet(uids);
        debug("Deleting IMAP messages for sequence set: " + seq);
        // Mark remote messages for deletion
        connection.uidStore(seq, "+FLAGS.SILENT", "(\\Deleted)");
        // If UIDPLUS supported, then expunge deleted messages
        if (uidPlus) {
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
        int newFlags = mergeFlags(localFlags, trackedFlags, remoteFlags);
        boolean updated = false;
        if (newFlags != localFlags) {
            mailbox.setTags(null, id, MailItem.TYPE_MESSAGE, newFlags,
                            MailItem.TAG_UNCHANGED);
            updated = true;
        }
        if (newFlags != remoteFlags) {
            String uids = String.valueOf(msg.getUid());
            Flags toAdd = FlagsUtil.getFlagsToAdd(flags, newFlags);
            Flags toRemove = FlagsUtil.getFlagsToRemove(flags, newFlags);
            // UID STORE never fails even if message was deleted
            if (!toAdd.isEmpty()) {
                connection.uidStore(uids, "+FLAGS.SILENT", toAdd);
            }
            if (!toRemove.isEmpty()) {
                connection.uidStore(uids, "-FLAGS.SILENT", toRemove);
            }
            updated = true;
        }
        if (newFlags != trackedFlags) {
            DbImapMessage.setFlags(mailbox, id, newFlags);
            updated = true;
        }
        if (updated) {
            stats.updated++;
            if (LOG.isDebugEnabled()) {
                debug("Updated flags for message with uid %d: local=%s, " +
                    "tracked=%s, remote=%s, new=%s", msg.getUid(),
                    Flag.bitmaskToFlags(localFlags),
                    Flag.bitmaskToFlags(trackedFlags),
                    Flag.bitmaskToFlags(remoteFlags),
                    Flag.bitmaskToFlags(newFlags));
            }
        } else {
            stats.matched++;
        }
    }

    private void fetchMsgs(long lastUid) throws ServiceException, IOException {
        List<Long> uids = connection.getUids((lastUid + 1) + ":*");
        // Sort UIDs in reverse order so we download latest messages first
        Collections.sort(uids, Collections.reverseOrder());
        debug("Fetching %d new IMAP message(s)", uids.size());
        for (int i = 0; i < uids.size(); i += FETCH_SIZE) {
            int j = Math.min(i + FETCH_SIZE - 1, uids.size() - 1);
            fetchMsgs(uids.get(i) + ":" + uids.get(j));
        }
    }

    private void fetchMsgs(String seq) throws ServiceException, IOException {
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
            // Message must have been deleted
            return;
        }
        if (trackedMsgs.getByUid(uid) != null) {
            debug("Skipped fetched message with uid %d because it already exists locally", uid);
            return;
        }
        debug("Found new IMAP message with uid %d", uid);
        ParsedMessage pm = parseMessage(body, flagsData);
        if (pm != null) {
            int flags = FlagsUtil.imapToZimbraFlags(flagsData.getFlags());
            int msgId = imapSync.addMessage(pm, folder.getId(), flags);
            DbImapMessage.storeImapMessage(
                mailbox, tracker.getItemId(), uid, msgId, flags);
            stats.addedLocally++;
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

    private Mailbox getMailbox() {
        return connection.getMailbox();
    }
}

