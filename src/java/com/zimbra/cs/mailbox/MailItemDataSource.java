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
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;


/**
 * Represents an external data source, such as a POP3 mail server, from which ZCS
 * can import <code>MailItem</code>s.
 * 
 * @author bburtin
 */
public class MailItemDataSource {

    private static final String SIMPLE_CLASS_NAME = 
        StringUtil.getSimpleClassName(MailItemDataSource.class.getName());
    public static final String TYPE_POP3 = "pop3";
    public static final String CONFIG_SECTION_DATASOURCE = "ds";

    public enum ConnectionType { CLEARTEXT, SSL };
    public static final String CT_CLEARTEXT = "cleartext";
    public static final String CT_SSL = "ssl";

    private static Map<String, MailItemImport> sImports =
        Collections.synchronizedMap(new HashMap<String, MailItemImport>());
    
    static {
        registerImport(TYPE_POP3, new Pop3Import());
    }

    private static final String NAME = MailService.A_NAME;
    private static final String IS_ENABLED = MailService.A_DS_IS_ENABLED;
    private static final String HOST = MailService.A_DS_HOST;
    private static final String PORT = MailService.A_DS_PORT;
    private static final String USERNAME = MailService.A_DS_USERNAME;
    private static final String PASSWORD = MailService.A_DS_PASSWORD;
    private static final String FOLDER_ID = MailService.A_FOLDER;
    private static final String TYPE = MailService.A_DS_TYPE;
    private static final String CONNECTION_TYPE = MailService.A_DS_CONNECTION_TYPE;
    
    private int mId;
    private int mMailboxId;
    private String mName;
    private boolean mIsEnabled = true;
    private String mHost;
    private Integer mPort;
    private ConnectionType mConnectionType;
    private String mUsername;
    private String mPassword;
    private int mFolderId;
    private String mUrl;
    private String mType;
    private MailItemImport mImport;
    private ImportStatus mImportStatus = new ImportStatus(this);

    private MailItemDataSource(String type)
    throws ServiceException {
        mType = type;
        getImport(); // Validate type
    }

    /**
     * Creates a new data source object with the specified properties.
     */
    MailItemDataSource(Mailbox mbox, int id, String type, String name, boolean isEnabled, int folderId)
    throws ServiceException {
        this(type);
        setMailboxId(mbox.getId());
        setId(id);
        setFolderId(folderId);
        setName(name);
        setEnabled(isEnabled);
    }
    
    public int getId() { return mId; }
    public int getMailboxId() { return mMailboxId; }
    public String getName() { return mName; }
    public boolean isEnabled() { return mIsEnabled; }
    public String getHost() { return mHost; }
    public Integer getPort() { return mPort; }
    public ConnectionType getConnectionType() { return mConnectionType; }
    public String getUsername() { return mUsername; }
    public String getPassword() { return mPassword; }
    public int getFolderId() { return mFolderId; }
    public String getUrl() { return mUrl; }
    public String getType() { return mType; }
    ImportStatus getImportStatus() { return mImportStatus; }
    
    public MailItemImport getImport()
    throws ServiceException {
        if (mImport == null) {
            String key = mType.toLowerCase();
            if (!sImports.containsKey(key)) {
                throw ServiceException.FAILURE("Invalid MailItemImport type: " + mType, null);
            }
            mImport = sImports.get(mType.toLowerCase());
        }
        return mImport;
    }

    public String getConnectionTypeString() {
        if (mConnectionType == null) {
            return null;
        } else if (mConnectionType == ConnectionType.SSL) {
            return CT_SSL;
        }
        return CT_CLEARTEXT;
    }

    public static ConnectionType getConnectionType(String connectionTypeString)
    throws ServiceException {
        if (connectionTypeString == null) {
            return null;
        } else if (connectionTypeString.equalsIgnoreCase(CT_CLEARTEXT)) {
            return ConnectionType.CLEARTEXT;
        } else if (connectionTypeString.equalsIgnoreCase(CT_SSL)) {
            return ConnectionType.SSL;
        }
        throw ServiceException.FAILURE("Invalid connection type string: " + connectionTypeString, null);
    }
    
    void setMailboxId(int mailboxId)
    throws ServiceException {
        MailboxManager.getInstance().getMailboxById(mailboxId); // Validate
        mMailboxId = mailboxId;
    }
    
