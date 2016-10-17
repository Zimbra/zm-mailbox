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

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

public class FileIntoCopyTest {

	private String filterScript = "require [\"copy\", \"fileinto\"];\n"

			+ "if header :contains \"Subject\" \"test\" { fileinto :copy \"Junk\"; }";

	private String filterPlainFileintoScript = "require [\"fileinto\"];\n"

			+ "if header :contains \"Subject\" \"test\" { fileinto \"Junk\"; }";

	@BeforeClass

	public static void init() throws Exception {
		MailboxTestUtil.initServer();
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
		Server server = Provisioning.getInstance().getServer(acct);
		server.setSieveFeatureVariablesEnabled(true);
	}

	@Before

	public void setUp() throws Exception {
		MailboxTestUtil.clearData();
	}

	@Test

	public void testCopyFileInto() {
		try {
			Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

			RuleManager.clearCachedRules(account);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

			account.setMailSieveScript(filterScript);

			String raw = "From: sender@zimbra.com\n" + "To: test1@zimbra.com\n" + "Subject: Test\n" + "\n" + "Hello World.";

			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);

			Assert.assertEquals(2, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("Test", msg.getSubject());
		} catch (Exception e) {
			e.printStackTrace();
			fail("No exception should be thrown");
		}
	}

	@Test

