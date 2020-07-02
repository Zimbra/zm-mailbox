/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016, 2017 Synacor, Inc.
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
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class BodyTest {

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
    public void testBug106637_InvalidCharset() throws Exception {
        String script  = "if anyof (body :contains \"Bug 106637\") {\n"
                       + "  flag \"flagged\";\n"
                       + "}\n";

        String message = "From: test@zimbra.com\n"
                       + "Subject: test\n"
                       + "Content-Type: text/plain; charset=\"undefined_charset\"\n"
                       + "\n"
                       + "Bug 106637\n";
        test(script, message);
    }

    @Test
    public void testValidCharset() throws Exception {
        String script  = "if anyof (body :contains \"utf-8\") {\n"
                + "  flag \"flagged\";\n"
                + "}\n";

        String message = "From: test@zimbra.com\n"
                + "Subject: test\n"
                + "Content-Type: text/plain; charset=\"UTF-8\"\n"
                + "\n"
                + "This message is written in utf-8 charset \n";
        test(script, message);
    }

    @Test
    public void testCharsetISO2022JP() throws Exception {
        String script  = "if anyof (body :contains \"シフトJIS\") {\n"
                + "  flag \"flagged\";\n"
                + "}\n";

        String message = "From: test@zimbra.com\n"
                + "Subject: test\n"
                + "Content-Type: text/plain; charset=\"ISO-2022-JP\"\n"
                + "\n"
                + "このメッセージはシフトJISで書かれています\n";

        test(script, new String(message.getBytes("ISO2022JP")));
    }

    private void test(String script, String message) throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(script);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertTrue(msg.isTagged(FlagInfo.FLAGGED));
    }

    /*
     * The ascii-numeric comparator should be looked up in the list of the "require".
     */
    @Test
    public void testMissingComparatorNumericDeclaration() throws Exception {
        // Default match type :is is used.
        // No "comparator-i;ascii-numeric" capability text in the require command
        String filterScript = "require [\"tag\"];"
                + "if body :contains :comparator \"i;ascii-numeric\" \"Sample message\" {\n"
                + "  tag \"contains\";\n"
                + "} else {\n"
                + "  tag \"not contains\";\n"
                + "}";
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.unsetAdminSieveScriptBefore();
            account.unsetMailSieveScript();
            account.unsetAdminSieveScriptAfter();
            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage("To: test1@zimbra.com\nSubject: test\n\nSample message".getBytes(), false), 0,
                    account.getName(),
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown " + e);
        }
    }
}
