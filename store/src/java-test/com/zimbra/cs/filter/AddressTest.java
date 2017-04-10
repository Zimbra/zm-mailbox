/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.JMSession;

/**
 * @author zimbra
 *
 */
public class AddressTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@in.telligent.com", "secret",
                new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void filterValidToField() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if anyof (address :domain :is :comparator \"i;ascii-casemap\" "
                    + "[\"to\"] \"in.telligent.com\",address :domain :is :comparator \"i;ascii-casemap\" [\"to\"] "
                    + "\"in.telligent.com\") {" + "tag \"Priority\";}";

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, new ParsedMessage(
                            "To: test1@in.telligent.com".getBytes(), false), 0,
                    account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("Priority",
                    ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown");
        }

    }

    @Test
    public void filterInValidToField() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if anyof (address :domain :is :comparator \"i;ascii-casemap\" "
                    + "[\"to\"] \"in.telligent.com\",address :domain :is :comparator \"i;ascii-casemap\" [\"to\"] "
                    + "\"in.telligent.com\") {" + "tag \"Priority\";}";

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, new ParsedMessage(
                            "To: undisclosed-recipients:;".getBytes(), false),
                    0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }

    }

    @Test
    public void noComparator() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if address :matches [\"to\"] \"*\" {"
                                + "  tag \"noComparator\";"
                                + "}";

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage("to: foo@example.com".getBytes(), false),
                    0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("noComparator", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testAddressContainingBackslash() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if address :comparator \"i;ascii-casemap\" :matches \"from\" \"\\\"user\\\\1\\\"@cosmonaut.zimbra.com\" {"
                                + "  tag \"TestBackslash\";"
                                + "}";

            account.setMailSieveScript(filterScript);
            InputStream is = getClass().getResourceAsStream("TestFilter-testBackslashDotInAddress.msg");
            MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(mm, false),
                    0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("TestBackslash", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testAddressContainingDot() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if address :comparator \"i;ascii-casemap\" :matches \"to\" \"user.1@cosmonaut.zimbra.com\" {"
                                + "  tag \"TestDot\";"
                                + "}";

            account.setMailSieveScript(filterScript);
            InputStream is = getClass().getResourceAsStream("TestFilter-testBackslashDotInAddress.msg");
            MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(mm, false),
                    0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("TestDot", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testAddressContainingDoubleQuote() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if address :comparator \"i;ascii-casemap\" :matches \"to\" \"\\\"user\\\"1\\\"@cosmonaut.zimbra.com\" {"
                                + "  tag \"TestDoubleQuote\";"
                                + "}";

            account.setMailSieveScript(filterScript);
            InputStream is = getClass().getResourceAsStream("TestFilter-testQuotesInAddress.msg");
            MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(mm, false),
                    0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("TestDoubleQuote", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testAddressContainingSingleQuote() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if address :comparator \"i;ascii-casemap\" :matches \"from\" \"user'1@cosmonaut.zimbra.com\" {"
                                + "  tag \"TestSingleQuote\";"
                                + "}";

            account.setMailSieveScript(filterScript);
            InputStream is = getClass().getResourceAsStream("TestFilter-testQuotesInAddress.msg");
            MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(mm, false),
                    0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("TestSingleQuote", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testAddressContainingQuestionMark() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if address :comparator \"i;ascii-casemap\" :matches \"from\" \"user?1@cosmonaut.zimbra.com\" {"
                                + "  tag \"TestQuestionMark\";"
                                + "}";

            account.setMailSieveScript(filterScript);
            InputStream is = getClass().getResourceAsStream("TestFilter-testQuestionMarkCommaInAddress.msg");
            MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(mm, false),
                    0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("TestQuestionMark", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testAddressContainingComma() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            String filterScript = "if address :comparator \"i;ascii-casemap\" :matches \"to\" \"\\\"user,1\\\"@cosmonaut.zimbra.com\" {"
                                + "  tag \"TestComma\";"
                                + "}";

            account.setMailSieveScript(filterScript);
            InputStream is = getClass().getResourceAsStream("TestFilter-testQuestionMarkCommaInAddress.msg");
            MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(mm, false),
                    0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("TestComma", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void compareEmptyStringWithAsciiNumeric() {
        try {
            Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(acct);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

            String filterScript = "if address :is :comparator \"i;ascii-numeric\" \"To\" \"\" {"
                                + "  tag \"compareEmptyStringWithAsciiNumeric\";"
                                + "}";

            acct.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, new ParsedMessage("To: test1@zimbra.com".getBytes(), false), 0,
                    acct.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("compareEmptyStringWithAsciiNumeric", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void compareHeaderNameWithLeadingSpaces() {
        try {
            Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(acct);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

            String filterScript = "require [\"tag\"];\n"
                    + "if address :is :comparator \"i;ascii-numeric\" \" To\" \"test1@zimbra.com\" {"
                    + "  tag \"t1\";"
                    + "}"
                    ;

            acct.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, new ParsedMessage("To: test1@zimbra.com".getBytes(), false), 0,
                    acct.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(0, msg.getTags().length);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void compareHeaderNameWithTrailingSpaces() {
        try {
            Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(acct);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

            String filterScript = "require [\"tag\"];\n"
                    + "if address :is :comparator \"i;ascii-numeric\" \"To \" \"test1@zimbra.com\" {"
                    + "  tag \"t2\";"
                    + "}"
                    ;

            acct.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, new ParsedMessage("To: test1@zimbra.com".getBytes(), false), 0,
                    acct.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(0, msg.getTags().length);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void compareHeaderNameWithLeadingAndTrailingSpaces() {
        try {
            Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(acct);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

            String filterScript = "require [\"tag\"];\n"
                    + "if address :is :comparator \"i;ascii-numeric\" \" To \" \"test1@zimbra.com\" {"
                    + "  tag \"t3\";"
                    + "}"
                    ;

            acct.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, new ParsedMessage("To: test1@zimbra.com".getBytes(), false), 0,
                    acct.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(0, msg.getTags().length);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }
}
