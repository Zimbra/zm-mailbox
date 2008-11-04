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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.ReadOnlyFolderException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.posisoft.jdavmail.JDAVContact;
import com.posisoft.jdavmail.JDAVContactFolder;
import com.posisoft.jdavmail.JDAVContactGroup;
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
import com.zimbra.cs.datasource.LiveData;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbImapMessage;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;

public class LiveImport extends MailItemImport {
    private JDAVMailStore store;

    public LiveImport(DataSource ds) throws ServiceException {
        super(ds);
        store = new JDAVMailStore(getSession(ds), null);

    }
    
    private static Session getSession(DataSource ds) {
        boolean debug = Boolean.getBoolean("ZimbraJavamailDebug") ||
            LC.javamail_imap_debug.booleanValue() || ds.isDebugTraceEnabled();
        Long timeout = LC.javamail_imap_timeout.longValue() * Constants.MILLIS_PER_SECOND;
        Properties props = new Properties();

        if (timeout > 0) {
            props.setProperty("mail.davmail.connectiontimeout", timeout.toString());
            props.setProperty("mail.davail.timeout", timeout.toString());
        }
        if (debug)
            props.setProperty("mail.debug", "true");
        Session session = Session.getInstance(props);
        if (debug)
            session.setDebug(true);
        return session;
    }

    public synchronized String test() throws ServiceException {
        DataSource ds = getDataSource();

        try {
            connect(ds);
        } catch (ServiceException e) {
            Throwable except = SystemUtil.getInnermostException(e);
            if (except == null)
                except = e;
            ZimbraLog.datasource.info("Error connecting to mail store: ", except);
            return except.toString();
        } finally {
            disconnect(ds);
        }
        return null;
    }
    
