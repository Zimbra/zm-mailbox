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
package com.zimbra.cs.datasource.imap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.datasource.DataSourceMapping;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;

public class ImapMessage extends DataSourceMapping {
    private int flags;
    private long uid;
    private static final String METADATA_KEY_FLAGS = "f";
    private static final String METADATA_KEY_UID = "u";
    
    public ImapMessage(DataSource ds, DataSourceItem dsi) throws ServiceException {
        super(ds, dsi);
    }
    
    public ImapMessage(DataSource ds, int itemId) throws ServiceException {
        super(ds, itemId);
    }
    
    public ImapMessage(DataSource ds, int folderId, long uid) throws ServiceException {
        super(ds, remoteId(folderId, uid));
    }
    
    public ImapMessage(DataSource ds, int folderId, int itemId, int flags,
        long uid) throws ServiceException {
        super(ds, folderId, itemId, remoteId(folderId, uid));
        setFlags(flags);
        setUid(uid);
    }

    public ImapMessage(DataSource ds, int folderId, int itemId, int flags,
        long uid, int itemFlags) throws ServiceException {
        this(ds, folderId, itemId, flags, uid);
        setItemFlags(itemFlags);
    }

    public int getFlags() { return flags; }
    
    public long getUid() { return uid; }

    public void setFlags(int flags) {
        dsi.md.put(METADATA_KEY_FLAGS, this.flags = flags);
    }

    public void setUid(long uid) {
        dsi.md.put(METADATA_KEY_UID, this.uid = uid);
        setRemoteId(remoteId(dsi.folderId, uid));
    }

    protected void parseMetaData() throws ServiceException {
        flags = (int)dsi.md.getLong(METADATA_KEY_FLAGS, 0);
        uid = dsi.md.getLong(METADATA_KEY_UID, 0);
    }

    private static String remoteId(int folderId, long uid) {
        return Integer.toString(folderId) + "_" + Long.toString(uid); 
    }

    public String toString() {
        return String.format("{folderId=%d,itemId=%d,remoteId=%s}",
            getFolderId(), getItemId(), getRemoteId());
    }
}
