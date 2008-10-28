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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.parser.generated.Node;

import com.zimbra.common.mime.MimeMessage;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.SieveToSoap;
import com.zimbra.cs.filter.SoapToSieve;
import com.zimbra.cs.filter.ZimbraMailAdapter;
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
import com.zimbra.cs.zclient.ZFilterAction.ZMarkAction;
import com.zimbra.cs.zclient.ZFilterAction.ZRedirectAction;
import com.zimbra.cs.zclient.ZFilterAction.ZTagAction;
import com.zimbra.cs.zclient.ZFilterCondition.BodyOp;
import com.zimbra.cs.zclient.ZFilterCondition.HeaderOp;
import com.zimbra.cs.zclient.ZFilterCondition.ZBodyCondition;
import com.zimbra.cs.zclient.ZFilterCondition.ZHeaderCondition;
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
        TestUtil.addMessageLmtp(subject, address, address);
        TestUtil.getMessage(mMbox, "in:\"" + folderName + "\" subject:\"" + subject + "\"");
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
        List<String> ruleNames = RuleManager.getRuleNames(script);
        Node node = SieveFactory.getInstance().parse(new FileInputStream(scriptPath));
        
        // Convert from Sieve to SOAP and back again. 
        SieveToSoap sieveToSoap = new SieveToSoap(XMLElement.mFactory, ruleNames);
        sieveToSoap.accept(node);
        SoapToSieve soapToSieve = new SoapToSieve(sieveToSoap.getRootElement());
        String convertedScript = soapToSieve.getSieveScript();
        
        // Compare result.
        script = normalizeWhiteSpace(script);
        convertedScript = normalizeWhiteSpace(convertedScript);
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

        // Add a message.
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " testRedirect";
        TestUtil.addMessageLmtp(subject, address, address);
        
        // Confirm that user1 did not receive it.
        List<ZMessage> messages = TestUtil.search(mMbox, "subject:\"" + subject + "\"");
        assertEquals(0, messages.size());
        
        // Confirm that user2 received it, and make sure X-ZimbraForwarded is set.
        ZMailbox remoteMbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ZMessage msg = TestUtil.waitForMessage(remoteMbox, "in:inbox subject:\"" + subject + "\"");
        byte[] content = TestUtil.getContent(remoteMbox, msg.getId()).getBytes();
        MimeMessage mimeMsg = new MimeMessage(new ByteArrayInputStream(content));
        Account user1 = TestUtil.getAccount(USER_NAME);
        assertEquals(user1.getName(), mimeMsg.getHeader(ZimbraMailAdapter.HEADER_FORWARDED));
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
        assertEquals(user1.getName(), mimeMsg.getHeader(ZimbraMailAdapter.HEADER_FORWARDED));
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
    
    private String normalizeWhiteSpace(String script) {
        StringBuilder buf = new StringBuilder(script.length());
        boolean inWhiteSpace = false;
        for (int i = 0; i < script.length(); i++) {
            String c = script.substring(i, i + 1);
            if (c.matches("\\s") || c.equals("\\n")) {
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
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(REMOTE_USER_NAME, NAME_PREFIX);
        
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