	public void testPlainFileInto() {

		try {

			Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

			RuleManager.clearCachedRules(account);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

			account.setMailSieveScript(filterPlainFileintoScript);

			String raw = "From: sender@zimbra.com\n" + "To: test1@zimbra.com\n" + "Subject: Test\n" + "\n" + "Hello World.";

			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);

			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("Test", msg.getSubject());
		} catch (Exception e) {
			e.printStackTrace();
			fail("No exception should be thrown");
		}
	}

	/*
     fileinto :copy foo;
     if header :contains "Subject" "test" {
     fileinto bar;
     }

     if message has "Subject: Test" ==> it should be stored in "foo" and "bar"
   */
	@Test

    public void testCopyFileIntoPattern1Test() {

        try {
            String filterScriptPattern1 = "require [\"copy\", \"fileinto\"];\n"
                + "fileinto :copy \"foo\";\n"
                + "if header :contains \"Subject\" \"Test\" {\n"
                + "fileinto \"bar\"; }";

            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            RuleManager.clearCachedRules(account);

            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.setMailSieveScript(filterScriptPattern1);

            String rawTest = "From: sender@zimbra.com\n" + "To: test1@zimbra.com\n" + "Subject: Test\n" + "\n" + "Hello World";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(rawTest.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // message should not be stored in inbox
            Assert.assertNull(mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE));

            // message should be stored in foo
            Integer item = mbox.getItemIds(null,mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "foo").getId()).getIds(MailItem.Type.MESSAGE).get(0);
            Message msg = mbox.getMessageById(null, item);
            Assert.assertEquals("Hello World", msg.getFragment());

            // message should be stored in bar
            item = mbox.getItemIds(null,mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "bar").getId()).getIds(MailItem.Type.MESSAGE).get(0);
            msg = mbox.getMessageById(null, item);
            Assert.assertEquals("Hello World", msg.getFragment());
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

	/*
    fileinto :copy foo;
    if header :contains "Subject" "test" {
    fileinto bar;
    }

    if message has "Subject: real" ==> it should be stored in "foo" and INBOX
    */

	@Test

    public void testCopyFileIntoPattern1Real() {

        try {
            String filterScriptPattern1 = "require [\"copy\", \"fileinto\"];\n"
                + "fileinto :copy \"foo\";\n"
                + "if header :contains \"Subject\" \"Test\" {\n"
                + "fileinto \"bar\"; }";

            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            RuleManager.clearCachedRules(account);

            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.setMailSieveScript(filterScriptPattern1);

            String rawReal = "From: sender@zimbra.com\n" + "To: test1@zimbra.com\n" + "Subject: Real\n" + "\n" + "Hello World";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(rawReal.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // message should be stored in foo
            Integer item = mbox.getItemIds(null,mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "foo").getId()).getIds(MailItem.Type.MESSAGE).get(0);
            Message msg = mbox.getMessageById(null, item);
            Assert.assertEquals("Hello World", msg.getFragment());

            // message should be stored in inbox
            item = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            msg = mbox.getMessageById(null, item);
            Assert.assertEquals("Hello World", msg.getFragment());
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

	/*
     fileinto :copy foo;
     if header :contains "Subject" "Test" {
     fileinto :copy bar;
     }

    if message has "Subject: test" ==> it should be stored in "foo", "bar" and INBOX
    */

    @Test

    public void testCopyFileIntoPattern2Test() {

        try {
            String filterScriptPattern1 = "require [\"copy\", \"fileinto\"];\n"
                + "fileinto :copy \"foo\";"
                + "if header :contains \"Subject\" \"Test\" {\n"
                + "fileinto :copy \"bar\"; }";

            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            RuleManager.clearCachedRules(account);

            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.setMailSieveScript(filterScriptPattern1);

            String rawReal = "From: sender@zimbra.com\n" + "To: test1@zimbra.com\n" + "Subject: Test\n" + "\n" + "Hello World";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(rawReal.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // message should be stored in bar
            Integer item = mbox.getItemIds(null,mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "bar").getId()).getIds(MailItem.Type.MESSAGE).get(0);
            Message msg = mbox.getMessageById(null, item);
            Assert.assertEquals("Hello World", msg.getFragment());

            // message should be stored in foo
            item = mbox.getItemIds(null,mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "foo").getId()).getIds(MailItem.Type.MESSAGE).get(0);
            msg = mbox.getMessageById(null, item);
            Assert.assertEquals("Hello World", msg.getFragment());

            // message should be stored in inbox
            item = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            msg = mbox.getMessageById(null, item);
            Assert.assertEquals("Hello World", msg.getFragment());
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    /*
     fileinto :copy foo;
     if header :contains "Subject" "Test" {
     fileinto :copy bar;
     }

     if message has "Subject: Real" ==> it should be stored in "foo" and INBOX
   */

   @Test

   public void testCopyFileIntoPattern2Real() {

       try {
           String filterScriptPattern1 = "require [\"copy\", \"fileinto\"];\n"
               + "fileinto :copy \"foo\";"
               + "if header :contains \"Subject\" \"Test\" {\n"
               + "fileinto :copy \"bar\"; }";

           Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

           RuleManager.clearCachedRules(account);

           Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

           account.setMailSieveScript(filterScriptPattern1);

           String rawReal = "From: sender@zimbra.com\n" + "To: test1@zimbra.com\n" + "Subject: Real\n" + "\n" + "Hello World";

           List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                   new ParsedMessage(rawReal.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                   Mailbox.ID_FOLDER_INBOX, true);

           // message should be stored in foo
           Integer item = mbox.getItemIds(null,mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "foo").getId()).getIds(MailItem.Type.MESSAGE).get(0);
           Message msg = mbox.getMessageById(null, item);
           Assert.assertEquals("Hello World", msg.getFragment());

           // message should be stored in inbox
           item = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
           msg = mbox.getMessageById(null, item);
           Assert.assertEquals("Hello World", msg.getFragment());
       } catch (Exception e) {
           e.printStackTrace();
           fail("No exception should be thrown");
       }
   }

   /*
      fileinto :copy foo;
      if header :contains "Subject" "Test" {
      discard;
      }
 
      if message has "Subject: Test" ==> it should be stored in "foo"
    */

   @Test

   public void testCopyFileIntoPattern3Test() {

       try {
           String filterScriptPattern1 = "require [\"copy\", \"fileinto\"];\n"
               + "fileinto :copy \"foo\";"
               + "if header :contains \"Subject\" \"Test\" {\n"
               + "discard; }";

           Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

           RuleManager.clearCachedRules(account);

           Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

           account.setMailSieveScript(filterScriptPattern1);

           String rawReal = "From: sender@zimbra.com\n" + "To: test1@zimbra.com\n" + "Subject: Test\n" + "\n" + "Hello World";

           List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                   new ParsedMessage(rawReal.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                   Mailbox.ID_FOLDER_INBOX, true);

           // message should be stored in foo
           Integer item = mbox.getItemIds(null,mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "foo").getId()).getIds(MailItem.Type.MESSAGE).get(0);
           Message msg = mbox.getMessageById(null, item);
           Assert.assertEquals("Hello World", msg.getFragment());

           // message should not be stored in inbox
           Assert.assertNull(mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE));
       } catch (Exception e) {
           e.printStackTrace();
           fail("No exception should be thrown");
       }
   }

   /*
   fileinto :copy foo;
   if header :contains "Subject" "Test" {
   discard;
   }

   if message has "Subject: real" ==> it should be stored in "foo" and INBOX
 */

@Test

public void testCopyFileIntoPattern3Real() {

    try {
        String filterScriptPattern1 = "require [\"copy\", \"fileinto\"];\n"
            + "fileinto :copy \"foo\";"
            + "if header :contains \"Subject\" \"Test\" {\n"
            + "discard; }";

        Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

        RuleManager.clearCachedRules(account);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        account.setMailSieveScript(filterScriptPattern1);

        String rawReal = "From: sender@zimbra.com\n" + "To: test1@zimbra.com\n" + "Subject: Real\n" + "\n" + "Hello World";

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(rawReal.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                Mailbox.ID_FOLDER_INBOX, true);

        // message should be stored in foo
        Integer item = mbox.getItemIds(null,mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "foo").getId()).getIds(MailItem.Type.MESSAGE).get(0);
        Message msg = mbox.getMessageById(null, item);
        Assert.assertEquals("Hello World", msg.getFragment());

        // message should be stored in inbox
        item = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
        msg = mbox.getMessageById(null, item);
        Assert.assertEquals("Hello World", msg.getFragment());
    } catch (Exception e) {
        e.printStackTrace();
        fail("No exception should be thrown");
    }
}
}
