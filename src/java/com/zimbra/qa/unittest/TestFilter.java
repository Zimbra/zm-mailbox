/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.List;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.RuleRewriter;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;


public class TestFilter
extends TestCase {

    private static String USER_NAME = "user1";
    private static String NAME_PREFIX = "TestFilter";
    private static String TAG_NAME = NAME_PREFIX + "-testBase64Subject";

    private static final String FILTER_RULES = StringUtil.join("\n", new String[] {
        "require [\"fileinto\", \"reject\", \"tag\", \"flag\"];",
        "",
        "# testBase64Subject",
        "if anyof (header :contains \"subject\" \"villanueva\" )",
        "{",
        "    tag \"" + TAG_NAME + "\";",
        "    stop;",
        "}"
    });
    
    private String mOriginalRules;
    
    public void setUp()
    throws Exception {
        super.setUp();
        cleanUp();

        // Remember original rules and set rules for this test
        RuleManager rm = RuleManager.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        mOriginalRules = rm.getRules(account);
        rm.setRules(account, FILTER_RULES);
    }
    
    public void testQuoteValidation()
    throws Exception {
        /*
          <rules>
            <r name="quote test" active="1">
              <g op="anyof">
                <c name="header" k0="subject" op=":is" k1="a &quot; b"/>
              </g>
              <action name="keep"/>
              <action name="stop"/>
            </r>
          </rules>
         */
        Element rules = Element.create(SoapProtocol.Soap12, MailConstants.E_RULES);

        Element rule = rules.addElement(MailConstants.E_RULE);
        rule.addAttribute(MailConstants.A_NAME, "testQuoteValidation");
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        
        Element group =
            rule.addElement(MailConstants.E_CONDITION_GROUP).addAttribute(MailConstants.A_OPERATION, "anyof");
        
        Element condition = group.addElement(MailConstants.E_CONDITION);
        condition.addAttribute(MailConstants.A_NAME, "header");
        condition.addAttribute("k0", "subject");
        condition.addAttribute("op", ":is");
        condition.addAttribute("k1", "a \" b");
        
        rule.addElement(MailConstants.E_ACTION).addAttribute(MailConstants.A_NAME, "keep");
        rule.addElement(MailConstants.E_ACTION).addAttribute(MailConstants.A_NAME, "stop");
        
        Mailbox mbox = TestUtil.getMailbox("user1");
        
        // Test quotes
        RuleRewriter rr = new RuleRewriter(rules, mbox);
        try {
            ZimbraLog.test.debug("Generated Sieve script:\n" + rr.getScript());
            fail("XML-to-script conversion should not have succeeded");
        } catch (ServiceException e) {
            boolean foundError = e.getMessage().contains("Doublequote not allowed");
            if (!foundError) {
                ZimbraLog.test.error("Unexpected exception", e);
                fail("Unexpected error message: " + e.getMessage());
            }
        }
        
        // Test backslash
        condition.addAttribute("k1", "a \\ b");
        rr = new RuleRewriter(rules, mbox);
        try {
            ZimbraLog.test.debug("Generated Sieve script:\n" + rr.getScript());
            fail("XML-to-script conversion should not have succeeded");
        } catch (ServiceException e) {
            boolean foundError = e.getMessage().contains("Backslash not allowed");
            if (!foundError) {
                ZimbraLog.test.error("Unexpected exception", e);
                fail("Unexpected error message: " + e.getMessage());
            }
        }
    }
    
    /**
     * Confirms that a message with a base64-encoded subject can be filtered correctly
     * (bug 11219).
     */
    public void testBase64Subject()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Tag tag = mbox.createTag(null, TAG_NAME, Tag.DEFAULT_COLOR);
        String address = TestUtil.getAddress(USER_NAME);
        TestUtil.insertMessageLmtp(1,
            "=?UTF-8?B?W2l0dnNmLUluY2lkZW5jaWFzXVs0OTc3Ml0gW2luY2lkZW5jaWFzLXZpbGxhbnVldmFdIENvcnRlcyBkZSBsdXosIGTDrWEgMjUvMDkvMjAwNi4=?=",
            address, address);
        List<Integer> ids = TestUtil.search(mbox, "villanueva", MailItem.TYPE_MESSAGE);
        assertEquals("Unexpected number of messages", 1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0));
        List<Tag> tagList = msg.getTagList();
        assertEquals("Unexpected number of tags", 1, tagList.size());
        assertEquals("Tag didn't match", tag.getId(), tagList.get(0).getId());
    }

    protected void tearDown() throws Exception {
        // Restore original rules
        RuleManager rm = RuleManager.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        rm.setRules(account, mOriginalRules);
        
        cleanUp();
        super.tearDown();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        
        // Clean up messages created byt testBase64Subject() 
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        List<Integer> ids = TestUtil.search(mbox, "villanueva", MailItem.TYPE_MESSAGE);
        for (int id : ids) {
            mbox.delete(null, id, MailItem.TYPE_MESSAGE);
        }
    }
}