    public void setName(String name)
    throws ServiceException {
        if (name == null) {
            throw ServiceException.INVALID_REQUEST("name cannot be null", null);
        }
        mName = name;
    }

    void setId(int id)
    throws ServiceException {
        if (id <= 0) {
            throw ServiceException.INVALID_REQUEST("Invalid id " + id, null);
        }
        mId = id;
    }

    public void setFolderId(int folderId)
    throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.getFolderById(folderId);
        mFolderId = folderId;
    }

    public void setConnectionType(String connectionType)
    throws ServiceException {
        if (connectionType == null) {
            mConnectionType = null;
        } else if (connectionType.equalsIgnoreCase(CT_CLEARTEXT)) {
            mConnectionType = ConnectionType.CLEARTEXT;
        } else if (connectionType.equalsIgnoreCase(CT_SSL)) {
            mConnectionType = ConnectionType.SSL;
        } else {
            throw ServiceException.FAILURE(this + ": invalid connectionType: " + connectionType, null);
        }
    }
    
    public void setEnabled(boolean isEnabled) { mIsEnabled = isEnabled; }
    public void setHost(String host) { mHost = host; }
    public void setPort(Integer port) { mPort = port; }
    public void setConnectionType(ConnectionType ct) { mConnectionType = ct; }
    public void setUsername(String username) { mUsername = username; }
    public void setPassword(String password) { mPassword = password; }
    public void setUrl(String url) { mUrl = url; }

    /**
     * Test connecting to a data source.  Do not actually create the
     * data source.
     * 
     * @return <code>null</code> if the test succeeded, or the error message
     * if it didn't.
     */
    public static String test(String type, String host, int port, ConnectionType connectionType,
                              String username, String password)
    throws ServiceException {
        MailItemDataSource ds = new MailItemDataSource(type);
        ds.setHost(host);
        ds.setPort(port);
        ds.setConnectionType(connectionType);
        ds.setUsername(username);
        ds.setPassword(password);
        
        ZimbraLog.mailbox.info("Testing connection to " + ds);
        
        String error = ds.getImport().test(ds);
        if (error == null) {
            ZimbraLog.mailbox.info("Test of " + ds + " succeeded");
        } else {
            ZimbraLog.mailbox.info("Test of " + ds + " failed: " + error);
        }
        
        return error;
    }

    static void deleteFromMetadata(Mailbox mbox, OperationContext octxt, int id)
    throws ServiceException {
        String key = Integer.toString(id);
        Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
        if (config == null) {
            throw ServiceException.INVALID_REQUEST("Data source " + id + " doesn't exist", null);
        }
        if (!config.containsKey(key)) {
            throw ServiceException.INVALID_REQUEST("Data source " + id + " doesn't exist", null);
        }

        config.remove(key);
        mbox.setConfig(octxt, CONFIG_SECTION_DATASOURCE, config);
    }
    
    /**
     * Extracts all <code>MailItemDataSource</code>s from the given mailbox's <code>Metadata</code>.
     * Data sources are return as a <code>Map</code> whose key is the data source id and the
     * value is the data source.
      */
    static Map<Integer, MailItemDataSource> extractFromMetadata(Mailbox mbox, OperationContext octxt)
    throws ServiceException {
        Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
        Map<Integer, MailItemDataSource> dataSources = new HashMap<Integer, MailItemDataSource>();
        if (config == null)
            return dataSources;

        for (Object k: config.asMap().keySet()) {
            String key = (String) k;
            int id = 0;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                ZimbraLog.mailbox.warn("Invalid data source key: " + key);
                continue;
            }
            Metadata dsMeta = config.getMap(key);

            String type = dsMeta.get(TYPE);
            String name = dsMeta.get(NAME);
            boolean isEnabled = dsMeta.getBool(IS_ENABLED);
            String host = dsMeta.get(HOST, null);
            Integer port = (dsMeta.containsKey(PORT) ? (Integer) (int) dsMeta.getLong(PORT) : null);
            String connectionType = dsMeta.get(CONNECTION_TYPE, null);
            String username = dsMeta.get(USERNAME, null);
            String password = dsMeta.get(PASSWORD, null);
            int folderId = (int) dsMeta.getLong(FOLDER_ID);

            MailItemDataSource ds = new MailItemDataSource(mbox, id, type, name, isEnabled, folderId);
            ds.setHost(host);
            ds.setPort(port);
            ds.setConnectionType(connectionType);
            ds.setUsername(username);
            ds.setPassword(password);

            // connectionType migration
            if (type.equals(TYPE_POP3) && connectionType == null) {
                ds.setConnectionType(CT_CLEARTEXT);
                saveToMetadata(mbox, octxt, ds);
            }
            
            dataSources.put(id, ds);
        }
        
    	return dataSources;
    }
    
    static void saveToMetadata(Mailbox mbox, OperationContext octxt, MailItemDataSource ds)
    throws ServiceException {
        if (mbox.getId() != ds.getMailboxId()) {
            throw ServiceException.FAILURE(
                ds + ": cannot save in a different mailbox (" + mbox.getId() + ")", null);
        }
        if (ds.getId() == 0) {
            throw ServiceException.FAILURE(ds + ": id has not been assigned", null);
        }
        if (ds.getFolderId() == 0) {
            throw ServiceException.FAILURE(ds + ": folder id has not been assigned", null);
        }
        ds.mMailboxId = mbox.getId();
        
        Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
        if (config == null) {
            config = new Metadata();
        }

        Metadata md = new Metadata();
        // Required
        md.put(NAME, ds.getName());
        md.put(TYPE, ds.getType());
        md.put(IS_ENABLED, ds.isEnabled());
        md.put(FOLDER_ID, ds.getFolderId());

        // Optional
        if (ds.getHost() != null) {
            md.put(HOST, ds.getHost());
        } else {
            md.remove(HOST);
        }
        if (ds.getPort() != null) {
            md.put(PORT, ds.getPort());
        } else {
            md.remove(PORT);
        }
        if (ds.getConnectionType() != null) {
            md.put(CONNECTION_TYPE, ds.getConnectionType());
        } else {
            md.remove(CONNECTION_TYPE);
        }
        if (ds.getUsername() != null) {
            md.put(USERNAME, ds.getUsername());
        } else {
            md.remove(USERNAME);
        }
        if (ds.getPassword() != null) {
            md.put(PASSWORD, ds.getPassword());
        } else {
            md.remove(PASSWORD);
        }

        config.put(Integer.toString(ds.getId()), md);
        mbox.setConfig(octxt, CONFIG_SECTION_DATASOURCE, config);
    }

    /**
     * Associate the specified type with the <code>MailItemImport</code>
     * implementation that will perform import of data of that type.
     * 
     * @param type the data type (see {@link MailItemDataSource#getType})
     * @param mii import implementation
     */
    public static void registerImport(String type, MailItemImport mii) {
        type = type.toLowerCase();
        sImports.put(type, mii);
    }

    /**
     * Execute this data source's <code>MailItemImport</code> implementation
     * to import data. 
     */
    public void importData() {
        synchronized (mImportStatus) {
            if (mImportStatus.mIsRunning) {
                ZimbraLog.mailbox.info(this + ": attempted to start import while " +
                    " an import process was already running.  Ignoring the second request.");
                return;
            }
            mImportStatus.mHasRun = true;
            mImportStatus.mIsRunning = true;
            mImportStatus.mSuccess = false;
            mImportStatus.mError = null;
        }
        
        boolean success = false;
        String error = null;

        try {
            getImport().importData(this);
            success = true;
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Import from " + this + " failed", e);
            error = e.getMessage();
        } finally {
            synchronized (mImportStatus) {
                mImportStatus.mSuccess = success;
                mImportStatus.mError = error;
                mImportStatus.mIsRunning = false;
            }
        }
    }

    public String toString() {
        List<String> parts = new ArrayList<String>();
        if (getMailboxId() != 0) {
            parts.add("mailboxId=" + getMailboxId());
        }
        if (getId() != 0) {
            parts.add("id=" + getId());
        }
        parts.add("type=" + getType());
        parts.add("isEnabled=" + isEnabled());
        if (getName() != null) {
            parts.add("name=" + getName());
        }
        if (getHost() != null) {
            parts.add("host=" + getHost());
        }
        if (getPort() != null) {
            parts.add("port=" + getPort());
        }
        if (getConnectionType() != null) {
            parts.add("connectionType=" + getConnectionTypeString());
        }
        if (getUsername() != null) {
            parts.add("username=" + getUsername());
        }
        parts.add("folderId=" + getFolderId());
        return String.format("%s: { %s }",
            SIMPLE_CLASS_NAME, StringUtil.join(", ", parts));
    }
}
