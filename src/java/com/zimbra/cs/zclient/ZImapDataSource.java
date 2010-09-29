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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.soap.account.type.AccountImapDataSource;
import com.zimbra.soap.type.DataSources;
import com.zimbra.soap.type.ImapDataSource;

public class ZImapDataSource implements ZDataSource, ToZJSONObject {

    private ImapDataSource data;

    public ZImapDataSource(ImapDataSource data) {
        this.data = DataSources.newImapDataSource(data);
    }
    
    public ZImapDataSource(String name, boolean enabled, String host, int port,
                           String username, String password, String folderid,
                           DataSource.ConnectionType connectionType, boolean isImportOnly)
    throws ServiceException {
        data = DataSources.newImapDataSource();
        data.setName(name);
        data.setEnabled(enabled);
        data.setHost(host);
        data.setPort(port);
        data.setUsername(username);
        data.setPassword(password);
        
        try {
            data.setFolderId(folderid);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("Invalid folder id", e);
        }
        
        data.setConnectionType(SoapConverter.TO_SOAP_CONNECTION_TYPE.apply(connectionType));
        data.setImportOnly(isImportOnly);
    }
    
    public ZImapDataSource(String name, boolean enabled, String host, int port,
                           String username, String password, String folderid,
                           DataSource.ConnectionType connectionType) throws ServiceException {
        this(name,enabled,host,port,username,password,folderid,connectionType,false);
    }

    public ZImapDataSource(DataSource dsrc) throws ServiceException {
        if (dsrc.getType() != DataSource.Type.imap)
            throw ServiceException.INVALID_REQUEST("can't instantiate ZImapDataSource for " + dsrc.getType(), null);

        data = new AccountImapDataSource();
        data.setName(dsrc.getName());
        data.setEnabled(dsrc.isEnabled());
        data.setHost(dsrc.getHost());
        data.setPort(dsrc.getPort());
        data.setUsername(dsrc.getUsername());
        data.setPassword(dsrc.getDecryptedPassword());
        data.setFolderId(Integer.toString(dsrc.getFolderId()));
        data.setConnectionType(SoapConverter.TO_SOAP_CONNECTION_TYPE.apply(dsrc.getConnectionType()));
        data.setImportOnly(dsrc.isImportOnly());
    }

    public Element toElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS_IMAP);
        src.addAttribute(MailConstants.A_ID, data.getId());
        src.addAttribute(MailConstants.A_NAME, data.getName());
        src.addAttribute(MailConstants.A_DS_IS_ENABLED, data.isEnabled());
        src.addAttribute(MailConstants.A_DS_HOST, data.getHost());
        src.addAttribute(MailConstants.A_DS_PORT, data.getPort());
        src.addAttribute(MailConstants.A_DS_USERNAME, data.getUsername());
        src.addAttribute(MailConstants.A_DS_PASSWORD, data.getPassword());
        src.addAttribute(MailConstants.A_FOLDER, data.getFolderId());
        src.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, data.getConnectionType().name());
        src.addAttribute(MailConstants.A_DS_IS_IMPORTONLY, data.isImportOnly());
        return src;
    }

    public Element toIdElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS_IMAP);
        src.addAttribute(MailConstants.A_ID, getId());
        return src;
    }

    public DataSource.Type getType() { return DataSource.Type.imap; }

    public String getId() { return data.getId(); }

    public String getName() { return data.getName(); }
    public void setName(String name) { data.setName(name); }

    public boolean isEnabled() { return SystemUtil.getValue(data.isEnabled(), Boolean.FALSE); }
    
    public void setEnabled(boolean enabled) { data.setEnabled(enabled); }

    public String getHost() { return data.getHost(); }
    public void setHost(String host) { data.setHost(host); }

    public int getPort() { return SystemUtil.getValue(data.getPort(), -1); }
    public void setPort(int port) { data.setPort(port); } 

    public String getUsername() { return data.getUsername(); }
    public void setUsername(String username) { data.setUsername(username); }

    public String getFolderId() { return data.getFolderId(); }
    public void setFolderId(String folderid) { data.setFolderId(folderid); }

    public DataSource.ConnectionType getConnectionType() {
        DataSource.ConnectionType ct = SoapConverter.FROM_SOAP_CONNECTION_TYPE.apply(data.getConnectionType());
        return (ct != null ? ct : DataSource.ConnectionType.cleartext);
    }
    
    public void setConnectionType(DataSource.ConnectionType connectionType) {
        data.setConnectionType(SoapConverter.TO_SOAP_CONNECTION_TYPE.apply(connectionType));
    }
    
    public Map<String, Object> getAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceId, getId());
        attrs.put(Provisioning.A_zimbraDataSourceName, getName());
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, isEnabled() ? LdapUtil.LDAP_TRUE : LdapUtil.LDAP_FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceUsername, getUsername());
        attrs.put(Provisioning.A_zimbraDataSourceHost, getHost());
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, getConnectionType().toString());
        if (getPort() > 0)
            attrs.put(Provisioning.A_zimbraDataSourcePort, "" + getPort());
        if (getFolderId() != null)
            attrs.put(Provisioning.A_zimbraDataSourceFolderId, getFolderId());
        attrs.put(Provisioning.A_zimbraDataSourceImportOnly, isImportOnly());
        return attrs;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", data.getId());
        zjo.put("name", data.getName());
        zjo.put("enabled", data.isEnabled());
        zjo.put("host", data.getHost());
        zjo.put("port", data.getPort());
        zjo.put("username", data.getUsername());
        zjo.put("folderId", data.getFolderId());
        zjo.put("connectionType", data.getConnectionType().toString());
        zjo.put("importOnly", data.isImportOnly());
        return zjo;
    }

    public String toString() {
        return String.format("[ZImapDataSource %s]", getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public void setImportOnly(boolean importOnly) {
        data.setImportOnly(importOnly);
    }

    public boolean isImportOnly() {
        return SystemUtil.getValue(data.isImportOnly(), Boolean.FALSE);
    }
}
