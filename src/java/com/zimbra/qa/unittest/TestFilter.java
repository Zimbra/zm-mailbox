/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.google.common.base.Strings;

import com.zimbra.common.filter.Sieve;
import com.zimbra.common.mime.MimeMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.RuleRewriter;
import com.zimbra.cs.filter.SieveToSoap;
import com.zimbra.cs.filter.SoapToSieve;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZFilterAction;
import com.zimbra.client.ZFilterAction.MarkOp;
import com.zimbra.client.ZFilterAction.ZDiscardAction;
import com.zimbra.client.ZFilterAction.ZFileIntoAction;
import com.zimbra.client.ZFilterAction.ZKeepAction;
import com.zimbra.client.ZFilterAction.ZMarkAction;
import com.zimbra.client.ZFilterAction.ZRedirectAction;
import com.zimbra.client.ZFilterAction.ZTagAction;
import com.zimbra.client.ZFilterCondition;
import com.zimbra.client.ZFilterCondition.BodyOp;
import com.zimbra.client.ZFilterCondition.DateOp;
import com.zimbra.client.ZFilterCondition.HeaderOp;
import com.zimbra.client.ZFilterCondition.SimpleOp;
import com.zimbra.client.ZFilterCondition.ZAttachmentExistsCondition;
import com.zimbra.client.ZFilterCondition.ZBodyCondition;
import com.zimbra.client.ZFilterCondition.ZDateCondition;
import com.zimbra.client.ZFilterCondition.ZHeaderCondition;
import com.zimbra.client.ZFilterCondition.ZInviteCondition;
import com.zimbra.client.ZFilterCondition.ZMimeHeaderCondition;
import com.zimbra.client.ZFilterRule;
import com.zimbra.client.ZFilterRules;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZItem.Flag;
import com.zimbra.client.ZTag;
import junit.framework.TestCase;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public final class TestFilter extends TestCase {

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
    private ZFilterRules mOriginalIncomingRules;
    private ZFilterRules mOriginalOutgoingRules;
    private String mOriginalSpamApplyUserFilters;
    private String mOriginalSmtpPort = null;
    private String mOriginalSetEnvelopeSender = null;
    private ZTag mTag1;
    private ZTag mTag2;

    @Override
    public void setUp() throws Exception {
        cleanUp();

        mMbox = TestUtil.getZMailbox(USER_NAME);
        mTag1 = mMbox.createTag(TAG1_NAME, null);
        mTag2 = mMbox.createTag(TAG2_NAME, null);

        // Create mountpoint for testMountpoint()
        ZMailbox remoteMbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        TestUtil.createMountpoint(remoteMbox, "/" + MOUNTPOINT_FOLDER_NAME, mMbox, MOUNTPOINT_FOLDER_NAME);
        TestUtil.createFolder(remoteMbox, MOUNTPOINT_SUBFOLDER_PATH);

        mOriginalIncomingRules = mMbox.getIncomingFilterRules();
        saveIncomingRules(mMbox, getTestIncomingRules());
        mOriginalOutgoingRules = mMbox.getOutgoingFilterRules();
        saveOutgoingRules(mMbox, getTestOutgoingRules());

        Account account = TestUtil.getAccount(USER_NAME);
        mOriginalSpamApplyUserFilters = account.getAttr(Provisioning.A_zimbraSpamApplyUserFilters);
        mOriginalSmtpPort = Provisioning.getInstance().getLocalServer().getSmtpPortAsString();
        mOriginalSetEnvelopeSender = TestUtil.getServerAttr(Provisioning.A_zimbraMailRedirectSetEnvelopeSender);
    }

    /**
     * Confirms that outgoing filters are applied as expected when a message is sent via SendMsgRequest.
     */
    public void testOutgoingFiltersWithSendMsg()
    throws Exception {
        String sender = TestUtil.getAddress(USER_NAME);
        String recipient = TestUtil.getAddress(REMOTE_USER_NAME);
        String subject = NAME_PREFIX + " outgoing";

        List<ZEmailAddress> addrs = new LinkedList<ZEmailAddress>();
        addrs.add(new ZEmailAddress(sender, null, null, ZEmailAddress.EMAIL_TYPE_FROM));
        addrs.add(new ZEmailAddress(recipient, null, null, ZEmailAddress.EMAIL_TYPE_TO));
        ZMailbox.ZOutgoingMessage outgoingMsg = new ZMailbox.ZOutgoingMessage();
        outgoingMsg.setAddresses(addrs);
        outgoingMsg.setSubject(subject);

        mMbox.sendMessage(outgoingMsg, null, false);

        // make sure that sent message has been correctly tagged and filed into the correct folder
        ZMessage msg = TestUtil.getMessage(mMbox, "in:" + FOLDER1_NAME + " " + subject);
        TestUtil.verifyTag(mMbox, msg, TAG1_NAME);

        //make sure that sent message has not been filed into the (default) Sent folder
        List<ZMessage> msgs = TestUtil.search(mMbox, "in:Sent" + " " + subject);
        assertTrue(msgs.isEmpty());
    }

    /**
     * Confirms that outgoing filters are applied as expected when a sent message is added via AddMsgRequest.
     */
    public void testOutgoingFiltersWithAddMsg()
    throws Exception {
        String sender = TestUtil.getAddress(USER_NAME);
        String recipient = TestUtil.getAddress(REMOTE_USER_NAME);
        String subject = NAME_PREFIX + " outgoing";
        String content = new MessageBuilder().withSubject(subject).withFrom(sender).withToRecipient(recipient).create();

        // add a msg flagged as sent; filterSent=TRUE
        mMbox.addMessage("" + Mailbox.ID_FOLDER_SENT, "s", null, System.currentTimeMillis(), content, false, true);

        // make sure that the message has been correctly tagged and filed into the correct folder
        ZMessage msg = TestUtil.getMessage(mMbox, "in:" + FOLDER1_NAME + " " + subject);
        TestUtil.verifyTag(mMbox, msg, TAG1_NAME);

        // make sure that the message has not been filed into the (default) Sent folder
        List<ZMessage> msgs = TestUtil.search(mMbox, "in:Sent" + " " + subject);
        assertTrue(msgs.isEmpty());

        // add another msg flagged as sent; this time filterSent=FALSE
        mMbox.addMessage("" + Mailbox.ID_FOLDER_SENT, "s", null, System.currentTimeMillis(), content, false, false);

        // make sure that the message has been added to the (default) Sent folder
        TestUtil.getMessage(mMbox, "in:Sent" + " " + subject);
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
        saveIncomingRules(mMbox, zRules);

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
        saveIncomingRules(mMbox, zRules);

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
    public void disabledTestBase64Subject()
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
        ZFilterRules rules = getTestIncomingRules();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "testSpam"));
        actions.add(new ZFileIntoAction(FOLDER1_PATH));
        rules.getRules().add(new ZFilterRule("testBug5455", true, false, conditions, actions));
        saveIncomingRules(mMbox, rules);

        // Set "apply user rules" attribute to TRUE and make sure the message gets filed into folder1
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraSpamApplyUserFilters, LdapConstants.LDAP_TRUE);
        TestUtil.addMessageLmtp(recipients, sender, message);
        msg = TestUtil.waitForMessage(mbox, "in:" + FOLDER1_PATH + " subject:testSpam");
        mbox.deleteMessage(msg.getId());

        // Set "apply user rules" attribute to FALSE and make sure the message gets filed into junk
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraSpamApplyUserFilters, LdapConstants.LDAP_FALSE);
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
     */
    public void testMountpoint() throws Exception {
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

    public void testMountpointSubfolder() throws Exception {
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

    public void testMimeHeader()
    throws Exception {
        // Create a text/plain message with a text/html attachment.
        String subject = NAME_PREFIX + " testMimeHeader";
        String attachmentData = "<html><body>I'm so attached to you.</body></html>";
        String content = new MessageBuilder().withSubject(subject).withToRecipient(USER_NAME)
            .withAttachment(attachmentData, "attachment.html", "text/html").create();

        // Create filter rules.
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();

        conditions.add(new ZMimeHeaderCondition("Content-Type", HeaderOp.CONTAINS, "text/plain"));
        actions.add(new ZTagAction(mTag1.getName()));
        rules.add(new ZFilterRule("testMarkRead 1", true, false, conditions, actions));

        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZMimeHeaderCondition("Content-Type", HeaderOp.CONTAINS, "text/html"));
        actions.add(new ZTagAction(mTag2.getName()));
        rules.add(new ZFilterRule("testMarkRead 2", true, false, conditions, actions));

        saveIncomingRules(mMbox, new ZFilterRules(rules));

        // Deliver message and make sure that tag 2 was applied, but not tag 1.
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, content);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        Set<String> tagIds = new HashSet<String>();
        tagIds.addAll(Arrays.asList(msg.getTagIds().split(",")));
        assertEquals(2, tagIds.size());
        assertTrue(tagIds.contains(mTag1.getId()));
        assertTrue(tagIds.contains(mTag2.getId()));
    }

    public void testToOrCc()
    throws Exception {

        // Create filter rules
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("to,cc", HeaderOp.CONTAINS, "checkthis.com"));
        actions.add(new ZTagAction(mTag1.getName()));
        rules.add(new ZFilterRule("testToOrCc", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        // Deliver message 1 and make sure that tag 1 was applied
        String subject = NAME_PREFIX + " testToOrCc 1";
        String content = new MessageBuilder().withSubject(subject).withToRecipient(USER_NAME).withCcRecipient("cc@checkthis.com").create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, content);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        Set<String> tagIds = new HashSet<String>();
        assertNotNull(msg.getTagIds());
        tagIds.addAll(Arrays.asList(msg.getTagIds().split(",")));
        assertEquals(1, tagIds.size());
        assertTrue(tagIds.contains(mTag1.getId()));

        // Deliver message 2 and make sure that tag 1 was applied
        subject = NAME_PREFIX + " testToOrCc 2";
        content = new MessageBuilder().withSubject(subject).withToRecipient("to@checkthis.com").withCcRecipient(USER_NAME).create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, content);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        tagIds = new HashSet<String>();
        assertNotNull(msg.getTagIds());
        tagIds.addAll(Arrays.asList(msg.getTagIds().split(",")));
        assertEquals(1, tagIds.size());
        assertTrue(tagIds.contains(mTag1.getId()));
    }

    public void testCaseSensitiveComparison()
    throws Exception {

        // Create case sensitive header and body filter rules
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("Subject", HeaderOp.CONTAINS, true, "CHECK THIS"));
        conditions.add(new ZBodyCondition(BodyOp.CONTAINS, true, "CHECK THIS"));
        actions.add(new ZTagAction(mTag1.getName()));
        rules.add(new ZFilterRule("testCaseSensitiveComparison", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        // Deliver message having lower case subject substring and make sure that tag1 was not applied
        String subject = NAME_PREFIX + " testCaseSensitiveComparison1 check this";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("message should not have been tagged", msg.getTagIds() == null || msg.getTagIds().isEmpty());

        // Deliver message having upper case subject substring and make sure that tag1 was applied
        subject = NAME_PREFIX + " testCaseSensitiveComparison2 CHECK THIS";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertNotNull("message should have been tagged", msg.getTagIds());
        Set<String> tagIds = new HashSet<String>();
        tagIds.addAll(Arrays.asList(msg.getTagIds().split(",")));
        assertTrue("message should have been tagged with tag1", tagIds.contains(mTag1.getId()));

        // Deliver message having lower case body content substring and make sure that tag1 was not applied
        subject = NAME_PREFIX + " testCaseSensitiveComparison3";
        String content = new MessageBuilder().withSubject(subject).withToRecipient(USER_NAME).withBody("Hi check this").create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, content);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("message should not have been tagged", msg.getTagIds() == null || msg.getTagIds().isEmpty());

        // Deliver message having upper case body content substring and make sure that tag1 was applied
        subject = NAME_PREFIX + " testCaseSensitiveComparison4";
        content = new MessageBuilder().withSubject(subject).withToRecipient(USER_NAME).withBody("Hi CHECK THIS").create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, content);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertNotNull("message should have been tagged", msg.getTagIds());
        tagIds = new HashSet<String>();
        tagIds.addAll(Arrays.asList(msg.getTagIds().split(",")));
        assertTrue("message should have been tagged with tag1", tagIds.contains(mTag1.getId()));
    }

    public void testCurrentTimeTest()
    throws Exception {
        TimeZone userTz = Util.getAccountTimeZone(TestUtil.getAccount(USER_NAME));
        Calendar calendar = Calendar.getInstance(userTz);
        // Add 5 mins to current time
        calendar.add(Calendar.MINUTE, 5);
        // format time in HHmm
        SimpleDateFormat format = new SimpleDateFormat("HHmm");
        format.setTimeZone(userTz);
        String timeStr = format.format(calendar.getTime());

        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        // Condition true if msg received after timeStr
        conditions.add(new ZFilterCondition.ZCurrentTimeCondition(DateOp.AFTER, timeStr));
        actions.add(new ZTagAction(mTag1.getName()));
        rules.add(new ZFilterRule("testCurrentTimeTest after", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String subject = NAME_PREFIX + " testCurrentTimeTest1";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("message should not have been tagged", msg.getTagIds() == null || msg.getTagIds().isEmpty());

        rules = new ArrayList<ZFilterRule>();
        conditions = new ArrayList<ZFilterCondition>();
        // Condition true if msg received before timeStr
        conditions.add(new ZFilterCondition.ZCurrentTimeCondition(DateOp.BEFORE, timeStr));
        rules.add(new ZFilterRule("testCurrentTimeTest before", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        subject = NAME_PREFIX + " testCurrentTimeTest2";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertNotNull("message should have been tagged", msg.getTagIds());
        Set<String> tagIds = new HashSet<String>();
        tagIds.addAll(Arrays.asList(msg.getTagIds().split(",")));
        assertTrue("message should have been tagged with tag1", tagIds.contains(mTag1.getId()));
    }

    public void testCurrentDayOfWeekTest()
    throws Exception {
        TimeZone userTz = Util.getAccountTimeZone(TestUtil.getAccount(USER_NAME));
        Calendar calendar = Calendar.getInstance(userTz);
        int dayToday = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int dayYesterday = dayToday == 0 ? 6 : dayToday - 1;
        int dayTomorrow = dayToday == 6 ? 0 : dayToday + 1;

        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        // Condition true if msg received on the day yesterday or tomorrow
        conditions.add(new ZFilterCondition.ZCurrentDayOfWeekCondition(SimpleOp.IS, dayYesterday + "," + dayTomorrow));
        actions.add(new ZTagAction(mTag1.getName()));
        rules.add(new ZFilterRule("testCurrentDayOfWeekTest day not today", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String subject = NAME_PREFIX + " testCurrentDayOfWeekTest1";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("message should not have been tagged", msg.getTagIds() == null || msg.getTagIds().isEmpty());

        rules = new ArrayList<ZFilterRule>();
        conditions = new ArrayList<ZFilterCondition>();
        // Condition true if msg received on the day today
        conditions.add(new ZFilterCondition.ZCurrentDayOfWeekCondition(SimpleOp.IS, Integer.toString(dayToday)));
        rules.add(new ZFilterRule("testCurrentDayOfWeekTest today", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        subject = NAME_PREFIX + " testCurrentDayOfWeekTest2";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertNotNull("message should have been tagged", msg.getTagIds());
        Set<String> tagIds = new HashSet<String>();
        tagIds.addAll(Arrays.asList(msg.getTagIds().split(",")));
        assertTrue("message should have been tagged with tag1", tagIds.contains(mTag1.getId()));
    }

    public void testAddressTest()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();

        // first use header test for address matching
        conditions.add(new ZFilterCondition.ZHeaderCondition("From", HeaderOp.IS, "john.doe@example.com"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testAddressTest1", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String subject = NAME_PREFIX + " testAddressTest1";
        String mime = new MessageBuilder().withFrom("John Doe <john.doe@example.com>").withSubject(subject).create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, mime);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertFalse("Unexpected message flag state", msg.isFlagged());

        // now use the address test
        conditions.add(new ZFilterCondition.ZAddressCondition(
                "From", Sieve.AddressPart.all, HeaderOp.IS, false, "john.doe@example.com"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testAddressTest2", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        subject = NAME_PREFIX + " testAddressTest2";
        mime = new MessageBuilder().withFrom("John Doe <john.doe@example.com>").withSubject(subject).create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, mime);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("Unexpected message flag state", msg.isFlagged());
    }

    public void testAddressTestPart()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();

        // use address test for address domain match
        conditions.add(new ZFilterCondition.ZAddressCondition(
                "From", Sieve.AddressPart.domain, HeaderOp.IS, false, "example.com"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testAddressTestPart1", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String subject = NAME_PREFIX + " testAddressTestPart1";
        String mime = new MessageBuilder().withFrom("John Doe <JOHN.DOE@EXAMPLE.COM>").withSubject(subject).create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, mime);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("Unexpected message flag state", msg.isFlagged());

        // use address test for address local-part match
        conditions.add(new ZFilterCondition.ZAddressCondition(
                "From", Sieve.AddressPart.localpart, HeaderOp.MATCHES, true, "j*doe"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testAddressTestPart2", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        subject = NAME_PREFIX + " testAddressTestPart2";
        mime = new MessageBuilder().withFrom("John Doe <john.doe@example.com>").withSubject(subject).create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, mime);
        msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("Unexpected message flag state", msg.isFlagged());
    }

    public void testReplyAction()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZFilterCondition.ZTrueCondition());
        actions.add(new ZFilterAction.ZReplyAction("Hi ${FROM}, Your message was: '${BODY}'. Thanks!"));
        rules.add(new ZFilterRule("testReplyAction", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String subject = NAME_PREFIX + " testReplyAction";
        String body = "Hi, How r u?";
        String msg = new MessageBuilder().withFrom(REMOTE_USER_NAME).withSubject(subject).withBody(body).create();
        // send msg from user2 to user1
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, REMOTE_USER_NAME, msg);
        // check msg got filed into user1's mailbox
        TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        // check auto reply from user1 in user2's mailbox
        ZMessage zMessage =
                TestUtil.waitForMessage(TestUtil.getZMailbox(REMOTE_USER_NAME),
                                        "in:inbox subject:\"Re: " + subject + "\"");
        String content = zMessage.getMimeStructure().getContent();
        assertTrue("template vars should be replaced", !content.contains("${FROM}") && !content.contains("${BODY}"));
        assertTrue(content.contains(TestUtil.getAddress(REMOTE_USER_NAME)) && content.contains(body));
    }

    public void testNotifyAction()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZFilterCondition.ZTrueCondition());
        // add an action to notify user2
        actions.add(new ZFilterAction.ZNotifyAction(
                TestUtil.getAddress(REMOTE_USER_NAME), "${SUBJECT}", "From: ${FROM}, Message: ${BODY}"));
        rules.add(new ZFilterRule("testNotifyAction", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String subject = NAME_PREFIX + " testNotifyAction";
        String body = "Hi, How r u?";
        String msg = new MessageBuilder().withFrom(REMOTE_USER_NAME).withSubject(subject).withBody(body).create();
        // send msg to user1
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, REMOTE_USER_NAME, msg);
        // check msg got filed into user1's mailbox
        TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        // check notification msg from user1 in user2's mailbox, it should have the same subject
        ZMessage zMessage =
                TestUtil.waitForMessage(TestUtil.getZMailbox(REMOTE_USER_NAME),
                                        "in:inbox subject:\"" + subject + "\"");
        String content = zMessage.getMimeStructure().getContent();
        assertTrue("template vars should be replaced", !content.contains("${FROM}") && !content.contains("${BODY}"));
        assertTrue(content.contains(TestUtil.getAddress(REMOTE_USER_NAME)) && content.contains(body));
    }

    public void testNotifyActionUseOrigHeaders()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZFilterCondition.ZTrueCondition());
        // add an action to notify user2
        // copy headers From,To,Cc,Subject from the original message onto the notification message
        actions.add(new ZFilterAction.ZNotifyAction(
                TestUtil.getAddress(REMOTE_USER_NAME), null, "${BODY}", -1, "From,To,Cc,Subject"));
        rules.add(new ZFilterRule("testNotifyAction", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String subject = NAME_PREFIX + " testNotifyActionUseOrigHeaders";
        String body = "Hi, How r u?";
        String msg = new MessageBuilder().withFrom(REMOTE_USER_NAME).withToRecipient(USER_NAME).
                withCcRecipient(USER_NAME).withSubject(subject).withBody(body).create();
        // send msg to user1
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, REMOTE_USER_NAME, msg);
        // check notification msg from user1 in user2's mailbox
        ZMessage zMessage =
                TestUtil.waitForMessage(TestUtil.getZMailbox(REMOTE_USER_NAME),
                                        "in:inbox subject:\"" + subject + "\"");
        boolean checkedFrom = false, checkedTo = false, checkedCc = false;
        List<ZEmailAddress> msgAddrs = zMessage.getEmailAddresses();
        for (ZEmailAddress addr : msgAddrs) {
            if ("f".equals(addr.getType())) {
                assertEquals(TestUtil.addDomainIfNecessary(REMOTE_USER_NAME), addr.getAddress());
                if (!checkedFrom) {
                    checkedFrom = true;
                } else {
                    fail("multiple From addresses");
                }
            }
            if ("t".equals(addr.getType())) {
                assertEquals(TestUtil.addDomainIfNecessary(USER_NAME), addr.getAddress());
                if (!checkedTo) {
                    checkedTo = true;
                } else {
                    fail("multiple To addresses");
                }
            }
            if ("c".equals(addr.getType())) {
                assertEquals(TestUtil.addDomainIfNecessary(USER_NAME), addr.getAddress());
                if (!checkedCc) {
                    checkedCc = true;
                } else {
                    fail("multiple Cc addresses");
                }
            }
        }
        assertEquals(subject, zMessage.getSubject());
    }

    public void testNotifyWithDiscard()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZFilterCondition.ZTrueCondition());
        // add an action to notify
        actions.add(new ZFilterAction.ZNotifyAction(
                TestUtil.getAddress(REMOTE_USER_NAME), "${SUBJECT}", "From: ${FROM}, Message: ${BODY}"));
        // add discard action
        actions.add(new ZDiscardAction());
        rules.add(new ZFilterRule("testNotifyWithDiscard", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String subject = NAME_PREFIX + " testNotifyWithDiscard";
        String body = "Hi, How r u?";
        String msg = new MessageBuilder().withFrom(REMOTE_USER_NAME).withSubject(subject).withBody(body).create();
        // send msg to user1
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, REMOTE_USER_NAME, msg);

        // check msg not filed into user1's mailbox
        List<ZMessage> msgs = TestUtil.search(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("original message should not have been filed", msgs.isEmpty());
    }

    /**
     * Tests fix for bug 57890 (https://issues.apache.org/jira/browse/JSIEVE-75).
     */
    public void testMultipleMultilineText()
    throws Exception {
        List<ZFilterRule> rules;
        List<ZFilterCondition> conditions;
        List<ZFilterAction> actions;

        rules = new ArrayList<ZFilterRule>();

        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZFilterCondition.ZTrueCondition());
        String notifyMsg1 = "From: ${FROM}\nMessage: ${BODY}\n";
        actions.add(new ZFilterAction.ZNotifyAction("abc@xyz.com", "${SUBJECT}", notifyMsg1));
        String notifyMsg2 = "you've got mail";
        actions.add(new ZFilterAction.ZNotifyAction("abc@xyz.com", "subject", notifyMsg2));
        rules.add(new ZFilterRule("testMultipleMultilineText 1", true, false, conditions, actions));

        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZFilterCondition.ZTrueCondition());
        String replyMsg = "Replying to:\n${BODY}";
        actions.add(new ZFilterAction.ZReplyAction(replyMsg));
        rules.add(new ZFilterRule("testMultipleMultilineText 2", true, false, conditions, actions));

        saveIncomingRules(mMbox, new ZFilterRules(rules));

        rules = mMbox.getIncomingFilterRules().getRules();
        assertEquals("There should be 2 rules", rules.size(), 2);
        assertEquals("Rule 1 should have 2 actions", rules.get(0).getActions().size(), 2);
        assertEquals(((ZFilterAction.ZNotifyAction) rules.get(0).getActions().get(0)).getBodyTemplate(), notifyMsg1);
        assertEquals(((ZFilterAction.ZNotifyAction) rules.get(0).getActions().get(1)).getBodyTemplate(), notifyMsg2);
        assertEquals("Rule 2 should have 1 action", rules.get(1).getActions().size(), 1);
        assertEquals(((ZFilterAction.ZReplyAction) rules.get(1).getActions().get(0)).getBodyTemplate(), replyMsg);
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
        saveIncomingRules(mMbox, new ZFilterRules(rules));

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
        saveIncomingRules(mMbox, zRules);

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
        saveIncomingRules(mMbox, zRules);

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
            mMbox.saveIncomingFilterRules(zRules);
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
        mMbox.saveIncomingFilterRules(zRules);

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
            tagIds.addAll(Arrays.asList(tagIdString.split(",")));
        }
        return tagIds;
    }

    private String removeHeader(String content, String headerName)
    throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line;
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
        saveIncomingRules(mMbox, zRules);

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
        saveIncomingRules(mMbox, zRules);

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
     * Tests fix for bug 55927.
     */
    public void testFullMatchAfterPartialMatch()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        conditions.add(new ZBodyCondition(BodyOp.CONTAINS, "MatchThis"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testFullMatchAfterPartialMatch", true, false, conditions, actions));

        ZFilterRules zRules = new ZFilterRules(rules);
        saveIncomingRules(mMbox, zRules);

        // Add a message and test the flagged state.
        String subject = NAME_PREFIX + " testFullMatchAfterPartialMatch";
        String content = new MessageBuilder().withSubject(subject).withBody("MatchMatchThis").create();
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, content);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        assertTrue("Unexpected message flag state", msg.isFlagged());
    }

    /**
     * Tests fix for bug 56019.
     */
    public void testSpecialCharInBody()
    throws Exception {
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        conditions.add(new ZBodyCondition(BodyOp.CONTAINS, "Andr\u00e9"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule("testSpecialCharInBody", true, false, conditions, actions));

        ZFilterRules zRules = new ZFilterRules(rules);
        saveIncomingRules(mMbox, zRules);

        // Add a message and test the flagged state.
        String address = TestUtil.getAddress(USER_NAME);
        // TestFilter-testSpecialCharInBody.msg's body contains base64 encoded content (containing "Andr\u00e9")
        String msgContent = new String(
            ByteUtil.getContent(new File("/opt/zimbra/unittest/TestFilter-testSpecialCharInBody.msg")));
        TestUtil.addMessageLmtp(new String[] { address }, address, msgContent);
        ZMessage msg = TestUtil.getMessage(mMbox, "in:inbox subject:testSpecialCharInBody");
        assertTrue("Unexpected message flag state", msg.isFlagged());
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
        saveIncomingRules(mMbox, zRules);

        // Add a message.  Set the From header to something bogus to make
        // sure we're not rewriting it
        String from = "joebob@mycompany.com";
        String subject = NAME_PREFIX + " testRedirect 1";
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
        DummySmtpServer smtp = startSmtpServer(port);
        Server server = Provisioning.getInstance().getLocalServer();
        server.setSmtpPort(port);
        server.setMailRedirectSetEnvelopeSender(false);

        TestUtil.addMessageLmtp(subject, USER_NAME, from);
        assertEquals(from, smtp.getMailFrom());

        // Check zimbraMailRedirectSetEnvelopeSender=TRUE.
        smtp = startSmtpServer(port);
        server.setMailRedirectSetEnvelopeSender(true);
        subject = NAME_PREFIX + " testRedirect 2";
        TestUtil.addMessageLmtp(subject, USER_NAME, from);
        String userAddress = Strings.nullToEmpty(
                TestUtil.getAddress(USER_NAME)).toLowerCase();
        assertEquals("testRedirect 2 mail from", userAddress, smtp.getMailFrom());

        // Check empty envelope sender.
        smtp = startSmtpServer(port);
        subject = NAME_PREFIX + " testRedirect 3";
        String msgContent = TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null);
        String[] recipients = new String[] { USER_NAME };
        TestUtil.addMessageLmtp(recipients, null, msgContent);
        assertTrue(smtp.getMailFrom(), StringUtil.isNullOrEmpty(smtp.getMailFrom()));

        // Check Auto-Submitted=yes.
        smtp = startSmtpServer(port);
        subject = NAME_PREFIX + " testRedirect 4";
        msgContent = "Auto-Submitted: yes\r\n" + TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null);
        TestUtil.addMessageLmtp(recipients, USER_NAME, msgContent);
        assertTrue(smtp.getMailFrom(), StringUtil.isNullOrEmpty(smtp.getMailFrom()));

        // Check Auto-Submitted=no.
        smtp = startSmtpServer(port);
        subject = NAME_PREFIX + " testRedirect 5";
        msgContent = "Auto-Submitted: no\r\n" + TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null);
        TestUtil.addMessageLmtp(recipients, USER_NAME, msgContent);
        assertEquals("testRedirect 5 mail from", userAddress, smtp.getMailFrom());

        // Check Content-Type=multipart/report.
        smtp = startSmtpServer(port);
        subject = NAME_PREFIX + " testRedirect 6";
        msgContent = TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null);
        msgContent = msgContent.replace("text/plain", "multipart/report");
        TestUtil.addMessageLmtp(recipients, USER_NAME, msgContent);
        assertTrue(smtp.getMailFrom(), StringUtil.isNullOrEmpty(smtp.getMailFrom()));
    }

    private DummySmtpServer startSmtpServer(int port) {
        DummySmtpServer smtp = new DummySmtpServer(port);
        Thread smtpServerThread = new Thread(smtp);
        smtpServerThread.start();
        return smtp;
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
        saveIncomingRules(mMbox, zRules);

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
        saveIncomingRules(mMbox, zRules);

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
     * Converts the script to XML and back again.
     */
    private String normalize(String script) throws ParseException, ServiceException {
        List<String> ruleNames = RuleManager.getRuleNames(script);
        Node node = RuleManager.getSieveFactory().parse(new ByteArrayInputStream(script.getBytes()));

        // Convert from Sieve to SOAP and back again.
        SieveToSoap sieveToSoap = new SieveToSoap(ruleNames);
        sieveToSoap.accept(node);
        SoapToSieve soapToSieve = new SoapToSieve(sieveToSoap.toFilterRules());

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
     * Confirms that we handle the negative test flag properly with multiple
     * tests (bug 46007).
     */
    public void testPositiveAndNegative()
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

    @Override
    protected void tearDown() throws Exception {
        mMbox.saveIncomingFilterRules(mOriginalIncomingRules);
        mMbox.saveOutgoingFilterRules(mOriginalOutgoingRules);
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
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        for (ZMessage msg : TestUtil.search(mbox, "cortes de luz")) {
            mbox.deleteMessage(msg.getId());
        }
    }

    private ZFilterRules getTestOutgoingRules()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        // if subject contains "outgoing", file into folder1 and tag with tag1
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions;
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "outgoing"));
        actions.add(new ZFileIntoAction(FOLDER1_PATH));
        actions.add(new ZTagAction(TAG1_NAME));
        rules.add(new ZFilterRule("testOutgoingFilters1", true, false, conditions, actions));

        return new ZFilterRules(rules);
    }

    private ZFilterRules getTestIncomingRules()
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
     * Saves the given incoming filter rules.  Then gets them from the server and confirms that
     * the element tree matches.
     */
    private void saveIncomingRules(ZMailbox mbox, ZFilterRules rules) throws Exception {
        mbox.saveIncomingFilterRules(rules);
        ZFilterRules result = mbox.getIncomingFilterRules(true);
        TestUtil.assertEquals(rules.dump(), result.dump());
    }

    /**
     * Saves the given outgoing filter rules.  Then gets them from the server and confirms that
     * the element tree matches.
     */
    private void saveOutgoingRules(ZMailbox mbox, ZFilterRules rules) throws Exception {
        mbox.saveOutgoingFilterRules(rules);
        ZFilterRules result = mbox.getOutgoingFilterRules(true);
        TestUtil.assertEquals(rules.dump(), result.dump());
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFilter.class);
    }
}
