/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.store.http;

import java.io.File;

import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTest;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.AbstractStoreManagerTest;
import com.zimbra.cs.store.StoreManager;

public class HttpStoreManagerTest extends AbstractStoreManagerTest {

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
        protected String getLocator(PostMethod post, String postDigest, long postSize, Mailbox mbox)
        throws ServiceException {
            String locator = post.getResponseHeader("Location").getValue();
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
}
