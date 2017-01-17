/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;

import com.zimbra.client.ZFolder;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.mail.WaitSetRequest;
import com.zimbra.cs.service.mail.WaitSetRequest.TypeEnum;
import com.zimbra.cs.session.IWaitSet;
import com.zimbra.cs.session.WaitSetAccount;
import com.zimbra.cs.session.WaitSetCallback;
import com.zimbra.cs.session.WaitSetError;
import com.zimbra.cs.session.WaitSetMgr;

import junit.framework.TestCase;

/**
 *
 */
public class TestWaitSet extends TestCase {

    private static final String WS_USER_NAME = "ws_test_user";
    private static final String USER_1_NAME = "waitsetuser1";
    private static final String USER_2_NAME = "waitsetuser2";
    private static final String NAME_PREFIX = TestWaitSet.class.getSimpleName();

    private static final String FAKE_ACCOUNT_ID = "fake";

    @Override
    public void setUp() throws Exception {
        cleanUp();
    }

    public void cleanUp()
    throws Exception {
        String acctNames[] = { USER_1_NAME, USER_2_NAME, WS_USER_NAME };
        for (String acctName : acctNames) {
            try {
                Mailbox wsMbox = TestUtil.getMailbox(acctName);
                wsMbox.deleteMailbox();
            } catch (Exception e) { }
            try {
                TestUtil.deleteAccount(acctName);
            } catch (Exception e) {}
        }
    }

    public void testWaitSets() throws Exception {
        TestUtil.createAccount(USER_1_NAME);
        TestUtil.createAccount(USER_2_NAME);
        runMeFirst();
        runMeSecond();
    }

    private void runMeFirst() throws Exception {
        String waitSetId;
        List<WaitSetError> errors;

        {
            Account user1Acct = TestUtil.getAccount(USER_1_NAME);
            List<WaitSetAccount> add = new ArrayList<WaitSetAccount>();
            add.add(new WaitSetAccount(user1Acct.getId(), null, TypeEnum.m.getTypes(), null));

            Pair<String, List<WaitSetError>> result =
                WaitSetMgr.create(FAKE_ACCOUNT_ID, true, TypeEnum.m.getTypes(), false, add);
            waitSetId = result.getFirst();
            errors = result.getSecond();
        }

        try {
            String curSeqNo = "0";
            assertEquals("Expecting sequence to be 0 when the test starts", 0, errors.size());

            { // waitset shouldn't signal until message added to a mailbox
                WaitSetCallback cb = new WaitSetCallback();

                // wait shouldn't find anything yet
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, "0", null, null);
                assertEquals("Expecting sequence to be 0 during first wait", 0, errors.size());
                synchronized(cb) { assertEquals("Callback should not be completed yet", false, cb.completed); }

                // inserting a message to existing account should trigger waitset
                String sender = TestUtil.getAddress(USER_1_NAME);
                String recipient = TestUtil.getAddress(USER_1_NAME);
                String subject = NAME_PREFIX + " testWaitSet 1";
                TestUtil.addMessageLmtp(subject, recipient, sender);
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertEquals("callback should be completed now", true, cb.completed); }
                curSeqNo = cb.seqNo;
            }

