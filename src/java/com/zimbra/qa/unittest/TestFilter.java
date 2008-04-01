/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
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
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.zclient.ZFilterAction;
import com.zimbra.cs.zclient.ZFilterCondition;
import com.zimbra.cs.zclient.ZFilterRule;
import com.zimbra.cs.zclient.ZFilterRules;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.cs.zclient.ZFilterAction.MarkOp;
import com.zimbra.cs.zclient.ZFilterAction.ZDiscardAction;
import com.zimbra.cs.zclient.ZFilterAction.ZFileIntoAction;
import com.zimbra.cs.zclient.ZFilterAction.ZKeepAction;
import com.zimbra.cs.zclient.ZFilterAction.ZMarkAction;
import com.zimbra.cs.zclient.ZFilterAction.ZTagAction;
import com.zimbra.cs.zclient.ZFilterCondition.HeaderOp;
import com.zimbra.cs.zclient.ZFilterCondition.ZHeaderCondition;
import com.zimbra.cs.zclient.ZMessage.Flag;


public class TestFilter
extends TestCase {

    private static String USER_NAME = "user1";
    private static String NAME_PREFIX = "TestFilter";
    private static String TAG_TEST_BASE64_SUBJECT = NAME_PREFIX + "-testBase64Subject";
    private static String FOLDER1_NAME = NAME_PREFIX + "-folder1";
    private static String FOLDER2_NAME = NAME_PREFIX + "-folder2";
    private static String FOLDER1_PATH = "/" + FOLDER1_NAME;
    private static String FOLDER2_PATH = "/" + FOLDER2_NAME;
    private static String TAG1_NAME = NAME_PREFIX + "-tag1";
    private static String TAG2_NAME = NAME_PREFIX + "-tag2";
    
    private ZMailbox mMbox;
    private ZFilterRules mOriginalRules;
    private String mOriginalSpamApplyUserFilters;

    public void setUp()
    throws Exception {
        mMbox = TestUtil.getZMailbox(USER_NAME);
        cleanUp();
        mOriginalRules = mMbox.getFilterRules();
        mMbox.saveFilterRules(getRules());
        
        Account account = TestUtil.getAccount(USER_NAME);
        mOriginalSpamApplyUserFilters = account.getAttr(Provisioning.A_zimbraSpamApplyUserFilters);
    }
    
    public void testQuoteValidation()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        // if subject contains "a " b", keep
        ZFilterCondition condition = new ZHeaderCondition("subject", HeaderOp.CONTAINS, "a \" b");
        ZFilterAction action = new ZKeepAction();
        conditions.add(condition);
        actions.add(action);
        rules.add(new ZFilterRule("test quotes", true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        try {
            mMbox.saveFilterRules(zRules);
            fail("Saving filter rules with quotes should not have succeeded");
        } catch (SoapFaultException e) {
            assertTrue("Unexpected exception: " + e, e.getMessage().contains("Doublequote not allowed"));
        }
        
        // if subject contains "a \ b", keep
        conditions.clear();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "a \\ b"));
        rules.clear();
        rules.add(new ZFilterRule("test backslash", true, false, conditions, actions));
        try {
            mMbox.saveFilterRules(zRules);
            fail("Saving filter rules with backslash should not have succeeded");
        } catch (SoapFaultException e) {
            assertTrue("Unexpected exception: " + e, e.getMessage().contains("Backslash not allowed"));
        }
    }
    
    /**
     * Confirms that a message with a base64-encoded subject can be filtered correctly
     * (bug 11219).
     */
    public void testBase64Subject()
    throws Exception {
        // Note: tag gets created implicitly when filter rules are saved
        String address = TestUtil.getAddress(USER_NAME);
        TestUtil.addMessageLmtp(
            "=?UTF-8?B?W2l0dnNmLUluY2lkZW5jaWFzXVs0OTc3Ml0gW2luY2lkZW5jaWFzLXZpbGxhbnVldmFdIENvcnRlcyBkZSBsdXosIGTDrWEgMjUvMDkvMjAwNi4=?=",
            address, address);
        List<ZMessage> messages = TestUtil.search(mMbox, "villanueva");
        assertEquals("Unexpected number of messages", 1, messages.size());
        List<ZTag> tags = mMbox.getTags(messages.get(0).getTagIds());
        assertEquals("Unexpected number of tags", 1, tags.size());
        assertEquals("Tag didn't match", TAG_TEST_BASE64_SUBJECT, tags.get(0).getName());
    }

    /**
     * Confirms that all actions are performed when a message matches multiple
     * filter rules.
     */
    public void testMatchMultipleFilters()
    throws Exception {
        String sender = TestUtil.getAddress("multiplefilters");
        String recipient = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " This goes into folder1 and folder2";
        TestUtil.addMessageLmtp(subject, recipient, sender);
        
        ZMessage msg = TestUtil.getMessage(mMbox, "in:" + FOLDER1_PATH + " " + subject);
        TestUtil.verifyTag(mMbox, msg, TAG1_NAME);
        TestUtil.verifyTag(mMbox, msg, TAG2_NAME);
        TestUtil.verifyFlag(mMbox, msg, Flag.flagged);
        
        msg = TestUtil.getMessage(mMbox, "in:" + FOLDER2_PATH + " " + subject);
        TestUtil.verifyTag(mMbox, msg, TAG1_NAME);
        TestUtil.verifyTag(mMbox, msg, TAG2_NAME);
        TestUtil.verifyFlag(mMbox, msg, Flag.flagged);
    }
    
    /**
     * Confirms that spam filtering takes a higher precedence than 
     * custom user filtering (bug 23886).
     */
    public void testSpam()
    throws Exception {
    	ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
    	String sender = TestUtil.getAddress(USER_NAME);
    	String[] recipients = new String[] { sender };
    	String message = TestUtil.getTestMessage(NAME_PREFIX + " testSpam", USER_NAME, USER_NAME, null);
    	Config config = Provisioning.getInstance().getConfig();
    	message = config.getAttr(Provisioning.A_zimbraSpamHeader) + ": " +
    		config.getAttr(Provisioning.A_zimbraSpamHeaderValue) + "\r\n" + message;
    	
    	// Make sure spam message doesn't already exist
    	assertEquals(0, TestUtil.search(mbox, "in:junk subject:testSpam").size());
    	assertEquals(0, TestUtil.search(mbox, "in:" + FOLDER1_PATH + " subject:testSpam").size());
    	
    	// Deliver spam message without matching rule, make sure it gets delivered
    	// to the junk folder, and delete. 
    	TestUtil.addMessageLmtp(recipients, sender, message);
    	ZMessage msg = TestUtil.waitForMessage(mbox, "in:junk subject:testSpam");
    	mbox.deleteMessage(msg.getId());
    	
    	// Add matching filter rule: if subject contains "testSpam", file into folder1
    	ZFilterRules rules = getRules();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "testSpam"));
        actions.add(new ZFileIntoAction(FOLDER1_PATH));
        rules.getRules().add(new ZFilterRule("testBug5455", true, false, conditions, actions));
        mbox.saveFilterRules(rules);
        
        // Set "apply user rules" attribute to TRUE and make sure the message gets filed into folder1
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraSpamApplyUserFilters, LdapUtil.LDAP_TRUE);
    	TestUtil.addMessageLmtp(recipients, sender, message);
    	msg = TestUtil.waitForMessage(mbox, "in:" + FOLDER1_PATH + " subject:testSpam");
    	mbox.deleteMessage(msg.getId());
        
    	// Set "apply user rules" attribute to FALSE and make sure the message gets filed into junk
    	TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraSpamApplyUserFilters, LdapUtil.LDAP_FALSE);
    	TestUtil.addMessageLmtp(recipients, sender, message);
    	TestUtil.waitForMessage(mbox, "in:junk subject:testSpam");
    }
    
    /**
     * Verifies the fix to bug 5455.  Tests sending a message that matches
     * two filter rules, each of which has a tag action and a flag action.   
     */
    public void testBug5455()
    throws Exception {
        String recipient = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " Testing bug5455";
        TestUtil.addMessageLmtp(subject, recipient, recipient);
        
        ZMessage msg = TestUtil.getMessage(mMbox, "in:" + FOLDER1_PATH + " " + subject);
        TestUtil.verifyFlag(mMbox, msg, Flag.flagged);
        TestUtil.verifyTag(mMbox, msg, TAG1_NAME);
        
        msg = TestUtil.getMessage(mMbox, "in:" + FOLDER2_PATH + " " + subject);
        TestUtil.verifyFlag(mMbox, msg, Flag.flagged);
        TestUtil.verifyTag(mMbox, msg, TAG1_NAME);
    }

    /**
     * Tests the discard filter rule (bug 26604).
     */
    public void testDiscard()
    throws Exception {
        String recipient = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " doDiscard";
        TestUtil.addMessageLmtp(subject, recipient, recipient);
        subject = NAME_PREFIX + " dontDiscard";
        TestUtil.addMessageLmtp(subject, recipient, recipient);
        TestUtil.waitForMessage(mMbox, "in:inbox dontDiscard");
        assertEquals(0, TestUtil.search(mMbox, "in:inbox doDiscard").size());
    }
    protected void tearDown() throws Exception {
        mMbox.saveFilterRules(mOriginalRules);
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraSpamApplyUserFilters, mOriginalSpamApplyUserFilters);
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        
        // Clean up messages created by testBase64Subject()
        for (ZMessage msg : TestUtil.search(mMbox, "villanueva")) {
            mMbox.deleteMessage(msg.getId());
        }
    }
    
    private ZFilterRules getRules()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        // if subject contains "villanueva", tag with testBase64Subject and stop
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "villanueva"));
        actions.add(new ZTagAction(TAG_TEST_BASE64_SUBJECT));
        rules.add(new ZFilterRule("testBase64Subject", true, false, conditions, actions));

        // if subject contains "folder1", file into folder1 and tag with tag1
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "folder1"));
        actions.add(new ZFileIntoAction(FOLDER1_PATH));
        actions.add(new ZTagAction(TAG1_NAME));
        rules.add(new ZFilterRule("testMatchMultipleFilters1", true, false, conditions, actions));

        // if from contains "multiplefilters", file into folder 2, tag with tag2 and flag
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("from", HeaderOp.CONTAINS, "multiplefilters"));
        actions.add(new ZFileIntoAction(FOLDER2_PATH));
        actions.add(new ZTagAction(TAG2_NAME));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testMatchMultipleFilters2", true, false, conditions, actions));
        
        // if subject contains bug5455, flag, file into folder1, tag with tag1, file into folder2
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "bug5455"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        actions.add(new ZFileIntoAction(FOLDER1_PATH));
        actions.add(new ZTagAction(TAG1_NAME));
        actions.add(new ZFileIntoAction(FOLDER2_PATH));
        rules.add(new ZFilterRule("testBug5455", true, false, conditions, actions));
        
        // if subject contains "discard", discard the message
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "doDiscard"));
        actions.add(new ZDiscardAction());
        rules.add(new ZFilterRule("testDiscard", true, false, conditions, actions));
        
        return new ZFilterRules(rules);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestFilter.class));
    }
}
