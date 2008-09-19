/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.ReadOnlyFolderException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.posisoft.jdavmail.JDAVMailFolder;
import com.posisoft.jdavmail.JDAVMailMessage;
import com.posisoft.jdavmail.JDAVMailStore;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbImapMessage;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;

public class LiveImport extends MailItemImport {
    private JDAVMailStore store;

    private static final Session SESSION;

    static {
        boolean debug = Boolean.getBoolean("ZimbraJavamailDebug") ||
            LC.javamail_imap_debug.booleanValue();
        Long timeout = LC.javamail_imap_timeout.longValue() * Constants.MILLIS_PER_SECOND;
        Properties props = new Properties();
        
        props.setProperty("mail.davmail.connectiontimeout", timeout.toString());
        props.setProperty("mail.davail.timeout", timeout.toString());
        if (debug)
            props.setProperty("mail.debug", "true");
        SESSION = Session.getInstance(props);
        if (debug)
            SESSION.setDebug(true);
    }

    public LiveImport(DataSource ds) throws ServiceException {
        super(ds);
        store = new JDAVMailStore(SESSION, null);

    }

    public synchronized String test() throws ServiceException {
        validateDataSource();
        try {
            connect();
        } catch (ServiceException e) {
            Throwable except = SystemUtil.getInnermostException(e);
            if (except == null) except = e;
            ZimbraLog.datasource.info("Error connecting to mail store: ", except);
            return except.toString();
        } finally {
            close();
        }
        return null;
    }
    
