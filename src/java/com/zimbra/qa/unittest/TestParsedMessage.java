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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;


public class TestParsedMessage
extends TestCase {

    private static final String SENDER_NAME = "user1";
    private static final String RECIPIENT_NAME = "user1";
    private static final String NAME_PREFIX = TestParsedMessage.class.getSimpleName();

    private File mFile;
    
    private class ExpectedResults {
        String convertedSubject;
        String rawContent;
        boolean wasMutated;
    }
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    public void testParsedMessage()
    throws Exception {
        ExpectedResults expected = new ExpectedResults();
        String subject = NAME_PREFIX + " testParsedMessage";
        expected.convertedSubject = subject;
        expected.rawContent = TestUtil.getTestMessage(subject, RECIPIENT_NAME, SENDER_NAME, null);
        expected.wasMutated = false;
        
        // Test ParsedMessage created from byte[]
        ParsedMessage pm = new ParsedMessage(expected.rawContent.getBytes(), false);
        verifyParsedMessage(pm, expected);
        pm = new ParsedMessage(expected.rawContent.getBytes(), true);
        verifyParsedMessage(pm, expected);
        
        // Test ParsedMessage created from File
        mFile = File.createTempFile("TestParsedMessage", ".msg");
        FileOutputStream out = new FileOutputStream(mFile);
        out.write(expected.rawContent.getBytes());
        
        pm = new ParsedMessage(mFile, null, false);
        verifyParsedMessage(pm, expected);
        pm = new ParsedMessage(mFile, null, true);
        verifyParsedMessage(pm, expected);
        
        // Test ParsedMessage created from MimeMessage. 
        MimeMessage mimeMsg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(expected.rawContent.getBytes()));
        pm = new ParsedMessage(mimeMsg, false);
        verifyParsedMessage(pm, expected);
        pm = new ParsedMessage(mimeMsg, true);
        verifyParsedMessage(pm, expected);
    }

    public void testMimeConverter()
    throws Exception {
        MimeVisitor.registerConverter(TestMimeVisitor.class);
        
        ExpectedResults expected = new ExpectedResults();
        String subject = NAME_PREFIX + " testMimeConverter oldsubject";
        expected.convertedSubject = NAME_PREFIX + " testMimeConverter newsubject";
        expected.rawContent = TestUtil.getTestMessage(subject, RECIPIENT_NAME, SENDER_NAME, null);
        expected.wasMutated = false;
        
        // Test ParsedMessage created from byte[]
        ParsedMessage pm = new ParsedMessage(expected.rawContent.getBytes(), false);
        verifyParsedMessage(pm, expected);
        pm = new ParsedMessage(expected.rawContent.getBytes(), true);
        verifyParsedMessage(pm, expected);
        
        // Test ParsedMessage created from File
        mFile = File.createTempFile("TestParsedMessage", ".msg");
        FileOutputStream out = new FileOutputStream(mFile);
        out.write(expected.rawContent.getBytes());
        out.close();
        
        pm = new ParsedMessage(mFile, null, false);
        verifyParsedMessage(pm, expected);
        pm = new ParsedMessage(mFile, null, true);
        verifyParsedMessage(pm, expected);
        
        // Test ParsedMessage created from MimeMessage.  Can't verify entire content
        // because JavaMail mangles the headers.
        MimeMessage mimeMsg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(expected.rawContent.getBytes()));
        pm = new ParsedMessage(mimeMsg, false);
        assertTrue((new String(pm.getRawData()).contains("oldsubject")));
        assertTrue(getContent(pm.getMimeMessage()).contains("newsubject"));
        assertTrue(pm.getSubject().contains("newsubject"));
        pm = new ParsedMessage(mimeMsg, true);
        assertTrue((new String(pm.getRawData()).contains("oldsubject")));
        assertTrue(getContent(pm.getMimeMessage()).contains("newsubject"));
        assertTrue(pm.getSubject().contains("newsubject"));
    }
    
    private void verifyParsedMessage(ParsedMessage pm, ExpectedResults expected)
    throws Exception {
        // Run tests multiple times to make sure the API's don't alter the state of the ParsedMessage
        for (int i = 1; i < 3; i++) {
            // Test accessors.
            assertEquals(expected.rawContent, new String(pm.getRawData()));
            assertEquals(expected.convertedSubject, pm.getSubject());
            
            // Test sender and recipient
            String sender = TestUtil.getAddress(SENDER_NAME);
            String recipient = TestUtil.getAddress(RECIPIENT_NAME);
            assertTrue(pm.getSender().contains(sender));
            assertEquals(sender, pm.getSenderEmail());
            assertTrue(pm.getRecipients().contains(recipient));

            // Test InputStream accessor
            String contentFromStream = new String(ByteUtil.getContent(pm.getRawInputStream(), expected.rawContent.length()));
            assertEquals(expected.rawContent, contentFromStream);

            // Test MimeMessage accessor
            assertTrue(getContent(pm.getMimeMessage()).contains(expected.convertedSubject));
    
            // Test mutated status
            assertEquals(expected.wasMutated, pm.wasMutated());
            
            pm.analyzeFully();
        }
    }
    
    /**
     * Tests message mutation.  We can't verify the entire content, since mutation
     * calls JavaMail, which mangles the headers.  We'll have to settle for confirming
     * that the subject was updated correctly.  
     */
    public void testMimeMutator()
    throws Exception {
        MimeVisitor.registerMutator(TestMimeVisitor.class);
        
        String subject = NAME_PREFIX + " testMimeConverter oldsubject";
        String content = TestUtil.getTestMessage(subject, RECIPIENT_NAME, SENDER_NAME, null);
        
        // Test ParsedMessage created from byte[]
        ParsedMessage pm = new ParsedMessage(content.getBytes(), false);
        
        pm = new ParsedMessage(content.getBytes(), true);
        String substring = "newsubject";
        verifyMutatedMessage(pm, substring, true);
        
        // Test ParsedMessage created from File
        mFile = createTempFile(content);
        pm = new ParsedMessage(mFile, null, false);
        verifyMutatedMessage(pm, substring, true);
        mFile.delete();
        
        mFile = createTempFile(content);
        pm = new ParsedMessage(mFile, null, true);
        verifyMutatedMessage(pm, substring, true);
        
        // Test ParsedMessage created from MimeMessage, attachment indexing off.
        MimeMessage mimeMsg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(content.getBytes()));
        pm = new ParsedMessage(mimeMsg, false);
        verifyMutatedMessage(pm, substring, true);
        
        // Test ParsedMessage created from MimeMessage, attachment indexing on.
        mimeMsg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(content.getBytes()));
        pm = new ParsedMessage(mimeMsg, true);
        verifyMutatedMessage(pm, substring, true);
    }
    
    private File createTempFile(String content)
    throws IOException {
        File file = File.createTempFile("TestParsedMessage", ".msg");
        FileOutputStream out = new FileOutputStream(file);
        out.write(content.getBytes());
        out.close();
        return file;
    }
    
    private void verifyMutatedMessage(ParsedMessage pm, String substring, boolean wasMutated)
    throws Exception {
        assertEquals(wasMutated, pm.wasMutated());
        assertTrue(pm.getSubject().contains(substring));
        assertTrue((new String(pm.getRawData()).contains(substring)));
        
        byte[] data = ByteUtil.getContent(pm.getRawInputStream(), 0);
        assertTrue((new String(data)).contains(substring));
        data = pm.getRawData();
        assertTrue((new String(data)).contains(substring));
    }
    
    /**
     * Confirms that the digest returned by a <tt>ParsedMessage</tt> is the same,
     * regardless of whether the source comes from a byte array, file or <tt>MimeMessage</tt>. 
     */
    public void testGetData()
    throws Exception {
        String msg = TestUtil.getTestMessage(NAME_PREFIX + " testGetData", SENDER_NAME, SENDER_NAME, null);
        
        // Test ParsedMessage from byte[]
        ParsedMessage pm = new ParsedMessage(msg.getBytes(), true);
        runContentTests(msg, pm);
        
        // Test ParsedMessage from File
        mFile = File.createTempFile("TestParsedMessage", null);
        FileOutputStream out = new FileOutputStream(mFile);
        out.write(msg.getBytes());
        pm = new ParsedMessage(mFile, null, true);
        runContentTests(msg, pm);
        
        // Test ParsedMessage from MimeMessage
        MimeMessage mimeMsg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(msg.getBytes()));
        pm = new ParsedMessage(mimeMsg, true);
        runContentTests(msg, pm);
    }
    
    private void runContentTests(String originalMsg, ParsedMessage pm)
    throws Exception {
        int size = originalMsg.length();
        
        // Test InputStream accessor
        String msg = new String(ByteUtil.getContent(pm.getRawInputStream(), size));
        assertEquals("expected: " + originalMsg + "\ngot: " + msg, originalMsg, msg);
        
        // Test byte[] accessor
        msg = new String(pm.getRawData());
        assertEquals("expected: " + originalMsg + "\ngot: " + msg, originalMsg, msg);
    }

    /**
     * Tests adding a <tt>ParsedMessage</tt> to a mailbox.
     */
    public void testAddMessage()
    throws Exception {
        String msg = TestUtil.getTestMessage(NAME_PREFIX + " testAddMessage", SENDER_NAME, SENDER_NAME, null);
        
        // Test ParsedMessage from byte[]
        ParsedMessage pm = new ParsedMessage(msg.getBytes(), true);
        runAddMessageTest(msg, pm);
        
        // Test ParsedMessage from File
        mFile = File.createTempFile("TestParsedMessage", null);
        FileOutputStream out = new FileOutputStream(mFile);
        out.write(msg.getBytes());
        pm = new ParsedMessage(mFile, null, true);
        runAddMessageTest(msg, pm);
        
        // Test ParsedMessage from MimeMessage
        MimeMessage mimeMsg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(msg.getBytes()));
        pm = new ParsedMessage(mimeMsg, true);
        runAddMessageTest(msg, pm);
    }
    
    private void runAddMessageTest(String originalMsg, ParsedMessage pm)
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(SENDER_NAME);
        Message msg = mbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX, false, 0, null);
        assertEquals(originalMsg, new String(ByteUtil.getContent(msg.getContentStream(), 0)));
    }
    
    private String getContent(MimeMessage msg)
    throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        msg.writeTo(buf);
        return new String(buf.toByteArray());
    }
    
    public void tearDown()
    throws Exception {
        if (mFile != null) {
            mFile.delete();
        }
        MimeVisitor.unregisterConverter(TestMimeVisitor.class);
        MimeVisitor.unregisterMutator(TestMimeVisitor.class);
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(SENDER_NAME, NAME_PREFIX);
    }
}