    public synchronized void importData(List<Integer> folderIds, boolean fullSync)
        throws ServiceException {
        DataSource ds = getDataSource();
        OperationContext octxt = new Mailbox.OperationContext(mbox);

        connect(ds);
        try {
            JDAVMailFolder remoteRootFolder = (JDAVMailFolder)store.getDefaultFolder();
            Collection<DataSourceItem> dsFolders =
                DbDataSource.getAllMappingsInFolder(ds, ds.getFolderId());
            Map<Integer, DataSourceItem> dsFoldersById = new HashMap<Integer,
                DataSourceItem>();
            com.zimbra.cs.mailbox.Folder localRootFolder = mbox.getFolderById(
                octxt, ds.getFolderId());
            Folder[] remoteFolders = remoteRootFolder.list("*");

            {
                // Delete old ImapFolder mappings
                ImapFolderCollection imapFolders = ds.getImapFolders();
                for (ImapFolder imapFolder : imapFolders) {
                    ImapMessageCollection imapMsgs =
                        DbImapMessage.getImapMessages(mbox, ds, imapFolder);
                    
                    for (ImapMessage msg : imapMsgs)
                        mbox.delete(octxt, msg.getItemId(), MailItem.TYPE_UNKNOWN);
                    ds.deleteImapFolder(imapFolder);
                }
            }
            for (DataSourceItem dsFolder : dsFolders)
                dsFoldersById.put(dsFolder.itemId, dsFolder);
            // Handle new remote folders and moved/renamed/deleted local folders
            for (Folder folder : remoteFolders) {
                JDAVMailFolder remoteFolder = (JDAVMailFolder)folder;
                DataSourceItem folderTracker = DbDataSource.getReverseMapping(ds,
                    remoteFolder.getUID());
                com.zimbra.cs.mailbox.Folder localFolder = null;
                String remotePath = remoteFolder.getFullName();
                String knownPath = ds.matchKnownLocalPath(remotePath);
                
                if (knownPath != null) {
                    if (knownPath.equals(""))
                        continue;
                    remotePath = knownPath;
                }
                while (remotePath.startsWith("/"))
                    remotePath = remotePath.substring(1);
                if (ds.getFolderId() == Mailbox.ID_FOLDER_USER_ROOT)
                    remotePath = "/" + remotePath;
                else
                    remotePath = mbox.getFolderById(octxt,
                        ds.getFolderId()).getPath() + "/" + remotePath;

                ZimbraLog.datasource.debug("Processing Live folder " +
                    remoteFolder.getFullName());
                if (folderTracker.itemId != 0) {
                    try {
                        localFolder = mbox.getFolderById(octxt, folderTracker.itemId);
                        if (!localFolder.getPath().equalsIgnoreCase(remotePath)) {
                            String jmPath = localPathToRemotePath(ds,
                                localRootFolder, localFolder, remoteFolder.getSeparator());
                            
                            if (jmPath != null && isParent(localRootFolder, localFolder)) {
                                // Folder has a new name/path but is still under the
                                // data source root
                                ZimbraLog.datasource.info("Local folder was renamed to %s",
                                    localFolder.getPath());
                                renameJavaMailFolder(remoteFolder, jmPath);
                            } else {
                                // Folder was moved outside the data source root, or folder setting is changed to "not to sync"
                                // Treat as a delete.
                                ZimbraLog.datasource.info("Local folder was renamed to %s and moved outside the data source root.",
                                    localFolder.getPath());
                                deleteFolderMappings(ds, folderTracker.itemId);
                                folderTracker.itemId = 0;
                                localFolder = null;
                            }
                        }
                    } catch (NoSuchItemException e) {
                        ZimbraLog.datasource.info("Local folder was deleted. Deleting remote folder %s.",
                            remoteFolder.getFullName());
                        try {
                            remoteFolder.delete(true);
                        } catch (MessagingException ee) {
                            // some special, unlisted folders such as drafts
                            // cannot be deleted
                            ZimbraLog.datasource.debug("Unable to delete remote folder %s.",
                                remoteFolder.getFullName());
                        }
                        dsFolders.remove(folderTracker.itemId);
                        deleteFolderMappings(ds, folderTracker.itemId);
                        folderTracker.itemId = 0;
                    }
                }
                // Handle new folder
                if (folderTracker.itemId == 0) {
            	    ZimbraLog.datasource.info("Found new remote folder %s. Creating local folder %s.",
            	        remoteFolder.getFullName(), remotePath);
                    // Try to get the folder first, in case it was manually created or the
                    // last sync failed between creating the folder and writing the mapping row.
                    try {
                        localFolder = mbox.getFolderByPath(octxt, remotePath);
                    } catch (NoSuchItemException e) {
                        localFolder = mbox.createFolder(octxt, remotePath,
                            (byte)0, MailItem.TYPE_MESSAGE);
                    }
                    folderTracker.itemId = localFolder.getId();
                    folderTracker.remoteId = remoteFolder.getUID();
                    DbDataSource.addMapping(ds, folderTracker);
                    dsFoldersById.put(folderTracker.itemId, folderTracker);
                }
            }
            // Handle new local folders and deleted remote folders
            for (com.zimbra.cs.mailbox.Folder zimbraFolder : localRootFolder.getSubfolderHierarchy()) {
                DataSourceItem dsFolder = dsFoldersById.get(zimbraFolder.getId());

                if (zimbraFolder.getId() == localRootFolder.getId())
                    continue;
                if (zimbraFolder.getDefaultView() == MailItem.TYPE_CONTACT)
                    continue;
                // Re-get the folder, in case it was implicitly deleted when its
                // parent was deleted
                try {
                    zimbraFolder = mbox.getFolderById(octxt, zimbraFolder.getId());
                } catch (NoSuchItemException e) {
                    ZimbraLog.datasource.info("Folder %s deleted with parent",
                        zimbraFolder.getName());
                    if (dsFolder != null) {
	                dsFoldersById.remove(dsFolder.itemId);
                        deleteFolderMappings(ds, dsFolder.itemId);
                    }
                    continue;
                }
                if (dsFolder == null) {
                    String jmPath = localPathToRemotePath(ds, localRootFolder,
                        zimbraFolder, remoteRootFolder.getSeparator());
                    
                    if (jmPath != null) {       //null means don't sync up
                        ZimbraLog.datasource.info("Found new local folder %s. Creating remote folder %s.",
                            zimbraFolder.getPath(), jmPath);
                        try {
                            JDAVMailFolder remoteFolder = createJavaMailFolder(jmPath);
                            
                            dsFolder = new DataSourceItem(zimbraFolder.getId(),
                                remoteFolder.getUID(), null);
                            DbDataSource.addMapping(ds, dsFolder);
                            dsFoldersById.put(dsFolder.itemId, dsFolder);
                        } catch (MessagingException e) {
                            ZimbraLog.datasource.warn("Cannot create remote folder %s",
                                jmPath, e);
                        }
                    }
                } else {
                    Folder jmFolder = null;
                    
                    for (Folder folder : remoteFolders) {
                        JDAVMailFolder remoteFolder = (JDAVMailFolder)folder;

                        if (remoteFolder.getUID().equals(dsFolder.remoteId)) {
                            jmFolder = remoteFolder;
                            break;
                        }
                    }
                    if (jmFolder == null || !jmFolder.exists()) {
                        ZimbraLog.datasource.info("Remote folder %s was deleted. Deleting local folder %s.",
                            dsFolder.remoteId, zimbraFolder.getPath());
                        try {
                            mbox.delete(octxt, zimbraFolder.getId(),
                                zimbraFolder.getType());
                         } catch (MailServiceException e) {
                             if (e.getCode() != MailServiceException.IMMUTABLE_OBJECT)
                                 throw e;
                         }
                         dsFoldersById.remove(dsFolder.itemId);
                         deleteFolderMappings(ds, dsFolder.itemId);
                     }
                }
            }
            // Import data for all ImapFolders that exist on both sides
            for (Integer id : dsFoldersById.keySet()) {
                DataSourceItem dsFolder = dsFoldersById.get(id);
                
                try {
                    importFolder(ds, octxt, remoteFolders, dsFolder);
                } catch (MessagingException e) {
                    ZimbraLog.datasource.warn("An error occurred while importing folder %s", dsFolder.remoteId, e);
                } catch (ServiceException e) {
                    ZimbraLog.datasource.warn("An error occurred while importing folder %s", dsFolder.remoteId, e);
                }
            }
            importContacts(ds, octxt);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            disconnect(ds);
        }
    }

