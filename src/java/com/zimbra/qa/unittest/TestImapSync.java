/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import org.junit.*;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZDataSource;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZImapDataSource;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.MailConfig;
import com.zimbra.cs.account.DataSource;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.type.DataSource.ConnectionType;

import java.io.IOException;
import java.util.List;

public class TestImapSync {
    private static final String LOCAL_USER = "TestImapSync";
    private static final String TEST_FOLDER_1 = "TestOne";
    private static final Log LOG = ZimbraLog.test;

    private final ImapConfig config;
    private final String pass;
    private final ImapFolder imapFolder1 = new ImapFolder(TEST_FOLDER_1);
    private ZMailbox localMailbox;
    private ImapConnection imapConnection;
    private ZDataSource dataSource;

    public TestImapSync(ImapConfig config, String pass) {
        this.config = config;
        this.pass = pass;
        config.getLogger().setLevel(Log.Level.trace);
    }

    public TestImapSync() {
        config = new ImapConfig();
        config.setHost("localhost");
        config.setPort(7143);
        config.setAuthenticationId("user1");
        pass = "test123";
    }

    @Test
    public void testSync() throws Exception {
        LOG.info("Testing adding message to local mailbox");
        ZFolder folder = TestUtil.createFolder(localMailbox, TEST_FOLDER_1);
        TestUtil.addMessage(localMailbox, "msg1", folder.getId(), "u");
        Assert.assertFalse(imapFolder1.exists());
        syncFolders();
        Assert.assertTrue(imapFolder1.exists());
        MailboxInfo mb = imapFolder1.select();
        Assert.assertEquals(1, mb.getExists());
        Assert.assertEquals(1, mb.getUnseen());
    }

    @Before
    public void setUp() throws Exception {
        if (TestUtil.accountExists(LOCAL_USER)) {
            TestUtil.deleteAccount(LOCAL_USER);
        }
        TestUtil.createAccount(LOCAL_USER);
        localMailbox = TestUtil.getZMailbox(LOCAL_USER);
        dataSource = createDataSource();
        connect();
        deleteImapFolders();
    }

    @After
    public void tearDown() throws Exception {
        if (TestUtil.accountExists(LOCAL_USER)) {
            TestUtil.deleteAccount(LOCAL_USER);
        }
        if (imapConnection != null) {
            deleteImapFolders();
            imapConnection.logout();
            imapConnection = null;
        }
        localMailbox = null;
        dataSource = null;
    }

    private void syncFolders() throws Exception {
        TestUtil.importDataSource(dataSource, localMailbox, null);
    }

    private ZDataSource createDataSource() throws Exception {
        ConnectionType ctype =
            config.getSecurity() == MailConfig.Security.SSL ?
                ConnectionType.ssl : ConnectionType.cleartext;
        String id = localMailbox.createDataSource(
            new ZImapDataSource(
                "TestImapSync", true, config.getHost(), config.getPort(),
                config.getAuthenticationId(), pass, "1", ctype));
        for (ZDataSource ds : localMailbox.getAllDataSources()) {
            if (ds.getId().equals(id)) {
                return ds;
            }
        }
        Assert.fail("Could not find data source");
        return null;
    }

    private void connect() throws IOException {
        imapConnection = new ImapConnection(config);
        imapConnection.connect();
        imapConnection.login(pass);
    }

    private void deleteImapFolders() throws IOException {
        if (imapConnection.isAuthenticated()) {
            if (imapFolder1.exists()) {
                imapFolder1.delete();
            }
            if (imapFolder1.exists()) {
                imapFolder1.delete();
            }
        }
    }

    private class ImapFolder {
        private String name;

        ImapFolder(String name) {
            this.name = name;
        }

        String name() { return name; }

        boolean exists() throws IOException {
            return getListData() != null;
        }

        ListData getListData() throws IOException {
            List<ListData> lds = imapConnection.list("", name);
            return lds.isEmpty() ? null : lds.get(0);
        }

        MailboxInfo select() throws IOException {
            return imapConnection.select(name());
        }

        void delete() throws IOException {
            imapConnection.delete(name());
        }
    }
}
