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
import java.io.StringReader;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.zclient.ZGetMessageParams;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;


public class TestSendAndReceive extends TestCase {

    private static final String NAME_PREFIX = TestSendAndReceive.class.getSimpleName();
    private static final String USER_NAME = "user1";
    private static final Pattern PAT_RECEIVED = Pattern.compile("Received: .*from.*LHLO.*");
    private static final Pattern PAT_RETURN_PATH = Pattern.compile("Return-Path: (.*)");
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    /**
     * Verifies that we set the Return-Path and Received headers
     * for incoming messages.
     */
    public void testReceivedHeaders()
    throws Exception {
        // Send message from user2 to user1
        String sender = TestUtil.getAddress("user2");
        String recipient = TestUtil.getAddress(USER_NAME);
        TestUtil.addMessageLmtp(NAME_PREFIX + " testReceivedHeaders()", recipient, sender);
        
        // Search
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, NAME_PREFIX);
        assertEquals("Unexpected message count", 1, messages.size());
        
        // Get the message content, since a search won't return the content
        ZGetMessageParams params = new ZGetMessageParams();
        params.setId(messages.get(0).getId());
        params.setRawContent(true);
        ZMessage message = mbox.getMessage(params);
        String content = message.getContent();
        
        // Check headers
        boolean foundReceived = false;
        boolean foundReturnPath = false;
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = PAT_RECEIVED.matcher(line);
            if (matcher.matches()) {
                ZimbraLog.test.debug("Found " + line);
                foundReceived = true;
            }
            
            matcher = PAT_RETURN_PATH.matcher(line);
            if (matcher.matches()) {
                foundReturnPath = true;
                assertEquals("Sender doesn't match", sender, matcher.group(1));
                ZimbraLog.test.debug("Found " + line);
            }
            line = reader.readLine();
        }
        reader.close();
        
        assertTrue("Received header not found.  Content=\n" + content, foundReceived);
        assertTrue("Return-Path header not found.  Content=\n" + content, foundReturnPath);
    }
    
    /**
     * Confirms that the message received date is set to the value of the
     * <tt>X-Zimbra-Received</tt> header.
     */
    public void testZimbraReceivedHeader()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, "subject:\"Test Phone Number Formats\"");
        assertEquals("Unexpected message count", 1, messages.size());
        ZMessage msg = messages.get(0);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(msg.getReceivedDate());
        assertEquals(2005, cal.get(Calendar.YEAR));
        assertEquals(1, cal.get(Calendar.MONTH));
        assertEquals(27, cal.get(Calendar.DAY_OF_MONTH));
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestSendAndReceive.class));
    }
}
