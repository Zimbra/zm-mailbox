/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;


/**
 * @author zimbra
 *
 */
public class ModifyFilterRulesTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testBug82649_BlankFilter() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);

        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        rule.addAttribute(MailConstants.A_NAME, "Test1");
        Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "Junk");
        filteraction.addElement(MailConstants.E_ACTION_STOP);
        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
            fail("This test is expected to throw exception. ");
        } catch (ServiceException e) {
            String expected = "invalid request: missing required attribute: value";
            assertTrue(e.getMessage().indexOf(expected)  != -1);
            assertNotNull(e);
        }
    }

}
