package com.zimbra.cs.datasource;

import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.CustomSSLSocketFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.StringUtil;

import java.io.IOException;

public class ImapSync extends AbstractMailItemImport {
    private final ImapConnection connection;
    private final Folder localRootFolder;
    private char delimiter; // IMAP hierarchy delimiter (or 0 if flat)
    private ImapFolderCollection trackedFolders;

    private static final Log LOG = ZimbraLog.datasource;

    private static final boolean DEBUG = true; 

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
        config.setTrace(DEBUG);
        // config.setRawMode(true);
        if (LC.data_source_trust_self_signed_certs.booleanValue()) {
            config.setSSLSocketFactory(new DummySSLSocketFactory());
        } else {
            config.setSSLSocketFactory(new CustomSSLSocketFactory());
        }
        return config;
    }

    protected void connect() throws ServiceException {
        if (!connection.isClosed()) return;
        try {
            connection.connect();
            connection.login(dataSource.getDecryptedPassword());
            delimiter = ImapUtil.getDelimiter(connection);
        } catch (IOException e) {
            throw ServiceException.FAILURE(
                "Unable to connect to IMAP server: " + dataSource, e);
        }
    }

    public void importData(boolean fullSync) throws ServiceException {
        validateDataSource();
        connect();
        trackedFolders = dataSource.getImapFolders();
        try {
            syncFolders(fullSync);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Folder sync failed", e);
        }
    }

    private void syncFolders(boolean fs) throws ServiceException, IOException {
        for (ListData ld : ImapUtil.listFolders(connection)) {
            ImapFolder tracker = trackedFolders.getByRemotePath(ld.getMailbox());
            try {
                new ImapFolderSync(this, tracker).syncFolder(ld, fs);
                trackedFolders.remove(tracker);
            } catch (Exception e) {
                LOG.error("Skipping synchronization of IMAP folder '%s' " +
                          "due to error", ld.getMailbox(), e);
            }
        }
        for (Folder folder : localRootFolder.getSubfolderHierarchy()) {
            if (folder.getId() != localRootFolder.getId()) {
                ImapFolder tracker = trackedFolders.getByItemId(folder.getId());
                try {
                    new ImapFolderSync(this, tracker).syncFolder(folder, fs);
                    trackedFolders.remove(tracker);
                } catch (Exception e) {
                    LOG.error("Skipping synchronization of local folder " +
                              "'%s'due to error", folder.getName(), e);
                }
            }
        }
        // Any remaining tracked folders should be deleted
        for (ImapFolder tracker : trackedFolders) {
            dataSource.deleteImapFolder(tracker);
        }
    }

    /*
     * Returns the path to the Zimbra folder that stores messages for the given
     * JavaMail folder. The Zimbra folder has the same path as the JavaMail
     * folder, but is relative to the root folder specified by the
     * <tt>DataSource</tt>.
     */
    String getLocalPath(String remotePath, char delimiter) {
        String relativePath = remotePath;
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
