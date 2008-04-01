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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.lmtpserver.LmtpMessageInputStream;
import com.zimbra.cs.lmtpserver.ZimbraLmtpBackend;
import com.zimbra.cs.lmtpserver.utils.LmtpClient;
import com.zimbra.cs.mime.handler.MessageRFC822Handler;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGetMessageParams;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.MessagePart;

public class TestLmtp
extends TestCase {
    
    private static final String USER_NAME = "user1";
    private static final String USER2_NAME = "user2";
    private static final String NAME_PREFIX = TestLmtp.class.getSimpleName();
    
    private ZMailbox mMbox;
    private Account mAccount;
    private String mOriginalWarnInterval;
    private int mOriginalWarnPercent;
    private String mOriginalServerDiskThreshold;
    private String mOriginalConfigDiskThreshold;
    
    public void setUp()
    throws Exception {
        mMbox = TestUtil.getZMailbox("user1");
        mAccount = TestUtil.getAccount("user1");
        mOriginalWarnInterval = mAccount.getAttr(Provisioning.A_zimbraQuotaWarnInterval);
        mOriginalWarnPercent = mAccount.getIntAttr(Provisioning.A_zimbraQuotaWarnPercent, 0);
        mOriginalServerDiskThreshold =
            TestUtil.getServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold);
        mOriginalConfigDiskThreshold = TestUtil.getConfigAttr(
            Provisioning.A_zimbraMailDiskStreamingThreshold);
        cleanUp();
    }
    
    /**
     * Tests {@link ZimbraLmtpBackend#readData} with various valid/invalid
     * values for the size hint and disk threshold.
     */
    public void testReadLmtpData()
    throws Exception {
        // Entire string
        assertEquals("123", read("123", 3, 3));
        assertEquals("123", read("123", -1, 3));
        assertEquals("123", read("123", 0, 3));
        assertEquals("123", read("123", 10, 3));
        assertEquals("123", read("123", 3, 10));
        
        // First two bytes
        assertEquals("12", read("123", -1, 2));
        assertEquals("12", read("123", 0, 2));
        assertEquals("12", read("123", 1, 2));
        assertEquals("12", read("123", 2, 2));
        assertEquals("12", read("123", 10, 2));
    }
    
    private String read(String dataString, int sizeHint, int limit)
    throws Exception {
        byte[] data = dataString.getBytes();
        int numToRead = Math.min(data.length, limit);
        int numRemaining = data.length - numToRead;
        byte[] expected = new byte[numToRead];
        System.arraycopy(data, 0, expected, 0, numToRead);
        InputStream in = new ByteArrayInputStream(data);
        
        byte[] bytesRead = ByteUtil.readInput(in, sizeHint, limit);
        assertEquals(numToRead, bytesRead.length);
        assertEquals(numRemaining, in.available());
        assertEquals(new String(expected), new String(bytesRead));
        if (numRemaining == 0) {
            assertEquals(-1, in.read());
        } else {
            assertTrue(in.read() >= 0);
        }
        return new String(bytesRead);
    }
    
    public void testQuotaWarning()
    throws Exception {
        // Initialize
        String address = TestUtil.getAddress(USER_NAME);
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraQuotaLastWarnTime, "");
        Provisioning.getInstance().modifyAttrs(mAccount, attrs);
        
        // Make sure there are no warnings already in the mailbox
        validateNumWarnings(0);
        
        // Make sure we haven't already hit the quota warning level
        TestUtil.addMessageLmtp(NAME_PREFIX + " 1", address, address);
        validateNumWarnings(0);
        
        // Make sure setting quota warning to 0 is a no-op
        setQuotaWarnPercent(0);
        TestUtil.addMessageLmtp(NAME_PREFIX + " 2", address, address);
        validateNumWarnings(0);
        
        // Make sure setting quota warning to 99 doesn't trigger the warning
        setQuotaWarnPercent(0);
        TestUtil.addMessageLmtp(NAME_PREFIX + " 3", address, address);
        validateNumWarnings(0);
        
        // Make sure setting quota warning to 1 triggers the warning
        setQuotaWarnPercent(1);
        TestUtil.addMessageLmtp(NAME_PREFIX + " 4", address, address);
        validateNumWarnings(1);
        
        // Make sure a second warning doesn't get sent (interval not exceeded)
        TestUtil.addMessageLmtp(NAME_PREFIX + " 5", address, address);
        validateNumWarnings(1);
        
        // Make sure that a warning is triggered when the interval is exceeded
        setQuotaWarnInterval("1s");
        Thread.sleep(1000);
        TestUtil.addMessageLmtp(NAME_PREFIX + " 6", address, address);
        validateNumWarnings(2);
        
        // Make sure that a second warning is not triggered when the interval is not set
        // (default: 1 day)
        setQuotaWarnInterval("");
        TestUtil.addMessageLmtp(NAME_PREFIX + " 7", address, address);
        validateNumWarnings(2);
    }
    
    private void validateNumWarnings(int numWarnings)
    throws Exception {
        List<ZMessage> messages = TestUtil.search(mMbox, "Quota warning");
        assertEquals("Number of quota warnings", numWarnings, messages.size());
    }
    
    private void setQuotaWarnPercent(int percent)
    throws Exception {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraQuotaWarnPercent, Integer.toString(percent));
        Provisioning.getInstance().modifyAttrs(mAccount, attrs);
    }
    
    private void setQuotaWarnInterval(String interval)
    throws Exception {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraQuotaWarnInterval, interval);
        Provisioning.getInstance().modifyAttrs(mAccount, attrs);
    }
    
    public void testLmtpMessageInputStream()
    throws Exception {
        String prefix = "12345678\r\n";
        
        // Negative tests
        runLmtpMessageTest("abcd\r\n", null, null);
        runLmtpMessageTest("abcd\r\n.\r", null, null);
        runLmtpMessageTest("\n\r.\r\n", null, null);
        runLmtpMessageTest("\n\r\r\r\r\n\r.\r\n", null, null);
        runLmtpMessageTest("\r\n.\n\r\n", null, null);
        runLmtpMessageTest("..\r\n", null, null);
        runLmtpMessageTest(".", null, null);
        runLmtpMessageTest(".\r", null, null);
        
        // Positive tests
        runLmtpMessageTest("ab\r\ncd\r\n.\r\n", "ab\r\ncd\r\n", null);
        runLmtpMessageTest("ab\r\ncd\r\n.\r\n", "ab\r\ncd\r\n", prefix);
        runLmtpMessageTest("ab\r\ncd\r\n\r\n.\r\n", "ab\r\ncd\r\n\r\n", null);
        runLmtpMessageTest("ab\r\ncd\r\n\r\n.\r\n", "ab\r\ncd\r\n\r\n", prefix);
        runLmtpMessageTest("ab\r\n..\n\r\n\r\n.\r\n", "ab\r\n.\n\r\n\r\n", null);
        runLmtpMessageTest("ab\r\n..\n\r\n\r\n.\r\n", "ab\r\n.\n\r\n\r\n", prefix);
        runLmtpMessageTest("ab\r\n.\rcd\r\n.\r\n", "ab\r\n\rcd\r\n", null);
        runLmtpMessageTest("ab\r\n.\rcd\r\n.\r\n", "ab\r\n\rcd\r\n", prefix);
        
        // Transparency
        runLmtpMessageTest(".\r\n", "", null);
        runLmtpMessageTest(".\r\n", "", prefix);
        runLmtpMessageTest("..\r\n.\r\n", ".\r\n", null);
        runLmtpMessageTest("..\r\n.\r\n", ".\r\n", prefix);
        runLmtpMessageTest("..\rabcd\r\n.\r\n", ".\rabcd\r\n", null);
        runLmtpMessageTest("..\rabcd\r\n.\r\n", ".\rabcd\r\n", prefix);
        runLmtpMessageTest("..a\r\n.\r\n", ".a\r\n", null);
        runLmtpMessageTest("..a\r\n.\r\n", ".a\r\n", prefix);
        runLmtpMessageTest("a\r\n..a\r\n.\r\n", "a\r\n.a\r\n", null);
        runLmtpMessageTest("a\r\n..a\r\n.\r\n", "a\r\n.a\r\n", prefix);
    }

    private void runLmtpMessageTest(String input, String expectedOutput, String prefix)
    throws Exception {
        // Test without prefix
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        LmtpMessageInputStream lin = new LmtpMessageInputStream(in, prefix);
        
        StringBuilder readContent = new StringBuilder();
        try {
            int c;
            while ((c = lin.read()) >= 0) {
                readContent.append((char) c);
            }
        } catch (IOException ioe) {
            if (expectedOutput == null) {
                return;
            } else {
                throw ioe;
            }
        }
        
        if (prefix == null) {
            prefix = "";
        }
        assertEquals(prefix + expectedOutput, readContent.toString());
        assertEquals(expectedOutput.length() + prefix.length(), lin.getMessageSize());
    }
    
    /**
     * Confirms that mail can successfully be delivered to one user when streaming to disk.
     */
    public void testDiskStreamingOneRecipient()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, "0");
        String recipient = TestUtil.getAddress(USER_NAME);
        TestUtil.addMessageLmtp(NAME_PREFIX + " testDiskStreamingOneRecipient", recipient, TestUtil.getAddress(USER_NAME));
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.waitForMessage(mbox, NAME_PREFIX);
    }
    
    /**
     * Confirms that mail can successfully be delivered to multiple users when streaming to disk.
     */
    public void testDiskStreamingMultipleRecipients()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, "0");
        String[] recipients = {
            TestUtil.getAddress(USER_NAME),
            TestUtil.getAddress(USER2_NAME)
        };
        
        String subject = NAME_PREFIX + " testDiskStreamingMultipleRecipients";
        ZMailbox mbox1 = TestUtil.getZMailbox(USER_NAME);
        ZMailbox mbox2 = TestUtil.getZMailbox(USER2_NAME);

        TestUtil.addMessageLmtp(subject, recipients, TestUtil.getAddress(USER_NAME));
        TestUtil.waitForMessage(mbox1, "in:inbox subject:\"" + subject + "\"");
        ZMessage msg2 = TestUtil.waitForMessage(mbox2, "in:inbox subject:\"" + subject + "\"");
        
        // Test bug 25484.  Make sure that user1 can still read the message after user2
        // deletes it.
        mbox2.deleteMessage(msg2.getId());
        mbox1 = TestUtil.getZMailbox(USER_NAME);
        TestUtil.waitForMessage(mbox1, "in:inbox subject:\"" + subject + "\"");
    }
    
    /**
     * Another test for bug 25484.  Delivers a message to user1 and user2, then confirms that
     * user1 can still read the message after user2 empties the folder that contains the message. 
     */
    public void testDiskStreamingEmptyFolder()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, "0");
        String[] recipients = {
            TestUtil.getAddress(USER_NAME),
            TestUtil.getAddress(USER2_NAME)
        };
        
        String subject = NAME_PREFIX + " testDiskStreamingMultipleRecipients";
        ZMailbox mbox1 = TestUtil.getZMailbox(USER_NAME);
        ZMailbox mbox2 = TestUtil.getZMailbox(USER2_NAME);

        TestUtil.addMessageLmtp(subject, recipients, TestUtil.getAddress(USER_NAME));
        TestUtil.waitForMessage(mbox1, "in:inbox subject:\"" + subject + "\"");
        ZMessage msg2 = TestUtil.waitForMessage(mbox2, "in:inbox subject:\"" + subject + "\"");
        
        // Test bug 25484.  Have user2 move the message to a folder, empty the folder,
        // and then have user1 read the message.
        ZFolder folder2 = TestUtil.createFolder(mbox2, "/" + NAME_PREFIX + " testDiskStreamingEmptyFolder");
        mbox2.moveMessage(msg2.getId(), folder2.getId());

        // Mark message as read, since unread messages result in uncache
        // getting called explicitly in Folder.propagateDeletion().
        mbox2.markItemRead(msg2.getId(), true, null);
        mbox2.emptyFolder(folder2.getId());
        mbox1 = TestUtil.getZMailbox(USER_NAME);
        System.out.println("Getting message content?");
        
        TestUtil.waitForMessage(mbox1, "in:inbox subject:\"" + subject + "\"");
    }

    /**
     * Confirms that a message gets delivered regardless of what the size hint is set to.
     */
    public void testSizeHint()
    throws Exception {
        // Send the same message 5 times with different size hints
        String address = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " testIncorrectSizeHint";
        String messageString = TestUtil.getTestMessage(subject, address, address, null);
        String[] recipients = new String[] { address };
        LmtpClient lmtp = new LmtpClient("localhost", 7025);
        byte[] data = messageString.getBytes();
        
        lmtp.sendMessage(new ByteArrayInputStream(data), recipients, address, "TestLmtp", null);
        lmtp.sendMessage(new ByteArrayInputStream(data), recipients, address, "TestLmtp", 0L);
        lmtp.sendMessage(new ByteArrayInputStream(data), recipients, address, "TestLmtp", 10L);
        lmtp.sendMessage(new ByteArrayInputStream(data), recipients, address, "TestLmtp", (long) data.length);
        lmtp.sendMessage(new ByteArrayInputStream(data), recipients, address, "TestLmtp", (long) Integer.MAX_VALUE);
        lmtp.close();
        
        // Wait until all messages have arrived
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = null;
        for (int i = 1; i < 20; i++) {
            messages = TestUtil.search(mbox, subject);
            if (messages.size() == 5) {
                break;
            }
            Thread.sleep(500);
        }
        assertEquals(5, messages.size());
        
        // Check message bodies
        ZGetMessageParams params = new ZGetMessageParams();
        params.setRawContent(true);
        for (ZMessage msg : messages) {
            // Download raw content
            params.setId(msg.getId());
            msg = mbox.getMessage(params);
            // Check contains instead of equality, since we prepend Received and Return-Path during LMTP.
            assertContains(msg.getContent(), messageString);
        }
    }
    
    /**
     * Assert the message contains the given substring, ignoring newlines.
     */
    private void assertContains(String message, String substring)
    throws IOException {
        BufferedReader msgReader = new BufferedReader(new StringReader(message));
        BufferedReader subReader = new BufferedReader(new StringReader(substring));
        String firstLine = subReader.readLine();
        String line;
        boolean foundFirstLine = false;
        
        while ((line = msgReader.readLine()) != null) {
            if (line.equals(firstLine)) {
                foundFirstLine = true;
                break;
            }
        }
        
        String context = String.format("Could not find '%s' in message:\n", firstLine, message);
        assertTrue(context, foundFirstLine);
        
        while(true) {
            line = msgReader.readLine();
            String subLine = subReader.readLine();
            if (line == null || subLine == null) {
                break;
            }
            assertEquals(subLine, line);
        }
        
    }
    
    /**
     * Sends a message with another message attached and confirms that the subject
     * of the attached message is indexed.
     * @see MessageRFC822Handler
     */
    public void testAttachedMessage()
    throws Exception {
        String outerSubject = NAME_PREFIX + " testAttachedMessage outer";
        String innerSubject = NAME_PREFIX + " testAttachedMessage inner";
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        
        // Assemble outer message
        ZOutgoingMessage msg = new ZOutgoingMessage();
        List<ZEmailAddress> addresses = new ArrayList<ZEmailAddress>();
        addresses.add(new ZEmailAddress(TestUtil.getAddress(USER_NAME),
            null, null, ZEmailAddress.EMAIL_TYPE_TO));
        msg.setAddresses(addresses);
        msg.setSubject(outerSubject);

        // Assemble body and inner message
        String attachedMessageString = TestUtil.getTestMessage(innerSubject, USER_NAME, USER_NAME, null);
        MessagePart attachedMessage = new MessagePart("message/rfc822", attachedMessageString);
        MessagePart body = new MessagePart("text/plain", "This is the outer message");
        msg.setMessagePart(new MessagePart("multipart/mixed", body, attachedMessage));
        
        // Send and wait for it to arrive
        mbox.sendMessage(msg, null, false);
        TestUtil.waitForMessage(mbox, "in:inbox " + outerSubject);
        
        // Test search for inner message subject
        List<ZMessage> msgs = TestUtil.search(mbox, "in:inbox " + innerSubject);
        assertEquals(1, msgs.size());
        msgs = TestUtil.search(mbox, "in:sent " + innerSubject);
        assertEquals(1, msgs.size());
        
        // Test search for inner message body
        msgs = TestUtil.search(mbox, NAME_PREFIX + " waves");
        assertEquals(1, msgs.size());
    }
    
    /**
     * Confirms that delivery succeeds when <tt>zimbraMailDiskStreamingThreshold</tt>
     * isn't set (bug 22536).
     */
    public void testMissingDiskThreshold()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, "");
        TestUtil.setConfigAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, "");
        String subject = NAME_PREFIX + " testMissingDiskThreshold";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
    }
    
    public void tearDown()
    throws Exception {
        setQuotaWarnPercent(mOriginalWarnPercent);
        setQuotaWarnInterval(mOriginalWarnInterval);
        TestUtil.setServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, mOriginalServerDiskThreshold);
        TestUtil.setConfigAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, mOriginalConfigDiskThreshold);
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(USER_NAME, "Quota warning");
        TestUtil.deleteTestData(USER2_NAME, NAME_PREFIX);
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestLmtp.class));
    }
}
