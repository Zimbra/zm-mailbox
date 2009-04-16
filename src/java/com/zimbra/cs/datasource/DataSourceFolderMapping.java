/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.util.Collection;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;

public class DataSourceFolderMapping extends DataSourceMapping {
    public DataSourceFolderMapping(DataSource ds, DataSourceItem dsi) throws ServiceException {
	super(ds, dsi);
    }
    
    public DataSourceFolderMapping(DataSource ds, int itemId) throws ServiceException {
	super(ds, itemId);
    }
    
    public DataSourceFolderMapping(DataSource ds, String remoteId) throws ServiceException {
	super(ds, remoteId);
    }
    
    public DataSourceFolderMapping(DataSource ds, int itemId, String remoteId) throws ServiceException {
        super(ds, ds.getFolderId(), itemId, remoteId);
    }

    @Override
    public void delete() throws ServiceException {
        deleteMappings(ds, dsi.itemId);
        super.delete();
    }
    
    public void deleteMappings() throws ServiceException {
        deleteMappings(ds, dsi.itemId);
    }
    
    public static void deleteMappings(DataSource ds, int itemId)
        throws ServiceException {
        DbDataSource.deleteAllMappingsInFolder(ds, itemId);
    }
    
    public Collection<DataSourceItem> getMappings() throws ServiceException {
        return getMappings(ds, dsi.itemId);
    }

    public static Collection<DataSourceItem> getMappings(DataSource ds, int
        folderId) throws ServiceException {
        return DbDataSource.getAllMappingsInFolder(ds, folderId);
    }

    protected void parseMetaData() throws ServiceException {}
}
