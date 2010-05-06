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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.datasource.DataSourceFolderMapping;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbImapFolder;
import com.zimbra.cs.db.DbImapMessage;

public class ImapFolder extends DataSourceFolderMapping {
    private String localPath;
    private long uidNext = -1; // Used to optimize offline data source
    private Long uidValidity;
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

    public ImapMessage getMessage(int itemId) throws ServiceException {
        return new ImapMessage(ds, itemId);
    }
    
    public ImapMessage getMessage(long uid) throws ServiceException {
        return new ImapMessage(ds, getItemId(), uid);
    }
    
    public ImapMessageCollection getMessages() throws ServiceException {
        Collection<DataSourceItem> mappings = getMappingsAndFlags(ds, getItemId());
        ImapMessageCollection imc = new ImapMessageCollection();

        for (DataSourceItem mapping : mappings)
            imc.add(new ImapMessage(ds, mapping));
        return imc;
    }
    
    public List<Integer> getNewMessageIds() throws ServiceException {
        Collection<DataSourceItem> mappings = getMappingsAndFlags(ds, getItemId());
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);
        List<Integer> allIds = mbox.listItemIds(mbox.getOperationContext(),
            MailItem.TYPE_MESSAGE, getItemId());
        List<Integer> newIds = new ArrayList<Integer>();
        
        loop:
        for (Integer id : allIds) {
            for (DataSourceItem mapping : mappings) {
                if (mapping.itemId == id) {
                    mappings.remove(mapping);
                    continue loop;
                }
            }
            newIds.add(id);
        }
        return newIds;
    }

    public static ImapFolderCollection getFolders(DataSource ds) throws ServiceException {
        Collection<DataSourceItem> mappings = getMappings(ds, ds.getFolderId());
        ImapFolderCollection ifc = new ImapFolderCollection();

        if (mappings.size() == 0) {
            Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);
            
            ZimbraLog.datasource.info("Upgrading IMAP data for %s", ds.getName());
            DbDataSource.deleteAllMappings(ds);
            try {
                for (ImapFolder folderTracker : DbImapFolder.getImapFolders(
                    mbox, ds)) {
                    folderTracker.add();
                    for (ImapMessage msgTracker : DbImapMessage.getImapMessages(
                        mbox, ds, folderTracker)) {
                        msgTracker.add();
                    }
                }
            } catch (Exception e) {
                DbDataSource.deleteAllMappings(ds);
                throw ServiceException.FAILURE("IMAP data upgrade failed for " +
                    ds.getName(), e);
            }
            mappings = getMappings(ds, ds.getFolderId());
            DbImapFolder.deleteImapData(mbox, ds.getId());
        }
        for (DataSourceItem mapping : mappings)
            ifc.add(new ImapFolder(ds, mapping));
        return ifc;
    }

    public String toString() {
        return String.format("ImapFolder: { itemId=%d, dataSourceId=%s, localPath=%s, remotePath=%s, uidValidity=%d }",
            dsi.itemId, ds.getId(), localPath, dsi.remoteId, uidValidity);
    }
}
