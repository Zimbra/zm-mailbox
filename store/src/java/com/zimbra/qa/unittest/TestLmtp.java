/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGetMessageParams;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.MessagePart;
import com.zimbra.client.ZMessage;
import com.zimbra.common.lmtp.LmtpClient;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.lmtpserver.LmtpMessageInputStream;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.handler.MessageRFC822Handler;

public class TestLmtp {

    @Rule
    public TestName testInfo = new TestName();

    private String USER_NAME = null;
    private String USER2_NAME = null;
    private static final String NAME_PREFIX = TestLmtp.class.getSimpleName().toLowerCase();

    private static final String STARTTLS = "STARTTLS";
    private static final String NOOP = "NOOP";
    private static final String RSET = "RSET";
    private static final String VRFY = "VRFY";

    private ZMailbox zmbox;
    private Account account;
    private String originalServerDiskThreshold;
    private String originalConfigDiskThreshold;
    private String originalQuota;
    private String originalDedupeCacheSize;
    private String originalDedupeCacheTimeout;

    private class LmtpClientThread
    implements Runnable {

        private final String mRecipient;
        private final String mContent;

        private LmtpClientThread(String recipient, String content) {
            mRecipient = recipient;
            mContent = content;
        }

        @Override
        public void run() {
            try {
                TestUtil.addMessageLmtp(new String[] { mRecipient }, mRecipient, mContent);
            } catch (Exception e) {
                ZimbraLog.test.error("Unable to send message to %s.", mRecipient, e);
            }
        }
    }

