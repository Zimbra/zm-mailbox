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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.CustomSSLSocketFactory;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
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
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.mailtest.UidFetch;
import com.zimbra.cs.mailtest.Literal;

public class ImapImport implements MailItemImport {

    private static final long TIMEOUT = 20 * Constants.MILLIS_PER_SECOND;

    private static Session sSession;
    private static Session sSelfSignedCertSession;
    private static FetchProfile FETCH_PROFILE;

    private static final boolean FAST_FETCH =
        LC.data_source_fast_fetch.booleanValue();
    
    static {
    	String idExt = "(\"vendor\" \"Zimbra\" \"os\" \"" + System.getProperty("os.name") +
    	               "\" \"os-version\" \"" + System.getProperty("os.version") + "\" \"guid\" \"" + BuildInfo.TYPE + "\")";
    	
        Properties props = new Properties();
        props.setProperty("mail.imap.connectiontimeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imap.timeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imaps.connectiontimeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imaps.timeout", Long.toString(TIMEOUT));    	
		props.setProperty("mail.imaps.socketFactory.class", CustomSSLSocketFactory.class.getName());
        props.setProperty("mail.imaps.socketFactory.fallback", "false");
        if (idExt != null) {
        	props.setProperty("mail.imap.idextension", idExt);
        	props.setProperty("mail.imaps.idextension", idExt);
        }
        sSession = Session.getInstance(props);
        if (LC.javamail_imap_debug.booleanValue())
        	sSession.setDebug(true);

        Properties sscProps = new Properties();
        sscProps.setProperty("mail.imaps.connectiontimeout", Long.toString(TIMEOUT));
        sscProps.setProperty("mail.imaps.timeout", Long.toString(TIMEOUT));    	
        sscProps.setProperty("mail.imaps.socketFactory.class", DummySSLSocketFactory.class.getName());
        sscProps.setProperty("mail.imaps.socketFactory.fallback", "false");
        if (idExt != null)
        	props.setProperty("mail.imaps.idextension", idExt);
        sSelfSignedCertSession = Session.getInstance(sscProps);
        if (LC.javamail_imap_debug.booleanValue())
        	sSelfSignedCertSession.setDebug(true);

        FETCH_PROFILE = new FetchProfile();
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.UID);
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.FLAGS);
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.ENVELOPE);
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
            error = SystemUtil.getInnermostException(e).getMessage();
            error = error != null ? error : e.toString();
        }
        return error;
    }
    
    private boolean hasAttribute(IMAPFolder folder, String attribute) throws MessagingException {
    	String[] attrs = folder.getAttributes();
    	if (attrs != null)
    		for (String attr : attrs)
    			if (attribute.equalsIgnoreCase(attr))
    				return true;
    	return false;
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
            //when deleting folders, especially remoted folders, we delete children before parents
            //to avoid complications.  so we want to sort in fullname length descending order.
            Arrays.sort(remoteFolders, new Comparator<Folder>() {
				public int compare(Folder o1, Folder o2) {
					return o2.getFullName().length() - o1.getFullName().length();
				}
            });
            for (int i = 0; i < remoteFolders.length; i++) {
                IMAPFolder remoteFolder = (IMAPFolder) remoteFolders[i];
                
                long remoteUvv = 0;
                if (!hasAttribute(remoteFolder, "\\Noselect"))
                	remoteUvv = remoteFolder.getUIDValidity();
                
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
                        DbImapFolder.deleteImapFolder(mbox, ds, folderTracker);
                    }
                    
                    if (folderTracker.getUidValidity() == null) {
                        // Migrate old data created before we added the uid_validity column
                        ZimbraLog.datasource.info("Initializing UIDVALIDITY of %s to %d", remoteFolder.getFullName(), remoteUvv);
                        folderTracker.setUidValidity(remoteUvv);
                        DbImapFolder.updateImapFolder(folderTracker);
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
                                DbImapFolder.updateImapFolder(folderTracker);
                            } else {
                                // Folder was moved outside the data source root, or folder setting is changed to "not to sync"
                                // Treat as a delete.
                                ZimbraLog.datasource.info("Local folder %s was renamed to %s and moved outside the data source root.",
                                    folderTracker.getLocalPath(), localFolder.getPath());
                                DbImapFolder.deleteImapFolder(mbox, ds, folderTracker);
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
                                copyFlags(localMsg.getFlagBitmask(), mimeMsg);
                                AppendUID[] newUids = remoteFolder.appendUIDMessages(new MimeMessage[] { mimeMsg });
                                if (newUids != null && newUids.length == 1 && newUids[0] != null && newUids[0].uid > 0)
                                	DbImapMessage.storeImapMessage(mbox, localFolder.getId(), newUids[0].uid, id);
                            }
                            
                            // Empty local folder so that it will be resynced later and store the new UIDVALIDITY value.
                            mbox.emptyFolder(null, localFolder.getId(), false);
                            folderTracker.setUidValidity(remoteUvv);
                            DbImapFolder.updateImapFolder(folderTracker);
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
	                    folderTracker = DbImapFolder.createImapFolder(mbox, ds, localFolder.getId(),
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
	                    DbImapFolder.deleteImapFolder(mbox, ds, imapFolder);
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
	                        DbImapFolder.deleteImapFolder(mbox, ds, imapFolder);
	                        imapFolders.remove(imapFolder);
	                    }
                	}
                } else {
                    String jmPath = localPathToRemotePath(ds, localRootFolder, zimbraFolder, remoteRootFolder.getSeparator());
                    if (jmPath != null) { //null means don't sync up
                    	ZimbraLog.datasource.info("Found new local folder %s.  Creating remote folder %s.", zimbraFolder.getPath(), jmPath);
	                    IMAPFolder jmFolder = createJavaMailFolder(store, jmPath);
	                    imapFolder = DbImapFolder.createImapFolder(mbox, ds, zimbraFolder.getId(),
	                        zimbraFolder.getPath(), jmFolder.getFullName(), jmFolder.getUIDValidity());
	                    imapFolders.add(imapFolder);
                    }
                }
            }

            // Import data for all ImapFolders that exist on both sides
            for (ImapFolder imapFolder : imapFolders) {
            	if (!ds.isSyncEnabled(imapFolder.getLocalPath()))
            		continue;
            	
                ZimbraLog.datasource.info("Importing from IMAP folder %s to local folder %s",
                    imapFolder.getRemotePath(), imapFolder.getLocalPath());
                if (imapFolder.getUidValidity() != 0) {
	                try {
	                	for (int i = 0; i < 3; ++i) //at most run 3 times on a single folder import to prevent a dead loop
	                		if (importFolder(account, ds, store, imapFolder))
	                			break;
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
        }
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

    private boolean importFolder(Account account, DataSource ds, Store store, final ImapFolder trackedFolder)
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
        final Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
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
        Set<Integer> localIds = new HashSet<Integer>();
        addMailItemIds(localIds, mbox, localFolder.getId(), MailItem.TYPE_MESSAGE);
        addMailItemIds(localIds, mbox, localFolder.getId(), MailItem.TYPE_CHAT);
        
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
                    ZimbraLog.datasource.debug("Found message with UID %d on both sides.  Syncing flags.", uid);
                    // We currently only sync flags from remote to local, not in both directions.
                    int appliedFlags = applyFlagsToBitfield(remoteMsg, trackedMsg.getFlags());
                    if (appliedFlags != trackedMsg.getFlags()) {
                        // Flags changed
                        mbox.setTags(null, trackedMsg.getItemId(), MailItem.TYPE_MESSAGE, appliedFlags,
                            MailItem.TAG_UNCHANGED);
                        numUpdated++;
                    } else {
                        numMatched++;
                    }
                    
                    // Remove id from the set, so we know we've already processed it.
                    localIds.remove(trackedMsg.getItemId());
                } else {
                    ZimbraLog.datasource.debug("Message %d was deleted locally.  Deleting remote copy.", uid);
                    remoteMsg.setFlag(Flags.Flag.DELETED, true);
                    numDeletedRemotely++;
                    DbImapMessage.deleteImapMessage(mbox, trackedFolder.getItemId(), trackedMsg.getUid());
                }
            } else if (!FAST_FETCH) {
                ZimbraLog.datasource.debug("Found new remote message %d.  Creating local copy.", uid);
                ParsedMessage pm = null;
                if (remoteMsg.getSentDate() != null) {
                    // Set received date to the original message's date
                    pm = new ParsedMessage(remoteMsg, remoteMsg.getSentDate().getTime(),
                        mbox.attachmentsIndexingEnabled());
                } else {
                    pm = new ParsedMessage(remoteMsg, mbox.attachmentsIndexingEnabled());
                }
                int flags = applyFlagsToBitfield(remoteMsg, 0);
                com.zimbra.cs.mailbox.Message zimbraMsg =
                    mbox.addMessage(null, pm, localFolder.getId(), false, flags, null);
                numAddedLocally++;
                DbImapMessage.storeImapMessage(mbox, trackedFolder.getItemId(), uid, zimbraMsg.getId());
            }
        }

        if (FAST_FETCH) {
            // Check for new messages using batch UID FETCH
            long fromUid = trackedMsgs.getMaxUid() + 1;
            long toUid = remoteFolder.getUIDNext();
            final AtomicInteger fetchCount = new AtomicInteger();

            UidFetch.fetch(remoteFolder, fromUid + ":" + toUid, new UidFetch.Handler() {
                public void handleResponse(Literal lit, long uid) throws Exception {
                    ZimbraLog.datasource.debug("Found new remote message %d.  Creating local copy.", uid);
                    if (trackedMsgs.getByUid(uid) != null) {
                        ZimbraLog.datasource.warn("Skipped message with uid = %d because it already exists locally", uid);
                        return;
                    }
                    IMAPMessage msg = remoteMsgs.get(uid);
                    if (msg == null) return;
                    Long date = msg.getSentDate() != null ? msg.getSentDate().getTime() : null;
                    boolean indexingEnabled = mbox.attachmentsIndexingEnabled();
                    ParsedMessage pm = lit.getFile() != null ?
                        new ParsedMessage(lit.getFile(), date, indexingEnabled) :
                        new ParsedMessage(lit.getBytes(), date, indexingEnabled);
                    int flags = applyFlagsToBitfield(msg, 0);
                    com.zimbra.cs.mailbox.Message zimbraMsg =
                        mbox.addMessage(null, pm, localFolder.getId(), false, flags, null);
                    fetchCount.incrementAndGet();
                    DbImapMessage.storeImapMessage(mbox, trackedFolder.getItemId(), uid, zimbraMsg.getId());
                }
            });
            numAddedLocally = fetchCount.intValue();
        }

        // Remaining local ID's are messages that were not found on the remote server
        boolean runAgain = false;
        for (int localId : localIds) {
            if (trackedMsgs.containsItemId(localId)) {
                ZimbraLog.datasource.debug("Message %d was deleted remotely.  Deleting local copy.", localId);
                ImapMessage tracker = trackedMsgs.getByItemId(localId);
                mbox.delete(null, localId, MailItem.TYPE_UNKNOWN);
                numDeletedLocally++;
                DbImapMessage.deleteImapMessage(mbox, trackedFolder.getItemId(), tracker.getUid());
            } else {
                ZimbraLog.datasource.debug("Found new local message %d.  Creating remote copy.", localId);
                com.zimbra.cs.mailbox.Message localMsg = mbox.getMessageById(null, localId);
                MimeMessage mimeMsg = localMsg.getMimeMessage(false);
                copyFlags(localMsg.getFlagBitmask(), mimeMsg);
                AppendUID[] newUids = remoteFolder.appendUIDMessages(new MimeMessage[] { mimeMsg });
                numAddedRemotely++;
                if (newUids != null && newUids.length == 1 && newUids[0] != null && newUids[0].uid > 0)
                	DbImapMessage.storeImapMessage(mbox, localFolder.getId(), newUids[0].uid, localId);
                else {
                    //remote doesn't give us a UID in response, so we delete first and wait for the bounce back
                	//TODO: this is pretty inefficient, so find another way!
                    mbox.delete(null, localId, MailItem.TYPE_UNKNOWN);
                    runAgain = true; //sync this folder again
                }
            }
        }
        
        remoteFolder.close(true);

        ZimbraLog.datasource.debug(
            "Import of %s completed.  Matched: %d, updated: %d, added locally: %d, " +
            "deleted locally: %d, added remotely: %d, deleted remotely: %d.%s",
            remoteFolder.getFullName(), numMatched, numUpdated, numAddedLocally,
            numDeletedLocally, numAddedRemotely, numDeletedRemotely, runAgain ? " Rerun import." : "");
        
        return !runAgain;
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

    private static final Flags.Flag[] IMAP_FLAGS = { Flags.Flag.ANSWERED, Flags.Flag.DELETED,
        Flags.Flag.DRAFT, Flags.Flag.FLAGGED, Flags.Flag.SEEN };

    private static final int[] ZIMBRA_FLAG_BITMASKS = { Flag.BITMASK_REPLIED, Flag.BITMASK_DELETED,
        Flag.BITMASK_DRAFT, Flag.BITMASK_FLAGGED, Flag.BITMASK_UNREAD };

    /**
     * Applies the flags from a JavaMail message to the given flag bitfield.
     */
    private int applyFlagsToBitfield(Message remoteMsg, int flagBitfield)
    throws MessagingException {
        for (int i = 0; i < IMAP_FLAGS.length; i++) {
            Flags.Flag imapFlag = IMAP_FLAGS[i];
            boolean isSet = remoteMsg.isSet(imapFlag);
            if (imapFlag == Flags.Flag.SEEN) {
                // IMAP uses "seen", we use "unread"
                isSet = !isSet;
            }
            if (isSet) {
                flagBitfield |= ZIMBRA_FLAG_BITMASKS[i];
            } else {
                flagBitfield &= ~ZIMBRA_FLAG_BITMASKS[i];
            }
        }

        return flagBitfield;
    }
    
    /**
     * Sets flags on a JavaMail message, based on a local message's flags.
     */
    private void copyFlags(long localFlags, Message remoteMsg)
    throws MessagingException {
        for (int i = 0; i < IMAP_FLAGS.length; i++) {
            int bitmask = ZIMBRA_FLAG_BITMASKS[i];
            Flags.Flag imapFlag = IMAP_FLAGS[i];
            boolean isSet = ((bitmask & localFlags) > 0);
            if (imapFlag == Flags.Flag.SEEN) {
                // IMAP uses "seen", we use "unread"
                isSet = !isSet;
            }
            remoteMsg.setFlag(imapFlag, isSet);
        }
    }
}
