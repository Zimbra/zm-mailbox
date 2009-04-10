/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
import com.zimbra.cs.db.DbDataSource.DataSourceItem;

public class ImapFolder extends DataSourceFolderMapping {
    private String localPath;
    private long uidNext = -1; // Used to optimize offline data source
    private Long uidValidity = -1L;
    private static final String METADATA_KEY_LOCAL_PATH = "lp";
    private static final String METADATA_KEY_UID_VALIDITY = "uv";

    public ImapFolder(DataSource ds, DataSourceItem dsi) throws ServiceException {
        super(ds, dsi);
    }
    
    public ImapFolder(DataSource ds, int itemId) throws ServiceException {
        super(ds, itemId);
    }
    
    public ImapFolder(DataSource ds, String remoteId) throws ServiceException {
        super(ds, remoteId);
    }
    
    public ImapFolder(DataSource ds, int itemId, String remoteId, String localPath,
        Long uidValidity) throws ServiceException {
        super(ds, itemId, remoteId);
        setLocalPath(localPath);
        setUidValidity(uidValidity);
    }

    public String getLocalPath() { return localPath; }
    
    public long getUidNext() { return uidNext; }
    
    public long getUidValidity() { return uidValidity; }
    
    public void setLocalPath(String localPath) {
        dsi.md.put(METADATA_KEY_LOCAL_PATH, this.localPath = localPath);
    }
    
    public void setUidNext(long uidNext) {
        this.uidNext = uidNext;
    }

    public void setUidValidity(Long uidValidity) {
        dsi.md.put(METADATA_KEY_UID_VALIDITY, this.uidValidity = uidValidity);
    }

    protected void parseMetaData() throws ServiceException {
        localPath = dsi.md.get(METADATA_KEY_LOCAL_PATH, "");
        uidValidity = dsi.md.getLong(METADATA_KEY_UID_VALIDITY, -1);
    }

    public String toString() {
        return String.format("ImapFolder: { itemId=%d, dataSourceId=%s, localPath=%s, remotePath=%s, uidValidity=%d }",
            dsi.itemId, ds.getId(), localPath, dsi.remoteId, uidValidity);
    }
    
    public static ImapFolderCollection getImapFolders(DataSource ds) throws ServiceException {
        Collection<DataSourceItem> mappings = getMappings(ds, ds.getFolderId());
        ImapFolderCollection ifc = new ImapFolderCollection();

        for (DataSourceItem mapping : mappings)
            ifc.add(new ImapFolder(ds, mapping));
        return ifc;
    }
}
