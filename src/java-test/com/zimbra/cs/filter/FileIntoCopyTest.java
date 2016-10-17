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
}
