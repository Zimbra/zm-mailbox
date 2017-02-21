/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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

    public Collection<DataSourceItem> getMappingsAndFlags() throws ServiceException {
        return getMappingsAndFlags(ds, dsi.itemId);
    }

    public static Collection<DataSourceItem> getMappings(DataSource ds, int
        folderId) throws ServiceException {
        return DbDataSource.getAllMappingsInFolder(ds, folderId);
    }

    public static Collection<DataSourceItem> getMappingsAndFlags(DataSource ds, int
        folderId) throws ServiceException {
        return DbDataSource.getAllMappingsAndFlagsInFolder(ds, folderId);
    }
    
    protected void parseMetaData() throws ServiceException {}
}
