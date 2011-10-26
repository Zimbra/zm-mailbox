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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.AttachedMessagePart;
import com.zimbra.client.ZMessage;

public class TestMaxMessageSize
extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestMaxMessageSize.class.getSimpleName();
    private static final long TEST_MAX_MESSAGE_SIZE = 2000;
    
    private String mOrigMaxMessageSize;
    private String mOrigFileUploadMaxSize;
    
    public void setUp()
    throws Exception {
        cleanUp();
        
        Provisioning prov = Provisioning.getInstance();
        mOrigMaxMessageSize = prov.getConfig().getAttr(Provisioning.A_zimbraMtaMaxMessageSize, null);
        mOrigFileUploadMaxSize = prov.getLocalServer().getAttr(Provisioning.A_zimbraFileUploadMaxSize, null); 
    }
    
    public void testMaxMessageSizeBelowThreshold()
    throws Exception {
        setMaxMessageSize(TEST_MAX_MESSAGE_SIZE);
        Map<String, byte[]> attachments = new HashMap<String, byte[]>();
        attachments.put("file1.exe", new byte[200]);
        attachments.put("file2.exe", new byte[300]);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String aid = mbox.uploadAttachments(attachments, 5000);
        TestUtil.sendMessage(mbox, USER_NAME, NAME_PREFIX, "Message size below threshold", aid);
    }
    
    public void testMaxMessageSizeAboveThreshold()
    throws Exception {
        setMaxMessageSize(TEST_MAX_MESSAGE_SIZE);
        Map<String, byte[]> attachments = new HashMap<String, byte[]>();
        attachments.put("file1.exe", new byte[800]);
        attachments.put("file2.exe", new byte[700]);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String aid = mbox.uploadAttachments(attachments, 5000);
        try {
            TestUtil.sendMessage(mbox, USER_NAME, NAME_PREFIX, "Message size above threshold", aid);
            fail("sendMessage() should not have succeeded");
        } catch (SoapFaultException e) {
            // Message send was not allowed, as expected.
            validateMessageTooBigFault(e);
        }
    }
    
    /**
     * Confirms that 
     * @throws Exception
     */
    public void testMaxMessageSizeSaveDraft()
    throws Exception {
        setMaxMessageSize(TEST_MAX_MESSAGE_SIZE);
        
        // Upload attachment whose size is 50% of the threshold.  If this number
        // gets incremented twice, it would exceed the threshold.
        Map<String, byte[]> attachments = new HashMap<String, byte[]>();
        attachments.put("file1.exe", new byte[(int) (TEST_MAX_MESSAGE_SIZE * 0.5)]);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String aid = mbox.uploadAttachments(attachments, 5000);

        // Save draft
        ZOutgoingMessage outgoing = new ZOutgoingMessage();
        List<ZEmailAddress> addresses = new ArrayList<ZEmailAddress>();
        addresses.add(new ZEmailAddress(TestUtil.getAddress(USER_NAME),
            null, null, ZEmailAddress.EMAIL_TYPE_TO));
        outgoing.setAddresses(addresses);
        outgoing.setAttachmentUploadId(aid);
        String subject = NAME_PREFIX + " testMaxMessageSizeSaveDraft";
        outgoing.setSubject(subject);
        ZMessage draft = mbox.saveDraft(outgoing, null, null);
        
        // Send the draft
        outgoing.setAttachmentUploadId(null);
        List<AttachedMessagePart> attachedParts = new ArrayList<AttachedMessagePart>();
        attachedParts.add(new AttachedMessagePart(draft.getId(), "1", null));
        outgoing.setMessagePartsToAttach(attachedParts);
        mbox.sendMessage(outgoing, null, false);
        TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        
        // Reduce max message size and confirm that the send fails.
        setMaxMessageSize((int) (TEST_MAX_MESSAGE_SIZE * 0.6));
        try {
            mbox.sendMessage(outgoing, null, false);
            fail("Message send should not have succeeded.");
        } catch (SoapFaultException e) {
            // Message send was not allowed, as expected.
            validateMessageTooBigFault(e);
        }
    }
    
    public void testUploadMaxSize()
    throws Exception {
        /*
         * bug 27610, default file upload size for messages is now limited by zimbraMtaMaxMessageSize
         */
        // TestUtil.setServerAttr(Provisioning.A_zimbraFileUploadMaxSize, "900");
        setMaxMessageSize(900); 
        
        // Upload an attachment that exceeds the max size
        Map<String, byte[]> attachments = new HashMap<String, byte[]>();
        attachments.put("file1.exe", new byte[1000]);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        try {
            mbox.uploadAttachments(attachments, 5000);
            fail("Attachment upload should have failed");
        } catch (ZClientException e) {
            assertEquals(ZClientException.UPLOAD_SIZE_LIMIT_EXCEEDED, e.getCode());
        }
    }
    
    private void validateMessageTooBigFault(SoapFaultException e)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        long maxSize = prov.getConfig().getLongAttr(Provisioning.A_zimbraMtaMaxMessageSize, -1);
        assertTrue("Unexpected error: " + e.getMessage(),
            e.getMessage().matches("Message of size \\d+ exceeded allowed size"));
        assertEquals(MailServiceException.MESSAGE_TOO_BIG, e.getCode());
        assertEquals(Long.toString(maxSize), e.getArgumentValue("maxSize"));
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
        TestUtil.setServerAttr(Provisioning.A_zimbraFileUploadMaxSize, mOrigFileUploadMaxSize);
        TestUtil.setConfigAttr(Provisioning.A_zimbraMtaMaxMessageSize, mOrigMaxMessageSize);
    }
    
    private void setMaxMessageSize(long numBytes)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMtaMaxMessageSize, Long.toString(numBytes));
        prov.modifyAttrs(config, attrs);
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestMaxMessageSize.class);
    }
}
