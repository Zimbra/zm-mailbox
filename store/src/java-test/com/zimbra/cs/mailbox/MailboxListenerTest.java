/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import org.junit.Ignore;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class MailboxListenerTest {

    private static boolean listenerWasCalled;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setup() throws Exception {
        MailboxTestUtil.clearData();
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

            Assert.assertNotNull(notification.mods.created);
            boolean newDocFound = false;
            for (BaseItemInfo item : notification.mods.created.values()) {
                if (item instanceof Document) {
                    Document doc = (Document) item;
                    if ("test".equals(doc.getName()))
                        newDocFound = true;
                }
            }
            Assert.assertTrue(newDocFound);

            Assert.assertNotNull(notification.mods);
            Change change = notification.mods.modified.get(
                    new ModificationKey(MockProvisioning.DEFAULT_ACCOUNT_ID, Mailbox.ID_FOLDER_BRIEFCASE));
            Assert.assertNotNull(change);
            Assert.assertEquals(change.why, Change.SIZE);
            Assert.assertNotNull(change.preModifyObj);
            Assert.assertEquals(((Folder) change.preModifyObj).getId(), Mailbox.ID_FOLDER_BRIEFCASE);
        }
    }
}
