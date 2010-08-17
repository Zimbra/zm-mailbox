/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class ZImapDataSource implements ZDataSource, ToZJSONObject {

    private String mId;
    private String mName;
    private boolean mEnabled;
    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mFolderId;
    private boolean mImportOnly;
    private DataSource.ConnectionType mConnectionType;
    
    public ZImapDataSource(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mName = e.getAttribute(MailConstants.A_NAME);
        mEnabled = e.getAttributeBool(MailConstants.A_DS_IS_ENABLED);
        mHost = e.getAttribute(MailConstants.A_DS_HOST);
        mPort = (int) e.getAttributeLong(MailConstants.A_DS_PORT, 110);
        mUsername = e.getAttribute(MailConstants.A_DS_USERNAME);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER);
        mConnectionType = DataSource.ConnectionType.fromString(
            e.getAttribute(MailConstants.A_DS_CONNECTION_TYPE));
        mImportOnly = e.getAttributeBool(MailConstants.A_DS_IS_IMPORTONLY);
    }

    public ZImapDataSource(String name, boolean enabled, String host, int port,
            String username, String password, String folderid,
            DataSource.ConnectionType connectionType, boolean isImportOnly) {
			mName = name;
			mEnabled = enabled;
			mHost = host;
			mPort = port;
			mUsername = username;
			mPassword = password;
			mFolderId = folderid;
			mConnectionType = connectionType;
			mImportOnly = isImportOnly;
	}
    
    public ZImapDataSource(String name, boolean enabled, String host, int port,
                           String username, String password, String folderid,
                           DataSource.ConnectionType connectionType) {
        this(name,enabled,host,port,username,password,folderid,connectionType,false);
    }

    public ZImapDataSource(DataSource dsrc) throws ServiceException {
        if (dsrc.getType() != DataSource.Type.imap)
            throw ServiceException.INVALID_REQUEST("can't instantiate ZImapDataSource for " + dsrc.getType(), null);

        mName = dsrc.getName();
        mEnabled = dsrc.isEnabled();
        mHost = dsrc.getHost();
        mPort = dsrc.getPort();
        mUsername = dsrc.getUsername();
        mPassword = dsrc.getDecryptedPassword();
        mFolderId = "" + dsrc.getFolderId();
        mConnectionType = dsrc.getConnectionType();
        mImportOnly = dsrc.isImportOnly();
    }

    public Element toElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS_IMAP);
        if (mId != null) src.addAttribute(MailConstants.A_ID, mId);
        src.addAttribute(MailConstants.A_NAME, mName);
        src.addAttribute(MailConstants.A_DS_IS_ENABLED, mEnabled);
        src.addAttribute(MailConstants.A_DS_HOST, mHost);
        src.addAttribute(MailConstants.A_DS_PORT, mPort);
        src.addAttribute(MailConstants.A_DS_USERNAME, mUsername);
        if (mPassword != null) src.addAttribute(MailConstants.A_DS_PASSWORD, mPassword);
        src.addAttribute(MailConstants.A_FOLDER, mFolderId);
        src.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, mConnectionType.name());
        src.addAttribute(MailConstants.A_DS_IS_IMPORTONLY, mImportOnly);
        return src;
    }

    public Element toIdElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS_IMAP);
        src.addAttribute(MailConstants.A_ID, mId);
        return src;
    }

    public DataSource.Type getType() { return DataSource.Type.imap; }

    public String getId() { return mId; }

    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    public boolean isEnabled() { return mEnabled; }
    public void setEnabled(boolean enabled) { mEnabled = enabled; }

    public String getHost() { return mHost; }
    public void setHost(String host) { mHost = host; }

    public int getPort() { return mPort; }
    public void setPort(int port) { mPort = port; } 

    public String getUsername() { return mUsername; }
    public void setUsername(String username) { mUsername = username; }

    public String getFolderId() { return mFolderId; }
    public void setFolderId(String folderid) { mFolderId = folderid; }

    public DataSource.ConnectionType getConnectionType() { return mConnectionType; }
    public void setConnectionType(DataSource.ConnectionType connectionType) { mConnectionType = connectionType; }
    
    public Map<String, Object> getAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceId, mId);
        attrs.put(Provisioning.A_zimbraDataSourceName, mName);
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, mEnabled ? "TRUE" : "FALSE");
        attrs.put(Provisioning.A_zimbraDataSourceUsername, mUsername);
        attrs.put(Provisioning.A_zimbraDataSourceHost, mHost);
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, mConnectionType);
        if (mPort > 0)
            attrs.put(Provisioning.A_zimbraDataSourcePort, "" + mPort);
        if (mFolderId != null)
            attrs.put(Provisioning.A_zimbraDataSourceFolderId, mFolderId);
        attrs.put(Provisioning.A_zimbraDataSourceImportOnly, mImportOnly);
        return attrs;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", mId);
        zjo.put("name", mName);
        zjo.put("enabled", mEnabled);
        zjo.put("host", mHost);
        zjo.put("port", mPort);
        zjo.put("username", mUsername);
        zjo.put("folderId", mFolderId);
        zjo.put("connectionType", mConnectionType.name());
        zjo.put("importOnly",mImportOnly);
        return zjo;
    }

    public String toString() {
        return String.format("[ZImapDataSource %s]", mName);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

	public void setImportOnly(boolean mImportOnly) {
		this.mImportOnly = mImportOnly;
	}

	public boolean isImportOnly() {
		return mImportOnly;
	}
}
