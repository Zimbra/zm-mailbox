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
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.ThreaderTest;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.LocalBlobCache;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;

public class HttpStoreManagerTest {

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
        protected StagedBlob getStagedBlob(PostMethod post, String postDigest, long postSize, Mailbox mbox)
        throws ServiceException {
            String locator = post.getResponseHeader("Location").getValue();
            if (locator == null || locator.isEmpty()) {
                throw ServiceException.FAILURE("no locator returned from POST", null);
            } else {
                String[] parts = locator.trim().split("/");
                return new HttpStagedBlob(mbox, postDigest, postSize, parts[parts.length - 1]);
            }
        }
    }

    private StoreManager originalStoreManager;
    File tmpDir;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        originalStoreManager = StoreManager.getInstance();
        StoreManager.setInstance(new MockHttpStoreManager());
        MockHttpStore.startup();
        tmpDir = Files.createTempDir();
        LC.zimbra_tmp_directory.setDefault(tmpDir.getPath());
        StoreManager.getInstance().startup();
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDown() throws Exception {
        StoreManager.getInstance().shutdown();
        StoreManager.setInstance(originalStoreManager);
        MockHttpStore.shutdown();
        if (tmpDir != null) {
            FileUtil.deleteDir(tmpDir);
        }
    }

    @Test
    public void sizes() throws Exception {
        ParsedMessage pm = ThreaderTest.getRootMessage();

        StoreManager sm = StoreManager.getInstance();
        Blob blob = sm.storeIncoming(pm.getRawInputStream(), null);
        Assert.assertEquals("blob size = message size", pm.getRawData().length, blob.getRawSize());

        StagedBlob staged = sm.stage(blob, null);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, null, 0, 0);
        Assert.assertEquals("mblob size = staged size", staged.getSize(), mblob.getSize());
    }

    /**
     * Tests putting two copies of the same message into the store (bug 67969).
     */
    @Test
    public void sameDigest() throws Exception {
        ParsedMessage pm = ThreaderTest.getRootMessage();
        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Blob blob1 = sm.storeIncoming(pm.getRawInputStream(), null);
        StagedBlob staged1 = sm.stage(blob1, mbox);
        MailboxBlob mblob1 = sm.link(staged1, mbox, 0, 0);

        Blob blob2 = sm.storeIncoming(pm.getRawInputStream(), null);
        StagedBlob staged2 = sm.stage(blob2, mbox);
        MailboxBlob mblob2 = sm.link(staged2, mbox, 0, 0);

        mblob1.getLocalBlob();
        mblob2.getLocalBlob();
    }

    /**
     * Test pruning the local cache when the maximum number of files or number of bytes
     * is exceeded (bug 67931).
     */
    @Test
    public void localCachePruning() throws Exception {
        ParsedMessage pm = ThreaderTest.getRootMessage();
        HttpStoreManager sm = (HttpStoreManager) StoreManager.getInstance();
        LocalBlobCache cache = sm.getBlobCache();
        cache.setMaxFiles(1);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // Store blob 1.
        MailboxBlob mblob1 = storeAndGet(sm, mbox, pm.getRawInputStream());
        long size = mblob1.getSize();
        Assert.assertEquals(1, cache.getNumFiles());
        Assert.assertEquals(size, cache.getNumBytes());
        Assert.assertNotNull(cache.get(mblob1.getLocator()));

        // Store blob 2.  We prune first, then cache.  Blob 1 won't
        // be ejected even though max files = 1.
        MailboxBlob mblob2 = storeAndGet(sm, mbox, pm.getRawInputStream());
        Assert.assertEquals(2, cache.getNumFiles());
        Assert.assertEquals(2 * size, cache.getNumBytes());
        Assert.assertNotNull(cache.get(mblob1.getLocator()));
        Assert.assertNotNull(cache.get(mblob2.getLocator()));

        // Store blob 3.  Make sure that blob 1 was ejected because the
        // number of files was exceeded.
        MailboxBlob mblob3 = storeAndGet(sm, mbox, pm.getRawInputStream());
        Assert.assertEquals(2, cache.getNumFiles());
        Assert.assertEquals(2 * size, cache.getNumBytes());
        Assert.assertNull(cache.get(mblob1.getLocator()));
        Assert.assertNotNull(cache.get(mblob2.getLocator()));
        Assert.assertNotNull(cache.get(mblob3.getLocator()));

        cache.setMaxFiles(100);
        cache.setMaxBytes(size * 3 - 1);

        // Store blob 4.
        MailboxBlob mblob4 = storeAndGet(sm, mbox, pm.getRawInputStream());
        Assert.assertEquals(3, cache.getNumFiles());
        Assert.assertEquals(3 * size, cache.getNumBytes());
        Assert.assertNotNull(cache.get(mblob2.getLocator()));
        Assert.assertNotNull(cache.get(mblob3.getLocator()));
        Assert.assertNotNull(cache.get(mblob4.getLocator()));

        // Access blob 2 to move it to the back of the LRU list.
        cache.get(mblob2.getLocator());

        // Store blob 5.  Make sure that blob 3 was ejected because the
        // number of bytes was exceeded.
        MailboxBlob mblob5 = storeAndGet(sm, mbox, pm.getRawInputStream());
        Assert.assertEquals(3, cache.getNumFiles());
        Assert.assertEquals(3 * size, cache.getNumBytes());
        Assert.assertNotNull(cache.get(mblob2.getLocator()));
        Assert.assertNull(cache.get(mblob3.getLocator()));
        Assert.assertNotNull(cache.get(mblob4.getLocator()));
        Assert.assertNotNull(cache.get(mblob5.getLocator()));
    }

    /**
     * Stores a new blob from the given stream and loads it into the file cache.
     */
    private MailboxBlob storeAndGet(StoreManager store, Mailbox mbox, InputStream in) throws Exception {
        Blob blob = store.storeIncoming(in, null);
        StagedBlob staged = store.stage(blob, mbox);
        MailboxBlob mblob = store.link(staged, mbox, 0, 0);
        mblob.getLocalBlob();
        return mblob;
    }
}
