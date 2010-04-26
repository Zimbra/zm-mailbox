/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;

import com.zimbra.common.mime.MimeMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.RuleRewriter;
import com.zimbra.cs.filter.SieveToSoap;
import com.zimbra.cs.filter.SoapToSieve;
import com.zimbra.cs.zclient.ZFilterAction;
import com.zimbra.cs.zclient.ZFilterCondition;
import com.zimbra.cs.zclient.ZFilterRule;
import com.zimbra.cs.zclient.ZFilterRules;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.cs.zclient.ZFilterAction.MarkOp;
import com.zimbra.cs.zclient.ZFilterAction.ZDiscardAction;
import com.zimbra.cs.zclient.ZFilterAction.ZFileIntoAction;
import com.zimbra.cs.zclient.ZFilterAction.ZKeepAction;
import com.zimbra.cs.zclient.ZFilterAction.ZMarkAction;
import com.zimbra.cs.zclient.ZFilterAction.ZRedirectAction;
import com.zimbra.cs.zclient.ZFilterAction.ZTagAction;
import com.zimbra.cs.zclient.ZFilterCondition.BodyOp;
import com.zimbra.cs.zclient.ZFilterCondition.DateOp;
import com.zimbra.cs.zclient.ZFilterCondition.HeaderOp;
import com.zimbra.cs.zclient.ZFilterCondition.ZAttachmentExistsCondition;
import com.zimbra.cs.zclient.ZFilterCondition.ZBodyCondition;
import com.zimbra.cs.zclient.ZFilterCondition.ZDateCondition;
import com.zimbra.cs.zclient.ZFilterCondition.ZHeaderCondition;
import com.zimbra.cs.zclient.ZFilterCondition.ZInviteCondition;
import com.zimbra.cs.zclient.ZMessage.Flag;

