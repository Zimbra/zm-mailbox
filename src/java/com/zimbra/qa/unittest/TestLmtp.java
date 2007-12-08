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
import java.io.IOException;
import java.io.InputStream;
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
import com.zimbra.cs.zclient.ZGetMessageParams;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;

public class TestLmtp
extends TestCase {
    
    private static final String USER_NAME = "user1";
    private static final String USER2_NAME = "user2";
    private static final String NAME_PREFIX = TestLmtp.class.getSimpleName();
    
    private ZMailbox mMbox;
    private Account mAccount;
    private String mOriginalWarnInterval;
    private int mOriginalWarnPercent;
    private String mOriginalDiskStreamingThreshold;
    
    public void setUp()
    throws Exception {
        mMbox = TestUtil.getZMailbox("user1");
        mAccount = TestUtil.getAccount("user1");
        mOriginalWarnInterval = mAccount.getAttr(Provisioning.A_zimbraQuotaWarnInterval);
        mOriginalWarnPercent = mAccount.getIntAttr(Provisioning.A_zimbraQuotaWarnPercent, 0);
        mOriginalDiskStreamingThreshold =
            TestUtil.getServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold);
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
        runLmtpMessageTest("abcd\r\n", null);
        runLmtpMessageTest("abcd\r\n.\r", null);
        runLmtpMessageTest("ab\r\ncd\r\n.\r\n", "ab\r\ncd\r\n");
        
        // Transparency
        runLmtpMessageTest(".\r\n", "\r\n");
        runLmtpMessageTest(".\rabcd\r\n.\r\n", "\rabcd\r\n");
        runLmtpMessageTest(".a\r\n.\r\n", "a\r\n");
        runLmtpMessageTest(".a\r\n.\r\n", "a\r\n");
        runLmtpMessageTest("a\r\n.a\r\n.\r\n", "a\r\na\r\n");
        runLmtpMessageTest(".\r\n", "\r\n");
    }

    private void runLmtpMessageTest(String input, String expectedOutput)
    throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        LmtpMessageInputStream lin = new LmtpMessageInputStream(in, null);
        
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
        
        assertEquals(expectedOutput, readContent.toString());
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
        TestUtil.addMessageLmtp(NAME_PREFIX + " testDiskStreamingMultipleRecipients", recipients, TestUtil.getAddress(USER_NAME));
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.waitForMessage(mbox, NAME_PREFIX);
        mbox = TestUtil.getZMailbox(USER2_NAME);
        TestUtil.waitForMessage(mbox, NAME_PREFIX);
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
            assertTrue("Unexpected message: " + msg.getContent(), msg.getContent().contains(messageString));
        }
    }
    
    public void tearDown()
    throws Exception {
        setQuotaWarnPercent(mOriginalWarnPercent);
        setQuotaWarnInterval(mOriginalWarnInterval);
        TestUtil.setServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, mOriginalDiskStreamingThreshold);
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
