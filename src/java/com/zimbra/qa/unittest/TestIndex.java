/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.List;

import junit.framework.TestCase;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;


public class TestIndex extends TestCase {

    private static final String NAME_PREFIX = TestIndex.class.getSimpleName();
    private static final String USER_NAME = "user1";

    private int mOriginalTextLimit;
    @Override
	public void setUp()
    throws Exception {
        mOriginalTextLimit = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit));
        cleanUp();
    }

    public void testIndexedTextLimit()
    throws Exception {
        // Test text attachment
        StringBuilder body = new StringBuilder();
        for (int i = 1; i < 100; i++) {
            body.append("Walrus walrus walrus walrus walrus walrus walrus.\n");
        }
        body.append("Goo goo goo joob.\n");

        // Test text truncated
        setTextLimit(50);
        String subject = NAME_PREFIX + " text attachment 1";
        String msgId = sendMessage(subject, body.toString().getBytes(), "attachment.txt", MimeConstants.CT_TEXT_PLAIN).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", null);

        // Test HTML truncated
        subject = NAME_PREFIX + " HTML attachment 1";
        String htmlBody = "<html>\n" + body + "</html>";
        msgId = sendMessage(subject, htmlBody.getBytes(), "attachment.html", MimeConstants.CT_TEXT_HTML).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", null);

        // Test text not truncated
        setTextLimit(100000);
        subject = NAME_PREFIX + " text attachment 2";
        msgId = sendMessage(subject, body.toString().getBytes(), "attachment.txt", MimeConstants.CT_TEXT_PLAIN).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", msgId);

        // Test HTML not truncated
        subject = NAME_PREFIX + " HTML attachment 2";
        msgId = sendMessage(subject, htmlBody.getBytes(), "attachment.html", MimeConstants.CT_TEXT_HTML).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", msgId);

        // Test attached message subject truncated
        subject = NAME_PREFIX + " subject";
        String attachedMsg = TestUtil.getTestMessage("Pigs from a gun", "recipient", "sender", null);
        setTextLimit(4);
        msgId = sendMessage(subject, attachedMsg.getBytes(), "attachment.msg", MimeConstants.CT_MESSAGE_RFC822).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" pigs", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" gun", null);
    }

    public void testRemovingStopWords() throws Exception {
    	Provisioning.getInstance().getConfig().removeDefaultAnalyzerStopWords("a");
    	String body = "Walrus walrus walrus walrus walrus walrus walrus is a walrus.\n";
    	String subject = NAME_PREFIX + " text with A letter";
    	String msgId = sendMessage(subject, body.toString().getBytes(), "attachment.txt", MimeConstants.CT_TEXT_PLAIN).getId();

    	//test removed stop word "a"
        checkQuery("in:inbox subject:\" a \"", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" A", msgId);
    }

    public void testExistingStopWords() throws Exception {
    	Provisioning.getInstance().getConfig().removeDefaultAnalyzerStopWords("a");
    	String body = "Walrus walrus walrus walrus walrus walrus walrus is a walrus.\n";
    	String subject = NAME_PREFIX + " text with A letter";
    	String msgId = sendMessage(subject, body.toString().getBytes(), "attachment.txt", MimeConstants.CT_TEXT_PLAIN).getId();

        //test existing stop word "s"
        checkQuery("in:inbox subject:\" is \"", null);
    }
    /**
     * Verifies the fix to bug 54613.
     */
    public void testFilenameSearch()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String filename = NAME_PREFIX + " testFilenameSearch.txt";
        TestUtil.createDocument(mbox, Integer.toString(Mailbox.ID_FOLDER_BRIEFCASE),
            filename, "text/plain", "This is the data for testFilenameSearch.".getBytes());
        assertEquals(0, TestUtil.search(mbox, "filename:Blob*", ZSearchParams.TYPE_DOCUMENT).size());
        assertEquals(1, TestUtil.search(mbox, "filename:\"" + filename + "\"", ZSearchParams.TYPE_DOCUMENT).size());
    }

    /**
     * Sends a message with the specified attachment, waits for the message to
     * arrives, and runs a query.
     * @param subject the subject of the message
     * @param attData attachment data
     * @param attName attachment name
     * @param attContentType attachment content type
     * @param query query to run after message arrives
     * @return <tt>true</tt> if the query returns the message
     */
    private ZMessage sendMessage(String subject, byte[] attData, String attName, String attContentType)
    throws Exception {

        // Send message
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String attachmentId = mbox.uploadAttachment(attName, attData, attContentType, 5000);
        TestUtil.sendMessage(mbox, USER_NAME, subject, "Cranberry sauce", attachmentId);
        String query = "in:inbox subject:\"" + subject + "\"";
        return TestUtil.waitForMessage(mbox, query);
    }

    private void checkQuery(String query, String msgId)
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, query);
        if (msgId == null) {
            assertEquals(0, messages.size());
        } else {
            assertEquals(1, messages.size());
            assertEquals(msgId, messages.get(0).getId());
        }
    }

    private void setTextLimit(int numBytes)
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit, Integer.toString(numBytes));
    }

    @Override
	public void tearDown()
    throws Exception {
        setTextLimit(mOriginalTextLimit);
        Provisioning.getInstance().getConfig().addDefaultAnalyzerStopWords("a");
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestIndex.class);
    }
}
