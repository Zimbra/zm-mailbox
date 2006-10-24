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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;


public class MailItemDataSource {

    private static final String SIMPLE_CLASS_NAME = 
        StringUtil.getSimpleClassName(MailItemDataSource.class.getName());
    public static final String TYPE_POP3 = "pop3";
    public static final String CONFIG_SECTION_DATASOURCE = "ds";

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
    
    private int mId;
    private int mMailboxId;
    private String mName;
    private boolean mIsEnabled = true;
    private String mHost;
    private Integer mPort;
    private String mUsername;
    private String mPassword;
    private int mFolderId;
    private String mUrl;
    private String mType;
    private MailItemImport mImport;

    private MailItemDataSource(String type)
    throws ServiceException {
        mType = type;
        getImport(); // Validate type
    }

    public int getId() { return mId; }
    public int getMailboxId() { return mMailboxId; }
    public String getName() { return mName; }
    public boolean isEnabled() { return mIsEnabled; }
    public String getHost() { return mHost; }
    public Integer getPort() { return mPort; }
    public String getUsername() { return mUsername; }
    public String getPassword() { return mPassword; }
    public int getFolderId() { return mFolderId; }
    public String getUrl() { return mUrl; }
    public String getType() { return mType; }
    
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

    private void setMailboxId(int mailboxId)
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

    private void setId(int id)
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
    
    public void setEnabled(boolean isEnabled) { mIsEnabled = isEnabled; }
    public void setHost(String host) { mHost = host; }
    public void setPort(Integer port) { mPort = port; }
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
    public static String test(String type, String host, int port,
                              String username, String password)
    throws ServiceException {
        MailItemDataSource ds = new MailItemDataSource(type);
        ds.setHost(host);
        ds.setPort(port);
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

    /**
     * Create a data source with the specified properties.
     */
    public static MailItemDataSource create(Mailbox mbox, OperationContext octxt, String type, String name,
                                            boolean isEnabled, String host, int port, String username,
                                            String password, int folderId)
    throws ServiceException {
        synchronized (mbox) {
            ZimbraLog.mailbox.info(
                String.format("Creating data source: type=%s, name=%s", type, name));
            
            // Calculate current max id
            Set<MailItemDataSource> dataSources = getAll(mbox, octxt);
            int maxId = 0;
            for (MailItemDataSource temp : dataSources) {
                if (temp.getId() > maxId) {
                    maxId = temp.getId();
                }
            }
            
            MailItemDataSource ds = new MailItemDataSource(type);
            ds.setMailboxId(mbox.getId());
            ds.setId(maxId + 1);
            ds.setFolderId(folderId);
            ds.setName(name);
            ds.setEnabled(isEnabled);
            ds.setHost(host);
            ds.setPort(port);
            ds.setUsername(username);
            ds.setPassword(password);
            
            // Validate import type
            ds.getImport();
            
            save(mbox, octxt, ds);
            
            ZimbraLog.mailbox.info("Created " + ds);
            return ds;
        }
    }
    
    /**
     * Update the persisted version of the given data source with the latest
     * values stored in the object.
     */
    public static void modify(Mailbox mbox, OperationContext octxt, MailItemDataSource ds)
    throws ServiceException {
        synchronized (mbox) {
            ZimbraLog.mailbox.info("Modifying " + ds);
            save(mbox, octxt, ds);
        }
    }

    /**
     * Get the data source with the given id.
     */
    public static MailItemDataSource get(Mailbox mbox, OperationContext octxt, int id)
    throws ServiceException {
        synchronized (mbox) {
            Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
            if (config == null) {
                throw ServiceException.INVALID_REQUEST("Data source "+ id +" doesn't exist", null);
            }
            Metadata md = config.getMap(Integer.toString(id), true);
            if (md == null) {
                throw ServiceException.INVALID_REQUEST("Data source "+ id +" doesn't exist", null);
            }

            return get(md, mbox, id);
        }
    }
    
    /**
     * Return all <code>MailItemDataSource</code>s for the given
     * <code>Mailbox</code> or an empty <code>Set</code> if none
     * exist.
     */
    public static Set<MailItemDataSource> getAll(Mailbox mbox, OperationContext octxt)
    throws ServiceException {
        synchronized (mbox) {
            HashSet<MailItemDataSource> ds = new HashSet<MailItemDataSource>();
            Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
            if (config == null)
                return ds;

            for (Object k: config.asMap().keySet()) {
                String key = (String) k;
                int id = 0;
                try {
                    id = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    ZimbraLog.mailbox.warn("Invalid data source key: " + key);
                    continue;
                }
                ds.add(get(config.getMap(key), mbox, id));
            }

            return ds;
        }
    }
    
    /**
     * Delete the data source with the given id.
     */
    public static void delete(Mailbox mbox, OperationContext octxt, int id)
    throws ServiceException {
        synchronized (mbox) {
            ZimbraLog.mailbox.info("Deleting data source " + id);
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
    }
    
    private static MailItemDataSource get(Metadata md, Mailbox mbox, int id)
    throws ServiceException {
    	MailItemDataSource ds = new MailItemDataSource(md.get(TYPE));
        ds.setMailboxId(mbox.getId());
        ds.setId(id);
        ds.setFolderId((int) md.getLong(FOLDER_ID));
        ds.setName(md.get(NAME));
    	ds.setEnabled(md.getBool(IS_ENABLED));
    	ds.setHost(md.get(HOST));
    	ds.setPort((int)md.getLong(PORT));
    	ds.setUsername(md.get(USERNAME));
    	ds.setPassword(md.get(PASSWORD));
    	ds.setFolderId((int)md.getLong(FOLDER_ID));
    	
    	return ds;
    }
    
    private static void save(Mailbox mbox, OperationContext octxt, MailItemDataSource ds)
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
        
        synchronized (mbox) {
            Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
            if (config == null) {
                config = new Metadata();
            }
            
            Metadata md = new Metadata();
            md.put(NAME, ds.getName());
            md.put(IS_ENABLED, ds.isEnabled());
            md.put(HOST, ds.getHost());
            md.put(PORT, ds.getPort());
            md.put(USERNAME, ds.getUsername());
            md.put(PASSWORD, ds.getPassword());
            md.put(FOLDER_ID, ds.getFolderId());
            md.put(TYPE, ds.getType());
            
            config.put(Integer.toString(ds.getId()), md);
            mbox.setConfig(octxt, CONFIG_SECTION_DATASOURCE, config);
        }
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
    public void importData()
    throws ServiceException {
        getImport().importData(this);
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
        if (getName() != null) {
            parts.add("name=" + getName());
        }
        return String.format("%s: { %s }",
            SIMPLE_CLASS_NAME, StringUtil.join(", ", parts));
    }
}
