/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

import com.sun.mail.imap.IMAPMessage;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbImapFolder;
import com.zimbra.cs.db.DbImapMessage;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;

public class ImapImport implements MailItemImport {

    private static final long TIMEOUT = 20 * Constants.MILLIS_PER_SECOND;

    private static Session sSession;
    private static Session sSelfSignedCertSession;
    private static FetchProfile FETCH_PROFILE;

    static {
        Properties props = new Properties();
        props.setProperty("mail.imap.connectiontimeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imap.timeout", Long.toString(TIMEOUT));
        sSession = Session.getInstance(props);

        props.put("mail.imap.socketFactory.class",
            com.zimbra.common.util.DummySSLSocketFactory.class.getName());
        sSelfSignedCertSession = Session.getInstance(props);

        FETCH_PROFILE = new FetchProfile();
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.UID);
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.FLAGS);
    }

    public String test(DataSource ds) throws ServiceException {
        String error = null;

        validateDataSource(ds);

        try {
            Store store = getStore(ds.getConnectionType());
            store.connect(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getDecryptedPassword());
            store.close();
        } catch (MessagingException e) {
            ZimbraLog.datasource.info("Testing connection to data source", e);
            error = e.getMessage();
        }
        return error;
    }

    public void importData(Account account, DataSource ds) throws ServiceException {
        try {
            validateDataSource(ds);

            Store store = getStore(ds.getConnectionType());
            store.connect(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getDecryptedPassword());
            Folder remoteRootFolder = store.getDefaultFolder();
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            ImapFolderCollection imapFolders = DbImapFolder.getImapFolders(mbox, ds);

            com.zimbra.cs.mailbox.Folder localRootFolder =
                mbox.getFolderById(null, ds.getFolderId());

            // Handle new remote folders and moved/renamed/deleted local folders
            Folder[]  remoteFolders = remoteRootFolder.list("*"); 
            for (Folder remoteFolder : remoteFolders) {
                ZimbraLog.datasource.debug("Processing IMAP folder " + remoteFolder.getFullName());

                // Update associations between remote and local folders
                ImapFolder imapFolder = imapFolders.getByRemotePath(remoteFolder.getFullName());
                com.zimbra.cs.mailbox.Folder localFolder = null;

                // Handle IMAP folder we already know about
                if (imapFolder != null) {
                    try {
                        localFolder = mbox.getFolderById(null, imapFolder.getItemId());
                    } catch (NoSuchItemException e) {
                        // Folder was deleted locally, so we'll need to delete
                        // the remote one as well.  Check whether the folder is open
                        // and exists, in case deleting this folder's parent implicitly
                        // this folder to be deleted as well.
                        if (remoteFolder.isOpen()) {
                            remoteFolder.close(true);
                        }
                        if (remoteFolder.exists()) {
                            remoteFolder.delete(true);
                        }
                        imapFolders.remove(imapFolder);
                        DbImapFolder.deleteImapFolder(mbox, ds, imapFolder);
                    }

                    // See if the folder was moved or renamed locally
                    if (localFolder != null
                        && !localFolder.getPath().equals(imapFolder.getLocalPath())) {
                        if (isParent(localRootFolder, localFolder)) {
                            // Folder has a new name/path but is still under the
                            // data source root
                            renameJavaMailFolder(remoteFolder, localRootFolder, localFolder);
                            imapFolder.setLocalPath(localFolder.getPath());
                            DbImapFolder.updateImapFolder(imapFolder);
                        } else {
                            // Folder was moved outside the data source root.
                            // Treat as an add.
                            DbImapFolder.deleteImapFolder(mbox, ds, imapFolder);
                            imapFolders.remove(imapFolder);
                            imapFolder = null;
                            localFolder = null;
                        }
                    }
                }

                // Handle new IMAP folder
                if (imapFolder == null) {
                    String zimbraPath = getZimbraFolderPath(mbox, ds, remoteFolder);
                    localFolder = mbox.createFolder(null, zimbraPath, (byte) 0,
                        MailItem.TYPE_UNKNOWN);
                    imapFolder = DbImapFolder.createImapFolder(mbox, ds, localFolder.getId(),
                        localFolder.getPath(), remoteFolder.getFullName());
                    imapFolders.add(imapFolder);
                }

            }

            // Handle new local folders and deleted remote folders
            for (com.zimbra.cs.mailbox.Folder zimbraFolder : localRootFolder.getSubfolderHierarchy()) {
                if (zimbraFolder.getId() == localRootFolder.getId()) {
                    continue;
                }
                
                // Re-get the folder, in case it was implicitly deleted when its
                // parent was deleted
                try {
                    zimbraFolder = mbox.getFolderById(null, zimbraFolder.getId());
                } catch (NoSuchItemException e) {
                    ZimbraLog.datasource.debug(
                        "Skipping folder %s, probably deleted by parent deletion", zimbraFolder.getName());
                    ImapFolder imapFolder = imapFolders.getByItemId(zimbraFolder.getId());
                    imapFolders.remove(imapFolder);
                    continue;
                }
                ZimbraLog.datasource.debug("Processing local folder %s", zimbraFolder.getPath());

                ImapFolder imapFolder = imapFolders.getByLocalPath(zimbraFolder.getPath());
                if (imapFolder != null) {
                    // Already know about this folder. See if it still exists on
                    // the remote server.
                    Folder jmFolder = store.getFolder(imapFolder.getRemotePath());
                    if (!jmFolder.exists()) {
                        // Folder was deleted on the remote server, so we'll
                        // need to delete it locally.
                        mbox.delete(null, zimbraFolder.getId(), zimbraFolder.getType());
                        DbImapFolder.deleteImapFolder(mbox, ds, imapFolder);
                        imapFolders.remove(imapFolder);
                    }
                } else {
                    // New local folder. Create the corresponding remote folder.
                    Folder jmFolder = createJavaMailFolder(store, remoteRootFolder.getSeparator(),
                        localRootFolder, zimbraFolder);
                    imapFolder = DbImapFolder.createImapFolder(mbox, ds, zimbraFolder.getId(),
                        zimbraFolder.getPath(), jmFolder.getFullName());
                    imapFolders.add(imapFolder);
                }
            }

            // Import data for all ImapFolders that exist on both sides
            for (ImapFolder imapFolder : imapFolders) {
                ZimbraLog.datasource.info("Importing from IMAP folder %s to local folder %s",
                    imapFolder.getRemotePath(), imapFolder.getLocalPath());
                Folder jmFolder = store.getFolder(imapFolder.getRemotePath());
                jmFolder.open(Folder.READ_ONLY);
                com.zimbra.cs.mailbox.Folder zimbraFolder =
                    mbox.getFolderById(null, imapFolder.getItemId());
                importFolder(account, ds, imapFolder, jmFolder, zimbraFolder);
                jmFolder.close(true);
            }

            store.close();
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
    }

    private void renameJavaMailFolder(Folder remoteFolder,
                                      com.zimbra.cs.mailbox.Folder localRootFolder,
                                      com.zimbra.cs.mailbox.Folder localFolder)
    throws MessagingException {
        String jmPath = localPathToRemotePath(localRootFolder, localFolder, remoteFolder
            .getSeparator());
        ZimbraLog.datasource.info("Renaming IMAP folder from " + remoteFolder.getFullName() + " to "
            + jmPath);
        Folder newName = remoteFolder.getStore().getFolder(jmPath);
        remoteFolder.renameTo(newName);
    }

    private Folder createJavaMailFolder(Store store, char separator,
                                        com.zimbra.cs.mailbox.Folder localRootFolder,
                                        com.zimbra.cs.mailbox.Folder localFolder)
    throws MessagingException {
        String jmPath = localPathToRemotePath(localRootFolder, localFolder, separator);
        ZimbraLog.datasource.info("Creating IMAP folder %s for local folder %s", jmPath,
            localFolder.getPath());
        Folder jmFolder = store.getFolder(jmPath);
        jmFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        return jmFolder;
    }

    private String localPathToRemotePath(com.zimbra.cs.mailbox.Folder localRootFolder,
                                         com.zimbra.cs.mailbox.Folder localFolder, char separator) {
        // Strip local root from the folder's path
        String rootPath = localRootFolder.getPath();
        String folderPath = localFolder.getPath();
        if (folderPath.startsWith(rootPath)) {
            folderPath = folderPath.substring(rootPath.length());
        } else {
            ZimbraLog.datasource.warn("Folder path %s is not under root %s", folderPath, rootPath);
        }

        // Generate IMAP path
        String imapPath = folderPath;
        if (separator != '/') {
            String[] parts = localFolder.getPath().split("/");
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

    private static final Pattern PAT_LEADING_SLASHES = Pattern.compile("^/+");

    /**
     * Returns the path to the Zimbra folder that stores messages for the given
     * JavaMail folder. The Zimbra folder has the same path as the JavaMail
     * folder, but is relative to the root folder specified by the
     * <tt>DataSource</tt>.
     */
    private String getZimbraFolderPath(Mailbox mbox, DataSource ds, Folder jmFolder)
    throws ServiceException, MessagingException {
        char separator = jmFolder.getSeparator();
        String relativePath = jmFolder.getFullName();

        // Change folder path to use our separator
        if (separator != '/') {
            // Make sure none of the elements in the path uses our path
            // separator
            char replaceChar = (separator == '-' ? 'x' : '-');
            relativePath.replace('/', replaceChar);
            relativePath.replace(separator, '/');
        }

        // Remove leading slashes and append to root folder path
        com.zimbra.cs.mailbox.Folder rootZimbraFolder = mbox.getFolderById(null, ds.getFolderId());
        Matcher matcher = PAT_LEADING_SLASHES.matcher(relativePath);
        relativePath = matcher.replaceFirst("");
        String absolutePath = rootZimbraFolder.getPath() + "/" + relativePath;
        return absolutePath;
    }

    private void validateDataSource(DataSource ds) throws ServiceException {
        if (ds.getHost() == null) {
            throw ServiceException.FAILURE(ds + ": host not set", null);
        }
        if (ds.getPort() == null) {
            throw ServiceException.FAILURE(ds + ": port not set", null);
        }
        if (ds.getConnectionType() == null) {
            throw ServiceException.FAILURE(ds + ": connectionType not set", null);
        }
        if (ds.getUsername() == null) {
            throw ServiceException.FAILURE(ds + ": username not set", null);
        }
    }

    private Store getStore(DataSource.ConnectionType connectionType)
    throws NoSuchProviderException, ServiceException {
        if (connectionType == DataSource.ConnectionType.cleartext) {
            return sSession.getStore("imap");
        } else if (connectionType == DataSource.ConnectionType.ssl) {
            if (LC.data_source_trust_self_signed_certs.booleanValue()) {
                return sSelfSignedCertSession.getStore("imaps");
            } else {
                return sSession.getStore("imaps");
            }
        } else {
            throw ServiceException.FAILURE("Invalid connectionType: " + connectionType, null);
        }
    }

    private void importFolder(Account account, DataSource ds, ImapFolder imapFolder,
                              Folder jmFolder, com.zimbra.cs.mailbox.Folder zimbraFolder)
    throws MessagingException, IOException, ServiceException {
        Message[] msgs = jmFolder.getMessages();
        ZimbraLog.datasource.debug("Found %d messages in %s", msgs.length, jmFolder.getFullName());

        if (msgs.length > 0) {
            // Fetch message UID's for reconciliation (UIDL)
            jmFolder.fetch(msgs, FETCH_PROFILE);
        }

        // TODO: Check the UID of the first message to make sure the user isn't
        // importing his own mailbox.
        /*
         * long uid = folder.getUID(msgs[0]); if (isOwnUid(mbox, uid)) {
         * folder.close(false); store.close(); throw
         * ServiceException.INVALID_REQUEST( "User attempted to import messages
         * from his own mailbox", null); }
         */

        // Insert new messages
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Map<Long, ImapMessage> storedMsgs = DbImapMessage.getImapMessages(mbox, ds, imapFolder);
        int numAdded = 0;
        int numSkipped = 0;
        int numUpdated = 0;
        int numDeleted = 0;

        for (int i = 0; i < msgs.length; i++) {
            IMAPMessage imapMsg = (IMAPMessage) msgs[i];
            long uid = ((com.sun.mail.imap.IMAPFolder) jmFolder).getUID(imapMsg);

            if (storedMsgs.containsKey(uid)) {
                // We already know about this message.
                ImapMessage storedMsg = storedMsgs.remove(uid);
                int appliedFlags = applyFlags(imapMsg, storedMsg.getFlags());
                if (appliedFlags != storedMsg.getFlags()) {
                    // Flags changed
                    mbox.setTags(null, storedMsg.getItemId(), MailItem.TYPE_MESSAGE, appliedFlags,
                        MailItem.TAG_UNCHANGED);
                    numUpdated++;
                } else {
                    numSkipped++;
                }
            } else {
                // Add message to mailbox
                ParsedMessage pm = null;
                if (imapMsg.getSentDate() != null) {
                    // Set received date to the original message's date
                    pm = new ParsedMessage(imapMsg, imapMsg.getSentDate().getTime(),
                        mbox.attachmentsIndexingEnabled());
                } else {
                    pm = new ParsedMessage(imapMsg, mbox.attachmentsIndexingEnabled());
                }
                int flags = applyFlags(imapMsg, 0);
                com.zimbra.cs.mailbox.Message zimbraMsg =
                    mbox.addMessage(null, pm, zimbraFolder.getId(), false, flags, null);
                numAdded++;
                DbImapMessage.storeImapMessage(mbox, imapFolder.getItemId(), uid, zimbraMsg.getId());
            }
        }

        // Remaining UID's are not on the remote server. Delete local copies.
        if (storedMsgs.size() > 0) {
            int[] idArray = new int[storedMsgs.size()];
            int i = 0;
            for (ImapMessage imapMsg : storedMsgs.values()) {
                idArray[i++] = imapMsg.getItemId();
            }
            mbox.delete(null, idArray, MailItem.TYPE_MESSAGE, null);
            numDeleted++;
        }

        ZimbraLog.datasource.debug(
            "Import of %s completed.  Added: %d, skipped: %d, updated: %d, deleted: %d.",
            jmFolder.getFullName(), numAdded, numSkipped, numUpdated, numDeleted);
    }

    private static final Flags.Flag[] IMAP_FLAGS = { Flags.Flag.ANSWERED, Flags.Flag.DELETED,
        Flags.Flag.DRAFT, Flags.Flag.FLAGGED, Flags.Flag.SEEN };

    private static final int[] ZIMBRA_FLAG_BITMASKS = { Flag.BITMASK_REPLIED, Flag.BITMASK_DELETED,
        Flag.BITMASK_DRAFT, Flag.BITMASK_FLAGGED, Flag.BITMASK_UNREAD };

    /**
     * Applies the flags from a JavaMail message to the given flag bitmask.
     */
    private int applyFlags(Message remoteMsg, int flags)
    throws MessagingException {
        for (int i = 0; i < IMAP_FLAGS.length; i++) {
            Flags.Flag imapFlag = IMAP_FLAGS[i];
            boolean isSet = remoteMsg.isSet(imapFlag);
            if (imapFlag == Flags.Flag.SEEN) {
                // IMAP uses "seen", we use "unread"
                isSet = !isSet;
            }
            if (isSet) {
                flags |= ZIMBRA_FLAG_BITMASKS[i];
            } else {
                flags &= ~ZIMBRA_FLAG_BITMASKS[i];
            }
        }

        return flags;
    }
}
