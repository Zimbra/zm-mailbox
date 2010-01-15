/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DataSource.DataImport;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;

/**
 * Imports data for RSS and remote calendar folders.
 */
public class RssImport implements DataImport {

    private DataSource mDataSource;
    
    public RssImport(DataSource ds) {
        mDataSource = ds;
    }
    
    public void importData(List<Integer> folderIds, boolean fullSync) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(mDataSource);
        int folderId = mDataSource.getFolderId();
        try {
            mbox.getFolderById(null, folderId);
            mbox.synchronizeFolder(null, folderId);
        } catch (NoSuchItemException e) {
            ZimbraLog.datasource.info("Folder %d was deleted.  Deleting data source %s.",
                folderId, mDataSource.getName());
            mbox.getAccount().deleteDataSource(mDataSource.getId());
        }
    }

    public void test()
    throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(mDataSource);
        int folderId = mDataSource.getFolderId();
        
        Folder folder = mbox.getFolderById(null, folderId);
        String urlString = folder.getUrl();
        if (StringUtil.isNullOrEmpty(urlString)) {
            throw ServiceException.FAILURE("URL not specified for folder " + folder.getPath(), null);
        }
        
        InputStream in = null;
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            in = conn.getInputStream();
            in.read();
        } catch (Exception e) {
            throw ServiceException.FAILURE("Data source test failed.", e);
        } finally {
            ByteUtil.closeStream(in);
        }
    }

}