            { // waitset should pick up added user
                WaitSetCallback cb = new WaitSetCallback();

                IWaitSet ws = WaitSetMgr.lookup(waitSetId);

                // create a new account, shouldn't trigger waitset
                Account user2Acct = TestUtil.getAccount(USER_2_NAME);
                List<WaitSetAccount> add2 = new ArrayList<WaitSetAccount>();
                add2.add(new WaitSetAccount(user2Acct.getId(), null, TypeEnum.m.getTypes(), null));
                errors = ws.doWait(cb, curSeqNo, add2, null);
                // wait shouldn't find anything yet
                assertEquals("Should have no errors", 0, errors.size());
                synchronized(cb) { assertEquals("Callback should not be completed before it is triggered by the second account", false, cb.completed); }

                // adding a message to the new account SHOULD trigger waitset
                String sender = TestUtil.getAddress(WS_USER_NAME);
                String recipient = TestUtil.getAddress(USER_2_NAME);
                String subject = NAME_PREFIX + " testWaitSet 3";
                TestUtil.addMessageLmtp(subject, recipient, sender);
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertEquals("Callback should be completed after it is triggered by the second account", true, cb.completed); }
                curSeqNo = cb.seqNo;
            }
        } finally {
            WaitSetMgr.destroy(null, FAKE_ACCOUNT_ID, waitSetId);
        }
    }

    public void runMeSecond() throws Exception {
        Pair<String, List<WaitSetError>> result =
            WaitSetMgr.create(FAKE_ACCOUNT_ID, true, TypeEnum.all.getTypes(), true, null);

        String waitSetId = result.getFirst();
        String curSeqNo = "0";
        List<WaitSetError> errors = result.getSecond();
        assertEquals("'errors' shoul dbe empty before creating a callback", 0, errors.size());

        try {

            { // waitset shouldn't signal until message added to a mailbox
                WaitSetCallback cb = new WaitSetCallback();

                // wait shouldn't find anything yet
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, "0", null, null);
                assertEquals("'errors' should be empty", 0, errors.size());
                synchronized(cb) { assertFalse("callback for all accounts should not be completed before it is triggered", cb.completed); }

                // inserting a message to existing account should trigger waitset
                String sender = TestUtil.getAddress(USER_1_NAME);
                String recipient = TestUtil.getAddress(USER_1_NAME);
                String subject = NAME_PREFIX + " testWaitSet 1";
                TestUtil.addMessageLmtp(subject, recipient, sender);
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertTrue("callback for all accounts should be completed after it is triggered by " + USER_1_NAME, cb.completed); }
                curSeqNo = cb.seqNo;
            }

            { // waitset should remain signalled until sequence number is increased
                WaitSetCallback cb = new WaitSetCallback();
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, "0", null, null);
                try { Thread.sleep(500); } catch (Exception e) {}
                assertEquals(0, errors.size());
                synchronized(cb) { assertTrue("call back for all accounts should remain completed until sequence number is increased", cb.completed); }
                curSeqNo = cb.seqNo;
            }

            { // waitset shouldn't signal until a document is added
                WaitSetCallback cb = new WaitSetCallback();
                // wait shouldn't find anything yet
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, curSeqNo, null, null);
                assertEquals(0, errors.size());
                synchronized(cb) { assertFalse("call back for all accounts should switch to not completed after sequence number is increased", cb.completed); }

                // creating a document in existing account should trigger waitset
                String subject = NAME_PREFIX + " testWaitSet document 1";
                TestUtil.createDocument(TestUtil.getZMailbox(USER_2_NAME),
                        ZFolder.ID_BRIEFCASE, subject, "text/plain", "Hello, world!".getBytes());
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertTrue("document waitset should be completed", cb.completed); }
                curSeqNo = cb.seqNo;
            }

            { // part 2: waitset for "all" should pick up new account added
                WaitSetCallback cb = new WaitSetCallback();

                // wait shouldn't find anything yet
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, curSeqNo, null, null);
                assertEquals(0, errors.size());
                synchronized(cb) { assertFalse("Doing nothing should not complete the callback", cb.completed); }

                // create a new account, shouldn't trigger waitset
                TestUtil.createAccount(WS_USER_NAME);
                synchronized(cb) { assertFalse("creating a new account should not complete the callback", cb.completed); }

                // adding a message to the new account SHOULD trigger waitset
                String sender = TestUtil.getAddress(WS_USER_NAME);
                String recipient = TestUtil.getAddress(WS_USER_NAME);
                String subject = NAME_PREFIX + " testWaitSet 2";
                TestUtil.addMessageLmtp(subject, recipient, sender);
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertTrue("Callback should be completed after adding a message to " + WS_USER_NAME, cb.completed); }
                curSeqNo = cb.seqNo;
            }
        } finally {
            WaitSetMgr.destroy(null, FAKE_ACCOUNT_ID, waitSetId);
        }
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }
}
