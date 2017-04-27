/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import com.zimbra.client.ZCalDataSource;
import com.zimbra.client.ZDataSource;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGrant.GranteeType;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZRssDataSource;
import com.zimbra.client.ZSearchHit;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.mail.message.ConvActionRequest;
import com.zimbra.soap.mail.message.CreateDataSourceRequest;
import com.zimbra.soap.mail.message.CreateDataSourceResponse;
import com.zimbra.soap.mail.message.CreateFolderRequest;
import com.zimbra.soap.mail.message.CreateFolderResponse;
import com.zimbra.soap.mail.message.GetImportStatusRequest;
import com.zimbra.soap.mail.message.GetImportStatusResponse;
import com.zimbra.soap.mail.message.ImportDataRequest;
import com.zimbra.soap.mail.type.ConvActionSelector;
import com.zimbra.soap.mail.type.DataSourceNameOrId;
import com.zimbra.soap.mail.type.ImapDataSourceNameOrId;
import com.zimbra.soap.mail.type.ImportStatusInfo;
import com.zimbra.soap.mail.type.MailPop3DataSource;
import com.zimbra.soap.mail.type.NewFolderSpec;
import com.zimbra.soap.type.DataSource.ConnectionType;

public class TestDataSource {
    @Rule
    public static TestName testInfo = new TestName();

    private static String USER_NAME;
    private static String USER_NAME_2;
    private static String TEST_USER_NAME;

    private static final String DS_NAME = "TestDataSource";

    private static String NAME_PREFIX;

    private String mOriginalAccountPollingInterval;
    private String mOriginalAccountPop3PollingInterval;
    private String mOriginalAccountImapPollingInterval;

    private String mOriginalCosPollingInterval;
    private String mOriginalCosPop3PollingInterval;
    private String mOriginalCosImapPollingInterval;
    private Account account;

