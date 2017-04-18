/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZIdentity;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMessage;
import com.zimbra.cs.mailbox.Mailbox;

public class TestSaveDraft {

    @Rule
    public TestName testInfo = new TestName();

    private static String USER_NAME = null;
    private static final String NAME_PREFIX = TestSaveDraft.class.getName();
    private static String REMOTE_USER_NAME = null;

    @Before
    public void setUp()
    throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user";
        REMOTE_USER_NAME = prefix + "remote-user";
        cleanUp();
    }

    @After
    public void cleanUp()
    throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(REMOTE_USER_NAME);
    }

    /**
     * Confirms that we update the identity id during a SaveDraft operation (bug 60066).
     */
    @Test
    public void testIdentityId()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
                // Save initial draft.
        ZOutgoingMessage outgoing = TestUtil.getOutgoingMessage(USER_NAME, NAME_PREFIX + " testIdentityId", "testIdentityId", null);
        ZIdentity ident = TestUtil.getDefaultIdentity(mbox);
        outgoing.setIdentityId(ident.getId());
        ZMessage msg = mbox.saveDraft(outgoing, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        Assert.assertEquals(ident.getId(), msg.getIdentityId());

        // Save another draft with a new identity id.
        outgoing.setIdentityId("xyz");
        msg = mbox.saveDraft(outgoing, msg.getId(), Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        Assert.assertEquals("xyz", msg.getIdentityId());

        // Unset identity id.
        outgoing.setIdentityId("");
        msg = mbox.saveDraft(outgoing, msg.getId(), Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        Assert.assertEquals(null, msg.getIdentityId());
    }

    /** Verifies "send-message-later" functionality. */
    @Test
    public void testAutoSendDraft() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        ZMailbox.ZOutgoingMessage outgoingMsg = new ZMailbox.ZOutgoingMessage();
        List<ZEmailAddress> addrs = new LinkedList<ZEmailAddress>();
        String senderAddr = TestUtil.getAddress(USER_NAME);
        addrs.add(new ZEmailAddress(senderAddr, null, null, ZEmailAddress.EMAIL_TYPE_FROM));
        String rcptAddr = TestUtil.getAddress(REMOTE_USER_NAME);
        addrs.add(new ZEmailAddress(rcptAddr, null, null, ZEmailAddress.EMAIL_TYPE_TO));
        outgoingMsg.setAddresses(addrs);
        String subject = NAME_PREFIX + " autoSendDraft";
        outgoingMsg.setSubject(subject);

        // auto-send after 0.5 sec
        mbox.saveDraft(outgoingMsg, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS), System.currentTimeMillis() + 500);

        // make sure message has arrived in the Sent folder
        TestUtil.waitForMessage(mbox, "in:Sent " + subject);

        // make sure message is no longer in the Drafts folder
        //there is a race here since auto send and delete from drafts are two transactions
        //handle the race by sleeping and trying again a few times
        boolean deletedFromSent = false;
        int tries = 0;
        while (!deletedFromSent && tries < 10) {
            deletedFromSent = TestUtil.search(mbox, "in:Drafts " + subject).isEmpty();
            if (!deletedFromSent) {
                Thread.sleep(500);
            }
            tries++;
        }
        Assert.assertTrue("message is still in the Drafts folder", deletedFromSent);
    }

     /**
     * Verifies message sent with no 'timeout' is not sent automatically.
     */
    @Test
    public void testDoesntAutoSendDraft() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        ZMailbox.ZOutgoingMessage outgoingMsg = new ZMailbox.ZOutgoingMessage();
        List<ZEmailAddress> addrs = new LinkedList<ZEmailAddress>();
        String senderAddr = TestUtil.getAddress(USER_NAME);
        addrs.add(new ZEmailAddress(senderAddr, null, null, ZEmailAddress.EMAIL_TYPE_FROM));
        String rcptAddr = TestUtil.getAddress(REMOTE_USER_NAME);
        addrs.add(new ZEmailAddress(rcptAddr, null, null, ZEmailAddress.EMAIL_TYPE_TO));
        outgoingMsg.setAddresses(addrs);
        String subject = NAME_PREFIX + " autoSendDraft";
        outgoingMsg.setSubject(subject);

        mbox.saveDraft(outgoingMsg, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS), 0);

        // make sure message is still in the Drafts folder
        boolean deletedFromSent = TestUtil.search(mbox, "in:Drafts " + subject).isEmpty();
        Assert.assertFalse("message should still be in the Drafts folder", deletedFromSent);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSaveDraft.class);
    }
}
