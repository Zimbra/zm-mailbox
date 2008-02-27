/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import com.zimbra.common.util.ByteUtil;
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
        String rawDigest;
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
        expected.rawDigest = ByteUtil.getDigest(expected.rawContent.getBytes());
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
        
        // Test ParsedMessage created from MimeMessage.  We can't do an exact comparison of
        // the data returned byt the MimeMessage because JavaMail adds headers. 
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
        expected.rawDigest = ByteUtil.getDigest(expected.rawContent.getBytes());
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
            // Test data, digest and size accessors
            assertEquals(expected.rawContent, new String(pm.getRawData()));
            assertEquals(expected.rawDigest, pm.getRawDigest());
            assertEquals(expected.rawContent.length(), pm.getRawSize());
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
            pm.closeFile();
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
        mFile = File.createTempFile("TestParsedMessage", ".msg");
        FileOutputStream out = new FileOutputStream(mFile);
        out.write(content.getBytes());
        out.close();
        
        pm = new ParsedMessage(mFile, null, false);
        verifyMutatedMessage(pm, substring, true);
        pm = new ParsedMessage(mFile, null, true);
        verifyMutatedMessage(pm, substring, true);
        
        // Test ParsedMessage created from MimeMessage.  We're currently
        // not mutating the message when the ParsedMessage is created from
        // a MimeMessage.
        MimeMessage mimeMsg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(content.getBytes()));
        pm = new ParsedMessage(mimeMsg, false);
        verifyMutatedMessage(pm, "oldsubject", false);
        pm = new ParsedMessage(mimeMsg, true);
        verifyMutatedMessage(pm, "oldsubject", false);
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
