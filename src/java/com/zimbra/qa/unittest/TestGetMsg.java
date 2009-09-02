/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.handler.TextEnrichedHandler;
import com.zimbra.cs.zclient.ZGetMessageParams;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZMessage.ZMimePart;
import com.zimbra.common.mime.MimeConstants;

public class TestGetMsg
extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestGetMsg.class.getSimpleName();
    
    private String mOriginalContentMaxSize;
    
    public void setUp()
    throws Exception {
        cleanUp();
        mOriginalContentMaxSize = TestUtil.getServerAttr(Provisioning.A_zimbraMailContentMaxSize);
    }
    
    public void testPlainMessageContent()
    throws Exception {
        doTestMessageContent(MimeConstants.CT_TEXT_PLAIN, "This is the body of a plain message.");
    }
    
    public void testHtmlMessageContent()
    throws Exception {
        doTestMessageContent(MimeConstants.CT_TEXT_HTML, "<html><head></head><body>HTML message</body></html>");
    }
    
    public void testEnrichedMessageContent()
    throws Exception {
        doTestMessageContent(MimeConstants.CT_TEXT_ENRICHED,
            "<color><param>red</param>Blood</color> is <bold>thicker</bold> than<color><param>blue</param>water</color>.");
    }
    
    private void doTestMessageContent(String contentType, String body)
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        MessageBuilder mb = new MessageBuilder();
        
        String subject = NAME_PREFIX + " testMessageContent " + contentType;
        String raw = mb.withSubject(subject).withBody(body).withContentType(contentType).create();
        String msgId = TestUtil.addRawMessage(mbox, raw);
        if (contentType.equals(MimeConstants.CT_TEXT_ENRICHED)) {
            body = TextEnrichedHandler.convertToHTML(body);
        }
        
        verifyMessageContent(mbox, msgId, false, null, null, false, body, contentType);
        verifyMessageContent(mbox, msgId, true, null, null, false, body, contentType);
        verifyMessageContent(mbox, msgId, false, 24, 24, true, body, contentType);
        verifyMessageContent(mbox, msgId, true, 24, 24, true, body, contentType);
        
        // Set zimbraMailMaxContentLength and confirm that the content
        // gets truncated.
        TestUtil.setServerAttr(Provisioning.A_zimbraMailContentMaxSize, "24");
        verifyMessageContent(mbox, msgId, false, null, 24, true, body, contentType);
        verifyMessageContent(mbox, msgId, true, null, 24, true, body, contentType);
    }
    
    private void verifyMessageContent(ZMailbox mbox, String msgId, boolean wantHtml,
                                      Integer requestMaxLength, Integer expectedLength,
                                      boolean expectedTruncated, String body, String contentType)
    throws Exception {
        ZGetMessageParams params = new ZGetMessageParams();
        params.setId(msgId);
        params.setWantHtml(wantHtml);
        params.setMax(requestMaxLength);
        ZMessage msg = mbox.getMessage(params);
        ZMimePart mp = msg.getMimeStructure();

        assertEquals(expectedTruncated, mp.wasTruncated());
        String expected = body;
        if (expectedLength != null) {
            expected = body.substring(0, expectedLength);
        }

        if (contentType.equals(MimeConstants.CT_TEXT_ENRICHED)) {
            // HTML conversion in TextEnrichedHandler will drop trailing
            // characters when the enriched data is malformed (tags not
            // closed, etc.).
            assertTrue(mp.getContent().length() > 0);
            assertTrue(expected.startsWith(mp.getContent()));
        } else {
            assertEquals(expected, mp.getContent());
        }
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
        TestUtil.setServerAttr(Provisioning.A_zimbraMailContentMaxSize, mOriginalContentMaxSize);
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestGetMsg.class);
    }
}
