/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DataSource.DataImport;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;

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
       HttpGet get = new HttpGet(urlString);
        try {
            HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
            HttpProxyUtil.configureProxy(clientBuilder);
            HttpResponse response = HttpClientUtil.executeMethod(clientBuilder.build(), get);
            response.getEntity().getContentLength();
        } catch (Exception e) {
            throw ServiceException.FAILURE("Data source test failed.", e);
        } finally {
            get.releaseConnection();
        }
    }

}
