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
                             
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.IDInfo;
import com.zimbra.cs.mailclient.imap.ImapCapabilities;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.util.ZimbraApplication;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.CustomSSLSocketFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class ImapSync extends AbstractMailItemImport {
    private final ImapConnection connection;
    private final Folder localRootFolder;
    private char delimiter; // Default IMAP hierarchy delimiter (0 if flat)
    private ImapFolderCollection trackedFolders;
    private Map<Integer, ImapFolderSync> syncedFolders;
    private boolean fullSync;

    private static final boolean DEBUG = true; // LC.javamail_imap_debug.booleanValue();
    
    private static final Log LOG = ZimbraLog.datasource;
    static {
        if (DEBUG) LOG.setLevel(Log.Level.debug);
    }

    private static final IDInfo ID_INFO = new IDInfo();
    static {
        ID_INFO.setVendor("Zimbra");
        ID_INFO.setOs(System.getProperty("os.name"));
        ID_INFO.setOsVersion(System.getProperty("os.version"));
        ID_INFO.put("guid", ZimbraApplication.getInstance().getId());
    }


    public ImapSync(DataSource ds) throws ServiceException {
        super(ds);
        connection = new ImapConnection(getImapConfig(ds));
        localRootFolder = ds.getMailbox().getFolderById(null, ds.getFolderId());
    }

    public ImapConnection getConnection() { return connection; }
    
    private static ImapConfig getImapConfig(DataSource ds) {
        ImapConfig config = new ImapConfig();
        config.setHost(ds.getHost());
        config.setPort(ds.getPort());
        config.setAuthenticationId(ds.getUsername());
        config.setMaxLiteralMemSize(LC.data_source_max_message_memory_size.intValue());
        config.setTlsEnabled(LC.javamail_imap_enable_starttls.booleanValue());
        config.setSslEnabled(ds.isSslEnabled());
        config.setDebug(DEBUG);
        config.setTrace(DEBUG || ds.isDebugTraceEnabled());
        // config.setRawMode(true);
        if (LC.data_source_trust_self_signed_certs.booleanValue()) {
            config.setSSLSocketFactory(new DummySSLSocketFactory());
        } else {
            config.setSSLSocketFactory(new CustomSSLSocketFactory());
        }
        return config;
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
            connection.close();
        }
        return null;
    }
    
    public synchronized void importData(boolean fullSync)
        throws ServiceException {
        validateDataSource();
        connect();
        this.fullSync = fullSync;
        try {
            syncFolders();
            connection.logout();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Folder sync failed", e);
        } finally {
            connection.close();
        }
    }

    private void connect() throws ServiceException {
        if (!connection.isClosed()) return;
        try {
            connection.connect();
            if (connection.hasCapability(ImapCapabilities.ID)) {
                try {
                    IDInfo id = connection.id(ID_INFO);
                    LOG.info("Server ID: " + id);
                } catch (CommandFailedException e) {
                    LOG.warn(e.getMessage());
                }
            }
            connection.login(dataSource.getDecryptedPassword());
            delimiter = connection.getDelimiter();
        } catch (IOException e) {
            connection.close();
            throw ServiceException.FAILURE(
                "Unable to connect to IMAP server: " + dataSource, e);
        }
    }

    private void syncFolders() throws ServiceException, IOException {
        trackedFolders = dataSource.getImapFolders();
        syncedFolders = new HashMap<Integer, ImapFolderSync>();
        // Synchronize local and remote folders
        for (ListData ld : ImapUtil.listFolders(connection)) {
            syncRemoteFolder(ld);
        }
        // Reverse order of local folders to ensure that children are processed
        // before parent folders. This avoids problems when deleting folders.
        List<Folder> folders = localRootFolder.getSubfolderHierarchy();
        Collections.reverse(folders);
        for (Folder folder : folders) {
            int id = folder.getId();
            if (id != localRootFolder.getId() && !syncedFolders.containsKey(id)) {
                syncLocalFolder(folder);
            }
        }
        // Append new IMAP messages for folders which have been synchronized.
        // This is done after IMAP messages have been deleted in order to
        // avoid problems when local messages are moved between folders
        // (see bug 27924).
        for (ImapFolderSync ifs : syncedFolders.values()) {
            try {
                ifs.finishSync();
            } catch (Exception e) {
                LOG.error("Synchronization of folder '%s' failed",
                          ifs.getLocalPath(), e);
            }
        }
        // Any remaining folder trackers are for folders which were deleted
        // both locally and remotely.
        for (ImapFolder tracker : trackedFolders) {
            dataSource.deleteImapFolder(tracker);
        }
    }

    private void syncRemoteFolder(ListData ld) {
        String path = ld.getMailbox();
        // LOG.debug("Processing remote folder '%s'", path);
        ImapFolder tracker = trackedFolders.getByRemotePath(path);
        if (tracker != null) {
            trackedFolders.remove(tracker);
        }
        try {
            ImapFolderSync ifs = new ImapFolderSync(this, tracker);
            tracker = ifs.syncFolder(ld, fullSync);
            if (tracker != null) {
                syncedFolders.put(tracker.getItemId(), ifs);
            }
        } catch (Exception e) {
            LOG.error("Synchronization of IMAP folder '%s' failed", path, e);
        }
    }
    
    private void syncLocalFolder(Folder folder) {
        // LOG.debug("Processing local folder '%s'", folder.getName());
        String name = folder.getName();
        int id = folder.getId();
        ImapFolder tracker = trackedFolders.getByItemId(id);
        if (tracker != null) {
            trackedFolders.remove(tracker);
        } else if (!dataSource.isSyncEnabled(folder.getPath())) {
            LOG.debug("Synchronization disabled for folder '%s'", name);
            return;
        }
        try {
            ImapFolderSync ifs = new ImapFolderSync(this, tracker);
            ifs.syncFolder(folder, fullSync);
            syncedFolders.put(id, ifs);
        } catch (Exception e) {
            LOG.error("Synchronization of folder '%s' failed", name, e);
        }
    }

    /*
     * Returns the path to the Zimbra folder that stores messages for the given
     * IMAP folder. The Zimbra folder has the same path as the IMAP folder,
     * but is relative to the root folder specified by the DataSource.
     */
    String getLocalPath(ListData ld) {
        String remotePath = ld.getMailbox();
        char delimiter = ld.getDelimiter();
        String relativePath = ld.getMailbox();
        if (delimiter != '/' && (remotePath.indexOf(delimiter) >= 0 ||
                                 remotePath.indexOf('/') >= 0)) {
            // Change remote path to use our separator
            String[] parts = remotePath.split("\\" + delimiter);
            for (int i = 0; i < parts.length; i++) {
                // TODO Handle case where separator is not valid in Zimbra folder name
                parts[i] = parts[i].replace('/', delimiter);
            }
            relativePath = StringUtil.join("/", parts);
        }
        String zimbraPath = dataSource.matchKnownLocalPath(relativePath);
        if ("".equals(zimbraPath)) {
            return null; // Do not synchronize folder
        }
        if (zimbraPath == null) {
            // Remove leading slashes and append to root folder
            while (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            if (localRootFolder.getId() == com.zimbra.cs.mailbox.Mailbox.ID_FOLDER_USER_ROOT) {
                zimbraPath = "/" + relativePath;
            } else {
                zimbraPath = localRootFolder.getPath() + "/" + relativePath;
            }
        }
        return zimbraPath;
    }

    /*
     * Returns the IMAP path name for the specified local folder. Returns null
     * if the folder should not be imported.
     */
    String getRemotePath(Folder folder) throws ServiceException {
        if (!localRootFolder.isDescendant(folder)) {
            return null;
        }
        String imapPath = dataSource.matchKnownRemotePath(folder.getPath());
        if ("".equals(imapPath)) {
            return null; // Ignore folder
        }
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
        if (delimiter != '/') {
            String[] parts = imapPath.split("/");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].replace(delimiter, '/');
            }
            imapPath = StringUtil.join(String.valueOf(delimiter), parts);
        }
        return imapPath;
    }
}