    public synchronized void importData(List<Integer> folderIds, boolean fullSync)
        throws ServiceException {
        validateDataSource();
        connect();
        DataSource ds = getDataSource();
        try {
            JDAVMailFolder remoteRootFolder = (JDAVMailFolder) store.getDefaultFolder();
            Mailbox mbox = ds.getMailbox();
            ImapFolderCollection imapFolders = ds.getImapFolders();
            com.zimbra.cs.mailbox.Folder localRootFolder = mbox.getFolderById(
                null, ds.getFolderId());
            Folder[] remoteFolders = remoteRootFolder.list("*");

for (Folder folder : remoteFolders) {
    System.out.println("xxxxxxxxxxxxxx: "+folder.getFullName());
}
            // Handle new remote folders and moved/renamed/deleted local folders
            for (Folder folder : remoteFolders) {
                JDAVMailFolder remoteFolder = (JDAVMailFolder)folder;
                ImapFolder folderTracker = imapFolders.getByRemotePath(remoteFolder.getFullName());
                com.zimbra.cs.mailbox.Folder localFolder = null;
                long remoteUvv = Math.abs(remoteFolder.getUID().hashCode());

                // Handle folder we already know about
                ZimbraLog.datasource.debug("Processing Live folder " + remoteFolder.getFullName());
                if (folderTracker != null) {
                    try {
                        localFolder = mbox.getFolderById(null, folderTracker.getItemId());
                    } catch (NoSuchItemException e) {
                        ZimbraLog.datasource.info("Local folder %s was deleted.  Deleting remote folder %s.",
                            folderTracker.getLocalPath(), folderTracker.getRemotePath());
                        // Folder was deleted locally, so we'll need to delete
                        // the remote one as well.  Check whether the folder is open
                        // and exists, in case deleting this folder's parent implicitly
                        // this folder to be deleted as well.
                        if (remoteFolder.isOpen()) {
                            remoteFolder.close(true);
                        }
                        if (remoteFolder.exists()) {
                            try {
                                // some special folders cannot be deleted
                                remoteFolder.delete(true);
                            } catch (MessagingException ee) {
                            }
                        }
                        imapFolders.remove(folderTracker);
                        ds.deleteImapFolder(folderTracker);
                    }
                    if (localFolder != null) {
                        if (!localFolder.getPath().equals(folderTracker.getLocalPath())) {
                                // Folder path does not match
                        	String jmPath = localPathToRemotePath(ds, localRootFolder, localFolder, remoteFolder.getSeparator());
                            if (jmPath != null && isParent(localRootFolder, localFolder)) {
                                // Folder has a new name/path but is still under the
                                // data source root
                                ZimbraLog.datasource.info("Local folder %s was renamed to %s",
                                    folderTracker.getLocalPath(), localFolder.getPath());
                                renameJavaMailFolder(remoteFolder, jmPath);
                                // XXX What about new remote path?s
                                folderTracker.setLocalPath(localFolder.getPath());
                                ds.updateImapFolder(folderTracker);
                            } else {
                                // Folder was moved outside the data source root, or folder setting is changed to "not to sync"
                                // Treat as a delete.
                                ZimbraLog.datasource.info("Local folder %s was renamed to %s and moved outside the data source root.",
                                    folderTracker.getLocalPath(), localFolder.getPath());
                                ds.deleteImapFolder(folderTracker);
                                imapFolders.remove(folderTracker);
                                folderTracker = null;
                                localFolder = null;
                            }
                        } else if (folderTracker.getUidValidity() != 0 && folderTracker.getUidValidity() != remoteUvv) {
                            // UIDVALIDITY value changed.  Save any new local messages to remote folder.
                            ZimbraLog.datasource.info("UIDVALIDITY of remote folder %s has changed from %d to %d.  Resyncing from scratch.",
                                remoteFolder.getFullName(), folderTracker.getUidValidity(), remoteUvv);
                            List<Integer> newLocalIds = DbImapMessage.getNewLocalMessageIds(mbox, folderTracker.getItemId());
                            if (newLocalIds.size() > 0) {
                                ZimbraLog.datasource.info("Copying %d messages from local folder %s to remote folder.",
                                    newLocalIds.size(), localFolder.getPath());
                            }
                            for (int id : newLocalIds) {
                                com.zimbra.cs.mailbox.Message localMsg = mbox.getMessageById(null, id);
                                MimeMessage mimeMsg = localMsg.getMimeMessage(false);
                                int flags = localMsg.getFlagBitmask();
                                setFlags(mimeMsg, flags);
                                remoteFolder.appendMessages(new MimeMessage[] { mimeMsg });
                            }

                            // Empty local folder so that it will be resynced later and store the new UIDVALIDITY value.
                            mbox.emptyFolder(null, localFolder.getId(), false);
                            folderTracker.setUidValidity(remoteUvv);
                            ds.updateImapFolder(folderTracker);
                            DbImapMessage.deleteImapMessages(mbox, folderTracker.getItemId());
                        }
                    }
                }

                // Handle new folder
                if (folderTracker == null) {
                    String path = remoteFolder.getFullName();
                    String knownPath = ds.matchKnownLocalPath(path);
                    
                    if (knownPath != null) {
                        if (knownPath.equals(""))
                            continue;
                        path = knownPath;
                    }
                    while (path.startsWith("/"))
                        path = path.substring(1);
                    if (ds.getFolderId() == Mailbox.ID_FOLDER_USER_ROOT)
                        path = "/" + path;
                    else
                        path = mbox.getFolderById(null,
                            ds.getFolderId()).getPath() + "/" + path;
            	    ZimbraLog.datasource.info("Found new remote folder %s. Creating local folder %s.",
            	        remoteFolder.getFullName(), path);
                    // Try to get the folder first, in case it was manually created or the
                    // last sync failed between creating the folder and writing the mapping row.
                    try {
                        localFolder = mbox.getFolderByPath(null, path);
                    } catch (NoSuchItemException e) {
                    }
                    if (localFolder == null)
                        localFolder = mbox.createFolder(null, path, (byte) 0,
                            MailItem.TYPE_UNKNOWN);
                    folderTracker = ds.createImapFolder(localFolder.getId(),
                        localFolder.getPath(), remoteFolder.getFullName(), remoteUvv);
                    imapFolders.add(folderTracker);
                }
            }

            // Handle new local folders and deleted remote folders
            for (com.zimbra.cs.mailbox.Folder zimbraFolder : localRootFolder.getSubfolderHierarchy()) {
                if (zimbraFolder.getId() == localRootFolder.getId()) {
                    // Root folder is always empty
                    continue;
                }
                
                // Re-get the folder, in case it was implicitly deleted when its
                // parent was deleted
                try {
                    zimbraFolder = mbox.getFolderById(null, zimbraFolder.getId());
                } catch (NoSuchItemException e) {
                    ZimbraLog.datasource.info(
                        "Skipping folder %s, probably deleted by parent deletion", zimbraFolder.getName());
                    ImapFolder imapFolder = imapFolders.getByItemId(zimbraFolder.getId());
                    if (imapFolder != null) {
	                    imapFolders.remove(imapFolder);
	                    ds.deleteImapFolder(imapFolder);
                    }
                    continue;
                }

                ImapFolder imapFolder = imapFolders.getByLocalPath(zimbraFolder.getPath());
                if (imapFolder != null) {
                	if (imapFolder.getUidValidity() != 0) { //otherwise it's a \Noselect
	                    // Already know about this folder. See if it still exists on
	                    // the remote server.
	                    Folder jmFolder = store.getFolder(imapFolder.getRemotePath());
	                    if (!jmFolder.exists()) {
	                        // Folder was deleted on the remote server, so we'll
	                        // need to delete it locally.
	                        ZimbraLog.datasource.info("Remote folder %s was deleted.  Deleting local folder %s.",
	                            imapFolder.getRemotePath(), zimbraFolder.getPath());
	                        try {
	                            mbox.delete(null, zimbraFolder.getId(),
	                                zimbraFolder.getType());
	                        } catch (MailServiceException e) {
	                            if (e.getCode() != MailServiceException.IMMUTABLE_OBJECT)
	                                throw e;
	                        }
	                        ds.deleteImapFolder(imapFolder);
	                        imapFolders.remove(imapFolder);
	                    }
                	}
                } else {
                    String jmPath = localPathToRemotePath(ds, localRootFolder, zimbraFolder, remoteRootFolder.getSeparator());
                    if (jmPath != null) { //null means don't sync up
                    	ZimbraLog.datasource.info("Found new local folder %s.  Creating remote folder %s.", zimbraFolder.getPath(), jmPath);
                    	try {
                    	    JDAVMailFolder jmFolder = createJavaMailFolder(jmPath);
                            imapFolder = ds.createImapFolder(zimbraFolder.getId(),
                                zimbraFolder.getPath(), jmFolder.getFullName(), 0);
                            imapFolders.add(imapFolder);
                    	} catch (MessagingException e) {
                            ZimbraLog.datasource.warn("An error occurred while creating remote folder %s", jmPath, e);
                    	}
                    }
                }
            }

            // Import data for all ImapFolders that exist on both sides
            for (ImapFolder imapFolder : imapFolders) {
            	if (!ds.isSyncEnabled(imapFolder.getLocalPath()))
            		continue;
                
                ZimbraLog.datasource.info("Importing from Live folder %s to local folder %s",
                    imapFolder.getRemotePath(), imapFolder.getLocalPath());
                if (imapFolder.getUidValidity() != 0) {
                    try {
                        importFolder(imapFolder);
                    } catch (MessagingException e) {
                        ZimbraLog.datasource.warn("An error occurred while importing folder %s", imapFolder.getRemotePath(), e);
                    } catch (ServiceException e) {
                        ZimbraLog.datasource.warn("An error occurred while importing folder %s", imapFolder.getRemotePath(), e);
                    }
                }
            }
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            close();
        }
    }

