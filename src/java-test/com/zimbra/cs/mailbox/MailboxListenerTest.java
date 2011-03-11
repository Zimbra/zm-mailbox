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
package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.mime.MockMimeTypeInfo;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;

public class MailboxListenerTest {

    private static boolean listenerWasCalled;

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();

        MockMimeTypeInfo mime = new MockMimeTypeInfo();
        mime.setHandlerClass(UnknownTypeHandler.class.getName());
        prov.addMimeType(MimeConstants.CT_DEFAULT, mime);
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();

        LC.zimbra_class_store.setDefault(MockStoreManager.class.getName());
        StoreManager.getInstance().startup();
    }

    @Before
    public void setup() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
        listenerWasCalled = false;
    }

    @Test
    public void listenerTest() throws Exception {
        Account acct = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        OperationContext octxt = new OperationContext(acct);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        MailboxListener.register(new TestListener());
        mbox.createDocument(octxt, Mailbox.ID_FOLDER_BRIEFCASE, "test", "text/plain", "test@zimbra.com",
                "hello", new ByteArrayInputStream("hello world".getBytes("UTF-8")));
    }

    @After
    public void cleanup() throws Exception {
        Assert.assertTrue(listenerWasCalled);
        MailboxListener.reset();
    }

    public static class TestListener extends MailboxListener {

        @Override
        public void notify(ChangeNotification notification) {
            listenerWasCalled = true;
            Assert.assertNotNull(notification);
            Assert.assertNotNull(notification.mailboxAccount);
            Assert.assertEquals(notification.mailboxAccount.getId(), MockProvisioning.DEFAULT_ACCOUNT_ID);
            Assert.assertNotNull(notification.mods);
            Assert.assertNotNull(notification.mods.created);
            boolean newDocFound = false;
            for (MailItem item : notification.mods.created.values()) {
                if (item instanceof Document) {
                    if ("test".equals(((Document)item).getName()))
                        newDocFound = true;
                }
            }
            Assert.assertTrue(newDocFound);
            Change change = notification.mods.modified.get(new PendingModifications.ModificationKey(
                    MockProvisioning.DEFAULT_ACCOUNT_ID, Mailbox.ID_FOLDER_BRIEFCASE));
            Assert.assertNotNull(change);
            Assert.assertEquals(change.why, Change.MODIFIED_SIZE);
        }
    }
}
