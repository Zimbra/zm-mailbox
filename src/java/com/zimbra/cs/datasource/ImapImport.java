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
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.ReadOnlyFolderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.Status;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.CommandFailedException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.CustomSSLSocketFactory;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbImapMessage;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.mailclient.imap.UidFetch;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.ImapData;

public class ImapImport extends AbstractMailItemImport {
    private final UidFetch uidFetch;
    private final byte[] buf = new byte[4096]; // Temp buffer for checksum calculation
    private IMAPStore store;

    private static final int FETCH_SIZE = LC.data_source_fetch_size.intValue();
    private static final int MAX_MESSAGE_MEMORY_SIZE =
        LC.data_source_max_message_memory_size.intValue();

    private static final boolean DEBUG = 
        Boolean.getBoolean("ZimbraJavamailDebug") || LC.javamail_imap_debug.booleanValue();

    private static final String ID_EXT = "(\"vendor\" \"Zimbra\" \"os\" \"" +
        System.getProperty("os.name") + "\" \"os-version\" \"" +
        System.getProperty("os.version") + "\" \"guid\" \"" + BuildInfo.TYPE + "\")";

    private static final long TIMEOUT =
        LC.javamail_imap_timeout.longValue() * Constants.MILLIS_PER_SECOND;

    private static final Session SESSION;
    private static final FetchProfile FETCH_PROFILE;