    @Before
    public void setUp()
    throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName().toLowerCase() + "-";
        USER_NAME = prefix + "user1";
        USER2_NAME = prefix + "user2";
        cleanUp();
        TestUtil.createAccount(USER_NAME);
        zmbox = TestUtil.getZMailbox(USER_NAME);
        account = TestUtil.getAccount(USER_NAME);
        originalServerDiskThreshold = TestUtil.getServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold);
        originalConfigDiskThreshold = TestUtil.getConfigAttr( Provisioning.A_zimbraMailDiskStreamingThreshold);
        originalQuota = TestUtil.getAccountAttr(USER_NAME, Provisioning.A_zimbraMailQuota);
        originalDedupeCacheSize = TestUtil.getConfigAttr(Provisioning.A_zimbraMessageIdDedupeCacheSize);
        originalDedupeCacheTimeout = TestUtil.getConfigAttr(Provisioning.A_zimbraMessageIdDedupeCacheTimeout);
    }

    @After
    public void tearDown()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, originalServerDiskThreshold);
        TestUtil.setConfigAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, originalConfigDiskThreshold);
        TestUtil.setConfigAttr(Provisioning.A_zimbraMessageIdDedupeCacheSize, originalDedupeCacheSize);
        TestUtil.setConfigAttr(Provisioning.A_zimbraMessageIdDedupeCacheTimeout, originalDedupeCacheTimeout);
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(USER2_NAME);
    }


    /**
     * Tests reading data with various valid/invalid
     * values for the size hint and disk threshold.
     */
    @Test
    public void testReadLmtpData()
    throws Exception {
        // Entire string
        Assert.assertEquals("123", read("123", 3, 3));
        Assert.assertEquals("123", read("123", -1, 3));
        Assert.assertEquals("123", read("123", 0, 3));
        Assert.assertEquals("123", read("123", 10, 3));
        Assert.assertEquals("123", read("123", 3, 10));

        // First two bytes
        Assert.assertEquals("12", read("123", -1, 2));
        Assert.assertEquals("12", read("123", 0, 2));
        Assert.assertEquals("12", read("123", 1, 2));
        Assert.assertEquals("12", read("123", 2, 2));
        Assert.assertEquals("12", read("123", 10, 2));
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
        Assert.assertEquals(numToRead, bytesRead.length);
        Assert.assertEquals(numRemaining, in.available());
        Assert.assertEquals(new String(expected), new String(bytesRead));
        if (numRemaining == 0) {
            Assert.assertEquals(-1, in.read());
        } else {
            Assert.assertTrue(in.read() >= 0);
        }
        return new String(bytesRead);
    }

    @Test
    public void testQuotaWarning()
    throws Exception {
        // Initialize
        Account account = TestUtil.getAccount(USER_NAME);
        account.setQuotaLastWarnTimeAsString("");
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        // Set the quota so that we trigger the warning for an empty mailbox
        // and don't exceed the quota for a mailbox that already has content.
        account.setMailQuota(Math.max(mbox.getSize() * 2, 10000));

        // Make sure there are no warnings already in the mailbox
        validateNumWarnings(0);

        // Make sure we haven't already hit the quota warning level
        TestUtil.addMessageLmtp(NAME_PREFIX + " testQuotaWarning 1", USER_NAME, USER_NAME);
        validateNumWarnings(0);

        // Make sure setting quota warning to 0 is a no-op
        setQuotaWarnPercent(0);
        TestUtil.addMessageLmtp(NAME_PREFIX + " testQuotaWarning 2", USER_NAME, USER_NAME);
        validateNumWarnings(0);

        // Make sure setting quota warning to 99 doesn't trigger the warning
        setQuotaWarnPercent(0);
        TestUtil.addMessageLmtp(NAME_PREFIX + " testQuotaWarning 3", USER_NAME, USER_NAME);
        validateNumWarnings(0);

        // Make sure setting quota warning to 1 triggers the warning
        setQuotaWarnPercent(1);
        TestUtil.addMessageLmtp(NAME_PREFIX + " testQuotaWarning 4", USER_NAME, USER_NAME);
        validateNumWarnings(1);

        // Make sure a second warning doesn't get sent (interval not exceeded)
        TestUtil.addMessageLmtp(NAME_PREFIX + " testQuotaWarning 5", USER_NAME, USER_NAME);
        validateNumWarnings(1);

        // Make sure that a warning is triggered when the interval is exceeded
        setQuotaWarnInterval("1s");
        Thread.sleep(1000);
        TestUtil.addMessageLmtp(NAME_PREFIX + " testQuotaWarning 6", USER_NAME, USER_NAME);
        validateNumWarnings(2);

        // Make sure that a second warning is not triggered when the interval is not set
        // (default: 1 day)
        setQuotaWarnInterval("");
        TestUtil.addMessageLmtp(NAME_PREFIX + " testQuotaWarning 7", USER_NAME, USER_NAME);
        validateNumWarnings(2);
    }

    private void validateNumWarnings(int numWarnings)
    throws Exception {
        List<ZMessage> messages = TestUtil.search(zmbox, "Quota warning");
        Assert.assertEquals("Number of quota warnings", numWarnings, messages.size());
    }

    private void setQuotaWarnPercent(int percent)
    throws Exception {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraQuotaWarnPercent, Integer.toString(percent));
        Provisioning.getInstance().modifyAttrs(account, attrs);
    }

    private void setQuotaWarnInterval(String interval)
    throws Exception {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraQuotaWarnInterval, interval);
        Provisioning.getInstance().modifyAttrs(account, attrs);
    }

    @Test
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
        Assert.assertEquals(prefix + expectedOutput, readContent.toString());
        Assert.assertEquals(expectedOutput.length() + prefix.length(), lin.getMessageSize());
    }

    /**
     * Confirms that mail can successfully be delivered to one user when streaming to disk.
     */
    @Test
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
    @Test
    public void testDiskStreamingMultipleRecipients()
    throws Exception {
        TestUtil.createAccount(USER2_NAME);
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
    @Test
    public void testDiskStreamingEmptyFolder()
    throws Exception {
        TestUtil.createAccount(USER2_NAME);
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
        TestUtil.waitForMessage(mbox1, "in:inbox subject:\"" + subject + "\"");
    }

    /**
     * Confirms that a message gets delivered regardless of what the size hint is set to.
     */
    @Test
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

        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, subject);
        Assert.assertEquals(5, messages.size());

        // Check message bodies
        ZGetMessageParams params = new ZGetMessageParams();
        params.setRawContent(true);
        for (ZMessage msg : messages) {
            String content = TestUtil.getContent(mbox, msg.getId());
            TestUtil.assertMessageContains(content, messageString);
        }
    }

    /**
     * Sends a message with another message attached and confirms that the subject
     * of the attached message is indexed.
     * @see MessageRFC822Handler
     */
    @Test
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

        // Test search for message subject
        List<ZMessage> msgs = TestUtil.search(mbox, "in:inbox " + innerSubject);
        Assert.assertEquals(1, msgs.size());
        msgs = TestUtil.search(mbox, "in:sent " + innerSubject);
        Assert.assertEquals(1, msgs.size());

        // Test search for message body
        msgs = TestUtil.search(mbox, "in:inbox " + NAME_PREFIX + " waves");
        Assert.assertEquals(1, msgs.size());
        msgs = TestUtil.search(mbox, "in:sent " + NAME_PREFIX + " waves");
        Assert.assertEquals(1, msgs.size());
    }

    /**
     * Confirms that delivery succeeds when <tt>zimbraMailDiskStreamingThreshold</tt>
     * isn't set (bug 22536).
     */
    @Test
    public void testMissingDiskThreshold()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, "");
        TestUtil.setConfigAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, "");
        String subject = NAME_PREFIX + " testMissingDiskThreshold";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
    }

    /**
     * Tests the LMTP deduping code.  Attempts to deliver multiple copies of the
     * same message to the same message to the same mailbox simultaneously.  Confirms
     * that only one copy got delivered.  Bug 38898.
     */
    @Test
    public void testConcurrentDedupe()
    throws Exception {
        String subject = NAME_PREFIX + " testConcurrentDedupe";
        String content = TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null);
        content = "Message-ID: " + System.currentTimeMillis() + "\r\n" + content;

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new LmtpClientThread(USER_NAME, content));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, "in:inbox subject:\"" + subject + "\"");
        Assert.assertEquals(1, messages.size());
    }

    /**
     * Confirms that delivery succeeds after an initial failure.  This confirms
     * that the message id was removed from the LMTP dedupe cache when a delivery
     * failure occurs.  Bug 38898.
     * @throws Exception
     */
    @Test
    public void testDeliveryAfterFailure()
    throws Exception {
        String subject = NAME_PREFIX + " testDeliveryAfterFailure";
        String content = TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null);
        content = "Message-ID: " + System.currentTimeMillis() + "\r\n" + content;
        String[] recipients = new String[] { USER_NAME };

        // Set quota to 1 byte and make sure delivery fails.
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailQuota, "1");
        Assert.assertFalse("LMTP should not have succeeded", TestUtil.addMessageLmtp(recipients, USER_NAME, content));

        // Reset quota, retry, and make sure the delivery succeeds.
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailQuota, originalQuota);
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");
    }

    /**
     * Verifies the behavior of {@code zimbraPrefMessageIdDedupingEnabled}.
     */
    @Test
    public void testDedupePref()
    throws Exception {
        String subject = NAME_PREFIX + " testDedupePref";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        Account account = TestUtil.getAccount(USER_NAME);
        String[] recipients = new String[] { USER_NAME };

        // Deliver initial message.
        String content = new MessageBuilder().withSubject(subject).withToRecipient(USER_NAME).withFrom(USER_NAME).
                withMessageIdHeader().create();
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);

        String query = "in:inbox subject:\"" + subject + "\"";

        // Redeliver with deduping enabled.
        account.setPrefMessageIdDedupingEnabled(true);
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        Assert.assertEquals(1, TestUtil.search(mbox, query).size());

        // Redeliver with deduping disabled;
        account.setPrefMessageIdDedupingEnabled(false);
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        Assert.assertEquals(2, TestUtil.search(mbox, query).size());
    }

    /**
     * Verifies the behavior of {@code zimbraMessageIdDedupeCacheTimeout}.
     */
    //disable due to bug 76332
    public void disableTestDedupeCacheTimeout()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        // Deliver initial message.
        String subject = NAME_PREFIX + " testDedupeCacheTimeout";
        String[] recipients = new String[] { USER_NAME };
        String content = new MessageBuilder().withSubject(subject).withToRecipient(USER_NAME).withFrom(USER_NAME).
                withMessageIdHeader().create();
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        String query = "in:inbox subject:\"" + subject + "\"";
        Assert.assertEquals("message should have been delivered", 1, TestUtil.search(mbox, query).size());

        // Set deduping cache timeout to 0.5 sec
        TestUtil.setConfigAttr(Provisioning.A_zimbraMessageIdDedupeCacheTimeout, "500ms");

        // Redeliver same message immediately
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        Assert.assertEquals("deduping should have happened", 1, TestUtil.search(mbox, query).size());

        // sleep for just over 0.5 sec
        Thread.sleep(501);

        // Redeliver
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        Assert.assertEquals("dedupe cache entry should have timed out", 2, TestUtil.search(mbox, query).size());
    }

    /**
     * Confirms that we reject messages that have a line that's longer
     * than the limit specified by {@link LC#zimbra_lmtp_max_line_length}.
     * Bug 42214.
     */
    @Test
    public void testValidation()
    throws Exception {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i <= LC.zimbra_lmtp_max_line_length.longValue(); i++) {
            buf.append('x');
        }
        Assert.assertFalse(TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, buf.toString()));
    }

    @Test
    public void testTransparency() throws Exception {
        String subject = NAME_PREFIX + " LMTPTransparency1";
        String body = "line1\r\n.line2\r\n..line3\r\n...line4\r\n";
        Assert.assertTrue(TestUtil.addMessageLmtp(subject, new String[] { USER_NAME } , USER_NAME, body));
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZMessage msg = TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        String currentBody = msg.getMimeStructure().getContent();
        TestUtil.assertMessageContains(currentBody, body);
    }

    /**
     * Verifies send/receive behavior for {@code zimbraMailAllowReceiveButNotSendWhenOverQuota}.
     */
    @Test
    public void testAllowReceiveButNotSendWhenOverQuota()
    throws Exception {
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailAllowReceiveButNotSendWhenOverQuota, LdapConstants.LDAP_TRUE);
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailQuota, "1");
        String subject = NAME_PREFIX + " testAllowReceiveButNotSendWhenOverQuota";

        // Verify that receive is allowed.
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");

        // Verify that send is disallowed.
        try {
            TestUtil.sendMessage(mbox, USER_NAME, subject);
            Assert.fail("Send should have failed");
        } catch (ServiceException e) {
            Assert.assertEquals(MailServiceException.QUOTA_EXCEEDED, e.getCode());
        }

        // Verify that adding a document is disallowed.
        try {
            byte[] data = new byte[1024];
            TestUtil.createDocument(mbox, Integer.toString(Mailbox.ID_FOLDER_BRIEFCASE), NAME_PREFIX + " receivenosend.bin", "application/content-stream", data);
            Assert.fail("Document creation should have failed");
        } catch (ServiceException e) {
            Assert.assertEquals(MailServiceException.QUOTA_EXCEEDED, e.getCode());
        }

        // Verify that saving a draft is allowed (bug 51457).
        String draftSubject1 = subject + " save draft 1";
        String draftSubject2 = subject + " save draft 2 two";
        ZOutgoingMessage outgoingDraft = TestUtil.getOutgoingMessage(USER_NAME, draftSubject1, draftSubject1, null);
        ZMessage draftMsg = mbox.saveDraft(outgoingDraft, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS)); // Add message
        outgoingDraft = TestUtil.getOutgoingMessage(USER_NAME, draftSubject2, draftSubject2, null);
        mbox.saveDraft(outgoingDraft, draftMsg.getId(), Integer.toString(Mailbox.ID_FOLDER_DRAFTS)); // Set content of existing message
    }

    /**
     * Verifies that duplicate suppression recognizes the {@code Resent-Message-ID} header
     * (bug 36297).
     */
    @Test
    public void testResentMessageId()
    throws Exception {
        Provisioning.getInstance().getConfig().setMessageIdDedupeCacheSize(1000);

        // Deliver first message.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " testResentMessageId";
        String content = TestUtil.getTestMessage(subject, USER_NAME, USER_NAME, null);
        content = "Message-ID: " + System.currentTimeMillis() + "\r\n" + content;
        String[] recipients = new String[] { USER_NAME };
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        String query = "in:inbox subject:\"" + subject + "\"";
        Assert.assertEquals(1, TestUtil.search(mbox, query).size());

        // Set Resent-Message-ID header and redeliver.
        content = "Resent-Message-ID: " + System.currentTimeMillis() + "\r\n" + content;
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        Assert.assertEquals(2, TestUtil.search(mbox, query).size());

        // Prepend a second Resent-Message-ID header and redeliver.
        content = "Resent-Message-ID: " + System.currentTimeMillis() + "\r\n" + content;
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        Assert.assertEquals(3, TestUtil.search(mbox, query).size());

        // Redeliver the same message, make sure it gets deduped.
        TestUtil.addMessageLmtp(recipients, USER_NAME, content);
        Assert.assertEquals(3, TestUtil.search(mbox, query).size());
    }

    // bug 53058
    @Test
    public void testFinalDotNotSent() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        LmtpClient lmtpClient =
                new LmtpClient("localhost",
                               Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraLmtpBindPort, 7025));
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        lmtpClient.sendLine("LHLO " + LC.zimbra_server_hostname.value());
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());

        if (lmtpClient.getResponse().contains(STARTTLS)) {
            lmtpClient.startTLS();
            lmtpClient.sendLine("LHLO " + LC.zimbra_server_hostname.value());
            Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        }
        lmtpClient.sendLine("MAIL FROM:<" + TestUtil.addDomainIfNecessary(USER_NAME) + ">");
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        lmtpClient.sendLine("RCPT TO:<" + TestUtil.addDomainIfNecessary(USER_NAME) + ">");
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        lmtpClient.sendLine("DATA");
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        String subject = NAME_PREFIX + " testFinalDotNotSent";
        lmtpClient.sendLine("Subject: " + subject);
        lmtpClient.abruptClose();
        // wait for some time
        Thread.sleep(1000);
        List<ZMessage> msgs = TestUtil.search(mbox, "in:inbox " + subject);
        Assert.assertTrue("msg got delivered via LMTP even though <CRLF>.<CRLF> was not received", msgs.isEmpty());
    }

    @Test
    public void testStartTLSSuccess() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        LmtpClient lmtpClient =
                new LmtpClient("localhost",
                               Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraLmtpBindPort, 7025));
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        lmtpClient.sendLine("LHLO " + LC.zimbra_server_hostname.value());
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        if (lmtpClient.getResponse().contains(STARTTLS)) {
            lmtpClient.startTLS();
            lmtpClient.sendLine("LHLO " + LC.zimbra_server_hostname.value());
            Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        }
        lmtpClient.sendLine("MAIL FROM:<" + TestUtil.addDomainIfNecessary(USER_NAME) + ">");
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        lmtpClient.sendLine("RCPT TO:<" + TestUtil.addDomainIfNecessary(USER_NAME) + ">");
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        lmtpClient.sendLine("DATA");
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        String subject = NAME_PREFIX + " testFinalDotNotSent";
        lmtpClient.sendLine("Subject: " + subject);
        lmtpClient.abruptClose();
        // wait for some time
        Thread.sleep(1000);
        List<ZMessage> msgs = TestUtil.search(mbox, "in:inbox " + subject);
        Assert.assertTrue("msg got delivered via LMTP even though <CRLF>.<CRLF> was not received", msgs.isEmpty());
    }

    @Test
    public void testServeShouldNotPublishStartTlsOnSecondLlhoCommand() throws Exception {
        LmtpClient lmtpClient =
                new LmtpClient("localhost",
                               Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraLmtpBindPort, 7025));
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        lmtpClient.sendLine("LHLO " + LC.zimbra_server_hostname.value());
        Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
        if(lmtpClient.getResponse().contains("STARTTLS")) {
            lmtpClient.startTLS();
            lmtpClient.sendLine("LHLO " + LC.zimbra_server_hostname.value());
            Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
            Assert.assertTrue(lmtpClient.getResponse(), !lmtpClient.getResponse().contains(STARTTLS));
        }
        lmtpClient.abruptClose();
    }

    @Test
    public void testLhloNotSendByClient() throws Exception {
        String [] commands = new String []{
                NOOP,
                RSET,
                VRFY + " " + USER_NAME,
                "MAIL FROM:<" + TestUtil.addDomainIfNecessary(USER_NAME) + ">"
        };
        LmtpClient lmtpClient;
        Provisioning prov = Provisioning.getInstance();

        boolean replyOk;
        boolean lhloRequired = prov.getLocalServer().getBooleanAttr(Provisioning.A_zimbraLmtpLHLORequired, true);
        for (String command : commands) {
            lmtpClient = new LmtpClient("localhost",
                                   Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraLmtpBindPort, 7025));
            Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
            lmtpClient.sendLine(command);
            replyOk = lmtpClient.replyOk();
            Assert.assertTrue("Response :"+ lmtpClient.getResponse() + " for command :" + command, lhloRequired? !replyOk : replyOk);
            lmtpClient.abruptClose();
        }
    }

    @Test
    public void testLhloNotSendByClientAfterStartTLS() throws Exception {
        String [] commands = new String []{
                NOOP,
                RSET,
                VRFY + " " + USER_NAME,
                "MAIL FROM:<" + TestUtil.addDomainIfNecessary(USER_NAME) + ">"
        };
        LmtpClient lmtpClient;
        Provisioning prov = Provisioning.getInstance();
        boolean lhloRequired = prov.getLocalServer().getBooleanAttr(Provisioning.A_zimbraLmtpLHLORequired, true);

        boolean replyOk;
        for (String command : commands) {
            lmtpClient = new LmtpClient("localhost", Provisioning.getInstance()
                    .getLocalServer()
                    .getIntAttr(Provisioning.A_zimbraLmtpBindPort, 7025));
            Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
            lmtpClient.sendLine("LHLO " + LC.zimbra_server_hostname.value());
            Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
            if (lmtpClient.getResponse().contains("STARTTLS")) {
                lmtpClient.startTLS();
                lmtpClient.sendLine(command);
                replyOk = lmtpClient.replyOk();
                Assert.assertTrue("Response :"+ lmtpClient.getResponse() + " for command :" + command, lhloRequired? !replyOk : replyOk);
            }
            lmtpClient.abruptClose();
        }
    }

    @Test
    public void testErrorWhenNoStartTlsOnSslEnforcedByServer() throws Exception {
        boolean tlsEnforcedByServer = LC.zimbra_require_interprocess_security.booleanValue();
        if (tlsEnforcedByServer) {
             LmtpClient lmtpClient =
                     new LmtpClient("localhost",
                                    Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraLmtpBindPort, 7025));
             Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
             lmtpClient.sendLine("LHLO " + LC.zimbra_server_hostname.value());
             Assert.assertTrue(lmtpClient.getResponse(), lmtpClient.replyOk());
             if(lmtpClient.getResponse().contains("STARTTLS")) {
                 lmtpClient.sendLine("MAIL FROM:<" + TestUtil.addDomainIfNecessary(USER_NAME) + ">");
                 Assert.assertTrue(lmtpClient.getResponse(), !lmtpClient.replyOk());
             }
             lmtpClient.abruptClose();
        }
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestLmtp.class);
    }
}