    private void close() {
        if (store != null) {
            try {
                store.close();
            } catch (MessagingException e) {
                ZimbraLog.datasource.warn("Error closing message store", e);
            }
        }
    }
    
    private void renameJavaMailFolder(Folder remoteFolder, String jmPath)
        throws MessagingException {
        ZimbraLog.datasource.info("Renaming folder from %s to %s", remoteFolder.getFullName(), jmPath);
        Folder newName = remoteFolder.getStore().getFolder(jmPath);
        remoteFolder.renameTo(newName);
    }

    private JDAVMailFolder createJavaMailFolder(String jmPath)
        throws MessagingException {
        JDAVMailFolder jmFolder = (JDAVMailFolder) store.getFolder(jmPath);
        try {
            jmFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        } catch (MessagingException e) {
            jmFolder.create(Folder.HOLDS_MESSAGES);
        }
        return jmFolder;
    }

    private String localPathToRemotePath(DataSource ds, com.zimbra.cs.mailbox.Folder localRootFolder,
        com.zimbra.cs.mailbox.Folder localFolder, char separator) {
        // Strip local root from the folder's path.  Remote paths don't start with "/".
        String rootPath = localRootFolder.getPath();
        if (!rootPath.endsWith("/"))
        	rootPath += "/";
        String folderPath = localFolder.getPath();
        if (folderPath.startsWith(rootPath)) {
            folderPath = folderPath.substring(rootPath.length());
        } else {
            ZimbraLog.datasource.warn("Folder path %s is not under root %s", folderPath, rootPath);
        }

        // Generate remote path
        String imapPath = ds.matchKnownRemotePath(localFolder.getPath());
        if ("".equals(imapPath)) //means to ignore
        	imapPath = null;
        else if (imapPath == null && localFolder.getId() >= Mailbox.FIRST_USER_ID)
        	imapPath = folderPath;
        if (imapPath != null && separator != '/') {
            String[] parts = localFolder.getPath().split("/");
            for (int i = 0; i < parts.length; ++i)
            	parts[i] = parts[i].replace(separator, '/');
            imapPath = StringUtil.join("" + separator, parts);
        }
        return imapPath;
    }

