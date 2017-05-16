/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.base.Joiner;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSearchHit;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.mail.message.CreateDataSourceRequest;
import com.zimbra.soap.mail.message.CreateDataSourceResponse;
import com.zimbra.soap.mail.message.CreateFolderRequest;
import com.zimbra.soap.mail.message.CreateFolderResponse;
import com.zimbra.soap.mail.message.ImportDataRequest;
import com.zimbra.soap.mail.type.DataSourceNameOrId;
import com.zimbra.soap.mail.type.ImapDataSourceNameOrId;
import com.zimbra.soap.mail.type.MailImapDataSource;
import com.zimbra.soap.mail.type.NewFolderSpec;
import com.zimbra.soap.type.DataSource.ConnectionType;

public class TestTrashImapMessage {
    private static String USER_NAME = "testtrashimapmessage";
    private static final String IMAP_DS_NAME_1 = "imapdatasource1";
    private static final String IMAP_ACCOUNT_NAME_1 = "trashtestaccount1";
    private static final String IMAP_DS_1_FOLDER_NAME = "imap_datasource_1";
    private static ZMailbox mbox;
    private static ZMailbox imapDsMbox1;
    private static String imapDsFolder1Id;
    private static String imapDsId1;
    private final List<String> msgIds = new LinkedList<String>();

    @Before
    public void setUp() throws Exception {
        if (!TestUtil.accountExists(USER_NAME))
            TestUtil.createAccount(USER_NAME);

        mbox = TestUtil.getZMailbox(USER_NAME);
        if (!TestUtil.accountExists(IMAP_ACCOUNT_NAME_1))
            TestUtil.createAccount(IMAP_ACCOUNT_NAME_1);
        imapDsMbox1 = TestUtil.getZMailbox(IMAP_ACCOUNT_NAME_1);
        createFolders();
        createDataSources();
    }

    private void createDataSources() throws ServiceException {
        CreateDataSourceRequest req = new CreateDataSourceRequest();
        MailImapDataSource imap = new MailImapDataSource();
        imap.setUsername(IMAP_ACCOUNT_NAME_1);
        imap.setPassword("test123");
        imap.setEnabled(true);
        imap.setHost("localhost");
        imap.setPort(Integer.valueOf(TestUtil.getServerAttr(Provisioning.A_zimbraImapBindPort)));
        imap.setConnectionType(ConnectionType.cleartext);
        imap.setFolderId(imapDsFolder1Id);
        imap.setName(IMAP_DS_NAME_1);
        req.setDataSource(imap);
        CreateDataSourceResponse resp = mbox.invokeJaxb(req);
        imapDsId1 = resp.getDataSource().getId();
    }

    private void createFolders() throws ServiceException {
        NewFolderSpec folder = new NewFolderSpec(IMAP_DS_1_FOLDER_NAME);
        folder.setParentFolderId("1");
        CreateFolderResponse resp = mbox.invokeJaxb(new CreateFolderRequest(folder));
        imapDsFolder1Id = resp.getFolder().getId();
    }

    private String addMessage(ZMailbox mbox, String subject, String body) throws Exception {
        String id = mbox.addMessage("2", null, null, System.currentTimeMillis(), getMessage(subject, body), true);
        Thread.sleep(1000);
        return id;
    }

    private String getMessage(String subject, String body) throws MessagingException, ServiceException, IOException {
        return new MessageBuilder().withSubject(subject).withBody(body).create();
    }

    private void refreshImapData() throws ServiceException, InterruptedException {
        ImportDataRequest req = new ImportDataRequest();
        DataSourceNameOrId imapDs1 = new ImapDataSourceNameOrId();
        imapDs1.setId(imapDsId1);
        List<DataSourceNameOrId> dsList = new LinkedList<DataSourceNameOrId>();
        dsList.add(imapDs1);
        req.setDataSources(dsList);
        mbox.invokeJaxb(req);
        TestDataSource.waitUntilImportsFinish(mbox);
    }

    @Test
    public void testTrashImapMessage() throws Exception {
        String subj = "testtrashimap";
        addMessage(imapDsMbox1, subj, "test");
        refreshImapData();
        ZSearchParams params = new ZSearchParams("subject:"+subj);
        params.setTypes("MESSAGE");
        ZSearchResult result = mbox.search(params);
        ZSearchHit hit = result.getHits().get(0);
        String id = hit.getId();
        msgIds.add(id);
        mbox.trashMessage(id);
        params = new ZSearchParams("in:\"" + IMAP_DS_1_FOLDER_NAME + "/Trash\"");
        params.setTypes("MESSAGE");
        result = mbox.search(params);
        List<ZSearchHit> hits = result.getHits();
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals(id, hits.get(0).getId());
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(IMAP_ACCOUNT_NAME_1);
        TestUtil.deleteAccount(USER_NAME);
    }
}