    private void deleteFolderMappings(DataSource ds, int folderId) throws
        ServiceException {
        Collection<DataSourceItem> dsItems =
            DbDataSource.getAllMappingsInFolder(ds, folderId);
        ArrayList<Integer> toDelete = new ArrayList<Integer>(dsItems.size() + 1);

        for (DataSourceItem dsItem : dsItems)
            toDelete.add(dsItem.itemId);
        toDelete.add(folderId);
        DbDataSource.deleteMappings(ds, toDelete);
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
            
            if (parentId == parent.getId())
                return true;
            if (parentId == Mailbox.ID_FOLDER_ROOT)
                return false;
            folder = child.getMailbox().getFolderById(null, parentId);
        }
    }

    private void importFolder(DataSource ds, OperationContext octxt,
        Folder[] remoteFolders, DataSourceItem dsFolder) throws IOException,
        MessagingException, ServiceException {
        int folderId = dsFolder.itemId;
        com.zimbra.cs.mailbox.Folder localFolder = mbox.getFolderById(
            octxt, folderId);
        JDAVMailFolder remoteFolder = null;

        if (!ds.isSyncEnabled(localFolder.getPath()))
            return;
        ZimbraLog.datasource.info("Importing from Live folder %s to local folder %s",
            dsFolder.remoteId, localFolder.getPath());

        for (Folder folder : remoteFolders) {
            remoteFolder = (JDAVMailFolder)folder;
            if (remoteFolder.getUID().equals(dsFolder.remoteId))
                break;
        }
        try {
            remoteFolder.open(Folder.READ_WRITE);
        } catch (ReadOnlyFolderException e) {
            ZimbraLog.datasource.info("Unable to open folder %s for write. Skipping this folder.",
                remoteFolder.getFullName());
            return;
        }

        Collection<DataSourceItem> dsMsgs =
            DbDataSource.getAllMappingsInFolder(ds, dsFolder.itemId);
        Map<Integer, DataSourceItem> dsMsgsById = new HashMap<Integer,
            DataSourceItem>();
        Set<Integer> localIds = new HashSet<Integer>();
        Message[] msgArray = remoteFolder.getMessages();
        int numMatched = 0;
        int numUpdated = 0;
        int numDeletedLocally = 0;
        int numAddedRemotely = 0;
        int numDeletedRemotely = 0;
        int numAddedLocally = 0;
        List<JDAVMailMessage> remoteMsgs = new LinkedList<JDAVMailMessage>();

        ZimbraLog.datasource.debug("Found %d messages in %s", msgArray.length,
            remoteFolder.getFullName());
        for (DataSourceItem dsMsg : dsMsgs)
            dsMsgsById.put(dsMsg.itemId, dsMsg);
        localIds.addAll(mbox.listItemIds(null, MailItem.TYPE_MESSAGE, folderId));
        for (Message msg : msgArray) {
            JDAVMailMessage remoteMsg = (JDAVMailMessage)msg;
            DataSourceItem trackedMsg = DbDataSource.getReverseMapping(ds,
                remoteMsg.getMessageID());
            
            if (trackedMsg.itemId == 0) {
                remoteMsgs.add(remoteMsg);
            } else if (localIds.contains(trackedMsg.itemId)) {
                LiveData ld = new LiveData(ds, trackedMsg);
                com.zimbra.cs.mailbox.Message localMsg =
                    mbox.getMessageById(octxt, trackedMsg.itemId);
                int localFlags = localMsg.getFlagBitmask();
                int newFlags = localFlags;
                int remoteFlags = getZimbraFlags(remoteMsg);
                int trackedFlags = ld.getFlags();
                boolean updated = false;
                final int[] REMOTE_FLAG_BITMASKS = {
                    Flag.BITMASK_DELETED, Flag.BITMASK_DRAFT,
                    Flag.BITMASK_UNREAD
                };

                for (int flag : REMOTE_FLAG_BITMASKS) {
                    if ((remoteFlags & flag) != (trackedFlags & flag))
                        newFlags = (remoteFlags & flag) == 0 ?
                            localFlags & ~flag : localFlags | flag;
                }
                if (newFlags != localFlags) {
                    mbox.setTags(octxt, trackedMsg.itemId,
                        MailItem.TYPE_MESSAGE, newFlags, MailItem.TAG_UNCHANGED);
                    updated = true;
                }
                if ((newFlags & Flag.BITMASK_UNREAD) !=
                    (remoteFlags & Flag.BITMASK_UNREAD)) {
                    setFlags(remoteMsg, newFlags);
                    updated = true;
                }
                if (newFlags != trackedFlags) {
                    ld.setFlags(newFlags);
                    ld.update();
                    updated = true;
                }
                if (updated) {
                    numUpdated++;
                    ZimbraLog.datasource.debug("Found message with UID %s on both sides; syncing flags: local=%s, tracked=%s, remote=%s, new=%s",
                       remoteMsg.getMessageID(), Flag.bitmaskToFlags(localFlags),
                       Flag.bitmaskToFlags(trackedFlags),
                       Flag.bitmaskToFlags(remoteFlags),
                       Flag.bitmaskToFlags(newFlags));
                } else {
                    numMatched++;
                }
                localIds.remove(trackedMsg.itemId);
            } else {
                ZimbraLog.datasource.debug("Message was deleted locally. Deleting remote copy with UID %s.",
                    remoteMsg.getMessageID());
                remoteMsg.setFlag(Flags.Flag.DELETED, true);
                LiveData.delete(ds, trackedMsg.itemId);
                numDeletedRemotely++;
            }
        }
        // Fetch new messages from remote folder
        for (JDAVMailMessage remoteMsg : remoteMsgs) {
            Date date = remoteMsg.getReceivedDate();
            int flags = getZimbraFlags(remoteMsg);

            ZimbraLog.datasource.debug("Found new remote message %s. Creating local copy.",
                remoteMsg.getMessageID());
            if (date == null)
                date = remoteMsg.getSentDate();

            com.zimbra.cs.mailbox.Message localMsg = addMessage(octxt,
                new ParsedMessage(remoteMsg, date == null ? -1 : date.getTime(),
                mbox.attachmentsIndexingEnabled()), folderId, flags);
            if (localMsg != null) {
                LiveData ld = new LiveData(ds, localMsg.getId(),
                    remoteMsg.getMessageID(), localMsg.getChangeDate(),
                    date.getTime(), flags);

                try {
                    ld.add();
                } catch (Exception e) {
                    ld.delete();
                    ld.add();
                }
                numAddedLocally++;
            }
        }
        // Remaining local ID's are messages that were not found on the remote server
        for (int localId : localIds) {
            DataSourceItem dsMsg = dsMsgsById.get(localId);
            
            if (dsMsg == null) {
                ZimbraLog.datasource.debug("Found new local message %d. Creating remote copy.",
                    localId);
                com.zimbra.cs.mailbox.Message localMsg = mbox.getMessageById(octxt, localId);
                MimeMessage mimeMsg = localMsg.getMimeMessage(false);
                int flags = localMsg.getFlagBitmask();
                
                setFlags(mimeMsg, flags);
                
                String[] newUids = remoteFolder.appendUIDMessages(new MimeMessage[] {
                    mimeMsg });
                JDAVMailMessage remoteMsg = (JDAVMailMessage)remoteFolder.getMessage(remoteFolder.getMessageCount());
                Date date = remoteMsg.getReceivedDate();

                if (date == null)
                    date = new Date(localMsg.getDate());

                LiveData ld = new LiveData(ds, localMsg.getId(),
                    newUids[0], localMsg.getChangeDate(), date.getTime(),
                    getZimbraFlags(remoteMsg));

                try {
                    ld.add();
                } catch (Exception e) {
                    ld.delete();
                    ld.add();
                }
                numAddedRemotely++;
            } else {
                ZimbraLog.datasource.debug("Message %s was deleted remotely. Deleting local copy.",
                    dsMsg.remoteId);
                mbox.delete(octxt, localId, MailItem.TYPE_UNKNOWN);
                LiveData.delete(ds, dsMsg.itemId);
                numDeletedLocally++;
            }
        }
        remoteFolder.close(true);
        ZimbraLog.datasource.debug(
            "Import of %s completed.  Matched: %d, updated: %d, added locally: %d, " +
            "deleted locally: %d, added remotely: %d, deleted remotely: %d",
            remoteFolder.getFullName(), numMatched, numUpdated, numAddedLocally,
            numDeletedLocally, numAddedRemotely, numDeletedRemotely);
    }

    private void importContacts(DataSource ds, OperationContext octxt) throws
        IOException, MessagingException, ServiceException {
        JDAVContactFolder contactFolder = store.getContactFolder();
        DataSourceItem folderTracker = DbDataSource.getReverseMapping(ds,
            contactFolder.getName());
        Set<Integer> localIds = new HashSet<Integer>();
        int numMatched = 0;
        int numUpdated = 0;
        int numDeletedLocally = 0;
        int numAddedRemotely = 0;
        int numDeletedRemotely = 0;
        int numAddedLocally = 0;
        List<JDAVContact> remoteContacts = new LinkedList<JDAVContact>();
        List<JDAVContactGroup> remoteGroups = new LinkedList<JDAVContactGroup>();
        com.zimbra.cs.mailbox.Folder localRootFolder = mbox.getFolderById(
            null, ds.getFolderId());
        com.zimbra.cs.mailbox.Folder localFolder = null;
        String remotePath = contactFolder.getName();
        String knownPath = ds.matchKnownLocalPath(remotePath);
        
        if (knownPath != null) {
            if (knownPath.equals(""))
                return;
            remotePath = knownPath;
        }
        while (remotePath.startsWith("/"))
            remotePath = remotePath.substring(1);
        if (ds.getFolderId() == Mailbox.ID_FOLDER_USER_ROOT)
            remotePath = "/" + remotePath;
        else
            remotePath = mbox.getFolderById(octxt,
                ds.getFolderId()).getPath() + "/" + remotePath;
        ZimbraLog.datasource.debug("Processing Live contacts folder");
        if (folderTracker.itemId != 0) {
            try {
                localFolder = mbox.getFolderById(octxt, folderTracker.itemId);
                localIds.addAll(mbox.listItemIds(octxt, MailItem.TYPE_CONTACT,
                    folderTracker.itemId));
                if (!localFolder.getPath().equalsIgnoreCase(remotePath)) {
                    String jmPath = localPathToRemotePath(ds, localRootFolder, localFolder, '/');

                    if (jmPath != null && isParent(localRootFolder, localFolder)) {
                        ZimbraLog.datasource.info("Local folder was renamed to %s",
                            localFolder.getPath());
                    } else {
                        ZimbraLog.datasource.info("Local folder was renamed to %s and moved outside the data source root.",
                            localFolder.getPath());
                        deleteFolderMappings(ds, folderTracker.itemId);
                        folderTracker.itemId = 0;
                        localFolder = null;
                    }
                }
            } catch (NoSuchItemException e) {
                deleteFolderMappings(ds, folderTracker.itemId);
            }
        }
        if (folderTracker.itemId == 0) {
            ZimbraLog.datasource.info("Creating local contact folder %s", remotePath);
            try {
                localFolder = mbox.getFolderByPath(octxt, remotePath);
            } catch (NoSuchItemException e) {
                localFolder = mbox.createFolder(octxt, remotePath,
                    (byte)0, MailItem.TYPE_CONTACT);
            }
            folderTracker.itemId = localFolder.getId();
            folderTracker.remoteId = contactFolder.getName();
            DbDataSource.addMapping(ds, folderTracker);
        }

        Collection<DataSourceItem> dsContacts =
            DbDataSource.getAllMappingsInFolder(ds, folderTracker.itemId);

        contactFolder.open(Folder.READ_WRITE);
        for (int i = 1; i <= contactFolder.getContactCount(); i++) {
            JDAVContact remoteContact = contactFolder.getContact(i);
            DataSourceItem trackedContact = DbDataSource.getReverseMapping(ds,
                remoteContact.getUID());
            
            if (trackedContact.itemId == 0) {
                remoteContacts.add(remoteContact);
            } else if (localIds.contains(trackedContact.itemId)) {
                LiveData ld = new LiveData(ds, trackedContact);
                Contact localContact = mbox.getContactById(octxt, trackedContact.itemId);

                if (remoteContact.getModifiedDate().getTime() > ld.getRemoteDate()) {
                    mbox.modifyContact(octxt, localContact.getId(),
                        LiveData.getParsedContact(remoteContact, localContact));
                    localContact = mbox.getContactById(octxt, trackedContact.itemId);
                    ld.setDates(localContact.getChangeDate(),
                        remoteContact.getModifiedDate().getTime());
                    ld.update();
                    numUpdated++;
                    ZimbraLog.datasource.debug("Updated local contact %s",
                        remoteContact.getName());
                } else if (localContact.getChangeDate() > ld.getLocalDate()) {
                    LiveData.updateJDAVContact(remoteContact, localContact);
                    remoteContact.modify();
                    ld.setDates(localContact.getChangeDate(),
                        remoteContact.getModifiedDate().getTime());
                    ld.update();
                    numUpdated++;
                    ZimbraLog.datasource.debug("Updated remote contact %s",
                        remoteContact.getName());
                } else {
                    numMatched++;
                }
                localIds.remove(trackedContact.itemId);
            } else {
                ZimbraLog.datasource.debug("Contact %s was deleted locally. Deleting remote copy.",
                    remoteContact.getName());
                LiveData.delete(ds, trackedContact.itemId);
                remoteContact.delete();
                numDeletedRemotely++;
            }
        }
        for (int i = 1; i <= contactFolder.getGroupCount(); i++) {
            JDAVContactGroup remoteGroup = contactFolder.getGroup(i);
            DataSourceItem trackedGroup = DbDataSource.getReverseMapping(ds,
                remoteGroup.getUID());
            
            if (trackedGroup.itemId == 0) {
                remoteGroups.add(remoteGroup);
            } else if (localIds.contains(trackedGroup.itemId)) {
                LiveData ld = new LiveData(ds, trackedGroup);
                Contact localGroup = mbox.getContactById(octxt, trackedGroup.itemId);

                if (remoteGroup.getModifiedDate().getTime() > ld.getRemoteDate()) {
                    mbox.modifyContact(octxt, localGroup.getId(),
                        LiveData.getParsedContact(remoteGroup, localGroup));
                    localGroup = mbox.getContactById(octxt, trackedGroup.itemId);
                    ld.setDates(localGroup.getChangeDate(),
                        remoteGroup.getModifiedDate().getTime());
                    ld.update();
                    numUpdated++;
                    ZimbraLog.datasource.debug("Updated local group %s",
                        remoteGroup.getName());
                /*
                } else if (localGroup.getChangeDate() > ld.getLocalDate()) {
                    LiveData.updateJDAVContact(remoteGroup, localContact);
                    remoteGroup.modify();
                    ld.setDates(localGroup.getChangeDate(),
                        remoteGroup.getModifiedDate().getTime());
                    ld.update();
                    numUpdated++;
                    ZimbraLog.datasource.debug("Updated remote group %s",
                        remoteGroup.getName());
                */
                } else {
                    numMatched++;
                }
                localIds.remove(trackedGroup.itemId);
            } else {
                ZimbraLog.datasource.debug("group %s was deleted locally. Deleting remote copy.",
                    remoteGroup.getName());
                remoteGroup.delete();
                LiveData.delete(ds, trackedGroup.itemId);
                numDeletedRemotely++;
            }
        }
        // Fetch new contacts from remote folder
        for (JDAVContact remoteContact : remoteContacts) {
            ZimbraLog.datasource.debug("Found new remote contact %s. Creating local copy.",
                remoteContact.getName());
            try {
                Contact localContact = mbox.createContact(octxt,
                    LiveData.getParsedContact(remoteContact, null),
                    folderTracker.itemId, null);
                LiveData ld = new LiveData(ds, localContact.getId(),
                    remoteContact.getUID(), localContact.getChangeDate(),
                    remoteContact.getModifiedDate().getTime(), 0);

                try {
                    ld.add();
                } catch (Exception e) {
                    ld.delete();
                    ld.add();
                }
                numAddedLocally++;
            } catch (Exception e) {
                ZimbraLog.datasource.warn("Creating local contact %s failed: %s",
                    remoteContact.getName(), e.toString());
            }
        }
        // Fetch new groups from remote folder
        for (JDAVContactGroup remoteGroup : remoteGroups) {
            if (remoteGroup.getMail().length() == 0) {
                ZimbraLog.datasource.debug("Found new, empty remote group %s.",
                    remoteGroup.getName());
                continue;
            }
            try {
                Contact localGroup = mbox.createContact(octxt,
                    LiveData.getParsedContact(remoteGroup, null),
                    folderTracker.itemId, null);
                LiveData ld = new LiveData(ds, localGroup.getId(),
                    remoteGroup.getUID(), localGroup.getChangeDate(),
                    remoteGroup.getModifiedDate().getTime(), 0);
    
                ZimbraLog.datasource.debug("Found new remote group %s. Creating local copy.",
                    remoteGroup.getName());
                try {
                    ld.add();
                } catch (Exception e) {
                    ld.delete();
                    ld.add();
                }
                numAddedLocally++;
            } catch (Exception e) {
                ZimbraLog.datasource.warn("Creating local group %s failed: %s",
                    remoteGroup.getName(), e.toString());
            }
        }
        // Remaining local ID's are contacts that were not found on the remote server
        for (int localId : localIds) {
            DataSourceItem dsContact = null;

            for (DataSourceItem contact : dsContacts) {
                if (contact.itemId == localId) {
                    dsContact = contact;
                    break;
                }
            }
            if (dsContact == null) {
                Contact localContact = mbox.getContactById(octxt, localId);
                String dlist = localContact.getFields().get(Contact.A_dlist);
                
                if (dlist == null) {
                } else {
                    JDAVContact remoteContact = LiveData.getJDAVContact(localContact);
                    String[] newUids = null;
                    
                    ZimbraLog.datasource.debug("Found new local contact %s. Creating remote copy.",
                        remoteContact.getName());
                    for (int i = 0; i < 5; i++) {
                        try {
                            newUids = contactFolder.appendUIDContacts(new JDAVContact[] { remoteContact } );
                            break;
                        } catch (Exception e) {
                            String s = remoteContact.getField(JDAVContact.Fields.nickname) + '-';
    
                            remoteContact.setField(JDAVContact.Fields.nickname, s);
                        }
                    }
                    if (newUids != null) {
                        LiveData ld = new LiveData(ds, localContact.getId(),
                            newUids[0], localContact.getChangeDate(),
                            remoteContact.getModifiedDate().getTime(), 0);

                        try {
                            ld.add();
                        } catch (Exception e) {
                            ld.delete();
                            ld.add();
                        }
                        numAddedRemotely++;
                    }
                }
            } else {
                ZimbraLog.datasource.debug("Contact %s was deleted remotely. Deleting local copy.",
                    dsContact.remoteId);
                mbox.delete(octxt, localId, MailItem.TYPE_UNKNOWN);
                LiveData.delete(ds, dsContact.itemId);
                numDeletedLocally++;
            }
        }
        contactFolder.close();
        ZimbraLog.datasource.debug(
            "Import of %s completed.  Matched: %d, updated: %d, added locally: %d, " +
            "deleted locally: %d, added remotely: %d, deleted remotely: %d",
            contactFolder.getName(), numMatched, numUpdated, numAddedLocally,
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
    
    protected void connect(DataSource ds) throws ServiceException  {
        if (!store.isConnected()) {
            validateDataSource();
            try {
                store.connect(null, ds.getUsername(), ds.getDecryptedPassword());
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Unable to connect to mail store: " + ds, e);
            }
        }
    }
    
    protected void disconnect(DataSource ds) throws ServiceException {
        if (store.isConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                ZimbraLog.datasource.warn("Unable to disconnect from mail store: " + ds);
            }
        }
    }
}
