/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class ZPop3DataSource implements ZDataSource, ToZJSONObject {

    private String mId;
    private String mName;
    private boolean mEnabled;
    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mFolderId;
    private DataSource.ConnectionType mConnectionType;
    private boolean mLeaveOnServer;
    
    public ZPop3DataSource(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mName = e.getAttribute(MailConstants.A_NAME);
        mEnabled = e.getAttributeBool(MailConstants.A_DS_IS_ENABLED);
        mHost = e.getAttribute(MailConstants.A_DS_HOST);
        mPort = (int) e.getAttributeLong(MailConstants.A_DS_PORT, 110);
        mUsername = e.getAttribute(MailConstants.A_DS_USERNAME);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER);
        mConnectionType = DataSource.ConnectionType.fromString(e.getAttribute(MailConstants.A_DS_CONNECTION_TYPE));
        mLeaveOnServer = e.getAttributeBool(MailConstants.A_DS_LEAVE_ON_SERVER);
    }

    public ZPop3DataSource(String name, boolean enabled, String host, int port,
                           String username, String password, String folderid,
                           DataSource.ConnectionType connectionType, boolean leaveOnServer) {
        mName = name;
        mEnabled = enabled;
        mHost = host;
        mPort = port;
        mUsername = username;
        mPassword = password;
        mFolderId = folderid;
        mConnectionType = connectionType;
        mLeaveOnServer = leaveOnServer;
    }

    public ZPop3DataSource(DataSource dsrc) throws ServiceException {
        if (dsrc.getType() != DataSource.Type.pop3)
            throw ServiceException.INVALID_REQUEST("can't instantiate ZPop3DataSource for " + dsrc.getType(), null);

        mName = dsrc.getName();
        mEnabled = dsrc.isEnabled();
        mHost = dsrc.getHost();
        mPort = dsrc.getPort();
        mUsername = dsrc.getUsername();
        mPassword = dsrc.getDecryptedPassword();
        mFolderId = "" + dsrc.getFolderId();
        mConnectionType = dsrc.getConnectionType();
        mLeaveOnServer = dsrc.leaveOnServer();
    }

    public Element toElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS_POP3);
        if (mId != null) src.addAttribute(MailConstants.A_ID, mId);
        src.addAttribute(MailConstants.A_NAME, mName);
        src.addAttribute(MailConstants.A_DS_IS_ENABLED, mEnabled);
        src.addAttribute(MailConstants.A_DS_HOST, mHost);
        src.addAttribute(MailConstants.A_DS_PORT, mPort);
        src.addAttribute(MailConstants.A_DS_USERNAME, mUsername);
        if (mPassword != null) src.addAttribute(MailConstants.A_DS_PASSWORD, mPassword);
        src.addAttribute(MailConstants.A_FOLDER, mFolderId);
        src.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, mConnectionType.name());
        src.addAttribute(MailConstants.A_DS_LEAVE_ON_SERVER, mLeaveOnServer);
        return src;
    }

    public Element toIdElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS_POP3);
        src.addAttribute(MailConstants.A_ID, mId);
        return src;
    }

    public DataSource.Type getType() { return DataSource.Type.pop3; }

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
    
    public boolean leaveOnServer() { return mLeaveOnServer; }
    public void setLeaveOnServer(boolean leaveOnServer) { mLeaveOnServer = leaveOnServer; }
    
    public Map<String, Object> getAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceId, mId);
        attrs.put(Provisioning.A_zimbraDataSourceName, mName);
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, mEnabled ? "TRUE" : "FALSE");
        attrs.put(Provisioning.A_zimbraDataSourceUsername, mUsername);
        attrs.put(Provisioning.A_zimbraDataSourceHost, mHost);
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, mConnectionType.toString());
        if (mPort > 0)
            attrs.put(Provisioning.A_zimbraDataSourcePort, "" + mPort);
        if (mFolderId != null)
            attrs.put(Provisioning.A_zimbraDataSourceFolderId, mFolderId);
        attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, LdapUtil.getBooleanString(mLeaveOnServer));
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
        zjo.put("leaveOnServer", mLeaveOnServer);
        return zjo;
    }

    public String toString() {
        return String.format("[ZPop3DataSource %s]", mName);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
