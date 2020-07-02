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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.DeliveryServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.jsieve.ErejectException;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.SendMsgTest.DirectInsertionMailboxManager;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class ErejectTest {

    private static String sampleBaseMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";

    private String filterScript = "require [\"ereject\"];\n"
            + "if header :contains \"from\" \"test2@zimbra.com\" {\n"
            + "  ereject text:\r\n"
            + "I am not taking mail from you, and I don’t\n"
            + "want your birdseed, either!\r\n"
            + ".\r\n"
            + "  ;\n"
            + "}";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new DirectInsertionMailboxManager());

    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    /*
     * applyRulesToIncomingMessage() should throw an exception to cancel the message delivery.
     * No message is delivered.
     *
     * The following error will be logged:
     * ERROR - Evaluation failed. Reason: 'ereject' action refuses delivery of a message. Sieve rule evaluation is cancelled
     */
    @Test
    public void test() {
        Account acct1 = null;
        Mailbox mbox1 = null;
        boolean isPassed = false;
        try {
            acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<test2@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                    sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                    env, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
        } catch (DeliveryServiceException e) {
            if (e.getCause() instanceof ErejectException){
                try {
                    List<Integer> items = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                            .getIds(MailItem.Type.MESSAGE);
                    Assert.assertEquals(null, items);
                    isPassed = true;
                } catch (Exception ex) {
                    fail("No exception should be thrown: " + ex.getMessage());
                }
            } else {
                fail("No exception other than DeliveryServiceException/ErejectException should be thrown: " + e.getMessage());
            }
        } catch (Exception e) {
             fail("No exception should be thrown: " + e.getMessage());
        }
        if (!isPassed) {
            fail("DeliveryServiceException/ErejectException should have been thrown, but no exception is thrown");
        }
    }
    
    /*
     * applyRulesToIncomingMessage() should throw an exception to cancel the message delivery.
     * No message is delivered.
     *
     * The following error will be logged:
     * ERROR - Evaluation failed. Reason: 'ereject' action refuses delivery of a message. Sieve rule evaluation is cancelled
     */
    @Test
    public void testThatSenderRcdUnDeliveredEmail() {
        Account acct1 = null;
        Mailbox mbox1 = null;
        
        Account acct2 = null;
        Mailbox mbox2 = null;

        try {
            acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            
            acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
            mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<test2@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                    sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                    env, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
        } catch (DeliveryServiceException e) {
            if (e.getCause() instanceof ErejectException){
                try {
                    List<Integer> items = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                            .getIds(MailItem.Type.MESSAGE);
                    Assert.assertEquals(null, items);
                } catch (Exception ex) {
                	ex.printStackTrace();
                    fail("No exception should be thrown: " + ex.getMessage());
                }
            } else {
                fail("No exception other than DeliveryServiceException/ErejectException should be thrown: " + e.getMessage());
            }
        } catch (Exception e) {
             fail("No exception should be thrown: " + e.getMessage());
        }
    }
}
