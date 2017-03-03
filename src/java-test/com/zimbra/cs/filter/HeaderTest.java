/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.JMSession;

public class HeaderTest {
    private static String sampleMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "x-priority: 1\n"
            + "from: xyz@example.com\n"
            + "Subject: =?ISO-2022-JP?B?GyRCJDMkcyRLJEEkTxsoQg==?=\n"
            + "to: foo@example.com, baz@example.com\n"
            + "cc: qux@example.com\n";

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
    public void testHeaderExistsNonEmptyKey() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"x-priority\"] [\"1\"] { tag \"zimbra\"; }";
        doTest(filterScript, "zimbra");
    }

    @Test
    public void testHeaderExistsEmptyKey() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"x-priority\"] [\"\"] { tag \"zimbra\"; }";
        doTest(filterScript, "zimbra");
    }

    @Test
    public void testHeaderDoesNotExists() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"not-exist\"] [\"1\"] { tag \"zimbra\"; }";
        doTest(filterScript, null);
    }

    @Test
    public void testHeaderDoesNotExistsEmptyKey() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"not-exist\"] [\"\"] { tag \"zimbra\"; }";
        doTest(filterScript, null);
    }

    @Test
    public void testEmptyHeaderEmptyKey() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"\"] [\"\"] { tag \"zimbra\"; }";
        doTest(filterScript, null);
    }

    private void doTest(String filterScript, String expectedResult) {
        try {
            LmtpEnvelope env = setEnvelopeInfo();
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(expectedResult, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    private LmtpEnvelope setEnvelopeInfo() {
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<abc@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient1 = new LmtpAddress("<xyz@zimbra.com>", null, null);
        LmtpAddress recipient2 = new LmtpAddress("<uvw@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient1);
        env.addLocalRecipient(recipient2);
        return env;
    }

    public void singleMimePart() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if header \"Subject\" \"important\" { flag \"priority\"; }");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        String msgContent = "From: test@zimbra.com\nSubject: important";
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(msgContent.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertTrue(msg.isTagged(FlagInfo.PRIORITY));
    }

    @Test
    public void RFC822Attached() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if header :is \"Subject\" \"Attached HTML message\" { flag \"priority\"; }");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        InputStream is = getClass().getResourceAsStream("TestFilter-testBodyContains.msg");
        MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(mm, false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertFalse(msg.isTagged(FlagInfo.PRIORITY));
    }

    @Test
    public void fileAttached() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if header :contains \"Content-Disposition\" \"attachment.txt\" { flag \"priority\"; }");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        InputStream is = getClass().getResourceAsStream("TestFilter-testBodyContains.msg");
        MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(mm, false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertFalse(msg.isTagged(FlagInfo.PRIORITY));
    }
}
