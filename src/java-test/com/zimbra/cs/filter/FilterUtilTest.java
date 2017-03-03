/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * Unit tests for {@link FilterUtil}.
 */
public class FilterUtilTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
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

    
    
    public void testVariableReplacement() {
    	Map<String, String> variables = new HashMap<String, String>();
    	variables.put("var", "hello");
    	List<String> matchedValues = new ArrayList<String>();
    	String varValue = FilterUtil.replaceVariables(variables, matchedValues, "${var}");
    	Assert.assertEquals("hello", varValue);
    	
    	
    	matchedValues = new ArrayList<String>();
    	matchedValues.add("test1");
    	matchedValues.add("test2");
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${0}");
    	Assert.assertEquals("test1", varValue);
    	
    	
    	variables = new HashMap<String, String>();
    	variables.put("var", "hello");
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${var!}");
    	Assert.assertEquals("${var!}", varValue);
    	
    	
    	variables = new HashMap<String, String>();
    	variables.put("var", "hello");
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${var2}");
    	Assert.assertEquals("", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${test${var}");
    	Assert.assertEquals("${testhello", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${test${var}");
    	Assert.assertEquals("${testhello", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "\\\\${President, ${var} Inc.}");
    	Assert.assertEquals("\\${President, hello Inc.}", varValue);
    	
    	// set "company" "ACME";
			// set "a.b" "おしらせ"; (or any non-ascii characters)
			// set "c_d" "C";
			// set "1" "One"; ==> Should be ignored or error [Note 1]
			// set "23" "twenty three"; ==> Should be ignored or error [Note 1]
			// set "combination" "Hello ${company}!!";
    	variables = new HashMap<String, String>();
    	variables.put("company", "ACME");
    	variables.put("a.b", "おしらせ");
    	variables.put("c_d", "C");
    	variables.put("1", "One");
    	variables.put("23", "twenty three");
    	variables.put("combination", "Hello ACME!!");

    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${full}");
    	Assert.assertEquals("", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${company}");
    	Assert.assertEquals("ACME", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${BAD${Company}");
    	Assert.assertEquals("${BADACME", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${company");
    	Assert.assertEquals("${company", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${${COMpANY}}");
    	Assert.assertEquals("${ACME}", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${a.b}}");
    	Assert.assertEquals("おしらせ", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "$c_d}}");
    	Assert.assertEquals("C", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "You've got a mail. ${a.b} ${combination} ${c_d}hao!");
    	Assert.assertEquals("You've got a mail. おしらせ Hello ACME!! Chao!", varValue);
    }
    
    @Test
    public void testVariableReplacementQutdAndEncoded() {
    	Map<String, String> variables = new HashMap<String, String>();
    	variables.put("var", "hello");
    	List<String> matchedValues = new ArrayList<String>();
    	String varValue = FilterUtil.replaceVariables(variables, matchedValues, "${va\\r}");
    	Assert.assertEquals("hello", varValue);
    	
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${va\\\\r}");
    	Assert.assertEquals("${va\\r}", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "\\${var}");
    	Assert.assertEquals("hello", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "\\\\${var}");
    	Assert.assertEquals("\\hello", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${}");
    	Assert.assertEquals("${}", varValue);
    	
    	variables.put("var", "hel\\*lo");
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "${var}");
    	Assert.assertEquals("hel\\*lo", varValue);
    	
    	varValue = FilterUtil.replaceVariables(variables, matchedValues, "hello${test}");
    	Assert.assertEquals("hello", varValue);
    	
    }
    
    @Test
    public void testToJavaRegex() {
    	String regex = FilterUtil.sieveToJavaRegex("coyote@**.com");
    	Assert.assertEquals("coyote@(.*)?(.*)?\\.com", regex);
    	
    }
}