public class TestFilter
extends TestCase {

    private static String USER_NAME = "user1";
    private static String REMOTE_USER_NAME = "user2";
    private static String NAME_PREFIX = "TestFilter";
    private static String TAG_TEST_BASE64_SUBJECT = NAME_PREFIX + "-testBase64Subject";
    private static String FOLDER1_NAME = NAME_PREFIX + "-folder1";
    private static String FOLDER2_NAME = NAME_PREFIX + "-folder2";
    private static String FOLDER1_PATH = "/" + FOLDER1_NAME;
    private static String FOLDER2_PATH = "/" + FOLDER2_NAME;
    private static String TAG1_NAME = NAME_PREFIX + "-tag1";
    private static String TAG2_NAME = NAME_PREFIX + "-tag2";
    private static String MOUNTPOINT_FOLDER_NAME = NAME_PREFIX + " mountpoint";
    private static String MOUNTPOINT_SUBFOLDER_NAME = NAME_PREFIX + " mountpoint subfolder";
    private static String MOUNTPOINT_SUBFOLDER_PATH = "/" + MOUNTPOINT_FOLDER_NAME + "/" + MOUNTPOINT_SUBFOLDER_NAME;
    
    private ZMailbox mMbox;
    private ZFilterRules mOriginalRules;
    private String mOriginalSpamApplyUserFilters;
    private String mOriginalSmtpPort = null;
    private String mOriginalSetEnvelopeSender = null;

    public void setUp()
    throws Exception {
        mMbox = TestUtil.getZMailbox(USER_NAME);
        cleanUp();

        // Create mountpoint for testMountpoint()
        ZMailbox remoteMbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        TestUtil.createMountpoint(remoteMbox, "/" + MOUNTPOINT_FOLDER_NAME, mMbox, MOUNTPOINT_FOLDER_NAME);
        TestUtil.createFolder(remoteMbox, MOUNTPOINT_SUBFOLDER_PATH);
        
        mOriginalRules = mMbox.getFilterRules();
        saveRules(mMbox, getRules());
        
        Account account = TestUtil.getAccount(USER_NAME);
        mOriginalSpamApplyUserFilters = account.getAttr(Provisioning.A_zimbraSpamApplyUserFilters);
        mOriginalSmtpPort = Provisioning.getInstance().getLocalServer().getSmtpPortAsString();
        mOriginalSetEnvelopeSender = TestUtil.getServerAttr(Provisioning.A_zimbraMailRedirectSetEnvelopeSender); 
    }
    
    public void testQuoteEscape()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        
        String folderName = NAME_PREFIX + " testQuoteEscape";
        TestUtil.createFolder(mMbox, folderName);

        // if subject contains "a " b", file into folder
        ZFilterCondition condition = new ZHeaderCondition("subject", HeaderOp.CONTAINS, "a \" b");
        ZFilterAction action = new ZFileIntoAction(folderName);
        conditions.add(condition);
        actions.add(action);
        rules.add(new ZFilterRule(folderName, true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);

        // Add a message and confirm it gets filed into the correct folder
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " a \" b y z";
        String searchSubject = NAME_PREFIX + " a b y z"; // Search barfs on unmatched quote 
        TestUtil.addMessageLmtp(subject, address, address);
        TestUtil.getMessage(mMbox, "in:\"" + folderName + "\" subject:\"" + searchSubject + "\"");
    }
    
    public void testBackslashEscape()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        
        String folderName = NAME_PREFIX + " testBackslashEscape";
        TestUtil.createFolder(mMbox, folderName);

        // if subject contains "a \ b", file into folder
        ZFilterCondition condition = new ZHeaderCondition("subject", HeaderOp.CONTAINS, "a \\ b");
        ZFilterAction action = new ZFileIntoAction(folderName);
        conditions.add(condition);
        actions.add(action);
        rules.add(new ZFilterRule(folderName, true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);

        // Add a message and confirm it gets filed into the correct folder
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " a \\ b y z";
        TestUtil.addMessageLmtp(subject, address, address);
        TestUtil.getMessage(mMbox, "in:\"" + folderName + "\" subject:\"" + subject + "\"");
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
        // bug 36705 List<ZMessage> messages = TestUtil.search(mMbox, "villanueva");
        List<ZMessage> messages = TestUtil.search(mMbox, "Cortes de luz");
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
    	
    	// Deliver spam message without matching rule, make sure it gets delivered
    	// to the junk folder, and delete.
    	TestUtil.addMessageLmtp(recipients, sender, message);
    	ZMessage msg = TestUtil.getMessage(mbox, "in:junk subject:testSpam");
    	mbox.deleteMessage(msg.getId());
    	
    	// Add matching filter rule: if subject contains "testSpam", file into folder1
    	ZFilterRules rules = getRules();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "testSpam"));
        actions.add(new ZFileIntoAction(FOLDER1_PATH));
        rules.getRules().add(new ZFilterRule("testBug5455", true, false, conditions, actions));
        saveRules(mMbox, rules);
        
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
    
    /**
     * Tests filing into a mountpoint folder.
     * @throws Exception
     */
    public void testMountpoint()
    throws Exception {
        // Send message.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " mountpointRoot1";
        TestUtil.addMessageLmtp(subject, address, address);
        
        // Confirm that it gets filed into the mountpoint folder and not in inbox.
        TestUtil.waitForMessage(mbox, "in:\"/" + MOUNTPOINT_FOLDER_NAME + "\" subject:\"" + subject + "\"");
        assertEquals(0, TestUtil.search(mbox, "in:inbox subject:\"" + subject + "\"").size());
        
        // Delete the remote folder.
        ZMailbox remote = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ZFolder remoteFolder = remote.getFolderByPath("/" + MOUNTPOINT_FOLDER_NAME);
        remote.deleteFolder(remoteFolder.getId());
        
        // Send another message, confirm that it gets filed into inbox and not in the mountpoint folder.
        subject = NAME_PREFIX + " mountpointRoot2";
        TestUtil.addMessageLmtp(subject, address, address);
        TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        assertEquals(0, TestUtil.search(mbox,
            "in:\"/" + MOUNTPOINT_FOLDER_NAME + "\" subject:\"" + subject + "\"").size());
    }
    
    public void testMountpointSubfolder()
    throws Exception {
        // Send message.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " mountpointSub1";
        TestUtil.addMessageLmtp(subject, address, address);
        
        // Confirm that it gets filed into the mountpoint subfolder and not in inbox.
        TestUtil.waitForMessage(mbox, "in:\"" + MOUNTPOINT_SUBFOLDER_PATH + "\" subject:\"" + subject + "\"");
        assertEquals(0, TestUtil.search(mbox, "in:inbox subject:\"" + subject + "\"").size());
        
        // Delete the remote subfolder.
        ZMailbox remote = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ZFolder remoteFolder = remote.getFolderByPath(MOUNTPOINT_SUBFOLDER_PATH);
        remote.deleteFolder(remoteFolder.getId());
        
        // Send another message, confirm that it gets filed into inbox and not in the mountpoint subfolder.
        subject = NAME_PREFIX + " mountpointSub2";
        TestUtil.addMessageLmtp(subject, address, address);
        TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        assertEquals(0, TestUtil.search(mbox,
            "in:\"" + MOUNTPOINT_SUBFOLDER_PATH + "\" subject:\"" + subject + "\"").size());
    }

    /**
     * Tests {@link FilterUtil#escape}.
     */
    public void testEscape() {
        doTestEscape("Hello, \"Dave\"", "Hello, \\\"Dave\\\"");
        doTestEscape("\\/\\/", "\\\\/\\\\/");
        doTestEscape("\"\\\"", "\\\"\\\\\\\"");
    }
    
    private void doTestEscape(String original, String escaped) {
        assertEquals(escaped, FilterUtil.escape(original));
        assertEquals(original, FilterUtil.unescape(escaped));
    }

    /**
     * Tests {@link SoapToSieve} and {@link SieveToSoap}.
     */
    public void testConversion()
    throws Exception {
        // Load script.
        String scriptPath = "/opt/zimbra/unittest/test.sieve";
        String script = new String(ByteUtil.getContent(new File(scriptPath)));
        assertNotNull(script);
        assertTrue(script.length() > 0);
        String convertedScript = normalize(script);

        // Compare result.
        script = normalizeWhiteSpace(script);
        convertedScript = normalizeWhiteSpace(convertedScript);
        
        // Change "fileInto" to "fileinto".  We have both in the test script
        // to test case insensitivity, but the converted script will only have
        // the lower case version.
        script = script.replace("fileInto", "fileinto");
        assertEquals(script, convertedScript);
    }
    
    public void testMarkRead()
    throws Exception {
        String folderName = NAME_PREFIX + " testMarkRead";
        
        // if the subject contains "testMarkRead", file into a folder and mark read
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "testMarkRead"));
        actions.add(new ZFileIntoAction(folderName));
        actions.add(new ZMarkAction(MarkOp.READ));
        rules.add(new ZFilterRule("testMarkRead", true, false, conditions, actions));
        saveRules(mMbox, new ZFilterRules(rules));
        
        // Deliver message.
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " testMarkRead";
        TestUtil.addMessageLmtp(subject, address, address);
        
        // Check folder and unread state.
        ZMessage msg = TestUtil.getMessage(mMbox, "in:\"" + folderName + "\" subject:\"" + subject + "\"");
        String flags = msg.getFlags();
        assertTrue("Unexpected flags: " + flags,
            flags == null || flags.indexOf(ZMessage.Flag.unread.getFlagChar()) < 0);
    }
    
    /**
     * Confirms that the header test works with headers that are folded.
     * See section 2.2.3 of RFC 2822 and bug 14942.
     */
    public void testHeaderFolding()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        
        // if subject contains "a b c", mark as flagged
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "a b c"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testHeaderFolding", true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);

        // Add a message and confirm it is flagged
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " a b\r\n c";
        TestUtil.addMessageLmtp(subject, address, address);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"a b c\"");
        assertTrue("Message was not flagged", msg.isFlagged());
    }
    
    public void testInvite()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        
        // Create tags.
        String prefix = NAME_PREFIX + " testInvite ";
        
        ZTag tagNoMethod = mbox.createTag(prefix + "no method", null);
        ZTag tagAnyReply = mbox.createTag(prefix + "any reply", null);
        ZTag tagAnyRequest = mbox.createTag(prefix + "any request", null);
        ZTag tagRequestOrCancel = mbox.createTag(prefix + "request or cancel", null);
        ZTag tagReply = mbox.createTag(prefix + "reply", null);
        
        // Create filter rules that set tags based on conditions.
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        ZFilterCondition condition = new ZInviteCondition(true);
        ZFilterAction action = new ZTagAction(tagNoMethod.getName());
        rules.add(createRule("testInvite - no method", condition, action));
        
        condition = new ZInviteCondition(true, ZInviteCondition.METHOD_ANYREPLY);
        action = new ZTagAction(tagAnyReply.getName());
        rules.add(createRule("testInvite - any reply", condition, action));
        
        condition = new ZInviteCondition(true, ZInviteCondition.METHOD_ANYREQUEST);
        action = new ZTagAction(tagAnyRequest.getName());
        rules.add(createRule("testInvite - any request", condition, action));
        
        condition = new ZInviteCondition(true, Arrays.asList(ZInviteCondition.METHOD_REQUEST, ZInviteCondition.METHOD_CANCEL));
        action = new ZTagAction(tagRequestOrCancel.getName());
        rules.add(createRule("testInvite - request or cancel", condition, action));
        
        condition = new ZInviteCondition(true, ZInviteCondition.METHOD_REPLY);
        action = new ZTagAction(tagReply.getName());
        rules.add(createRule("testInvite - reply", condition, action));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);

        // Send an invite from user2 and check tags.
        ZMailbox organizer = TestUtil.getZMailbox(REMOTE_USER_NAME);
        String subject = NAME_PREFIX + " testInvite request 1";
        Date startDate = new Date(System.currentTimeMillis() + Constants.MILLIS_PER_DAY);
        Date endDate = new Date(startDate.getTime() + Constants.MILLIS_PER_HOUR);
        TestUtil.createAppointment(organizer, subject, mbox.getName(), startDate, endDate);
        
        // Get message and check tags.
        ZMessage msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        Set<String> tagIds = getTagIds(msg);
        assertTrue(tagIds.contains(tagNoMethod.getId()));
        assertFalse(tagIds.contains(tagAnyReply.getId()));
        assertTrue(tagIds.contains(tagAnyRequest.getId()));
        assertTrue(tagIds.contains(tagRequestOrCancel.getId()));
        assertFalse(tagIds.contains(tagReply.getId()));
        
        // Now test filtering a reply to an invite.  Send an invite to user2,
        // and have user2 accept the appointment.
        organizer = TestUtil.getZMailbox(USER_NAME);
        mbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        subject = NAME_PREFIX + " testInvite request 2";
        startDate = new Date(startDate.getTime() + Constants.MILLIS_PER_DAY);
        endDate = new Date(endDate.getTime() + Constants.MILLIS_PER_DAY);
        TestUtil.createAppointment(organizer, subject, mbox.getName(), startDate, endDate);
        
        // Receive the invite and accept the appointment.
        msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        subject = NAME_PREFIX + " testInvite reply";
        TestUtil.sendInviteReply(mbox, msg.getId(), organizer.getName(), subject, ZMailbox.ReplyVerb.ACCEPT);
        msg = TestUtil.waitForMessage(organizer, "in:inbox subject:\"" + subject + "\"");
        
        // Check tags on the invite reply.
        tagIds = getTagIds(msg);
        assertTrue(tagIds.contains(tagNoMethod.getId()));
        assertTrue(tagIds.contains(tagAnyReply.getId()));
        assertFalse(tagIds.contains(tagAnyRequest.getId()));
        assertFalse(tagIds.contains(tagRequestOrCancel.getId()));
        assertTrue(tagIds.contains(tagReply.getId()));
    }
    
    private ZFilterRule createRule(String name, ZFilterCondition condition, ZFilterAction action) {
        return new ZFilterRule(name, true, false, Arrays.asList(condition), Arrays.asList(action));
    }
    
    /**
     * Make sure we disallow more than four asterisks in a :matches condition (bug 35983).
     */
    public void testManyAsterisks()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        ZFilterCondition condition = new ZHeaderCondition(
            "from", HeaderOp.MATCHES, "*****address@yahoo.com");
        ZFilterAction action = new ZKeepAction();
        conditions.add(condition);
        actions.add(action);
        rules.add(new ZFilterRule("test many asterisks", true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        try {
            mMbox.saveFilterRules(zRules);
            fail("Saving filter rules with quotes should not have succeeded");
        } catch (SoapFaultException e) {
            assertTrue("Unexpected exception: " + e, e.getMessage().contains("four asterisks"));
        }
    }
    
    public void testDateFiltering()
    throws Exception {
        // Before tomorrow.
        ZTag tagBeforeTomorrow = mMbox.createTag(NAME_PREFIX + " before tomorrow", null);
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        conditions.add(new ZDateCondition(DateOp.BEFORE, new Date(System.currentTimeMillis() + Constants.MILLIS_PER_DAY)));
        actions.add(new ZTagAction(tagBeforeTomorrow.getName()));
        rules.add(new ZFilterRule("before tomorrow", true, false, conditions, actions));

        // After yesterday.
        ZTag tagAfterYesterday = mMbox.createTag(NAME_PREFIX + " after yesterday", null);
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZDateCondition(DateOp.AFTER, new Date(System.currentTimeMillis() - Constants.MILLIS_PER_DAY)));
        actions.add(new ZTagAction(tagAfterYesterday.getName()));
        rules.add(new ZFilterRule("after yesterday", true, false, conditions, actions));
        
        // Save rules.
        ZFilterRules zRules = new ZFilterRules(rules);
        mMbox.saveFilterRules(zRules);
        
        // Old message.
        String[] recipients = new String[] { USER_NAME };
        String subject = NAME_PREFIX + " testDateFiltering old";
        String content = TestUtil.getTestMessage(subject, USER_NAME, USER_NAME,
            new Date(System.currentTimeMillis() - (2 * Constants.MILLIS_PER_DAY)));
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZMessage msg = TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        Set<String> tagIds = getTagIds(msg);
        assertEquals(1, tagIds.size());
        assertTrue(tagIds.contains(tagBeforeTomorrow.getId()));
        
        // Current message.
        subject = NAME_PREFIX + " testDateFiltering current";
        content = TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null);
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        msg = TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        tagIds = getTagIds(msg);
        assertEquals(2, tagIds.size());
        assertTrue(tagIds.contains(tagAfterYesterday.getId()));
        assertTrue(tagIds.contains(tagBeforeTomorrow.getId()));
        
        // Future message.
        subject = NAME_PREFIX + " testDateFiltering future";
        content = TestUtil.getTestMessage(subject, USER_NAME, USER_NAME,
            new Date(System.currentTimeMillis() + (2 * Constants.MILLIS_PER_DAY)));
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        msg = TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        tagIds = getTagIds(msg);
        assertEquals(1, tagIds.size());
        assertTrue(tagIds.contains(tagAfterYesterday.getId()));
        
        // No date header (bug 44079).
        subject = NAME_PREFIX + " testDateFiltering no date header";
        content = removeHeader(TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null), "Date");
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        msg = TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        tagIds = getTagIds(msg);
        assertEquals(2, tagIds.size());
        assertTrue(tagIds.contains(tagAfterYesterday.getId()));
        assertTrue(tagIds.contains(tagBeforeTomorrow.getId()));
    }
    
    private Set<String> getTagIds(ZMessage msg) {
        Set<String> tagIds = new HashSet<String>();
        String tagIdString = msg.getTagIds();
        if (tagIdString != null) {
            for (String tagId : tagIdString.split(",")) {
                tagIds.add(tagId);
            }
        }
        return tagIds;
    }
    
    private String removeHeader(String content, String headerName)
    throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line = null;
        String start = headerName + ": ";
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith(start)) {
                buf.append(line).append("\r\n");
            }
        }
        return buf.toString();
    }
    
    /**
     * Tests matching a header against a wildcard expression.  See bug 21701.
     */
    public void testHeaderMatches()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        
        // If subject matches the first for characters + *, mark as flagged.
        String pattern = NAME_PREFIX.substring(0, 4) + "*";
        conditions.add(new ZHeaderCondition("subject", HeaderOp.MATCHES, pattern));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testHeaderMatches", true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);

        // Add a message and confirm it is flagged
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " testHeaderMatches";
        TestUtil.addMessageLmtp(subject, address, address);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:testHeaderMatches");
        assertTrue("Message was not flagged", msg.isFlagged());
    }
    
    /**
     * Confirms that the body test looks for text in the main message
     * body and attached message bodies, but not in attachments.
     */
    public void testBodyContains()
    throws Exception {
        doBodyContainsTest("text version of the main body", true);
        doBodyContainsTest("HTML version of the main body", true);
        doBodyContainsTest("text attachment", false);
        doBodyContainsTest("HTML attachment", false);
        doBodyContainsTest("text version of the attached message body", true);
        doBodyContainsTest("HTML version the attached message body", true);
        doBodyContainsTest("body of a plain attached message", true);
        
        // Make sure test is case-insensitive (bug 36905).
        doBodyContainsTest("TeXt VeRsIoN", true);
        doBodyContainsTest("hTmL vErSiOn", true);
        
        // Check the case where the entire line matches (bug 33793).
        doBodyContainsTest("This is the text version of the main body of the message.", true);
        
        // Check the case where the substring spans multiple lines.
        doBodyContainsTest("This is the text version of the main body of the message. This is the second line.", true);
    }
    
    private void doBodyContainsTest(String substring, boolean contains)
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        
        // If subject matches the first for characters + *, mark as flagged.
        conditions.add(new ZBodyCondition(BodyOp.CONTAINS, substring));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testBodyContains", true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);

        // Add a message and test the flagged state.
        String address = TestUtil.getAddress(USER_NAME);
        String msgContent = new String(
            ByteUtil.getContent(new File("/opt/zimbra/unittest/TestFilter-testBodyContains.msg")));
        TestUtil.addMessageLmtp(new String[] { address }, address, msgContent);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:testBodyContains");
        assertEquals("Unexpected message flag state", contains, msg.isFlagged());
        mMbox.deleteMessage(msg.getId());
    }

    /**
     * Tests the redirect filter action and confirms that the X-ZimbraForwarded
     * header is set on the redirected message.
     */
    public void testRedirect()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        
        // if subject contains "testRedirect", redirect to user2
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "testRedirect"));
        actions.add(new ZRedirectAction(TestUtil.getAddress(REMOTE_USER_NAME)));
        rules.add(new ZFilterRule("testRedirect", true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);

        // Add a message.  Set the From header to something bogus to make
        // sure we're not rewriting it
        String from = "joebob@mycompany.com";
        String subject = NAME_PREFIX + " testRedirect";
        TestUtil.addMessageLmtp(subject, USER_NAME, from);
        
        // Confirm that user1 did not receive it.
        List<ZMessage> messages = TestUtil.search(mMbox, "subject:\"" + subject + "\"");
        assertEquals(0, messages.size());
        
        // Confirm that user2 received it, and make sure X-ZimbraForwarded is set.
        ZMailbox remoteMbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ZMessage msg = TestUtil.waitForMessage(remoteMbox, "in:inbox subject:\"" + subject + "\"");
        byte[] content = TestUtil.getContent(remoteMbox, msg.getId()).getBytes();
        MimeMessage mimeMsg = new MimeMessage(new ByteArrayInputStream(content));
        Account user1 = TestUtil.getAccount(USER_NAME);
        assertEquals(user1.getName(), mimeMsg.getHeader(FilterUtil.HEADER_FORWARDED));
        assertEquals(from, mimeMsg.getHeader("From"));
        
        // Check zimbraMailRedirectSetEnvelopeSender=FALSE. 
        int port = 6025;
        DummySmtpServer smtp = new DummySmtpServer(port);
        Thread smtpServerThread = new Thread(smtp);
        smtpServerThread.start();
        Server server = Provisioning.getInstance().getLocalServer();
        server.setSmtpPort(port);
        server.setMailRedirectSetEnvelopeSender(false);
        
        TestUtil.addMessageLmtp(subject, USER_NAME, from);
        assertEquals(from, smtp.getMailFrom());

        // Check zimbraMailRedirectSetEnvelopeSender=TRUE. 
        smtp = new DummySmtpServer(port);
        smtpServerThread = new Thread(smtp);
        smtpServerThread.start();
        server.setMailRedirectSetEnvelopeSender(true);
        
        TestUtil.addMessageLmtp(subject, USER_NAME, from);
        assertEquals(TestUtil.getAddress(USER_NAME), smtp.getMailFrom());
    }
    
    
    /**
     * Confirms that the message gets delivered even when a mail loop occurs.
     */
    public void testRedirectMailLoop()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        
        // if subject contains "testRedirectMailLoop", redirect to user1
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "testRedirectMailLoop"));
        actions.add(new ZRedirectAction(TestUtil.getAddress(USER_NAME)));
        rules.add(new ZFilterRule("testRedirectMailLoop", true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);

        // Add a message.
        String subject = NAME_PREFIX + " testRedirectMailLoop";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        
        // Confirm that user1 received it.
        ZMessage msg = TestUtil.waitForMessage(mMbox, "subject:\"" + subject + "\"");
        byte[] content = TestUtil.getContent(mMbox, msg.getId()).getBytes();
        MimeMessage mimeMsg = new MimeMessage(new ByteArrayInputStream(content));
        Account user1 = TestUtil.getAccount(USER_NAME);
        assertEquals(user1.getName(), mimeMsg.getHeader(FilterUtil.HEADER_FORWARDED));
    }
    
    public void testAttachment()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        
        // If attachment exists, set tag1.  If attachment doesn't exist, set tag2.
        ZTag tag1 = mMbox.createTag(NAME_PREFIX + " testAttachment1", null);
        ZTag tag2 = mMbox.createTag(NAME_PREFIX + " testAttachment2", null);
        
        conditions.add(new ZAttachmentExistsCondition(true));
        actions.add(new ZTagAction(tag1.getName()));
        rules.add(new ZFilterRule("testAttachment1", true, false, conditions, actions));
        
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZAttachmentExistsCondition(false));
        actions.add(new ZTagAction(tag2.getName()));
        rules.add(new ZFilterRule("testAttachment2", true, false, conditions, actions));
        
        ZFilterRules zRules = new ZFilterRules(rules);
        saveRules(mMbox, zRules);
        
        // Add a message with an attachment.
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " testAttachment1";
        String msgContent =  new MessageBuilder().withSubject(subject).withAttachment("attach this", "attach.txt", "text/plain").create();
        TestUtil.addMessageLmtp(new String[] { address }, address, msgContent);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        
        // Check the tag states.
        Set<String> tagIds = getTagIds(msg);
        assertEquals(1, tagIds.size());
        assertTrue(tagIds.contains(tag1.getId()));

        // Add a message with no attachment.
        subject = NAME_PREFIX + " testAttachment2";
        msgContent =  new MessageBuilder().withSubject(subject).create();
        TestUtil.addMessageLmtp(new String[] { address }, address, msgContent);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        
        // Check the tag states.
        tagIds = getTagIds(msg);
        assertEquals(1, tagIds.size());
        assertTrue(tagIds.contains(tag2.getId()));
    }
    
    /**
     * Tests {@link FilterUtil#parseSize}.
     */
    public void testParseSize()
    throws Exception {
        assertEquals(10, FilterUtil.parseSize("10"));
        assertEquals(10 * 1024, FilterUtil.parseSize("10k"));
        assertEquals(10 * 1024, FilterUtil.parseSize("10K"));
        assertEquals(10 * 1024 * 1024, FilterUtil.parseSize("10M"));
        assertEquals(10 * 1024 * 1024 * 1024, FilterUtil.parseSize("10G"));
        try {
            FilterUtil.parseSize("10Q");
            fail("Should not have parsed bogus size value.");
        } catch (NumberFormatException e) {
        }
    }
    
    /**
     * Tests the old <tt>GetRulesRequest</tt> and <tt>SaveRulesRequest</tt>
     * SOAP API's (bug 42320).
     */
    public void testOldApi()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        Account account = TestUtil.getAccount(USER_NAME);
        String oldRules = account.getMailSieveScript();
        
        // Get rules and save the same rules.
        Element response = mbox.invoke(new XMLElement(MailConstants.GET_RULES_REQUEST));
        Element rulesEl = response.getElement(MailConstants.E_RULES).detach();
        RuleRewriter.sanitizeRules(rulesEl);
        Element request = rulesEl.getFactory().createElement(MailConstants.SAVE_RULES_REQUEST);
        request.addElement(rulesEl);
        response = mbox.invoke(request);
        
        // Make sure the rules haven't changed.
        account = TestUtil.getAccount(USER_NAME);
        oldRules = normalize(oldRules);
        String newRules = normalize(account.getMailSieveScript());
        assertEquals(oldRules, newRules);
    }
    
    /**
     * Converts the script to XML and back again.
     */
    private String normalize(String script)
    throws ParseException, ServiceException {
        List<String> ruleNames = RuleManager.getRuleNames(script);
        Node node = RuleManager.getSieveFactory().parse(new ByteArrayInputStream(script.getBytes()));
        
        // Convert from Sieve to SOAP and back again. 
        SieveToSoap sieveToSoap = new SieveToSoap(XMLElement.mFactory, ruleNames);
        sieveToSoap.accept(node);
        SoapToSieve soapToSieve = new SoapToSieve(sieveToSoap.getRootElement());
        
        return soapToSieve.getSieveScript();
    }
    
    public void testStripBracketsAndQuotes()
    throws Exception {
        assertEquals(null, RuleRewriter.stripBracketsAndQuotes(null));
        assertEquals("", RuleRewriter.stripBracketsAndQuotes(""));
        assertEquals("x", RuleRewriter.stripBracketsAndQuotes("x"));
        assertEquals("x", RuleRewriter.stripBracketsAndQuotes("[\"x\"]"));
        assertEquals("x", RuleRewriter.stripBracketsAndQuotes("\"x\""));
        assertEquals("x\"", RuleRewriter.stripBracketsAndQuotes("x\""));
        assertEquals("[\"x\"", RuleRewriter.stripBracketsAndQuotes("[\"x\""));
    }
    
    /**
     * Confirms that we handle the negative test flag properly when the first
     * test is negative and the second test is not (bug 46007).
     */
    public void testNegativeAndPositiveTest()
    throws Exception {
        String script = new String(ByteUtil.getContent(new File("/opt/zimbra/unittest/bug46007.sieve")));
        String normalized = normalize(script); // Convert to XML and back again.
        assertEquals(normalizeWhiteSpace(script), normalizeWhiteSpace(normalized));
    }
    
    private String normalizeWhiteSpace(String script) {
        StringBuilder buf = new StringBuilder(script.length());
        boolean inWhiteSpace = false;
        for (int i = 0; i < script.length(); i++) {
            String c = script.substring(i, i + 1);
            if (c.matches("\\s") || c.equals("\r") || c.equals("\n")) {
                if (!inWhiteSpace) {
                    buf.append(" ");
                    inWhiteSpace = true;
                }
            } else {
                inWhiteSpace = false;
                buf.append(c);
            }
        }
        return buf.toString();
    }
    
    protected void tearDown() throws Exception {
        mMbox.saveFilterRules(mOriginalRules);
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraSpamApplyUserFilters, mOriginalSpamApplyUserFilters);
        TestUtil.setServerAttr(Provisioning.A_zimbraSmtpPort, mOriginalSmtpPort);
        TestUtil.setServerAttr(Provisioning.A_zimbraMailRedirectSetEnvelopeSender, mOriginalSetEnvelopeSender);
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(REMOTE_USER_NAME, NAME_PREFIX);
        
        // Clean up messages created by testBase64Subject()
        // bug 36705 for (ZMessage msg : TestUtil.search(mMbox, "villanueva")) {
        for (ZMessage msg : TestUtil.search(mMbox, "cortes de luz")) {
            mMbox.deleteMessage(msg.getId());
        }
    }
    
    private ZFilterRules getRules()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        // if subject contains "Cortes de luz", tag with testBase64Subject and stop
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        // bug 36705 conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "villanueva"));
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "Cortes de luz"));
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
        
        // if subject contains "mountpointRoot", file into the mountpoint root folder
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "mountpointRoot"));
        actions.add(new ZFileIntoAction("/" + MOUNTPOINT_FOLDER_NAME));
        rules.add(new ZFilterRule("testMountpoint", true, false, conditions, actions));
        
        // if subject contains "mountpointSub", file into the mountpoint subfolder
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "mountpointSub"));
        actions.add(new ZFileIntoAction(MOUNTPOINT_SUBFOLDER_PATH));
        rules.add(new ZFilterRule("testMountpointSubfolder", true, false, conditions, actions));

        return new ZFilterRules(rules);
    }
    
    /**
     * Saves the given filter rules.  Then gets them from the server and confirms that
     * the element tree matches.
     */
    private void saveRules(ZMailbox mbox, ZFilterRules rules)
    throws Exception {
        Element root = new XMLElement("test");
        Element expected = rules.toElement(root);
        mbox.saveFilterRules(rules);
        
        rules = mbox.getFilterRules(true);
        Element actual = rules.toElement(root);
        
        TestUtil.assertEquals(expected, actual);
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFilter.class);
    }
}