    private boolean isParent(com.zimbra.cs.mailbox.Folder parent, com.zimbra.cs.mailbox.Folder child)
        throws ServiceException {
        com.zimbra.cs.mailbox.Folder folder = child;
        while (true) {
            int parentId = folder.getParentId();
            if (parentId == parent.getId()) {
                return true;
            }
            if (parentId == Mailbox.ID_FOLDER_ROOT) {
                return false;
            }
            folder = child.getMailbox().getFolderById(null, parentId);
        }
    }

    private void importFolder(final ImapFolder trackedFolder)
        throws MessagingException, IOException, ServiceException {
        JDAVMailFolder remoteFolder = (JDAVMailFolder)store.getFolder(
            trackedFolder.getRemotePath());
        try {
            remoteFolder.open(Folder.READ_WRITE);
        } catch (ReadOnlyFolderException e) {
            ZimbraLog.datasource.info("Unable to open folder %s for write.  Skipping this folder.",
                remoteFolder.getFullName());
            return;
        }

        DataSource ds = getDataSource();
        int folderId = trackedFolder.getItemId();
        Set<Integer> localIds = new HashSet<Integer>();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ds.getAccount());
        Message[] msgArray = remoteFolder.getMessages();
        int numMatched = 0;
        int numUpdated = 0;
        int numDeletedLocally = 0;
        int numAddedRemotely = 0;
        int numDeletedRemotely = 0;
        int numAddedLocally = 0;
        List<JDAVMailMessage> remoteMsgs = new LinkedList<JDAVMailMessage>();
        ImapMessageCollection trackedMsgs = DbImapMessage.getImapMessages(mbox, ds, trackedFolder);

