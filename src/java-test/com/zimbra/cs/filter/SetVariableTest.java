package com.zimbra.cs.filter;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

public class SetVariableTest {
	private String filterScript = "require [\"variables\"];\n" + "set \"var\" \"hello\" ;"
			+ "if header :matches \"Subject\" \"*\" { tag \"${var}\"; }";

	@BeforeClass
	public static void init() throws Exception {
		MailboxTestUtil.initServer();
		Provisioning prov = Provisioning.getInstance();
		prov.createAccount("test@in.telligent.com", "secret", new HashMap<String, Object>());
	}

	@Before
	public void setUp() throws Exception {
		MailboxTestUtil.clearData();
	}

	@Test
	public void testSetVar() {
		try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			System.out.println(filterScript);
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
					+ "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));

		} catch (Exception e) {
			fail("No exception should be thrown");
		}

	}

	@Test
	public void testSetVarAndUseInHeader() {
		try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			filterScript = "require [\"variables\"];\n" + "set \"var\" \"hello\" ;"
					+ "if header :contains \"Subject\" \"${var}\" { tag \"blue\"; }";
			System.out.println(filterScript);
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n"
					+ "Subject: hello version 1.0 is out\n" + "\n" + "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("blue", ArrayUtil.getFirstElement(msg.getTags()));

		} catch (Exception e) {
			fail("No exception should be thrown");
		}

	}

	@Test
	public void testSetVarAndUseInAction() {
		try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			filterScript = "require [\"variables\"];\n"
					+ "if header :matches \"Subject\" \"*\"{set \"var\" \"hello\" ; tag \"${var}\"; }";
			System.out.println(filterScript);
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n"
					+ "Subject: hello version 1.0 is out\n" + "\n" + "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));

		} catch (Exception e) {
			fail("No exception should be thrown");
		}

	}


}
