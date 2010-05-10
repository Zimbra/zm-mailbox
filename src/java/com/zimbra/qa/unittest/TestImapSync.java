/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import org.junit.*;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZDataSource;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZImapDataSource;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.MailConfig;
import com.zimbra.cs.account.DataSource;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

import java.io.IOException;
import java.util.List;

public class TestImapSync {
    private static final String LOCAL_USER = "TestImapSync";
    private static final String TEST_FOLDER_1 = "TestOne";
    private static final String TEST_FOLDER_2 = "TestTwo";
    private static final Log LOG = ZimbraLog.test;
    
    private final ImapConfig config;
    private final String pass;
    private final ImapFolder imapFolder1 = new ImapFolder(TEST_FOLDER_1);
    private final ImapFolder imapFolder2 = new ImapFolder(TEST_FOLDER_2);
    private ZMailbox localMailbox;
    private ImapConnection imapConnection;
    private ZDataSource dataSource;

    public TestImapSync(ImapConfig config, String pass) {
        this.config = config;
        this.pass = pass;
        config.setTrace(true);
        config.setDebug(true);
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
        DataSource.ConnectionType ctype =
            config.getSecurity() == MailConfig.Security.SSL ?
                DataSource.ConnectionType.ssl : DataSource.ConnectionType.cleartext;
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

        void create() throws IOException {
            imapConnection.create(name());
        }

        void delete() throws IOException {
            imapConnection.delete(name());
        }

        void renameTo(String newName) throws IOException {
            imapConnection.rename(name(), newName);
        }
    }
}
