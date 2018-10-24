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
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class MimeHeaderTest {
    private static String sampleMsg = "from: xyz@example.com\n"
            + "Subject: test message\n"
            + "to: foo@example.com, baz@example.com\n"
            + "cc: qux@example.com\n"
            + "Subject: Bonjour\n"
            + "MIME-Version: 1.0\n"
            + "Content-Type: multipart/mixed; boundary=\"----=_Part_64_1822363563.1505482033554\"\n"
            + "\n"
            + "------=_Part_64_1822363563.1505482033554\n"
            + "Content-Type: text/plain; charset=utf-8\n"
            + "Content-Transfer-Encoding: 7bit\n"
            + "\n"
            + "Test message 2\n"
            + "------=_Part_64_1822363563.1505482033554\n"
            + "Content-Type: message/rfc822\n"
            + "Content-Disposition: attachment\n"
            + "\n"
            + "Date: Fri, 15 Sep 2017 22:26:43 +0900 (JST)\n"
            + "From: admin@synacorjapan.com\n"
            + "To: user1 <user1@synacorjapan.com>\n"
            + "Message-ID: <523389747.44.1505482003470.JavaMail.zimbra@synacorjapan.com>\n"
            + "Subject: Hello\n"
            + "MIME-Version: 1.0\n"
            + "Content-Type: multipart/alternative; boundary=\"=_37c6ca38-873e-4a06-ad29-25a254075e83\"\n"
            + "\n"
            + "--=_37c6ca38-873e-4a06-ad29-25a254075e83\n"
            + "Content-Type: text/plain; charset=utf-8\n"
            + "Content-Transfer-Encoding: 7bit\n"
            + "\n"
            + "This is a sample email\n"
            + "\n"
            + "--=_37c6ca38-873e-4a06-ad29-25a254075e83\n"
            + "Content-Type: text/html; charset=utf-8\n"
            + "Content-Transfer-Encoding: 7bit\n"
            + "\n"
            + "<html><body><div style=\"font-family: arial, helvetica, sans-serif; font-size: 12pt; color: #000000\"><div>Test message</div></div></body></html>\n"
            + "--=_37c6ca38-873e-4a06-ad29-25a254075e83--\n"
            + "\n"
            + "------=_Part_64_1822363563.1505482033554--\n";

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
        // Default match type :is is used.
        String filterScript = "require [\"tag\", \"comparator-i;ascii-numeric\"];"
                + "if mime_header :comparator \"i;ascii-numeric\" \"Subject\" \"Hello\" {\n"
                + "  tag \"is\";\n"
                + "} else {\n"
                + "  tag \"not is\";\n"
                + "}";
        doTest(filterScript, "is");
    }

    /*
     * The ascii-numeric comparator should be looked up in the list of the "require".
     */
    @Test
    public void testMissingComparatorNumericDeclaration() throws Exception {
        // Default match type :is is used.
        // No "comparator-i;ascii-numeric" capability text in the require command
        String filterScript = "require [\"tag\"];"
                + "if mime_header :comparator \"i;ascii-numeric\" \"Subject\" \"Hello\" {\n"
                + "  tag \"is\";\n"
                + "} else {\n"
                + "  tag \"not is\";\n"
                + "}";
        doTest(filterScript, null);
    }

    private void doTest(String filterScript, String expected) throws Exception {
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
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(),
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(expected, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }
}
