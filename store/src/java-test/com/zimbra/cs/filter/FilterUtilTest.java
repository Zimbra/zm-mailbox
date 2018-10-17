/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2015, 2016, 2017 Synacor, Inc.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;

import static com.zimbra.cs.filter.JsieveConfigMapHandler.CAPABILITY_VARIABLES;

/**
 * Unit tests for {@link FilterUtil}.
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class FilterUtilTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Account acct1 = prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Server server = Provisioning.getInstance().getServer(acct1);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void truncateBody() throws Exception {
        // truncate a body containing a multi-byte char
        String body = StringUtil.truncateIfRequired("Andr\u00e9", 5);

        Assert.assertTrue("truncated body should not have a partial char at the end", "Andr".equals(body));
    }

    @Test
    public void noBody() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String content =
                "From: user1@example.com\r\n"
                + "To: user2@example.com\r\n"
                + "Subject: test\r\n"
                + "Content-Type: application/octet-stream;name=\"test.pdf\"\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + "R0a1231312ad124svsdsal=="; //obviously not a real pdf
        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);
        Map<String, String> vars = FilterUtil.getVarsMap(mbox, parsedMessage, parsedMessage.getMimeMessage());
    }

    @Test
    public void noHeaders() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String content = "just some content";
        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);
        Map<String, String> vars = FilterUtil.getVarsMap(mbox, parsedMessage, parsedMessage.getMimeMessage());

    }

    /*
     * Create and initialize the ZimbraMailAdapter object 
     */
    private ZimbraMailAdapter initZimbraMailAdapter() throws ServiceException {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        IncomingMessageHandler handler = new IncomingMessageHandler(
                new OperationContext(mbox), new DeliveryContext(),
                mbox, "test@zimbra.com",
                new ParsedMessage("From: test1@zimbra.com".getBytes(), false),
                0, Mailbox.ID_FOLDER_INBOX, true);
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mbox, handler);
        
        // Set various variables
        mailAdapter.addVariable("var", "hello");
        List<String> matchedValues = new ArrayList<String>();
        matchedValues.add("test1");
        matchedValues.add("test2");
        mailAdapter.setMatchedValues(matchedValues);

        return mailAdapter;
    }

    @Test
    public void testVariableReplacementVariableOn() {
        try {
            ZimbraMailAdapter mailAdapter = initZimbraMailAdapter();

            // Variable feature: ON
            mailAdapter.setVariablesExtAvailable(ZimbraMailAdapter.VARIABLEFEATURETYPE.AVAILABLE);
            mailAdapter.addCapabilities(CAPABILITY_VARIABLES);

            String varValue = FilterUtil.replaceVariables(mailAdapter, "${var}");
            Assert.assertEquals("hello", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${0}");
            Assert.assertEquals("test1", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${var!}");
            Assert.assertEquals("${var!}", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${var2}");
            Assert.assertEquals("", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${test${var}");
            Assert.assertEquals("${testhello", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${test${var}");
            Assert.assertEquals("${testhello", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "\\\\${President, ${var} Inc.}");
            Assert.assertEquals("\\\\${President, hello Inc.}", varValue);

            // set "company" "ACME";
            // set "a.b" "おしらせ"; (or any non-ascii characters)
            // set "c_d" "C";
            // set "1" "One"; ==> Should be ignored or error [Note 1]
            // set "23" "twenty three"; ==> Should be ignored or error [Note 1]
            // set "combination" "Hello ${company}!!";
            mailAdapter.addVariable("var", "hello");

            mailAdapter.addVariable("company", "ACME");
            mailAdapter.addVariable("a_b", "\u304a\u3057\u3089\u305b");
            mailAdapter.addVariable("c_d", "C");
            mailAdapter.addVariable("1", "One");
            mailAdapter.addVariable("23", "twenty three");
            mailAdapter.addVariable("combination", "Hello ACME!!");

            varValue = FilterUtil.replaceVariables(mailAdapter, "${full}");
            Assert.assertEquals("", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${company}");
            Assert.assertEquals("ACME", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${BAD${Company}");
            Assert.assertEquals("${BADACME", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${company");
            Assert.assertEquals("${company", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${${COMpANY}}");
            Assert.assertEquals("${ACME}", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${a_b}}");
            Assert.assertEquals("\u304a\u3057\u3089\u305b}", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "$c_d}}");
            Assert.assertEquals("$c_d}}", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "You've got a mail. ${a_b} ${combination} ${c_d}hao!");
            Assert.assertEquals("You've got a mail. \u304a\u3057\u3089\u305b Hello ACME!! Chao!", varValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e);
        }
    }
    
    @Test
    public void testVariableReplacementQutdAndEncoded() {
        try {
            ZimbraMailAdapter mailAdapter = initZimbraMailAdapter();
            mailAdapter.setVariablesExtAvailable(ZimbraMailAdapter.VARIABLEFEATURETYPE.AVAILABLE);
            mailAdapter.addCapabilities(CAPABILITY_VARIABLES);

            String varValue = FilterUtil.replaceVariables(mailAdapter, "${va\\r}");
            Assert.assertEquals("hello", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${}");
            Assert.assertEquals("${}", varValue);

            mailAdapter.addVariable("var", "hel\\*lo");
            varValue = FilterUtil.replaceVariables(mailAdapter, "${var}");
            Assert.assertEquals("hel\\*lo", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "hello${test}");
            Assert.assertEquals("hello", varValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e);
        }
    }

    @Test
    public void testToJavaRegex() {
        String regex = FilterUtil.sieveToJavaRegex("coyote@**.com");
        Assert.assertEquals("coyote@(.*?)(.*)\\.com", regex);
    }
}
