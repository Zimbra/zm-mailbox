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

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.mail.WaitSetRequest;
import com.zimbra.cs.service.mail.WaitSetRequest.TypeEnum;
import com.zimbra.cs.session.IWaitSet;
import com.zimbra.cs.session.WaitSetAccount;
import com.zimbra.cs.session.WaitSetError;
import com.zimbra.cs.session.WaitSetMgr;

import junit.framework.TestCase;

/**
 * 
 */
public class TestWaitSet extends TestCase {

    private static final String WS_USER_NAME = "ws_test_user";
    private static final String USER_1_NAME = "user1";
    private static final String USER_2_NAME = "user3";
    private static final String NAME_PREFIX = TestWaitSet.class.getSimpleName();
    
    private static final String FAKE_ACCOUNT_ID = "fake";
    
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    public void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_1_NAME, NAME_PREFIX);
        try {
            Mailbox wsMbox = TestUtil.getMailbox(WS_USER_NAME);
            wsMbox.deleteMailbox();
        } catch (Exception e) { }
        try { TestUtil.deleteAccount(WS_USER_NAME); } catch (Exception e) {}
    }
    
    public void testWaitSets() throws Exception {
        runMeFirst();
        runMeSecond();
    }
    
    private void runMeFirst() throws Exception {
        String waitSetId;
        List<WaitSetError> errors;

        {
            Account user1Acct = TestUtil.getAccount(USER_1_NAME);
            List<WaitSetAccount> add = new ArrayList<WaitSetAccount>();
            add.add(new WaitSetAccount(user1Acct.getId(), null, TypeEnum.m.getMask()));
            
            Pair<String, List<WaitSetError>> result = 
                WaitSetMgr.create(FAKE_ACCOUNT_ID, true, TypeEnum.m.getMask(), false, add);
            waitSetId = result.getFirst();
            errors = result.getSecond();
        }
        
        try {
            String curSeqNo = "0";
            assertEquals(0, errors.size());
            
            { // waitset shouldn't signal until message added to a mailbox
                WaitSetRequest.Callback cb = new WaitSetRequest.Callback();
                
                // wait shouldn't find anything yet
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, "0", null, null, null);
                assertEquals(0, errors.size());
                synchronized(cb) { assertEquals(false, cb.completed); }
                
                // inserting a message to existing account should trigger waitset
                String sender = TestUtil.getAddress(USER_1_NAME);
                String recipient = TestUtil.getAddress(USER_1_NAME);
                String subject = NAME_PREFIX + " testWaitSet 1";
                TestUtil.addMessageLmtp(subject, recipient, sender);
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertEquals(true, cb.completed); }
                curSeqNo = cb.seqNo;
            }
            
            { // waitset should pick up added user
                WaitSetRequest.Callback cb = new WaitSetRequest.Callback();
                
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                
                // create a new account, shouldn't trigger waitset
                Account user2Acct = TestUtil.getAccount(USER_2_NAME);
                List<WaitSetAccount> add2 = new ArrayList<WaitSetAccount>();
                add2.add(new WaitSetAccount(user2Acct.getId(), null, TypeEnum.m.getMask()));
                errors = ws.doWait(cb, curSeqNo, add2, null, null);
                // wait shouldn't find anything yet
                assertEquals(0, errors.size());
                synchronized(cb) { assertEquals(false, cb.completed); }
                
                // adding a message to the new account SHOULD trigger waitset
                String sender = TestUtil.getAddress(WS_USER_NAME);
                String recipient = TestUtil.getAddress(USER_2_NAME);
                String subject = NAME_PREFIX + " testWaitSet 3";
                TestUtil.addMessageLmtp(subject, recipient, sender);
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertEquals(true, cb.completed); }
                curSeqNo = cb.seqNo;
            }
        } finally {
            WaitSetMgr.destroy(FAKE_ACCOUNT_ID, waitSetId);
        }
    }
    
    public void runMeSecond() throws Exception {
        Pair<String, List<WaitSetError>> result = 
            WaitSetMgr.create(FAKE_ACCOUNT_ID, true, TypeEnum.all.getMask(), true, null);
        
        String waitSetId = result.getFirst();
        String curSeqNo = "0";
        List<WaitSetError> errors = result.getSecond();
        assertEquals(0, errors.size());
        
        try {

            { // waitset shouldn't signal until message added to a mailbox
                WaitSetRequest.Callback cb = new WaitSetRequest.Callback();

                // wait shouldn't find anything yet
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, "0", null, null, null);
                assertEquals(0, errors.size());
                synchronized(cb) { assertEquals(false, cb.completed); }

                // inserting a message to existing account should trigger waitset
                String sender = TestUtil.getAddress(USER_1_NAME);
                String recipient = TestUtil.getAddress(USER_1_NAME);
                String subject = NAME_PREFIX + " testWaitSet 1";
                TestUtil.addMessageLmtp(subject, recipient, sender);
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertEquals(true, cb.completed); }
                curSeqNo = cb.seqNo;
            }

            { // waitset should remain signalled until sequence number is increased
                WaitSetRequest.Callback cb = new WaitSetRequest.Callback();
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, "0", null, null, null);
                try { Thread.sleep(500); } catch (Exception e) {}
                assertEquals(0, errors.size());
                synchronized(cb) { assertEquals(true, cb.completed); }
                curSeqNo = cb.seqNo;
            }

            { // part 2: waitset for "all" should pick up new account added  
                WaitSetRequest.Callback cb = new WaitSetRequest.Callback();

                // wait shouldn't find anything yet
                IWaitSet ws = WaitSetMgr.lookup(waitSetId);
                errors = ws.doWait(cb, curSeqNo, null, null, null);
                assertEquals(0, errors.size());
                synchronized(cb) { assertEquals(false, cb.completed); }

                // create a new account, shouldn't trigger waitset
                TestUtil.createAccount(WS_USER_NAME);
                synchronized(cb) { assertEquals(false, cb.completed); }

                // adding a message to the new account SHOULD trigger waitset
                String sender = TestUtil.getAddress(WS_USER_NAME);
                String recipient = TestUtil.getAddress(WS_USER_NAME);
                String subject = NAME_PREFIX + " testWaitSet 2";
                TestUtil.addMessageLmtp(subject, recipient, sender);
                try { Thread.sleep(500); } catch (Exception e) {}
                synchronized(cb) { assertEquals(true, cb.completed); }
                curSeqNo = cb.seqNo;
            }
        } finally {
            WaitSetMgr.destroy(FAKE_ACCOUNT_ID, waitSetId);
        }
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
}
