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

package com.zimbra.cs.zclient;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

public class ZPop3DataSource implements ZDataSource {

    private String mId;
    private String mName;
    private boolean mEnabled;
    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mFolderId;
    private String mConnectionType;
    
    public ZPop3DataSource(Element e) throws ServiceException {
        mId = e.getAttribute(MailService.A_ID);
        mName = e.getAttribute(MailService.A_NAME);
        mEnabled = e.getAttributeBool(MailService.A_DS_IS_ENABLED);
        mHost = e.getAttribute(MailService.A_DS_HOST);
        mPort = (int) e.getAttributeLong(MailService.A_DS_PORT, 110);
        mUsername = e.getAttribute(MailService.A_DS_USERNAME);
        mFolderId = e.getAttribute(MailService.A_FOLDER);
        mConnectionType = e.getAttribute(MailService.A_DS_CONNECTION_TYPE);
    }

    public ZPop3DataSource(String name, boolean enabled, String host, int port, String username, String password, String folderid, String connectionType) {
        mName = name;
        mEnabled = enabled;
        mHost = host;
        mPort = port;
        mUsername = username;
        mPassword = password;
        mFolderId = folderid;
        mConnectionType = connectionType;
    }

    public Element toElement(Element parent) {
        Element src = parent.addElement(MailService.E_DS_POP3);
        if (mId != null) src.addAttribute(MailService.A_ID, mId);
        src.addAttribute(MailService.A_NAME, mName);
        src.addAttribute(MailService.A_DS_IS_ENABLED, mEnabled);
        src.addAttribute(MailService.A_DS_HOST, mHost);
        src.addAttribute(MailService.A_DS_PORT, mPort);
        src.addAttribute(MailService.A_DS_USERNAME, mUsername);
        if (mPassword != null) src.addAttribute(MailService.A_DS_PASSWORD, mPassword);
        src.addAttribute(MailService.A_FOLDER, mFolderId);
        src.addAttribute(MailService.A_DS_CONNECTION_TYPE, mConnectionType);
        return src;
    }

    public Element toIdElement(Element parent) {
        Element src = parent.addElement(MailService.E_DS_POP3);
        src.addAttribute(MailService.A_ID, mId);
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

    public String getConnectionType() { return mConnectionType; }
    public void setConnectionType(String connectionType) { mConnectionType = connectionType; }
    
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
        return attrs;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("name", mName);
        sb.add("enabled", mEnabled);
        sb.add("host", mHost);
        sb.add("port", mPort);
        sb.add("username", mUsername);
        sb.add("folderId", mFolderId);
        sb.add("connectionType", mConnectionType);
        sb.endStruct();
        return sb.toString();
    }
}
