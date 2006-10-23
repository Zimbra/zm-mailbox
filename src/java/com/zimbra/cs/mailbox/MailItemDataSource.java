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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;


public class MailItemDataSource {

    private static final String CLASS_NAME = StringUtil.getSimpleClassName(MailItemDataSource.class);
    public static final String TYPE_POP3 = "pop3";
    public static final String CONFIG_SECTION_DATASOURCE = "ds";

    private static Map<String, MailItemImport> sImports =
        Collections.synchronizedMap(new HashMap<String, MailItemImport>());
    
    static {
        registerImport("pop3", new Pop3Import());
    }

    private static final String NAME = "name";
    private static final String ENABLED = "enabled";
    private static final String HOST= "host";
    private static final String PORT = "port";
    private static final String USER = "user";
    private static final String PASS = "pass";
    private static final String FID = "fid";
    private static final String URL = "url";
    private static final String TYPE = "type";
    
    public static synchronized void create(Mailbox mbox, OperationContext octxt, MailItemDataSource ds) throws ServiceException {
		String name = ds.getName();
		
    	Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
    	if (config == null) {
    		config = new Metadata();
    	}
    	
    	// md is a hash of name : datasource
    	Metadata md = config.getMap(name, true);
    	if (md != null) {
    		throw ServiceException.INVALID_REQUEST("pop3 account "+name+" already exists", null);
    	}
    	
    	md = new Metadata();
    	populate(md, ds);
    	config.put(name, md);
    	mbox.setConfig(octxt, CONFIG_SECTION_DATASOURCE, config);
    }
    
    public static synchronized void modify(Mailbox mbox, OperationContext octxt, MailItemDataSource ds) throws ServiceException {
		String name = ds.getName();
		
    	Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
    	if (config == null) {
    		throw ServiceException.INVALID_REQUEST("pop3 account "+name+" doesn't exist", null);
    	}
    	if (!config.containsKey(name)) {
    		throw ServiceException.INVALID_REQUEST("pop3 account "+name+" doesn't exist", null);
    	}
    	
    	Metadata md = new Metadata();
    	populate(md, ds);
    	config.put(name, md);
    	mbox.setConfig(octxt, CONFIG_SECTION_DATASOURCE, config);
    }
    
    public static MailItemDataSource get(Mailbox mbox, OperationContext octxt, String name) throws ServiceException {
    	Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
    	if (config == null) {
    		throw ServiceException.INVALID_REQUEST("pop3 account "+name+" doesn't exist", null);
    	}
    	Metadata md = config.getMap(name, true);
    	if (md == null) {
    		throw ServiceException.INVALID_REQUEST("pop3 account "+name+" doesn't exist", null);
    	}

    	return get(md, name, mbox.getId());
    }
    
    public static Set<MailItemDataSource> getAll(Mailbox mbox, OperationContext octxt) throws ServiceException {
    	HashSet<MailItemDataSource> ds = new HashSet<MailItemDataSource>();
    	Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
    	if (config == null)
    		return ds;
    	
    	int mboxid = mbox.getId();
    	for (Object k: config.asMap().keySet()) {
    		String key = (String) k;
    		ds.add(get(config.getMap(key), key, mboxid));
    	}
    	
    	return ds;
    }
    
    public static synchronized void delete(Mailbox mbox, OperationContext octxt, String name) throws ServiceException {
    	Metadata config = mbox.getConfig(octxt, CONFIG_SECTION_DATASOURCE);
    	if (config == null) {
    		throw ServiceException.INVALID_REQUEST("pop3 account "+name+" doesn't exist", null);
    	}
    	if (!config.containsKey(name)) {
    		throw ServiceException.INVALID_REQUEST("pop3 account "+name+" doesn't exist", null);
    	}
    	
    	config.remove(name);
    	mbox.setConfig(octxt, CONFIG_SECTION_DATASOURCE, config);
    }
    
    private static MailItemDataSource get(Metadata md, String name, int id) throws ServiceException {
    	MailItemDataSource ds = new MailItemDataSource(id, md.get(TYPE), name, (int)md.getLong(FID));
    	ds.setEnabled(md.getBool(ENABLED));
    	ds.setHost(md.get(HOST));
    	ds.setPort((int)md.getLong(PORT));
    	ds.setUsername(md.get(USER));
    	ds.setPassword(md.get(PASS));
    	ds.setFolderId((int)md.getLong(FID));
    	ds.setUrl(md.get(URL, null));  // optional
    	
    	return ds;
    }
    
    private static void populate(Metadata md, MailItemDataSource ds) {
    	md.put(NAME, ds.getName());
    	md.put(ENABLED, ds.isEnabled());
    	md.put(HOST, ds.getHost());
    	md.put(PORT, ds.getPort());
    	md.put(USER, ds.getUsername());
    	md.put(PASS, ds.getPassword());
    	md.put(FID, ds.getFolderId());
    	md.put(URL, ds.getUrl());
    	md.put(TYPE, ds.getType());
    }
    
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
    
    public MailItemDataSource(int mailboxId, String type, String name, int folderId)
    throws ServiceException {
        // Validate arguments and set member variables
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
        mMailboxId = mailboxId;
        
        mType = type;
        getImport();
        
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        setName(name);
        
        mbox.getFolderById(folderId);
        setFolderId(folderId);
    }

    public MailItemDataSource(String name) {
    	setName(name);
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
    
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        mName = name;
    }

    public void setId(int id) { mId = id; }
    public void setEnabled(boolean isEnabled) { mIsEnabled = isEnabled; }
    public void setHost(String host) { mHost = host; }
    public void setPort(Integer port) { mPort = port; }
    public void setUsername(String username) { mUsername = username; }
    public void setPassword(String password) { mPassword = password; }
    public void setFolderId(int folderId) { mFolderId = folderId; }
    public void setUrl(String url) { mUrl = url; }
    
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

    public void test()
    throws ServiceException {
        getImport().test(this);
    }
    
    public void importData()
    throws ServiceException {
        getImport().importData(this);
    }

    public String toString() {
        return String.format("%s: { mailboxId=%d, id=%d, name=%s }", CLASS_NAME, mMailboxId, mId, mName);
    }
}
