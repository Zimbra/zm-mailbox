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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.ThreaderTest;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StorageCallback;
import com.zimbra.cs.store.StoreManager;

public class HttpStoreManagerTest {

    public static class MockHttpStoreManager extends HttpStoreManager {
        @Override
        public void startup()  { }

        @Override
        public void shutdown()  { }

        @Override
        protected String getGetUrl(Mailbox mbox, String locator) {
            return MockHttpStore.URL_PREFIX + locator;
        }

        @Override
        protected String getPostUrl(Mailbox mbox) {
            return MockHttpStore.URL_PREFIX + UUID.randomUUID().toString();
        }

        @Override
        protected String getDeleteUrl(Mailbox mbox, String locator) {
            return MockHttpStore.URL_PREFIX + locator;
        }

        @Override
        public Blob storeIncoming(InputStream data, StorageCallback callback, boolean storeAsIs) throws IOException {
            return new MockBlob(ByteStreams.toByteArray(data));
        }

        @Override
        public InputStream getContent(Blob blob) throws IOException {
            return blob.getInputStream();
        }

        @Override
        protected StagedBlob getStagedBlob(PostMethod post, String postDigest, long postSize, Mailbox mbox)
        throws ServiceException, IOException {
            String locator = post.getResponseHeader("Location").getValue();
            if (locator == null || locator.isEmpty()) {
                throw ServiceException.FAILURE("no locator returned from POST", null);
            } else {
                return new HttpStagedBlob(mbox, postDigest, postSize, locator);
            }
        }

        private static final class MockBlob extends Blob {
            private final byte[] content;

            MockBlob(byte[] data) {
                super(new MockFile(data));
                content = data;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(content);
            }

            @Override
            public long getRawSize() {
                return content.length;
            }

            @SuppressWarnings("serial")
            private static class MockFile extends File {
                public MockFile(byte[] data) {
                    super("build/test/store");
                }

                @Override
                public boolean exists() {
                    return true;
                }
            }
        }
    }

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();

        LC.zimbra_class_store.setDefault(MockHttpStoreManager.class.getName());
        StoreManager.getInstance().startup();
    }

    @Before
    public void setUp() throws Exception {
        MockHttpStore.startup();
    }

    @After
    public void tearDown() throws Exception {
        MockHttpStore.shutdown();
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
}