    @Before
    public void setUp()
    throws Exception {
        NAME_PREFIX = String.format("%s-%s", TestDataSource.class.getSimpleName(), testInfo.getMethodName()).toLowerCase();
        USER_NAME = String.format("%s-user1", NAME_PREFIX);
        USER_NAME_2 = String.format("%s-user2", NAME_PREFIX);
        TEST_USER_NAME = String.format("%s-testuser1", NAME_PREFIX);
        cleanUp();

        account = TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(USER_NAME_2);
        TestUtil.createAccount(TEST_USER_NAME);

        if (!TestUtil.fromRunUnitTests) {
            TestUtil.cliSetup();
        }

        // Remember original polling intervals.
        Cos cos = account.getCOS();
        mOriginalAccountPollingInterval = account.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false);
        if (mOriginalAccountPollingInterval == null) {
            mOriginalAccountPollingInterval = "";
        }
        mOriginalAccountPop3PollingInterval = account.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, false);
        if (mOriginalAccountPop3PollingInterval == null) {
            mOriginalAccountPop3PollingInterval = "";
        }
        mOriginalAccountImapPollingInterval = account.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval, false);
        if (mOriginalAccountImapPollingInterval == null) {
            mOriginalAccountImapPollingInterval = "";
        }

        mOriginalCosPollingInterval = cos.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, "");
        mOriginalCosPop3PollingInterval = cos.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, "");
        mOriginalCosImapPollingInterval = cos.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval, "");

    }

    @Test
    public void testPollingInterval()
    throws Exception {
        // Create data source
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "testhost");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "0");
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "testuser");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "testpass");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "1");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.cleartext.toString());
        DataSource ds = prov.createDataSource(account, DataSourceType.pop3, NAME_PREFIX + " testPollingInterval", attrs);

        // Valid polling interval
        assertNotNull("Min not defined", account.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
        long min = account.getTimeInterval(Provisioning.A_zimbraDataSourceMinPollingInterval, 0) / 1000;
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, Long.toString(min));
        prov.modifyDataSource(account, ds.getId(), attrs);

        // Invalid polling interval
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, Long.toString(min - 1));
        try {
            prov.modifyDataSource(account, ds.getId(), attrs);
            fail("modifyDataSource() was not supposed to succeed");
        } catch (ServiceException e) {
            assertTrue("Unexpected message: " + e.getMessage(),
                e.getMessage().contains("shorter than the allowed minimum"));
        }
    }

    /**
     * Tests the <tt>lastError</tt> element and <tt>failingSince</tt> attribute
     * for <tt>GetInfoRequest</tt> and <tt>GetDataSourcesRequest</tt>.
     */
    @Test
    public void testErrorStatus()
    throws Exception {
        // Create data source.
        Account testAccount = TestUtil.getAccount(TEST_USER_NAME);
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "localhost");
        attrs.put(Provisioning.A_zimbraDataSourcePort, TestUtil.getServerAttr(Provisioning.A_zimbraPop3BindPort));
        attrs.put(Provisioning.A_zimbraDataSourceUsername, USER_NAME_2);
        attrs.put(Provisioning.A_zimbraDataSourcePassword, TestUtil.DEFAULT_PASSWORD);
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, Integer.toString(Mailbox.ID_FOLDER_INBOX));
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.cleartext.toString());
        attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, LdapConstants.LDAP_TRUE);
        DataSource ds = prov.createDataSource(testAccount, DataSourceType.pop3, DS_NAME, attrs);

        // Make sure error status is not set.
        ZMailbox mbox = TestUtil.getZMailbox(TEST_USER_NAME);
        confirmErrorStatus(mbox, null);

        // Invoke data source sync and make sure error status is not set.
        ZDataSource zds = TestUtil.getDataSource(mbox, DS_NAME);
        TestUtil.importDataSource(zds, mbox, null, true);
        confirmErrorStatus(mbox, null);

        // Change to an invalid password, make sure error status is set.
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "bogus");
        prov.modifyDataSource(testAccount, ds.getId(), attrs);
        Thread.sleep(500);
        zds = TestUtil.getDataSource(mbox, DS_NAME);
        long startTimestamp = System.currentTimeMillis() / 1000; // timestamp is returned in seconds, not millis
        TestUtil.importDataSource(zds, mbox, null, false);
        confirmErrorStatus(mbox, startTimestamp);

        // Fix password, make sure that error status is reset (bug 39050).
        attrs.put(Provisioning.A_zimbraDataSourcePassword, TestUtil.DEFAULT_PASSWORD);
        prov.modifyDataSource(testAccount, ds.getId(), attrs);
        Thread.sleep(500);
        confirmErrorStatus(mbox, null);

        // Do another sync, make sure error password is not set.
        zds = TestUtil.getDataSource(mbox, DS_NAME);
        startTimestamp = System.currentTimeMillis();
        TestUtil.importDataSource(zds, mbox, null, true);
        confirmErrorStatus(mbox, null);
    }

    private void confirmErrorStatus(ZMailbox mbox, Long laterThanTimestamp)
    throws Exception {
        // Check GetInfoRequest.
        Element request = new XMLElement(AccountConstants.GET_INFO_REQUEST);
        Element response = mbox.invoke(request);
        Element eDS = response.getElement(AccountConstants.E_DATA_SOURCES);
        Element ePop3 = null;
        for (Element e : eDS.listElements(MailConstants.E_DS_POP3)) {
            if (e.getAttribute(MailConstants.A_NAME).equals(DS_NAME)) {
                ePop3 = e;
            }
        }
        assertNotNull("Could not find data source in response: " + response.prettyPrint(), ePop3);
        confirmErrorStatus(ePop3, laterThanTimestamp);

        // Check GetDataSources.
        ePop3 = null;
        request = new XMLElement(MailConstants.GET_DATA_SOURCES_REQUEST);
        response = mbox.invoke(request);
        for (Element e : response.listElements(MailConstants.E_DS_POP3)) {
            if (e.getAttribute(MailConstants.A_NAME).equals(DS_NAME)) {
                ePop3 = e;
            }
        }
        assertNotNull("Could not find data source in response: " + response.prettyPrint(), ePop3);
        confirmErrorStatus(ePop3, laterThanTimestamp);
    }

    private void confirmErrorStatus(Element ePop3, Long timestampBeforeSync)
    throws Exception {
        if (timestampBeforeSync != null) {
            assertTrue(ePop3.getElement(MailConstants.E_DS_LAST_ERROR).getText().length() > 0);
            long failingSince = ePop3.getAttributeLong(MailConstants.A_DS_FAILING_SINCE) * 1000;
            long now = System.currentTimeMillis();

            assertTrue(failingSince + " is earlier than " + timestampBeforeSync, failingSince >= timestampBeforeSync);
            assertTrue(failingSince + " is later than " + now, failingSince < now);
        } else {
            assertNull("Last error was not reset", ePop3.getOptionalElement(MailConstants.E_DS_LAST_ERROR));
            assertNull("Error timestamp was not reset", ePop3.getAttribute(MailConstants.A_DS_FAILING_SINCE, null));
        }
    }

    /**
     * Tests {@link DataSource#isScheduled()}.
     */
    @Test
    public void testIsScheduled()
    throws Exception {
        // Create data source
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "testhost");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "0");
        attrs.put(Provisioning.A_zimbraDataSourceUsername, TEST_USER_NAME);
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "test123");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "1");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.cleartext.toString());
        String name = NAME_PREFIX + " testNegativePollingInterval";
        DataSource ds = prov.createDataSource(account, DataSourceType.pop3, name, attrs);

        // Test polling interval not set.
        ds = account.getDataSourceByName(name);
        assertFalse("DataSource was scheduled when NOT expected - Missing interval", ds.isScheduled());

        // Test polling interval = 0.
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "0");
        prov.modifyDataSource(account, ds.getId(), attrs);
        ds = account.getDataSourceByName(name);
        assertFalse("DataSource was scheduled when NOT expected - Interval \"0\"", ds.isScheduled());

        // Test polling interval > 0.
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "365d");
        prov.modifyDataSource(account, ds.getId(), attrs);
        ds = account.getDataSourceByName(name);
        assertTrue("DataSource was NOT scheduled when expected - 365d interval", ds.isScheduled());
    }

    @Test
    public void testMigratePollingInterval()
    throws Exception {
        Cos cos = account.getCOS();

        // Create data source
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = TestUtil.createFolder(mbox, NAME_PREFIX + " testMigratePollingInterval");

        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "localhost");
        int port = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraPop3BindPort));
        attrs.put(Provisioning.A_zimbraDataSourcePort, Integer.toString(port));
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "user2");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "test123");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, folder.getId());
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.cleartext.toString());
        String dsName = NAME_PREFIX + " testMigratePollingInterval";
        DataSource ds = prov.createDataSource(account, DataSourceType.pop3, dsName, attrs);

        // Set old polling intervals and unset new ones.
        account.unsetDataSourcePop3PollingInterval();
        account.unsetDataSourceImapPollingInterval();
        cos.unsetDataSourcePop3PollingInterval();
        cos.unsetDataSourceImapPollingInterval();
        account.setDataSourcePollingInterval("1h");
        cos.setDataSourcePollingInterval("2h");

        // Trigger the migration.
        ds.getPollingInterval();

        // Refresh and verify migrated values.
        account = TestUtil.getAccount(USER_NAME);
        cos = account.getCOS();
        ds = account.getDataSourceByName(dsName);

        assertEquals("1h", account.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval));
        assertEquals("1h", account.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval));
        assertEquals("2h", cos.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval));
        assertEquals("2h", cos.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval));
    }

    public static String createPop3DataSource(ZMailbox shareeMbox, String sharedAcctName, String dsFolderId)
    throws ServiceException {
        CreateDataSourceRequest req = new CreateDataSourceRequest();
        MailPop3DataSource pop3 = new MailPop3DataSource();
        pop3.setUsername(sharedAcctName);
        pop3.setPassword("test123");
        pop3.setEnabled(true);
        pop3.setHost("localhost");
        pop3.setPort(Integer.valueOf(TestUtil.getServerAttr(Provisioning.A_zimbraPop3BindPort)));
        pop3.setConnectionType(ConnectionType.cleartext);
        pop3.setFolderId(dsFolderId);
        pop3.setName("pop3datasource1");
        req.setDataSource(pop3);
        CreateDataSourceResponse resp = shareeMbox.invokeJaxb(req);
        return resp.getDataSource().getId();
    }

    public static String createFolderForDataSource(ZMailbox shareeMBox, String datasource) throws ServiceException {
        CreateFolderResponse resp = shareeMBox.invokeJaxb(
                new CreateFolderRequest( NewFolderSpec.createForNameAndParentFolderId(
                        datasource, Integer.toString(Mailbox.ID_FOLDER_USER_ROOT))));
        return resp.getFolder().getId();
    }

    public static String addMessage(ZMailbox mbox, String subject, String body) throws Exception {
        String id = mbox.addMessage("2", null, null, System.currentTimeMillis(),
                new MessageBuilder().withSubject(subject).withBody(body).create(), true);
        Thread.sleep(1000);
        return id;
    }

    public static void waitUntilImportsFinish(ZMailbox mbox) throws InterruptedException, ServiceException {
        GetImportStatusResponse status = null;
        int slept = 0;
        boolean happy = false;
        while(slept < 30000) {
            Thread.sleep(100);
            slept += 100;
            status = mbox.invokeJaxb(new GetImportStatusRequest());
            List<ImportStatusInfo> statuses = status.getStatuses();
            boolean allDone = true;
            for (ImportStatusInfo info: statuses) {
                if (info.getRunning()) {
                    allDone = false;
                }
            }
            if (allDone) {
                happy = true;
                break;
            }
        }
        assertTrue("DS Imports did not finish in a reasonable time", happy);
    }

    public void refreshPop3DatasourceData(ZMailbox mbox, String dsId) throws ServiceException, InterruptedException {
        ImportDataRequest req = new ImportDataRequest();
        req.setDataSources(Lists.newArrayList((DataSourceNameOrId)ImapDataSourceNameOrId.createForId(dsId)));
        mbox.invokeJaxb(req);
        waitUntilImportsFinish(mbox);
    }

    @Test
    public void testPop3() throws Exception {
        Account pop3acct = TestUtil.getAccount(TEST_USER_NAME);
        ZMailbox pop3mbox = TestUtil.getZMailbox(TEST_USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String pop3DSFolder = NAME_PREFIX + " testPop3 source";
        String pop3DSFolderId = createFolderForDataSource(mbox, pop3DSFolder);
        String dsId = createPop3DataSource(mbox, pop3acct.getName(), pop3DSFolderId);
        String subj = NAME_PREFIX + " testtrashpop3";
        addMessage(pop3mbox, subj, "test");
        refreshPop3DatasourceData(mbox, dsId);
        ZSearchParams params = new ZSearchParams(String.format("subject:\"%s\"", subj));
        params.setTypes("MESSAGE");
        ZSearchResult result = mbox.search(params);
        ZSearchHit hit = result.getHits().get(0);
        String id = hit.getId();
        try {
            mbox.trashMessage(id);
        } catch (SoapFaultException sfe) {
            fail("SoapFaultException caught when deleting item from Pop3 datasource folder - " + sfe.getMessage());
        }
        params = new ZSearchParams("in:Trash");
        params.setTypes("MESSAGE");
        result = mbox.search(params);
        List<ZSearchHit> hits = result.getHits();
        assertEquals(1, hits.size());
        assertEquals(id, hits.get(0).getId());
    }

    /**
     * Creates a folder that syncs to another folder via RSS, and verifies that an
     * RSS data source was implicitly created.
     */
    @Test
    public void testRss()
    throws Exception {
        // Create source folder, make it publicly readable, and add a message to it.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String parentId = Integer.toString(Mailbox.ID_FOLDER_USER_ROOT);
        ZFolder sourceFolder = TestUtil.createFolder(mbox, "/" + NAME_PREFIX + " testRss source");
        mbox.modifyFolderGrant(sourceFolder.getId(), GranteeType.pub, null, "r", null);
        String subject = NAME_PREFIX + " testRss";
        TestUtil.addMessage(mbox, subject, sourceFolder.getId());

        // Create destination folder that syncs to the source folder via RSS.
        String urlString = String.format("https://%s:%s/home/%s%s.rss",
                TestUtil.getServerAttr(Provisioning.A_zimbraServiceHostname),
                TestUtil.getServerAttr(Provisioning.A_zimbraMailSSLPort), USER_NAME, sourceFolder.getPath());
        urlString = HttpUtil.encodePath(urlString);
        ZFolder rssFolder = mbox.createFolder(parentId, NAME_PREFIX + " testRss destination", null, null, null, urlString);

        // Get the data source that was implicitly created.
        ZRssDataSource ds = (ZRssDataSource) getDataSource(mbox, rssFolder.getId());
        assertNotNull(ds);
        assertNull(mbox.testDataSource(ds));

        // Import data and validate the synced message.
        List<ZDataSource> list = new ArrayList<ZDataSource>();
        list.add(ds);
        mbox.importData(list);
        waitForData(mbox, rssFolder);
        ZMessage syncedMsg = TestUtil.getMessage(mbox, "in:\"" + rssFolder.getPath() + "\"");
        assertEquals(subject, syncedMsg.getSubject());
        /*
         *   Bug 102261 - simulate ZWC deleting an item from the folder
         */
        ConvActionSelector sel = ConvActionSelector.createForIdsAndOperation(syncedMsg.getConversationId(), "trash");
        sel.setConstraint("-dtjs");
        sel.setFolder(syncedMsg.getFolderId());
        try {
            mbox.invokeJaxb(new ConvActionRequest(sel));
        } catch (SoapFaultException sfe) {
            fail("SoapFaultException caught when deleting item from RSS datasource folder - " + sfe.getMessage());
        }

        // Delete folder, import data, and make sure that the data source was deleted.
        // Data source import runs asynchronously, so poll until the data source is gone.
        mbox.deleteFolder(rssFolder.getId());
        //JBF - do not do the import; it will fail if DS is already deleted
        //mbox.importData(list);

        // XXX bburtin: disabled check to avoid false positives (bug 54816).  Some sort
        // of race condition is causing this check to fail intermittently.  I was unable
        // to consistently repro.

        /*
        for (int i = 1; i <= 10; i++) {
            ds = (ZRssDataSource) getDataSource(mbox, rssFolder.getId());
            if (ds == null) {
                break;
            }
            Thread.sleep(500);
        }
        assertNull(ds);
        */
    }

    // XXX bburtin: disabled test due to bug 37222 (unable to parse Google calendar).
    public void disabledTestCal()
    throws Exception {
        // Create folder.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String parentId = Integer.toString(Mailbox.ID_FOLDER_USER_ROOT);
        String urlString = "http://www.google.com/calendar/ical/k2kh7ncij3s05dog63g0o0n254%40group.calendar.google.com/public/basic.ics";
        ZFolder folder;
        try {
            folder = mbox.createFolder(parentId, NAME_PREFIX + " testCal", ZFolder.View.appointment, null, null, urlString);
        } catch (ServiceException e) {
            assertEquals(ServiceException.RESOURCE_UNREACHABLE, e.getCode());
            ZimbraLog.test.warn("Unable to test calendar data source for %s: %s", urlString, e.toString());
            return;
        }

        // Get the data source that was implicitly created.
        ZCalDataSource ds = (ZCalDataSource) getDataSource(mbox, folder.getId());
        assertNotNull(ds);

        // Test data source.  If the test fails, skip validation so we don't
        // get false positives when the feed is down or the test
        // is running on a box that's not connected to the internet.
        String error = mbox.testDataSource(ds);
        if (error != null) {
            ZimbraLog.test.warn("Unable to test iCal data source for %s: %s.", urlString, error);
            return;
        }

        // Import data and confirm that the folder is not empty.
        List<ZDataSource> list = new ArrayList<ZDataSource>();
        list.add(ds);
        mbox.importData(list);
        waitForData(mbox, folder);

        // Delete folder, import data, and make sure that the data source was deleted.
        mbox.deleteFolder(folder.getId());
        mbox.importData(list);
        ds = (ZCalDataSource) getDataSource(mbox, folder.getId());
        assertNull(ds);
    }

    private void waitForData(ZMailbox mbox, ZFolder folder)
    throws Exception {
        for (int i = 1; i <= 10; i++) {
            mbox.noOp();
            if (folder.getSize() > 0) {
                return;
            }
            Thread.sleep(500);
        }
        fail("No items found in folder " + folder.getPath());
    }

    private ZDataSource getDataSource(ZMailbox mbox, String folderId)
    throws ServiceException {
        for (ZDataSource i : mbox.getAllDataSources()) {
            if (i instanceof ZRssDataSource && ((ZRssDataSource) i).getFolderId().equals(folderId)) {
                return i;
            }
        }
        return null;
    }


    @After
    public void tearDown()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);

        // Reset original polling intervals.
        Cos cos = account.getCOS();

        account.setDataSourcePollingInterval(mOriginalAccountPollingInterval);
        account.setDataSourcePop3PollingInterval(mOriginalAccountPop3PollingInterval);
        account.setDataSourceImapPollingInterval(mOriginalAccountImapPollingInterval);

        cos.setDataSourcePollingInterval(mOriginalCosPollingInterval);
        cos.setDataSourcePop3PollingInterval(mOriginalCosPop3PollingInterval);
        cos.setDataSourceImapPollingInterval(mOriginalCosImapPollingInterval);

        cleanUp();
    }

    public void cleanUp()
    throws Exception {
        TestUtil.deleteAccount(TEST_USER_NAME);
        TestUtil.deleteAccount(USER_NAME_2);
        TestUtil.deleteAccount(USER_NAME);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestDataSource.class);
    }
}
