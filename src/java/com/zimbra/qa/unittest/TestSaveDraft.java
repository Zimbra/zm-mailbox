/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.client.ZIdentity;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMessage;

import junit.framework.TestCase;

public class TestSaveDraft extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestSaveDraft.class.getName();
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    /**
     * Confirms that we update the identity id during a SaveDraft operation (bug 60066).
     */
    public void testIdentityId()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        
        // Save initial draft.
        ZOutgoingMessage outgoing = TestUtil.getOutgoingMessage(USER_NAME, NAME_PREFIX + " testIdentityId", "testIdentityId", null);
        ZIdentity ident = TestUtil.getDefaultIdentity(mbox);
        outgoing.setIdentityId(ident.getId());
        ZMessage msg = mbox.saveDraft(outgoing, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        assertEquals(ident.getId(), msg.getIdentityId());
        
        // Save another draft with a new identity id.
        outgoing.setIdentityId("xyz");
        msg = mbox.saveDraft(outgoing, msg.getId(), Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        assertEquals("xyz", msg.getIdentityId());
        
        // Unset identity id.
        outgoing.setIdentityId("");
        msg = mbox.saveDraft(outgoing, msg.getId(), Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        assertEquals(null, msg.getIdentityId());
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
        TestUtil.runTest(TestSaveDraft.class);
    }
}
