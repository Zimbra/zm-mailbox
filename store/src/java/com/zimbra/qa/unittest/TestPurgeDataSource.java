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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import junit.framework.TestCase;

import org.apache.http.HttpException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZConversationHit;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Fetch;
import com.zimbra.client.ZSearchHit;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.prov.soap.SoapTest;
import com.zimbra.soap.admin.message.ModifyAccountRequest;
import com.zimbra.soap.mail.message.CreateDataSourceRequest;
import com.zimbra.soap.mail.message.CreateDataSourceResponse;
import com.zimbra.soap.mail.message.CreateFolderRequest;
import com.zimbra.soap.mail.message.CreateFolderResponse;
import com.zimbra.soap.mail.message.DataSourceUsage;
import com.zimbra.soap.mail.message.GetDataSourceUsageRequest;
import com.zimbra.soap.mail.message.GetDataSourceUsageResponse;
import com.zimbra.soap.mail.message.GetImportStatusRequest;
import com.zimbra.soap.mail.message.GetImportStatusResponse;
import com.zimbra.soap.mail.message.ImportDataRequest;
import com.zimbra.soap.mail.type.DataSourceNameOrId;
import com.zimbra.soap.mail.type.ImapDataSourceNameOrId;
import com.zimbra.soap.mail.type.ImportStatusInfo;
import com.zimbra.soap.mail.type.MailImapDataSource;
import com.zimbra.soap.mail.type.MailPop3DataSource;
import com.zimbra.soap.mail.type.NewFolderSpec;
import com.zimbra.soap.mail.type.Pop3DataSourceNameOrId;
import com.zimbra.soap.type.DataSource.ConnectionType;
import com.zimbra.soap.type.SearchSortBy;

public class TestPurgeDataSource extends TestCase {
    private static final String USER_NAME = "user1";
    private static final String IMAP_DS_NAME_1 = "imapdatasource1";
    private static final String IMAP_DS_NAME_2 = "imapdatasource2";
    private static final String POP3_DS_NAME = "pop3datasource";
    private static final String IMAP_ACCOUNT_NAME_1 = "purgetestaccount1";
    private static final String IMAP_ACCOUNT_NAME_2 = "purgetestaccount2";
    private static final String POP3_ACCOUNT_NAME = "purgetestaccount3";
    private static final String IMAP_DS_1_FOLDER_NAME = "imap_datasource_1";
    private static final String IMAP_DS_2_FOLDER_NAME = "imap_datasource_2";
    private static final String POP3_DS_FOLDER_NAME = "pop3_datasource";
    private static Account account;
    private static ZMailbox mbox;
    private static ZMailbox imapDsMbox1;
    private static ZMailbox imapDsMbox2;
    private static ZMailbox pop3DsMbox;
    private static String imapDsFolder1Id;
    private static String imapDsFolder2Id;
    private static String pop3DsFolderId;
    private static String imapDsId1;
    private static String imapDsId2;
    private static String pop3DsId;
    private static SoapTransport transport;

    @Override
    @BeforeClass
    public void setUp() throws Exception {
        transport = TestUtil.getAdminSoapTransport();
        account = TestUtil.getAccount(USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        createAccount(IMAP_ACCOUNT_NAME_1);
        createAccount(IMAP_ACCOUNT_NAME_2);
        createAccount(POP3_ACCOUNT_NAME);
        imapDsMbox1 = TestUtil.getZMailbox(IMAP_ACCOUNT_NAME_1);
        imapDsMbox2 = TestUtil.getZMailbox(IMAP_ACCOUNT_NAME_2);
        pop3DsMbox = TestUtil.getZMailbox(POP3_ACCOUNT_NAME);
        createFolders();
        createDataSources();
        setThreadingAlgorithm("subject");
    }

    private void createAccount(String acctName) throws ServiceException {
        try {
            TestUtil.createAccount(acctName);
        } catch (ServiceException e) {
            if (e.getCode().equals(AccountServiceException.ACCOUNT_EXISTS)) {
                TestUtil.deleteAccount(acctName);
                TestUtil.createAccount(acctName);
            }
        }
    }
    private void setPurgeParams(boolean purgeEnabled, long dsQuota, long totalQuota) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraFeatureDataSourcePurgingEnabled, purgeEnabled ? ProvisioningConstants.TRUE: ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceQuota, String.valueOf(dsQuota));
        attrs.put(Provisioning.A_zimbraDataSourceTotalQuota, String.valueOf(totalQuota));
        ModifyAccountRequest modifyRequest = new ModifyAccountRequest(account.getId());
        modifyRequest.setAttrs(attrs);
        SoapTest.invokeJaxb(transport, modifyRequest);
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

