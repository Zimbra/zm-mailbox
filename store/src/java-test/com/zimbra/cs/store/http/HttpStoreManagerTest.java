/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.http;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTest;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.external.AbstractExternalStoreManagerTest;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class HttpStoreManagerTest extends AbstractExternalStoreManagerTest {

    public static class MockHttpStoreManager extends HttpStoreManager {
        @Override
        protected String getGetUrl(Mailbox mbox, String locator) {
            return MockHttpStore.URL_PREFIX + locator;
        }

        @Override
        protected String getPostUrl(Mailbox mbox) {
            return MockHttpStore.URL_PREFIX;
        }

        @Override
        protected String getDeleteUrl(Mailbox mbox, String locator) {
            return MockHttpStore.URL_PREFIX + locator;
        }

        @Override
        protected String getLocator(HttpPost post, String postDigest, long postSize, Mailbox mbox, HttpResponse resp)
        throws ServiceException {
            String locator = resp.getFirstHeader("Location").getValue();
            if (locator == null || locator.isEmpty()) {
                throw ServiceException.FAILURE("no locator returned from POST", null);
            } else {
                String[] parts = locator.trim().split("/");
                return parts[parts.length - 1];
            }
        }
    }

    @Override
    public StoreManager getStoreManager() {
        return new MockHttpStoreManager();
    }

    File tmpDir;

    @Before
    public void setUpHttp() throws Exception {
        MockHttpStore.startup();
        tmpDir = Files.createTempDir();
        LC.zimbra_tmp_directory.setDefault(tmpDir.getPath());
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDownHttp() throws Exception {
        MockHttpStore.shutdown();
        if (tmpDir != null) {
            FileUtil.deleteDir(tmpDir);
        }
    }

    @Test
    public void mailboxDelete() throws Exception {
    	Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals("start with no blobs in the store", 0, MockHttpStore.size());

        mbox.addMessage(null, MailboxTestUtil.generateMessage("test"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        Assert.assertEquals("1 blob in the store", 1, MockHttpStore.size());

        mbox.addMessage(null, MailboxTestUtil.generateMessage("test"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        Assert.assertEquals("2 blobs in the store", 2, MockHttpStore.size());

        mbox.deleteMailbox();
        Assert.assertEquals("end with no blobs in the store", 0, MockHttpStore.size());
    }

    @Test
    public void fail() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        int count = countMailItems(mbox);
        MockHttpStore.setFail();
        try {
            mbox.addMessage(null, MailboxTestUtil.generateMessage("test"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
            Assert.fail("expected exception not thrown");
        } catch (ServiceException expected) {

        }
        Assert.assertEquals(count, countMailItems(mbox));
    }

    @Ignore("long running test")
    @Test
    public void timeout() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        int count = countMailItems(mbox);
        MockHttpStore.setDelay();
        try {
            mbox.addMessage(null, MailboxTestUtil.generateMessage("test"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
            Assert.fail("expected exception not thrown");
        } catch (ServiceException expected) {

        }
        Assert.assertEquals(count, countMailItems(mbox));
    }


    private int countMailItems(Mailbox mbox) throws ServiceException, SQLException {
        DbConnection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            connection = DbPool.getConnection();
            stmt = connection.prepareStatement("select count(*) from " + DbMailItem.getMailItemTableName(mbox));
            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            if (connection != null) {
                connection.closeQuietly();
            }
        }

    }
}
