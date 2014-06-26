/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014 Zimbra, Inc.
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