        req = new CreateDataSourceRequest();
        imap = new MailImapDataSource();
        imap.setUsername(IMAP_ACCOUNT_NAME_2);
        imap.setPassword("test123");
        imap.setEnabled(true);
        imap.setHost("localhost");
        imap.setPort(Integer.valueOf(TestUtil.getServerAttr(Provisioning.A_zimbraImapBindPort)));
        imap.setConnectionType(ConnectionType.cleartext);
        imap.setFolderId(imapDsFolder2Id);
        imap.setName(IMAP_DS_NAME_2);
        req.setDataSource(imap);
        resp = mbox.invokeJaxb(req);
        imapDsId2 = resp.getDataSource().getId();

        req = new CreateDataSourceRequest();
        MailPop3DataSource pop3 = new MailPop3DataSource();
        pop3.setUsername(POP3_ACCOUNT_NAME);
        pop3.setPassword("test123");
        pop3.setEnabled(true);
        pop3.setHost("localhost");
        pop3.setPort(Integer.valueOf(TestUtil.getServerAttr(Provisioning.A_zimbraPop3BindPort)));
        pop3.setConnectionType(ConnectionType.cleartext);
        pop3.setFolderId(pop3DsFolderId);
        pop3.setName(POP3_DS_NAME);
        pop3.setLeaveOnServer(true);
        req.setDataSource(pop3);
        resp = mbox.invokeJaxb(req);
        pop3DsId = resp.getDataSource().getId();

    }

    private void createFolders() throws ServiceException {
        NewFolderSpec folder = new NewFolderSpec(IMAP_DS_1_FOLDER_NAME);
        folder.setParentFolderId("1");
        CreateFolderResponse resp = mbox.invokeJaxb(new CreateFolderRequest(folder));
        imapDsFolder1Id = resp.getFolder().getId();

        folder = new NewFolderSpec(IMAP_DS_2_FOLDER_NAME);
        folder.setParentFolderId("1");
        resp = mbox.invokeJaxb(new CreateFolderRequest(folder));
        imapDsFolder2Id = resp.getFolder().getId();

        folder = new NewFolderSpec(POP3_DS_FOLDER_NAME);
        folder.setParentFolderId("1");
        resp = mbox.invokeJaxb(new CreateFolderRequest(folder));
        pop3DsFolderId = resp.getFolder().getId();
    }

    private void waitUntilImportsFinish() throws InterruptedException, ServiceException {
        GetImportStatusResponse status = null;
        while(true) {
            Thread.sleep(1000);
            status = mbox.invokeJaxb(new GetImportStatusRequest());
            List<ImportStatusInfo> statuses = status.getStatuses();
            boolean allDone = true;
            for (ImportStatusInfo info: statuses) {
                if (info.getRunning()) {
                    allDone = false;
                }
            }
            if (allDone) {
                break;
            }
        }
    }

    private void checkDataSourceUsage() throws ServiceException {
        GetDataSourceUsageResponse usage = mbox.invokeJaxb(new GetDataSourceUsageRequest());
        long quota = usage.getDataSourceQuota();
        for (DataSourceUsage usg: usage.getUsages()) {
            assertTrue(usg.getId() + " is over quota", usg.getUsage() <= quota);
        }
    }

    private void refreshImapData() throws ServiceException, InterruptedException {
        ImportDataRequest req = new ImportDataRequest();
        DataSourceNameOrId imapDs1 = new ImapDataSourceNameOrId();
        imapDs1.setId(imapDsId1);
        DataSourceNameOrId imapDs2 = new ImapDataSourceNameOrId();
        imapDs2.setId(imapDsId2);
        List<DataSourceNameOrId> dsList = new LinkedList<DataSourceNameOrId>();
        dsList.add(imapDs1);
        dsList.add(imapDs2);
        req.setDataSources(dsList);
        mbox.invokeJaxb(req);
        waitUntilImportsFinish();
    }

    private void refreshPop3Data() throws ServiceException, InterruptedException {
        ImportDataRequest req = new ImportDataRequest();
        DataSourceNameOrId popDs = new Pop3DataSourceNameOrId();
        popDs.setId(pop3DsId);
        List<DataSourceNameOrId> dsList = new LinkedList<DataSourceNameOrId>();
        dsList.add(popDs);
        req.setDataSources(dsList);
        mbox.invokeJaxb(req);
        waitUntilImportsFinish();
    }

    private List<ZSearchHit> searchImapDataSource(String folder) throws ServiceException {
        String query = new StringBuilder("in:").append(folder).append("/INBOX").toString();
        return doSearch(query);
    }

    private List<ZSearchHit> searchPop3DataSource(String folder) throws ServiceException {
        String query = new StringBuilder("in:").append(folder).toString();
        return doSearch(query);
    }

    private List<ZSearchHit> doSearch(String query) throws ServiceException {
        ZSearchParams params = new ZSearchParams(query);
        params.setFetch(Fetch.all);
        params.setSortBy(SearchSortBy.dateDesc);
        ZSearchResult result = mbox.search(params);
        List<ZSearchHit> hits = result.getHits();
        return hits;
    }

    @Test
    public void testPurgeImap() throws Exception {
        // This is enough space for two messages, so message 1 should be purged
        setPurgeParams(true, 6000, 10000);
        addMessage(imapDsMbox1, "conversation 1");
        addMessage(imapDsMbox1, "conversation 2");
        addMessage(imapDsMbox1, "conversation 3");
        refreshImapData();
        checkDataSourceUsage();
        List<ZSearchHit> hits = searchImapDataSource(IMAP_DS_1_FOLDER_NAME);
        assertEquals(2, hits.size());
        assertEquals("conversation 3", getSubject(hits.get(0)));
        assertEquals("conversation 2", getSubject(hits.get(1)));
        addMessage(imapDsMbox1, "re:conversation 1");
        // this should restore conversation 1, purging conversations 2 and 3 in the process
        refreshImapData();
        checkDataSourceUsage();
        hits = searchImapDataSource(IMAP_DS_1_FOLDER_NAME);
        assertEquals(1, hits.size());
        assertEquals("re:conversation 1", getSubject(hits.get(0)));
        assertEquals(2, getMsgCount(hits.get(0)));
    }

    @Test
    public void testPurgePop3() throws Exception {
        setPurgeParams(true, 6000, 10000);
        addMessage(pop3DsMbox, "conversation 1");
        addMessage(pop3DsMbox, "conversation 2");
        addMessage(pop3DsMbox, "conversation 3");
        refreshPop3Data();
        checkDataSourceUsage();
        List<ZSearchHit> hits = searchPop3DataSource(POP3_DS_FOLDER_NAME);
        assertEquals(2, hits.size());
        assertEquals("conversation 3", getSubject(hits.get(0)));
        assertEquals("conversation 2", getSubject(hits.get(1)));

        addMessage(pop3DsMbox, "re:conversation 1");
        // unlike IMAP, POP3 doesn't restore purged messages
        refreshPop3Data();
        checkDataSourceUsage();
        hits = searchPop3DataSource(POP3_DS_FOLDER_NAME);
        assertEquals(2, hits.size());
        assertEquals("re:conversation 1", getSubject(hits.get(0)));
        assertEquals("conversation 3", getSubject(hits.get(1)));
        assertEquals(1, getMsgCount(hits.get(0)));
    }

    @Test
    public void testPurgeOverTotalQuota() throws Exception {
        // this is enough for two messages per data source
        setPurgeParams(true, 8000, 20000);
        addMessage(imapDsMbox1, "conversation 1a");
        addMessage(imapDsMbox2, "conversation 1b");
        addMessage(imapDsMbox1, "conversation 2a");
        addMessage(imapDsMbox2, "conversation 2b");
        refreshImapData();
        checkDataSourceUsage();
        List<ZSearchHit> hits = searchImapDataSource(IMAP_DS_1_FOLDER_NAME);
        assertEquals(2, hits.size());
        assertEquals("conversation 2a", getSubject(hits.get(0)));
        assertEquals("conversation 1a", getSubject(hits.get(1)));
        hits = searchImapDataSource(IMAP_DS_2_FOLDER_NAME);
        assertEquals(2, hits.size());
        assertEquals("conversation 2b", getSubject(hits.get(0)));
        assertEquals("conversation 1b", getSubject(hits.get(1)));

        // lower the total DS quota to below the current total usage,
        // triggering a purge across all data sources when the next message comes in.
        // with the fifth incoming message, two oldest messages will be purged,
        setPurgeParams(true, 8000, 8000);
        addMessage(imapDsMbox2, "conversation 3b");
        refreshImapData();
        checkDataSourceUsage();
        hits = searchImapDataSource(IMAP_DS_1_FOLDER_NAME);
        assertEquals(1, hits.size());
        assertEquals("conversation 2a", getSubject(hits.get(0)));

        hits = searchImapDataSource(IMAP_DS_2_FOLDER_NAME);
        assertEquals(2, hits.size());
        assertEquals("conversation 3b", getSubject(hits.get(0)));
        assertEquals("conversation 2b", getSubject(hits.get(1)));
    }

    @Test
    public void testPurgeSharedConversation() throws Exception {
        setPurgeParams(true, 6000, 20000);
        addMessage(imapDsMbox1, "shared conversation");
        refreshImapData();
        Thread.sleep(2000);
        addMessage(imapDsMbox2, "shared conversation");
        refreshImapData();
        addMessage(imapDsMbox1, "conversation 1");
        addMessage(imapDsMbox1, "conversation 2");
        refreshImapData();
        List<ZSearchHit> hits = searchImapDataSource(IMAP_DS_1_FOLDER_NAME);
        assertEquals(2, hits.size());
        assertEquals("conversation 2", getSubject(hits.get(0)));
        assertEquals("conversation 1", getSubject(hits.get(1)));
        hits = searchImapDataSource(IMAP_DS_2_FOLDER_NAME);
        assertEquals(1, hits.size());
        assertEquals("shared conversation", getSubject(hits.get(0)));
        assertEquals(1, getMsgCount(hits.get(0)));
        // add another message to the shared thread, restoring the purged message
        // and causing conversation 1 and 2 to be purged
        addMessage(imapDsMbox1, "shared conversation");
        refreshImapData();
        hits = searchImapDataSource(IMAP_DS_1_FOLDER_NAME);
        assertEquals(1, hits.size());
        assertEquals("shared conversation", getSubject(hits.get(0)));
        assertEquals(3, getMsgCount(hits.get(0)));
        hits = searchImapDataSource(IMAP_DS_2_FOLDER_NAME);
        assertEquals(1, hits.size());
        assertEquals("shared conversation", getSubject(hits.get(0)));
        assertEquals(3, getMsgCount(hits.get(0)));
    }

    private void setThreadingAlgorithm(String algorithm) throws ServiceException, IOException, HttpException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailThreadingAlgorithm, algorithm);
        ModifyAccountRequest modifyRequest = new ModifyAccountRequest(account.getId());
        modifyRequest.setAttrs(attrs);
        SoapTest.invokeJaxb(transport, modifyRequest);
    }

    private String getSubject(ZSearchHit hit) {
        return ((ZConversationHit) hit).getSubject();
    }

    private int getMsgCount(ZSearchHit hit) {
        return ((ZConversationHit) hit).getMessageCount();
    }

    private void addMessage(ZMailbox mbox, String subject) throws Exception {
        mbox.addMessage("2", null, null, System.currentTimeMillis(), getMessage(subject), true);
        Thread.sleep(1000);
    }

    private String getMessage(String subject) throws MessagingException, ServiceException, IOException {
        return new MessageBuilder().withSubject(subject).withBody(getBody()).create();
    }

    private String getBody() {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis euismod in dui vitae mollis. "
                + "Vivamus ac tortor mattis, vestibulum neque auctor, scelerisque justo. "
                + "Vestibulum fermentum tortor non nunc rutrum viverra. "
                + "Integer quis mauris ullamcorper lacus interdum aliquam vel rhoncus enim. "
                + "Quisque suscipit interdum ex, non mattis massa euismod non. S"
                + "ed semper, odio ac condimentum aliquet, metus arcu dictum nisl, vel rhoncus neque elit quis sem. "
                + "Integer non eleifend felis. Aliquam lacus quam, semper sed nulla ac, dapibus iaculis justo. "
                + "Pellentesque dapibus, ligula et sollicitudin interdum, sapien libero porttitor ipsum, ut consectetur justo neque sed augue. "
                + "Integer volutpat vestibulum nisl, et maximus mauris mattis eget. "
                + "In ullamcorper dui eget quam efficitur imperdiet. Phasellus sit amet sem est.\n"
                + "Proin tincidunt, metus eget egestas venenatis, urna sem imperdiet justo, et elementum dolor nibh hendrerit augue. "
                + "Sed a lobortis enim. In mattis felis orci, quis gravida eros maximus non. "
                + "Suspendisse iaculis libero tempus, vestibulum mauris quis, tempor neque. "
                + "Pellentesque rhoncus sollicitudin leo, sed auctor nunc mattis et. "
                + "Fusce laoreet placerat nunc, ac tempus diam sollicitudin vulputate. "
                + "Praesent imperdiet auctor turpis, sit amet consequat quam venenatis non. "
                + "Aenean eget porta dui, et maximus nibh. In hac habitasse platea dictumst. "
                + "Aliquam mi erat, scelerisque at ipsum vitae, bibendum tincidunt quam. Fusce non ante diam. "
                + "Interdum et malesuada fames ac ante ipsum primis in faucibus. "
                + "Vivamus ac mauris rhoncus, mattis massa et, congue elit. Etiam sagittis scelerisque tristique. "
                + "Vivamus blandit non est et sagittis. Vestibulum feugiat nec diam quis iaculis.\n"
                + "Donec dolor justo, imperdiet et commodo ac, commodo non dui. Ut in pretium augue, dictum finibus augue. "
                + "Aenean est massa, luctus et hendrerit sit amet, varius id augue. Aliquam erat volutpat. "
                + "Maecenas sed odio eu nunc ullamcorper rutrum. Proin et accumsan neque. "
                + "Quisque accumsan augue nec nisi euismod, ac cursus tortor posuere.";
    }

    @Override
    @AfterClass
    public void tearDown() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        prov.deleteDataSource(account, imapDsId1);
        prov.deleteDataSource(account, imapDsId2);
        prov.deleteDataSource(account, pop3DsId);
        TestUtil.deleteAccount(IMAP_ACCOUNT_NAME_1);
        TestUtil.deleteAccount(IMAP_ACCOUNT_NAME_2);
        TestUtil.deleteAccount(POP3_ACCOUNT_NAME);
        mbox.deleteFolder(imapDsFolder1Id);
        mbox.deleteFolder(imapDsFolder2Id);
        mbox.deleteFolder(pop3DsFolderId);
        setPurgeParams(false, 0, 0);
        setThreadingAlgorithm("references");
    }
}