    static {
        Properties props = new Properties();
        props.setProperty("mail.imap.connectiontimeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imap.timeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imaps.connectiontimeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imaps.timeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imaps.socketFactory.class",
            LC.data_source_trust_self_signed_certs.booleanValue() ?
                DummySSLSocketFactory.class.getName() : CustomSSLSocketFactory.class.getName());
        props.setProperty("mail.imaps.socketFactory.fallback", "false");
        if (DEBUG) {
            props.setProperty("mail.debug", "true");
        }
        props.setProperty("mail.imap.idextension", ID_EXT);
        props.setProperty("mail.imaps.idextension", ID_EXT);
        if (LC.javamail_imap_enable_starttls.booleanValue()) {
            props.setProperty("mail.imap.starttls.enable", "true");
            props.setProperty("mail.imap.socketFactory.class", TlsSocketFactory.class.getName());
        }
        SESSION = Session.getInstance(props);
        if (DEBUG) {
            SESSION.setDebug(true);
        }
        FETCH_PROFILE = new FetchProfile();
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.UID);
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.FLAGS);
    }

    public ImapImport(DataSource ds) throws ServiceException {
        super(ds);
        ImapConfig config = new ImapConfig();
        config.setMaxLiteralMemSize(MAX_MESSAGE_MEMORY_SIZE);
        uidFetch = new UidFetch(config);
        store = getStore(ds);
    }

    public void importData(boolean fullSync) throws ServiceException {
        validateDataSource();
        connect();
        DataSource ds = getDataSource();
        try {
            IMAPFolder remoteRootFolder = (IMAPFolder) store.getDefaultFolder();
            Mailbox mbox = ds.getMailbox();
            ImapFolderCollection imapFolders = ds.getImapFolders();

            com.zimbra.cs.mailbox.Folder localRootFolder =
                mbox.getFolderById(null, ds.getFolderId());

            // Handle new remote folders and moved/renamed/deleted local folders
            List<IMAPFolder> remoteFolders = listFolders(remoteRootFolder, "*");

            //when deleting folders, especially remoted folders, we delete children before parents
            //to avoid complications.  so we want to sort in fullname length descending order.
            Collections.sort(remoteFolders, new Comparator<Folder>() {
                public int compare(Folder o1, Folder o2) {
                    return o2.getFullName().length() - o1.getFullName().length();
                }
            });
            Map<String, Status> folderStatus = new HashMap<String, Status>();

            for (IMAPFolder remoteFolder : remoteFolders) {
                long remoteUvv = 0;
                if (!hasAttribute(remoteFolder, "\\Noselect")) {
                    Status status = getStatus(remoteFolder, "UIDVALIDITY", "UIDNEXT");
                    if (status != null && status.uidvalidity == 0 &&
                        store.hasCapability("XAOL-NETMAIL")) {
                        // Workaround for bug 25623: if this is AOL mail and
                        // STATUS returns a UIDVALIDITY of 0, then assume
                        // a correct value of 1 (always seems to be the case).
                        status.uidvalidity = 1;
                    } else if (status == null || status.uidvalidity <= 0 ||
                               status.uidnext <= 0) {
                        // Skip folder with bad STATUS results (see bug 26425)
                        ZimbraLog.datasource.warn(
                            "Not importing remote folder '%s' because STATUS command failed",
                            remoteFolder.getFullName());
                        continue;
                    }
                    folderStatus.put(remoteFolder.getFullName(), status);
                    remoteUvv = status.uidvalidity;
                }
                
                ZimbraLog.datasource.debug("Processing IMAP folder " + remoteFolder.getFullName());

                // Update associations between remote and local folders
                ImapFolder folderTracker = imapFolders.getByRemotePath(remoteFolder.getFullName());
                com.zimbra.cs.mailbox.Folder localFolder = null;

                // Handle IMAP folder we already know about
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
                            remoteFolder.delete(true);
                        }
                        imapFolders.remove(folderTracker);
                        ds.deleteImapFolder(folderTracker);
                    }

                    if (folderTracker.getUidValidity() == null) {
                        // Migrate old data created before we added the uid_validity column
                        ZimbraLog.datasource.info("Initializing UIDVALIDITY of %s to %d", remoteFolder.getFullName(), remoteUvv);
                        folderTracker.setUidValidity(remoteUvv);
                        ds.updateImapFolder(folderTracker);
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
                            List<Integer> newLocalIds = DbImapMessage.getNewLocalMessageIds(mbox, ds, folderTracker);
                            if (newLocalIds.size() > 0) {
                                ZimbraLog.datasource.info("Copying %d messages from local folder %s to remote folder.",
                                    newLocalIds.size(), localFolder.getPath());
                            }
                            for (int id : newLocalIds) {
                                com.zimbra.cs.mailbox.Message localMsg = mbox.getMessageById(null, id);
                                MimeMessage mimeMsg = localMsg.getMimeMessage(false);
                                int flags = localMsg.getFlagBitmask();
                                setFlags(mimeMsg, flags);
                                AppendUID[] newUids = remoteFolder.appendUIDMessages(new MimeMessage[] { mimeMsg });
                                assert newUids != null && newUids.length == 1;
                                long uid = newUids[0] != null ? newUids[0].uid : -id;
                                // If no UID returned by APPEND, then store the message locally with a negative UID.
                                // When we sync again we will calculate the checksum of remote message and use that
                                // to find the corresponding local entry and update the UID with the correct value.
                                DbImapMessage.storeImapMessage(mbox, localFolder.getId(), uid, id, flags);
                            }

                            // Empty local folder so that it will be resynced later and store the new UIDVALIDITY value.
                            mbox.emptyFolder(null, localFolder.getId(), false);
                            folderTracker.setUidValidity(remoteUvv);
                            ds.updateImapFolder(folderTracker);
                            DbImapMessage.deleteImapMessages(mbox, folderTracker.getItemId());
                        }
                    }
                }

                // Handle new IMAP folder
                if (folderTracker == null) {
                    String zimbraPath = getZimbraFolderPath(mbox, ds, remoteFolder);
                    if (zimbraPath != null) { //null means don't sync this folder
                    	ZimbraLog.datasource.info("Found new remote folder %s. Creating local folder %s.", remoteFolder.getFullName(), zimbraPath);
	                    // Try to get the folder first, in case it was manually created or the
	                    // last sync failed between creating the folder and writing the mapping row.
	                    try {
	                        localFolder = mbox.getFolderByPath(null, zimbraPath);
	                    } catch (NoSuchItemException e) {
	                    }

	                    if (localFolder == null) {
	                        localFolder = mbox.createFolder(null, zimbraPath, (byte) 0,
	                            MailItem.TYPE_UNKNOWN);
	                    }
	                    ds.initializedLocalFolder(zimbraPath); //offline can disable sync this way
	                    folderTracker = ds.createImapFolder(localFolder.getId(),
	                        localFolder.getPath(), remoteFolder.getFullName(), remoteUvv);
	                    imapFolders.add(folderTracker);
                    }
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
	                        mbox.delete(null, zimbraFolder.getId(), zimbraFolder.getType());
	                        ds.deleteImapFolder(imapFolder);
	                        imapFolders.remove(imapFolder);
	                    }
                	}
                } else {
                    String jmPath = localPathToRemotePath(ds, localRootFolder, zimbraFolder, remoteRootFolder.getSeparator());
                    if (jmPath != null) { //null means don't sync up
                    	ZimbraLog.datasource.info("Found new local folder %s.  Creating remote folder %s.", zimbraFolder.getPath(), jmPath);
                    	ds.initializedLocalFolder(zimbraFolder.getPath()); //offline can disable sync this way
	                    IMAPFolder jmFolder = createJavaMailFolder(store, jmPath);
	                    imapFolder = ds.createImapFolder(zimbraFolder.getId(),
	                        zimbraFolder.getPath(), jmFolder.getFullName(), jmFolder.getUIDValidity());
	                    imapFolders.add(imapFolder);
                    }
                }
            }

            // Import data for all ImapFolders that exist on both sides
            for (ImapFolder imapFolder : imapFolders) {
            	if (!ds.isSyncEnabled(imapFolder.getLocalPath()))
            		continue;
                
                // Don't import folder unless full sync requested or there are
                // new messages in the folder (uidnext has advanced).
                Status status = folderStatus.get(imapFolder.getRemotePath());
                if (!fullSync && status != null) {
                    long trackedUid = imapFolder.getUidNext();
                    if (trackedUid != -1 && status.uidnext != -1 &&
                        trackedUid == status.uidnext) {
                        continue; // No new messages have been detected
                    }
                }
                
                ZimbraLog.datasource.info("Importing from IMAP folder %s to local folder %s",
                    imapFolder.getRemotePath(), imapFolder.getLocalPath());
                if (imapFolder.getUidValidity() != 0) {
                    try {
                        for (int i = 0; i < 3; ++i) //at most run 3 times on a single folder import to prevent a dead loop
                            if (importFolder(imapFolder)) break;
                    } catch (MessagingException e) {
                        ZimbraLog.datasource.warn("An error occurred while importing folder %s", imapFolder.getRemotePath(), e);
                    } catch (ServiceException e) {
                        ZimbraLog.datasource.warn("An error occurred while importing folder %s", imapFolder.getRemotePath(), e);
                    }
                }
            }

            store.close();
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            if (store != null) {
                try {
                    store.close();
                } catch (MessagingException e) {
                    ZimbraLog.datasource.warn("Error closing IMAP store", e);
                }
            }
        }
    }

    /*
     * Returns list of subfolders, removing duplicates (see bug 26483).
     */
    private static List<IMAPFolder> listFolders(IMAPFolder root, String pattern)
            throws MessagingException {
        Folder[] folders = root.list(pattern);
        List<IMAPFolder> folderList = new ArrayList<IMAPFolder>(folders.length);
        Set<String> names = new HashSet<String>(folders.length);
        for (Folder folder : folders) {
            String name = getNormalizedName(folder.getFullName(), folder.getSeparator());
            if (names.contains(name)) {
                ZimbraLog.datasource.warn("Not importing duplicate folder name: " + folder.getFullName());
            } else {
                names.add(name);
                folderList.add((IMAPFolder) folder);
            }
        }
        return folderList;
    }

    /*
     * Normalize IMAP path name. If specified path is INBOX or a subfolder
     * of INBOX, then return path with INBOX converted to upper case. This
     * is needed to workaround issues where GMail sometimes returns the same
     * folder differing only in the case of the INBOX part (see bug 26483).
     */
    private static String getNormalizedName(String path, char separator) {
        int len = path.length();
        if (len < 5 || !path.substring(0, 5).equalsIgnoreCase("INBOX")) {
            return path;
        }
        if (len == 5) return "INBOX";
        return path.charAt(5) == separator ? "INBOX" + path.substring(5) : path;
    }

    private Status getStatus(final IMAPFolder folder, final String... items)
            throws MessagingException {
        return (Status) folder.doCommand(new IMAPFolder.ProtocolCommand() {
            public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                try {
                    return protocol.status(folder.getFullName(), items);
                } catch (CommandFailedException e) {
                    return null;
                }
            }
        });
    }
    
    private void renameJavaMailFolder(Folder remoteFolder, String jmPath)
    throws MessagingException {
        ZimbraLog.datasource.info("Renaming IMAP folder from %s to %s", remoteFolder.getFullName(), jmPath);
        Folder newName = remoteFolder.getStore().getFolder(jmPath);
        remoteFolder.renameTo(newName);
    }

    private IMAPFolder createJavaMailFolder(Store store, String jmPath)
    throws MessagingException {
        IMAPFolder jmFolder = (IMAPFolder) store.getFolder(jmPath);
        jmFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        return jmFolder;
    }

    private String localPathToRemotePath(DataSource ds, com.zimbra.cs.mailbox.Folder localRootFolder,
                                         com.zimbra.cs.mailbox.Folder localFolder, char separator) {
        // Strip local root from the folder's path.  IMAP paths don't start with "/".
        String rootPath = localRootFolder.getPath();
        if (!rootPath.endsWith("/"))
        	rootPath += "/";
        String folderPath = localFolder.getPath();
        if (folderPath.startsWith(rootPath)) {
            folderPath = folderPath.substring(rootPath.length());
        } else {
            ZimbraLog.datasource.warn("Folder path %s is not under root %s", folderPath, rootPath);
        }

        // Generate IMAP path
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

    private static final Pattern PAT_LEADING_SLASHES = Pattern.compile("^/+");

    /*
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
        if (separator != '/' && (relativePath.indexOf(separator) >=0 || relativePath.indexOf('/') >= 0)) {
            // Make sure none of the elements in the path uses our path separator
        	String[] parts = relativePath.split("\\" + separator);
        	for (int i = 0; i < parts.length; ++i) {
        		//TODO: we need to deal with the case when the separator is not valid in zimbra folder name
        		parts[i] = parts[i].replace('/', separator);
        	}
        	relativePath = StringUtil.join("/", parts);
        }
        
        String zimbraPath = ds.matchKnownLocalPath(relativePath);
        if (zimbraPath == null) {
	        // Remove leading slashes and append to root folder path
	        com.zimbra.cs.mailbox.Folder rootZimbraFolder = mbox.getFolderById(null, ds.getFolderId());
	        Matcher matcher = PAT_LEADING_SLASHES.matcher(relativePath);
	        relativePath = matcher.replaceFirst("");
	        zimbraPath = (rootZimbraFolder.getId() == Mailbox.ID_FOLDER_USER_ROOT ? "" : rootZimbraFolder.getPath()) + "/" + relativePath;
        } else if (zimbraPath.length() == 0) //means to ignore
        	zimbraPath = null;
        return zimbraPath;
    }

    private static IMAPStore getStore(DataSource ds) throws ServiceException {
        DataSource.ConnectionType type = ds.getConnectionType();
        String provider = getProvider(type);
        if (provider == null) {
            throw ServiceException.FAILURE("Invalid connection type: " + type, null);
        }
        try {
            return (IMAPStore) SESSION.getStore(provider);
        } catch (NoSuchProviderException e) {
            throw ServiceException.FAILURE("Unknown provider: " + provider, e);
        }
    }

    private static String getProvider(DataSource.ConnectionType type) {
        switch (type) {
        case cleartext:
            return "imap";
        case ssl:
            return "imaps";
        default:
            return null;
        }
    }

    private boolean hasAttribute(IMAPFolder folder, String attribute) throws MessagingException {
        String[] attrs = folder.getAttributes();
        if (attrs != null) {
            for (String attr : attrs) {
                if (attribute.equalsIgnoreCase(attr)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean importFolder(final ImapFolder trackedFolder)
        throws MessagingException, IOException, ServiceException {
        // Instantiate folders
        com.sun.mail.imap.IMAPFolder remoteFolder =
            (com.sun.mail.imap.IMAPFolder) store.getFolder(trackedFolder.getRemotePath());
        try {
            remoteFolder.open(Folder.READ_WRITE);
        } catch (ReadOnlyFolderException e) {
            ZimbraLog.datasource.info("Unable to open folder %s for write.  Skipping this folder.",
                remoteFolder.getFullName());
            return true;
        }

        // Update next uid for tracked folder data
        trackedFolder.setUidNext(remoteFolder.getUIDNext());
        
        final DataSource ds = getDataSource();
        final Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ds.getAccount());
        final com.zimbra.cs.mailbox.Folder localFolder = mbox.getFolderById(null, trackedFolder.getItemId());

        // Get remote messages
        Message[] msgArray = remoteFolder.getMessages();
        ZimbraLog.datasource.debug("Found %d messages in %s", msgArray.length, remoteFolder.getFullName());
        if (msgArray.length > 0) {
            // Fetch message UID's for reconciliation (UIDL)
            remoteFolder.fetch(msgArray, FETCH_PROFILE);
        }
        
        // Check for duplicate UID's, in case the server sends bad data.
        final Map<Long, IMAPMessage> remoteMsgs = new LinkedHashMap<Long, IMAPMessage>();
        for (Message msg : msgArray) {
            IMAPMessage imapMsg = (IMAPMessage) msg;
            long uid = remoteFolder.getUID(imapMsg);
            if (remoteMsgs.containsKey(uid)) {
                ZimbraLog.datasource.warn("IMAP server sent duplicate UID %d.", uid);
            }
            remoteMsgs.put(uid, imapMsg);
        }
        
        // TODO: Check the UID of the first message to make sure the user isn't
        // importing his own mailbox.
        /*
         * long uid = folder.getUID(msgs[0]); if (isOwnUid(mbox, uid)) {
         * folder.close(false); store.close(); throw
         * ServiceException.INVALID_REQUEST( "User attempted to import messages
         * from his own mailbox", null); }
         */

        // Get stored message ID'S
        final ImapMessageCollection trackedMsgs = DbImapMessage.getImapMessages(mbox, ds, trackedFolder);
        final Set<Integer> localIds = new HashSet<Integer>();
        addMailItemIds(localIds, mbox, localFolder.getId(), MailItem.TYPE_MESSAGE);
        addMailItemIds(localIds, mbox, localFolder.getId(), MailItem.TYPE_CHAT);

        // For servers not supporting UIDVALIDITY, get tracked messages that
        // were appended to mailbox but have no UID. We will match to downloaded
        // messages by checksum.
        final Map<Long, ImapMessage> noUidMsgs = new HashMap<Long, ImapMessage>();
        for (ImapMessage trackedMsg : trackedMsgs.getNoUid()) {
            int id = trackedMsg.getItemId();
            MimeMessage msg = mbox.getMessageById(null, id).getMimeMessage(false);
            long cksum = computeChecksum(msg);
            ZimbraLog.datasource.debug("Local message with no UID (checksum = %x)", cksum);
            noUidMsgs.put(cksum, trackedMsg);
        }

        int numMatched = 0;
        int numUpdated = 0;
        int numDeletedLocally = 0;
        int numAddedRemotely = 0;
        int numDeletedRemotely = 0;
        int numAddedLocally = 0;

        for (long uid : remoteMsgs.keySet()) {
            IMAPMessage remoteMsg = remoteMsgs.get(uid);

            if (trackedMsgs.containsUid(uid)) {
                // We already know about this message.
                ImapMessage trackedMsg = trackedMsgs.getByUid(uid);
                
                if (localIds.contains(trackedMsg.getItemId())) {
                    int localFlags = trackedMsg.getFlags();
                    int trackedFlags = trackedMsg.getTrackedFlags();
                    int remoteFlags = getZimbraFlags(remoteMsg.getFlags());
                    int flags = getNewFlags(localFlags, trackedFlags, remoteFlags);
                    boolean updated = false;
                    if (flags != localFlags) {
                        // Local flags changed
                        mbox.setTags(null, trackedMsg.getItemId(), MailItem.TYPE_MESSAGE, flags,
                            MailItem.TAG_UNCHANGED);
                        updated = true;
                    }
                    if (flags != remoteFlags) {
                        setFlags(remoteMsg, flags);
                        updated = true;
                    }
                    if (flags != trackedFlags) {
                        DbImapMessage.setFlags(mbox, trackedMsg.getItemId(), flags);
                        updated = true;
                    }
                    if (updated) {
                        numUpdated++;
                        ZimbraLog.datasource.debug("Found message with UID %d on both sides; syncing flags: local=%s, tracked=%s, remote=%s, new=%s",
                           uid, Flag.bitmaskToFlags(localFlags), Flag.bitmaskToFlags(trackedFlags),
                           Flag.bitmaskToFlags(remoteFlags), Flag.bitmaskToFlags(flags));
                    } else {
                        numMatched++;
                    }
                    
                    // Remove id from the set, so we know we've already processed it.
                    localIds.remove(trackedMsg.getItemId());
                } else {
                    ZimbraLog.datasource.debug("Message %d was deleted locally.  Deleting remote copy.", uid);
                    remoteMsg.setFlag(Flags.Flag.DELETED, true);
                    numDeletedRemotely++;
                    DbImapMessage.deleteImapMessage(mbox, trackedFolder.getItemId(), trackedMsg.getItemId());
                }
            }
        }

        // Fetch new IMAP messages from remote folder
        long lastUid = trackedMsgs.getMaxUid();
        int startIndex = 0;
        int endIndex = msgArray.length - 1;
        while (startIndex <= endIndex) {
            long startUid = remoteFolder.getUID(msgArray[startIndex]);
            if (startUid <= lastUid) {
                ++startIndex;
                continue;
            }

            int stopIndex = startIndex + FETCH_SIZE - 1;
            stopIndex = stopIndex < endIndex ? stopIndex : endIndex;
            long stopUid = remoteFolder.getUID(msgArray[stopIndex]);

            final AtomicInteger fetchCount = new AtomicInteger();

            // Check for new messages using batch FETCH
            uidFetch.fetch(remoteFolder, startUid + ":" + stopUid, new UidFetch.Handler() {
                public void handleResponse(MessageData md) throws Exception {
                    long uid = md.getUid();
                    ZimbraLog.datasource.debug("Found new remote message %d.  Creating local copy.", uid);
                    if (trackedMsgs.getByUid(uid) != null) {
                        ZimbraLog.datasource.warn("Skipped message with uid = %d because it already exists locally", uid);
                        return;
                    }
                    IMAPMessage msg = remoteMsgs.get(uid);
                    if (msg == null) return;
                    ImapData data = md.getBodySections()[0].getData();
                    // If server does not support UIDVALIDITY and there are
                    // local messages without UID, match downloaded messages
                    // based on checksum and assign UID.
                    if (!noUidMsgs.isEmpty()) {
                        long cksum = computeChecksum(data.getInputStream());
                        ImapMessage trackedMsg = noUidMsgs.remove(cksum);
                        if (trackedMsg != null) {
                            ZimbraLog.datasource.debug(
                                "Matched fetched message to local with checksum %x (UID = %x", cksum, uid);
                            DbImapMessage.setUid(mbox, trackedMsg.getItemId(), uid);
                            localIds.remove(trackedMsg.getItemId());
                        }
                    } else {
                        Date receivedDate = md.getInternalDate();
                        Long time = receivedDate != null ? (Long) receivedDate.getTime() : null;
                        ParsedMessage pm = getParsedMessage(data, time, mbox.attachmentsIndexingEnabled());
                        int flags = getZimbraFlags(msg.getFlags());
                        int msgId = addMessage(mbox, ds, pm, localFolder.getId(), flags);
                        DbImapMessage.storeImapMessage(mbox, trackedFolder.getItemId(), uid, msgId, flags);
                    }
                    fetchCount.incrementAndGet();
                }
            });
            numAddedLocally = fetchCount.intValue();
            startIndex = stopIndex + 1;
        }

        // Remaining local ID's are messages that were not found on the remote server
        boolean runAgain = false;
        for (int localId : localIds) {
            if (trackedMsgs.containsItemId(localId)) {
                ZimbraLog.datasource.debug("Message %d was deleted remotely.  Deleting local copy.", localId);
                ImapMessage tracker = trackedMsgs.getByItemId(localId);
                mbox.delete(null, localId, MailItem.TYPE_UNKNOWN);
                numDeletedLocally++;
                DbImapMessage.deleteImapMessage(mbox, trackedFolder.getItemId(), tracker.getItemId());
            } else {
                ZimbraLog.datasource.debug("Found new local message %d.  Creating remote copy.", localId);
                com.zimbra.cs.mailbox.Message localMsg = mbox.getMessageById(null, localId);
                MimeMessage mimeMsg = localMsg.getMimeMessage(false);
                int flags = localMsg.getFlagBitmask();
                setFlags(mimeMsg, flags);
                AppendUID[] newUids = remoteFolder.appendUIDMessages(new MimeMessage[] { mimeMsg });
                assert newUids != null && newUids.length == 1;
                long uid = newUids[0] != null ? newUids[0].uid : -localId;
                numAddedRemotely++;
                // If no UID returned by APPEND, then store the message locally with a negative UID.
                // When we sync again we will calculate the checksum of remote message and use that
                // to find the corresponding local entry and update the UID with the correct value.
                DbImapMessage.storeImapMessage(mbox, localFolder.getId(), uid, localId, flags);
                if (uid < 0) {
                    runAgain = true; // Sync this folder again since APPEND did not give back a UID
                }
            }
        }

        // Discard local messages with unassigned UIDs that were not matched to
        // any fetched email based on checksum (must have been deleted on remote server).
        for (ImapMessage trackedMsg : noUidMsgs.values()) {
            DbImapMessage.deleteImapMessage(mbox, localFolder.getId(), trackedMsg.getItemId());
        }
        remoteFolder.close(true);

        ZimbraLog.datasource.debug(
            "Import of %s completed.  Matched: %d, updated: %d, added locally: %d, " +
            "deleted locally: %d, added remotely: %d, deleted remotely: %d.%s",
            remoteFolder.getFullName(), numMatched, numUpdated, numAddedLocally,
            numDeletedLocally, numAddedRemotely, numDeletedRemotely, runAgain ? " Rerun import." : "");
        
        return !runAgain;
    }

    private int addMessage(Mailbox mbox, DataSource ds, ParsedMessage pm, int folderId, int flags) throws ServiceException, IOException {
    	com.zimbra.cs.mailbox.Message msg = null;
    	SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext();
        if (folderId == Mailbox.ID_FOLDER_INBOX) {
        	try {
	            msg = RuleManager.getInstance().applyRules(mbox.getAccount(), mbox, pm, pm.getRawSize(), ds.getEmailAddress(), sharedDeliveryCtxt);
	            if (msg != null)
	            	mbox.setTags(null, msg.getId(), MailItem.TYPE_MESSAGE, flags, MailItem.TAG_UNCHANGED);
	            else
	            	return 0;  //null if DISCARD
        	} catch (Throwable t) {
        		ZimbraLog.datasource.warn("failed applying filter rules", t);
        	}
        }
        if (msg == null)
        	msg = mbox.addMessage(null, pm, folderId, false, flags, null);
        return msg.getId();
    }

    private static ParsedMessage getParsedMessage(ImapData id,
                                                  Long receivedDate,
                                                  boolean indexAttachments)
            throws IOException, MessagingException {
        if (id.isLiteral()) {
            Literal lit = (Literal) id;
            File f = lit.getFile();
            if (f != null) {
                return new ParsedMessage(f, receivedDate, indexAttachments);
            }
        }
        return new ParsedMessage(id.getBytes(), receivedDate, indexAttachments);
    }
    
    /**
     * Adds item id's to the given set.
     */
    private void addMailItemIds(Set<Integer> idSet, Mailbox mbox, int folderId, byte type)
    throws ServiceException {
        int[] ids = mbox.listItemIds(null, type, folderId);
        for (int id : ids) {
            idSet.add(id);
        }
    }

    private static final Flags.Flag[] IMAP_FLAGS = {
        Flags.Flag.ANSWERED, Flags.Flag.DELETED, Flags.Flag.DRAFT,
        Flags.Flag.FLAGGED, Flags.Flag.SEEN };

    private static final int[] ZIMBRA_FLAG_BITMASKS = {
        Flag.BITMASK_REPLIED, Flag.BITMASK_DELETED, Flag.BITMASK_DRAFT,
        Flag.BITMASK_FLAGGED, Flag.BITMASK_UNREAD };

    private int getNewFlags(int localFlags, int trackedFlags, int remoteFlags) {
        return trackedFlags & (localFlags & remoteFlags) |
              ~trackedFlags & (localFlags | remoteFlags);
    }

    /*
     * Sets flags on IMAP message.
     */
    private void setFlags(Message msg, int newFlags) throws MessagingException {
        Flags remoteFlags = msg.getFlags();
        Flags flagsToAdd = null;
        Flags flagsToRemove = null;
        for (int i = 0; i < IMAP_FLAGS.length; i++) {
            boolean isSet = (newFlags & ZIMBRA_FLAG_BITMASKS[i]) != 0;
            Flags.Flag f = IMAP_FLAGS[i];
            if (f == Flags.Flag.SEEN) {
                // IMAP uses "seen" but we use "unread"
                isSet = !isSet;
            }
            if (remoteFlags.contains(f)) {
                if (!isSet) {
                    if (flagsToRemove == null) {
                        flagsToRemove = new Flags();
                    }
                    flagsToRemove.add(f);
                }
            } else if (isSet) {
                if (flagsToAdd == null) {
                    flagsToAdd = new Flags();
                }
                flagsToAdd.add(f);
            }
        }
        if (flagsToAdd != null) {
            msg.setFlags(flagsToAdd, true);
        }
        if (flagsToRemove != null) {
            msg.setFlags(flagsToRemove, false);
        }
    }

    // Get ZIMBRA mail flags from IMAP flags
    private int getZimbraFlags(Flags flags) throws MessagingException {
        int zflags = 0;
        for (int i = 0; i < IMAP_FLAGS.length; i++) {
            Flags.Flag f = IMAP_FLAGS[i];
            boolean isSet = flags.contains(f);
            if (f == Flags.Flag.SEEN) {
                // IMAP uses "seen" but we use "unread"
                isSet = !isSet;
            }
            if (isSet) {
                zflags |= ZIMBRA_FLAG_BITMASKS[i];
            }
        }
        return zflags;
    }
    
    private static long computeChecksum(MimeMessage msg) throws IOException, MessagingException {
        final CRC32 crc = new CRC32();
        msg.writeTo(new OutputStream() {
            public void write(int b) { crc.update(b); }
            public void write(byte[] b, int off, int len) { crc.update(b, off, len); }
        });
        return crc.getValue();
    }

    private long computeChecksum(InputStream is) throws IOException {
        CRC32 crc = new CRC32();
        int len;
        while ((len = is.read(buf)) != -1) {
            crc.update(buf, 0, len);
        }
        return crc.getValue();
    }

    public Store getStore() {
        return store;
    }
}
