/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.RuleRewriter;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapProtocol;



public class TestFilter
extends TestCase {

    /*
     * <pre>
     * <rules>
     * <r name="quote test" active="1">
     * <g op="anyof">
     * <c name="header" k0="subject" op=":is" k1="a &quot; b"/>
     * </g>
     * <action name="keep"/>
     * <action name="stop"/>
     * </r>
     * </rules>
     * </pre>
     */
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
        Element rules = Element.create(SoapProtocol.Soap12, MailService.E_RULES);

        Element rule = rules.addElement(MailService.E_RULE);
        rule.addAttribute(MailService.A_NAME, "testQuoteValidation");
        rule.addAttribute(MailService.A_ACTIVE, true);
        
        Element group =
            rule.addElement(MailService.E_CONDITION_GROUP).addAttribute(MailService.A_OPERATION, "anyof");
        
        Element condition = group.addElement(MailService.E_CONDITION);
        condition.addAttribute(MailService.A_NAME, "header");
        condition.addAttribute("k0", "subject");
        condition.addAttribute("op", ":is");
        condition.addAttribute("k1", "a \" b");
        
        rule.addElement(MailService.E_ACTION).addAttribute(MailService.A_NAME, "keep");
        rule.addElement(MailService.E_ACTION).addAttribute(MailService.A_NAME, "stop");
        
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
}