        ZimbraLog.datasource.debug("Found %d messages in %s", msgArray.length, remoteFolder.getFullName());
        localIds.addAll(mbox.listItemIds(null, MailItem.TYPE_CHAT, folderId));
        localIds.addAll(mbox.listItemIds(null, MailItem.TYPE_MESSAGE, folderId));
        for (Message msg : msgArray) {
            JDAVMailMessage remoteMsg = (JDAVMailMessage)msg;
            long uid = Math.abs(remoteMsg.getMessageID().hashCode());
            ImapMessage trackedMsg = trackedMsgs.getByUid(uid);
            
            if (trackedMsg == null) {
                remoteMsgs.add(remoteMsg);
            } else if (localIds.contains(trackedMsg.getItemId())) {
                int localFlags = trackedMsg.getFlags();
                int newFlags = localFlags;
                int remoteFlags = getZimbraFlags(remoteMsg);
                int trackedFlags = trackedMsg.getTrackedFlags();
                boolean updated = false;
                final int[] REMOTE_FLAG_BITMASKS = {
                    Flag.BITMASK_DELETED, Flag.BITMASK_DRAFT,
                    Flag.BITMASK_UNREAD
                };

                for (int flag : REMOTE_FLAG_BITMASKS) {
                    if ((remoteFlags & flag) != (trackedFlags & flag))
                        newFlags = (remoteFlags & flag) == 0 ?
                            localFlags & ~flag :
                            localFlags | flag;
                }
                if (newFlags != localFlags) {
                    mbox.setTags(null, trackedMsg.getItemId(),
                        MailItem.TYPE_MESSAGE, newFlags, MailItem.TAG_UNCHANGED);
                    updated = true;
                }
                if ((newFlags & Flag.BITMASK_UNREAD) !=
                    (remoteFlags & Flag.BITMASK_UNREAD)) {
                    setFlags(remoteMsg, newFlags);
                    updated = true;
                }
                if (newFlags != trackedFlags) {
                    DbImapMessage.setFlags(mbox, trackedMsg.getItemId(), newFlags);
                    updated = true;
                }
                if (updated) {
                    numUpdated++;
                    ZimbraLog.datasource.debug("Found message with UID %d on both sides; syncing flags: local=%s, tracked=%s, remote=%s, new=%s",
                       uid, Flag.bitmaskToFlags(localFlags), Flag.bitmaskToFlags(trackedFlags),
                       Flag.bitmaskToFlags(remoteFlags), Flag.bitmaskToFlags(newFlags));
                } else {
                    numMatched++;
                }
                // Remove id from the set, so we know we've already processed it.
                localIds.remove(trackedMsg.getItemId());
            } else {
                ZimbraLog.datasource.debug("Message %d was deleted locally.  Deleting remote copy.", uid);
                remoteMsg.setFlag(Flags.Flag.DELETED, true);
                numDeletedRemotely++;
                DbImapMessage.deleteImapMessage(mbox, folderId, trackedMsg.getItemId());
            }
        }
        // Fetch new messages from remote folder
        for (JDAVMailMessage remoteMsg : remoteMsgs) {
            Date date = remoteMsg.getReceivedDate();
            int flags = getZimbraFlags(remoteMsg);
            long uid = Math.abs(remoteMsg.getMessageID().hashCode());

            ZimbraLog.datasource.debug("Found new remote message %d.  Creating local copy.", uid);
            if (date == null)
                date = remoteMsg.getSentDate();
            
            ParsedMessage pm = new ParsedMessage(remoteMsg, date == null ? -1 :
                date.getTime(), mbox.attachmentsIndexingEnabled());
            com.zimbra.cs.mailbox.Message m = ds.isOffline() ?
                offlineAddMessage(pm, folderId, flags) :
                mbox.addMessage(null, pm, folderId, true, flags, null);
            
            if (m != null) {
                try {
                    DbImapMessage.storeImapMessage(mbox, folderId, uid,
                        m.getId(), flags);
                } catch (Exception e) {
                    DbImapMessage.deleteImapMessage(mbox, folderId,
                        DbImapMessage.getLocalMessageId(mbox, folderId, uid));
                    DbImapMessage.storeImapMessage(mbox, folderId, uid,
                        m.getId(), flags);
                }
                numAddedLocally++;
            }
        }
        // Remaining local ID's are messages that were not found on the remote server
        for (int localId : localIds) {
            if (trackedMsgs.containsItemId(localId)) {
                ZimbraLog.datasource.debug("Message %d was deleted remotely.  Deleting local copy.", localId);
                ImapMessage tracker = trackedMsgs.getByItemId(localId);
                mbox.delete(null, localId, MailItem.TYPE_UNKNOWN);
                numDeletedLocally++;
                DbImapMessage.deleteImapMessage(mbox, folderId, tracker.getItemId());
            } else {
                ZimbraLog.datasource.debug("Found new local message %d.  Creating remote copy.", localId);
                com.zimbra.cs.mailbox.Message localMsg = mbox.getMessageById(null, localId);
                MimeMessage mimeMsg = localMsg.getMimeMessage(false);
                int flags = localMsg.getFlagBitmask();
                setFlags(mimeMsg, flags);
                String[] newUids = remoteFolder.appendUIDMessages(new MimeMessage[] { mimeMsg });
                assert newUids != null && newUids.length == 1 && newUids[0] != null;
                numAddedRemotely++;
                DbImapMessage.storeImapMessage(mbox, folderId,
                    newUids[0].hashCode(), localId, flags);
            }
        }
        remoteFolder.close(true);
        ZimbraLog.datasource.debug(
            "Import of %s completed.  Matched: %d, updated: %d, added locally: %d, " +
            "deleted locally: %d, added remotely: %d, deleted remotely: %d",
            remoteFolder.getFullName(), numMatched, numUpdated, numAddedLocally,
            numDeletedLocally, numAddedRemotely, numDeletedRemotely);
    }

    private void setFlags(Message msg, int newFlags) throws MessagingException {
        Flags remoteFlags = msg.getFlags();
        
        try {
            if (remoteFlags.contains(Flags.Flag.SEEN)) {
                if ((newFlags & Flag.BITMASK_UNREAD) != 0)
                    msg.setFlag(Flags.Flag.SEEN, false);
            } else {
                if ((newFlags & Flag.BITMASK_UNREAD) == 0)
                    msg.setFlag(Flags.Flag.SEEN, true);
            }
        } catch (Exception e) {
            ZimbraLog.datasource.warn("Unable to set msg flags: " + e);
        }
    }

    // Get ZIMBRA mail flags from remote flags
    private int getZimbraFlags(JDAVMailMessage msg) throws MessagingException {
        int flags = msg.getFlags().contains(Flags.Flag.SEEN) ? 0 :
            Flag.BITMASK_UNREAD;
        String fldr = msg.getFolder().getFullName();
        
        if (fldr.equals("deleteditems"))
            flags |= Flag.BITMASK_DELETED;
        else if (fldr.equals("draftitems"))
            flags |= Flag.BITMASK_DRAFT;
        return flags;
    }
    
    protected void connect() throws ServiceException  {
        if (!store.isConnected()) {
            DataSource ds = getDataSource();
            try {
                store.connect(null, ds.getUsername(), ds.getDecryptedPassword());
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Unable to connect to mail store: " + ds, e);
            }
        }
    }

    protected void disconnect() throws ServiceException {
        if (store.isConnected()) {
            DataSource ds = getDataSource();
            try {
                store.close();
            } catch (MessagingException e) {
                ZimbraLog.datasource.warn("Unable to disconnect from mail store: " + ds);
            }
        }
    }
}
