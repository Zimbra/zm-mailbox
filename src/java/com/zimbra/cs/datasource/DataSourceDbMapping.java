/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
