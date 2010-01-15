/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Metadata;

public class DataSourceMapping {
    protected DataSource ds;
    protected DataSourceItem dsi;
    
    public DataSourceMapping(DataSource ds, DataSourceItem dsi) throws ServiceException {
        this.ds = ds;
        this.dsi = dsi;
        parseMetaData();
    }
    
    public DataSourceMapping(DataSource ds, int itemId) throws ServiceException {
        this.ds = ds;
        dsi = DbDataSource.getMapping(ds, itemId);
        if (dsi.remoteId == null)
            throw MailServiceException.NO_SUCH_ITEM(itemId);
        parseMetaData();
    }
    
    public DataSourceMapping(DataSource ds, String remoteId) throws ServiceException {
        this.ds = ds;
        dsi = DbDataSource.getReverseMapping(ds, remoteId);
        if (dsi.itemId == 0)
            throw MailServiceException.NO_SUCH_ITEM(remoteId);
        parseMetaData();
    }
    
    public DataSourceMapping(DataSource ds, int folderId, int itemId, String
        remoteId) throws ServiceException {
        this.ds = ds;
        dsi = new DataSourceItem(folderId, itemId, remoteId, new Metadata());
    }
    
    public DataSourceMapping(DataSource ds, int folderId, int itemId, String
        remoteId, int localFlags) throws ServiceException {
        this.ds = ds;
        dsi = new DataSourceItem(folderId, itemId, remoteId, new Metadata(),
            localFlags);
    }

    public DataSource getDataSource() { return ds; }
    
    public DataSourceItem getDataSourceItem() { return dsi; }
    
    public int getFolderId() { return dsi.folderId; }

    public int getItemId() { return dsi.itemId; }
    
    public int getItemFlags() throws ServiceException {
        if (dsi.itemFlags == -1) {
            com.zimbra.cs.mailbox.Message localMsg =
                DataSourceManager.getInstance().getMailbox(ds).getMessageById(null, dsi.itemId);
            dsi.itemFlags = localMsg.getFlagBitmask();
        }
        return dsi.itemFlags;
    }
    
    public String getRemoteId() { return dsi.remoteId; }
    
    public void setFolderId(int folderId) { dsi.folderId = folderId; }
    
    public void setItemId(int itemId) { dsi.itemId = itemId; }
    
    public void setItemFlags(int itemFlags) { dsi.itemFlags = itemFlags; }
    
    public void setRemoteId(String remoteId) { dsi.remoteId = remoteId; }
    
    public void add() throws ServiceException {
        DbDataSource.addMapping(ds, dsi);
    }
    
    public void delete() throws ServiceException {
        DbDataSource.deleteMapping(ds, dsi.itemId);
    }
    
    public void set() throws ServiceException {
        try {
            DbDataSource.addMapping(ds, dsi);
        } catch (Exception e) {
            delete();
            DbDataSource.addMapping(ds, dsi);
        }
    }
    
    public void update() throws ServiceException {
        DbDataSource.updateMapping(ds, dsi);
    }
    
    protected void parseMetaData() throws ServiceException {}
}
