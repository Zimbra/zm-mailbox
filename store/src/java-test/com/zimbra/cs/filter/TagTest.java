/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.filter;

import static org.junit.Assert.fail;
import org.junit.Ignore;

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class TagTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void test() throws Exception {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            account.setMailSieveScript("tag \"Hello World\";");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage("From: sender@zimbra.com\nSubject: test1".getBytes(), false),
                    0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Tag tag = mbox.getTagByName(null, "Hello World");
            Assert.assertTrue(tag.isListed());

            // Send one more message to verify that no exception occurs
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage("From: sender@zimbra.com\nSubject: test2".getBytes(), false),
                    0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Assert.assertTrue(tag.isListed());
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMimeVariable() throws Exception {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            account.setMailSieveScript("require \"variables\"; tag \"${subject} World\";");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage("From: sender@zimbra.com\nSubject: Hello".getBytes(), false),
                    0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Tag tag = mbox.getTagByName(null, "Hello World");
            Assert.assertTrue(tag.isListed());
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }
}
