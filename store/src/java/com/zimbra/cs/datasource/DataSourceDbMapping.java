/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;

public class DataSourceDbMapping {

    private static DataSourceDbMapping instance;

    public static synchronized DataSourceDbMapping getInstance() {
        if (instance == null) {
            instance = new DataSourceDbMapping();
        }
        return instance;
    }

    @VisibleForTesting
    public static synchronized void setInstance(DataSourceDbMapping mapping) {
        instance = mapping;
    }

    public void updateMapping(DataSource ds, DataSourceItem item) throws ServiceException {
        DbDataSource.updateMapping(ds, item);
    }

    public void addMapping(DataSource ds, DataSourceItem item) throws ServiceException {
        DbDataSource.addMapping(ds, item);
    }

    public void deleteMapping(DataSource ds, int itemId) throws ServiceException {
        DbDataSource.deleteMapping(ds, itemId);
    }

    public DataSourceItem getReverseMapping(DataSource ds, String remoteId) throws ServiceException {
        return DbDataSource.getReverseMapping(ds, remoteId);
    }

    public DataSourceItem getMapping(DataSource ds, int itemId) throws ServiceException {
        return DbDataSource.getMapping(ds, itemId);
    }

    public Collection<DataSourceItem> getAllMappingsInFolder(DataSource ds, int folderId) throws ServiceException {
        return DbDataSource.getAllMappingsInFolder(ds, folderId);
    }
}
