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
package com.zimbra.cs.datasource;
                             
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.cs.mailclient.imap.ImapCapabilities;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.DataHandler;
import com.zimbra.cs.mailclient.imap.ImapData;
import com.zimbra.cs.mailclient.imap.IDInfo;
import com.zimbra.cs.mailclient.auth.Authenticator;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.RemoteServiceException;
import com.zimbra.common.datasource.SyncState;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.StringUtil;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class ImapSync extends MailItemImport {
    private final ImapConnection connection;
    private Folder localRootFolder;
    private char delimiter; // Default IMAP hierarchy delimiter (0 if flat)
    private ImapFolderCollection trackedFolders;
    private Map<Integer, ImapFolderSync> syncedFolders;
    // Optional mail client authenticator (default is plaintext login)
    private Authenticator authenticator;
    private Pattern ILLEGAL_FOLDER_CHARS = Pattern.compile("[\\:\\*\\?\\\"\\<\\>\\|]");

    private static final Log LOG = ZimbraLog.datasource;

    public ImapSync(DataSource ds) throws ServiceException {
        super(ds);
        connection = new ImapConnection(newImapConfig(ds));
    }

    public ImapConnection getConnection() { return connection; }

    public void setAuthenticator(Authenticator auth) {
        authenticator = auth;
    }
    
    public ImapFolderCollection getTrackedFolders() {
        return trackedFolders;
    }

    public ImapFolder createFolderTracker(int itemId, String localPath,
        String remotePath, long uidValidity) throws ServiceException {
        ImapFolder tracker = new ImapFolder(dataSource, itemId, remotePath,
            localPath, uidValidity);
        
        tracker.add();
        trackedFolders.add(tracker);
        return tracker;
    }

    public void deleteFolderTracker(ImapFolder tracker) throws ServiceException {
        tracker.delete();
        trackedFolders.remove(tracker);
    }

    public ImapFolderSync getSyncedFolder(int folderId) {
        return syncedFolders.get(folderId);
    }

    public static ImapConfig newImapConfig(DataSource ds) {
        ImapConfig config = new ImapConfig();
        config.setHost(ds.getHost());
        config.setPort(ds.getPort());
        config.setAuthenticationId(ds.getUsername());
        config.setSecurity(getSecurity(ds.getConnectionType()));
        // bug 37982: Disable use of LITERAL+ due to problems with Yahoo IMAP.
        // Avoiding LITERAL+ also gives servers a chance to reject uploaded
        // messages that are too big, since the server must send a continuation
        // response before the literal data can be sent.
        config.setUseLiteralPlus(false);
        if (ds.isDebugTraceEnabled()) {
            config.setDebug(true);
            enableTrace(ds, config);
        }
        config.setSocketFactory(SocketFactories.defaultSocketFactory());
        config.setSSLSocketFactory(SocketFactories.defaultSSLSocketFactory());
        config.setConnectTimeout(ds.getConnectTimeout(LC.javamail_imap_timeout.intValue()));
        config.setReadTimeout(ds.getReadTimeout(LC.javamail_imap_timeout.intValue()));
        return config;
    }

    public synchronized void test() throws ServiceException {
        validateDataSource();
        enableTrace(dataSource, connection.getImapConfig());
        try {
            connect();
        } finally {
            connection.close();
        }
    }

    private static void enableTrace(DataSource ds, ImapConfig config) {
        config.setTrace(true);
        config.setMaxLiteralTraceSize(ds.getMaxTraceSize());
        PrintStream ps = ds.isOffline() ?
            new PrintStream(new LogOutputStream(ZimbraLog.imap), true) : System.out;
        config.setTraceStream(ps);
    }

    public synchronized void importData(List<Integer> folderIds, boolean fullSync)
        throws ServiceException {
        validateDataSource();
        connect();
        int folderId = dataSource.getFolderId();
        localRootFolder = getMailbox().getFolderById(null, folderId);
        connection.setDataHandler(new FetchDataHandler());
        try {
            syncFolders(folderIds, fullSync);
            connection.logout();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Folder sync failed", e);
        } finally {
            connection.close();
        }
    }

    // Handler for fetched message data which uses ParsedMessage to stream
    // the message data to disk if necessary.
    private static class FetchDataHandler implements DataHandler {
        public Object handleData(ImapData data) throws Exception {
            try {
                return MessageContent.read(data.getInputStream(), data.getSize());
            } catch (OutOfMemoryError e) {
                Zimbra.halt("Out of memory");
                return null;
            }
        }
    }

    protected void connect() throws ServiceException {
        if (!connection.isClosed()) return;
        try {
            connection.connect();
            try {
                if (authenticator != null) {
                    connection.authenticate(authenticator);
                } else {
                    connection.login(dataSource.getDecryptedPassword());
                }
            } catch (CommandFailedException e) {
                throw new LoginException(e.getError());
            }
            checkImportingSelf();
            delimiter = connection.getDelimiter();
        } catch (ServiceException e) {
            connection.close();
            throw e;
        } catch (Exception e) {
            connection.close();
            throw ServiceException.FAILURE(
                "Unable to connect to IMAP server: " + dataSource, e);
        }
    }

    private void checkImportingSelf() throws IOException, ServiceException {
        if (dataSource.isOffline() || !connection.hasCapability(ImapCapabilities.ID))
            return;
        try {
            IDInfo id = connection.id();
            if ("Zimbra".equalsIgnoreCase(id.getName())) {
                String user = id.get("user");
                if (user != null && user.equals(dataSource.getAccount().getName())) {
                    throw ServiceException.INVALID_REQUEST(
                        "User attempted to import messages from his/her own mailbox", null);
                }
            }
        } catch (CommandFailedException e) {
            // Skip check if ID command fails
        }
    }

    public void checkIsEnabled() throws ServiceException {
        if (!getDataSource().isManaged()) {
            throw ServiceException.FAILURE(
                "Import aborted because data source has been deleted or disabled", null);
        }
    }
    
    private void syncFolders(List<Integer> folderIds, boolean fullSync)
        throws ServiceException, IOException {
        if (dataSource.isOffline()) {
            getMailbox().beginTrackingSync();
        }
        // For offline if full sync then automatically re-enable sync on Inbox
        if (dataSource.isOffline() && fullSync) {
            SyncUtil.setSyncEnabled(mbox, Mailbox.ID_FOLDER_INBOX, true);
        }
        trackedFolders = ImapFolder.getFolders(dataSource);
        syncedFolders = new LinkedHashMap<Integer, ImapFolderSync>();
        syncRemoteFolders(ImapUtil.listFolders(connection, "*"));
        syncLocalFolders(getLocalFolders());
        syncMessages(folderIds, fullSync);
        finishSync();
    }

    private List<Folder> getLocalFolders() {
        List<Folder> folders = localRootFolder.getSubfolderHierarchy();
        List<Folder> mailFolders = new ArrayList<Folder>(folders.size());
        for (Folder f : folders)
            if (f.getDefaultView() == MailItem.TYPE_MESSAGE)
                mailFolders.add(f);
        // Reverse order of local folders to ensure that children are
        // processed before parent folders. This avoids problems when
        // deleting folders.
        Collections.reverse(mailFolders);
        return mailFolders;
    }

    private void syncRemoteFolders(List<ListData> folders) throws ServiceException {
        for (ListData ld : folders) {
            checkIsEnabled();
            try {
                ImapFolderSync ifs = new ImapFolderSync(this);
                ImapFolder tracker = ifs.syncFolder(ld);
                if (tracker != null) {
                    syncedFolders.put(tracker.getItemId(), ifs);
                }
            } catch (Exception e) {
                syncFailed(ld.getMailbox(), e);
            }
        }
    }

    private void syncLocalFolders(List<Folder> folders) throws ServiceException {
        for (Folder folder : folders) {
            checkIsEnabled();
            int id = folder.getId();
            if (id != localRootFolder.getId() && !syncedFolders.containsKey(id)) {
                try {
                    folder = getFolder(id);
                    if (folder != null) {
                        ImapFolderSync ifs = new ImapFolderSync(this);
                        ImapFolder tracker = ifs.syncFolder(folder);
                        if (tracker != null) {
                            syncedFolders.put(tracker.getItemId(), ifs);
                        }
                    }
                } catch (Exception e) {
                    syncFailed(folder.getPath(), e);
                }
            }
        }
    }

    private void syncMessages(List<Integer> folderIds, boolean fullSync)
        throws ServiceException {
        // If folder ids specified, then only sync messages for specified
        // folders, otherwise sync messages for all folders.
        int lastModSeq = getMailbox().getLastChangeID();
        for (ImapFolderSync ifs : syncedFolders.values()) {
            checkIsEnabled();
            LocalFolder folder = ifs.getLocalFolder();
            int folderId = folder.getId();
            try {
                if (folderIds == null || folderIds.contains(folderId) ||
                    hasLocalChanges(folderId, lastModSeq)) {
                    ifs.syncMessages(fullSync);
                }
            } catch (Exception e) {
                syncFailed(folder.getPath(), e);
            }
        }
    }

    private boolean hasLocalChanges(int folderId, int lastModSeq) {
        SyncState ss = dataSource.getSyncState(folderId);
        return ss == null || ss.getLastModSeq() < lastModSeq;
    }

    private void finishSync() throws ServiceException {
        // Append new IMAP messages for folders which have been synchronized.
        // This is done after IMAP messages have been deleted in order to
        // avoid problems when local messages are moved between folders
        // (see bug 27924).
        for (ImapFolderSync ifs : syncedFolders.values()) {
            try {
                ifs.finishSync();
            } catch (Exception e) {
                LocalFolder folder = ifs.getLocalFolder();
                syncFailed(folder.getPath(), e);
            }
        }
    }

    public Folder getFolder(int id) throws ServiceException {
        try {
            return localRootFolder.getMailbox().getFolderById(null, id);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }

    private void syncFailed(String path, Exception e)
        throws ServiceException {
        String error = String.format("Synchronization of folder '%s' failed", path);
        LOG.error(error, e);
        if (!canContinue(e)) {
            throw ServiceException.FAILURE(error, e);
        }
    }

    /*
     * Returns true if synchronization of other folders can continue following
     * the specified sync error.
     */
    private boolean canContinue(Throwable e) {
        if (!dataSource.isOffline()) {
            return false;
        } else if (e instanceof RemoteServiceException) {
            return false;
        } else if (e instanceof ServiceException) {
            Throwable cause = e.getCause();
            return cause == null || canContinue(cause);
        } else {
            return e instanceof CommandFailedException;
        }
    }
    
    /*
     * Returns the path to the Zimbra folder that stores messages for the given
     * IMAP folder. The Zimbra folder has the same path as the IMAP folder,
     * but is relative to the root folder specified by the DataSource.
     */
    String getLocalPath(ListData ld) throws ServiceException {
        String remotePath = ld.getMailbox();
        char localDelimiter = ld.getDelimiter();
        String relativePath = ld.getMailbox();
        
        if (localDelimiter != '/' && (remotePath.indexOf(localDelimiter) >= 0 ||
                                 remotePath.indexOf('/') >= 0)) {
            // Change remote path to use our separator
            String[] parts = remotePath.split("\\" + localDelimiter);
            for (int i = 0; i < parts.length; i++) {
                // TODO Handle case where separator is not valid in Zimbra folder name
                parts[i] = parts[i].replace('/', localDelimiter);
            }
            relativePath = StringUtil.join("/", parts);
        }
        relativePath = ILLEGAL_FOLDER_CHARS.matcher(relativePath).replaceAll("_");

        if (dataSource.ignoreRemotePath(relativePath)) {
            return null; // Do not synchronize folder
        }

        String localPath = dataSource.mapRemoteToLocalPath(relativePath);
        if (localPath == null) {
            // Remove leading slashes and append to root folder
            while (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            if (localRootFolder.getId() == com.zimbra.cs.mailbox.Mailbox.ID_FOLDER_USER_ROOT) {
                localPath = "/" + relativePath;
            } else {
                localPath = localRootFolder.getPath() + "/" + relativePath;
            }
        }

        if (isUniqueLocalPathNeeded(localPath)) {
            int count = 1;
            for (;;) {
                String path = String.format("%s-%d", localPath, count++);
                if (LocalFolder.fromPath(mbox, path) == null) {
                    return path;
                }
            }
        } else {
            return localPath;
        }
    }

    /*
     * When mapping a new remote folder, check if original local path can be
     * used or a new unique name must be generated. A unique name must be
     * generated if the local path is already associated with a tracked
     * folder, or the local path refers to a system folder that is not one
     * of the known IMAP folders (e.g. INBOX, Sent). This ensures that a
     * remote folder named 'Contacts', for example, will get mapped to a unique
     * name since a local non-IMAP system folder already exists with the same
     * name.
     */
    private boolean isUniqueLocalPathNeeded(String localPath) throws ServiceException {
        LocalFolder lf = LocalFolder.fromPath(mbox, localPath);
        return lf != null && (
            trackedFolders.getByItemId(lf.getId()) != null ||
            lf.isSystem() && !lf.isKnown());
    }

    /*
     * Returns the IMAP path name for the specified local folder. Returns null
     * if the folder should not be imported.
     */
    String getRemotePath(Folder folder) throws ServiceException {
        if (!localRootFolder.isDescendant(folder)) {
            return null;
        }
        String imapPath = dataSource.mapLocalToRemotePath(folder.getPath());
        if (imapPath == null) {
            if (folder.getId() < com.zimbra.cs.mailbox.Mailbox.FIRST_USER_ID) {
                return null;
            }
            // Determine imap path from folder path
            imapPath = folder.getPath();
            // Strip root path from folder path
            String rootPath = localRootFolder.getPath();
            if (!rootPath.endsWith("/")) {
                rootPath += "/";
            }
            if (!imapPath.startsWith(rootPath)) {
                return null; // Folder no longer data source root
            }
            imapPath = imapPath.substring(rootPath.length());
        }
        // Handling for IMAP folder delimiter different from Zimbra's
        if (delimiter != 0 && delimiter != '/') {
            String[] parts = imapPath.split("/");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].replace(delimiter, '/');
            }
            imapPath = StringUtil.join(String.valueOf(delimiter), parts);
        }
        return imapPath;
    }
}
