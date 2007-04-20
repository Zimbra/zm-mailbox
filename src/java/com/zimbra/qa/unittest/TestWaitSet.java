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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.admin.WaitMultipleAccounts;
import com.zimbra.cs.service.admin.WaitMultipleAccounts.TypeEnum;
import com.zimbra.cs.session.WaitSet;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.cs.session.WaitSet.WaitSetAccount;
import com.zimbra.cs.session.WaitSet.WaitSetError;

import junit.framework.TestCase;

/**
 * 
 */
public class TestWaitSet extends TestCase {

    private static final String WS_USER_NAME = "ws_test_user";
    private static final String USER_1_NAME = "user1";
    private static final String USER_2_NAME = "user3";
    private static final String NAME_PREFIX = TestWaitSet.class.getSimpleName();
    
    
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
                WaitSetMgr.create(TypeEnum.m.getMask(), false, add);
            waitSetId = result.getFirst();
            errors = result.getSecond();
        }
        
        int curSeqNo = 0;
        assertEquals(0, errors.size());

        { // waitset shouldn't signal until message added to a mailbox
            WaitMultipleAccounts.Callback cb = new WaitMultipleAccounts.Callback();
            
            // wait shouldn't find anything yet
            WaitSet ws = WaitSetMgr.lookup(waitSetId);
            errors = ws.doWait(cb, 0, true, null, null, null);
            assertEquals(0, errors.size());
            synchronized(cb) { assertEquals(false, cb.completed); }
            
            // inserting a message to existing account should trigger waitset
            String sender = TestUtil.getAddress(USER_1_NAME);
            String recipient = TestUtil.getAddress(USER_1_NAME);
            String subject = NAME_PREFIX + " testWaitSet 1";
            TestUtil.insertMessageLmtp(1, subject, recipient, sender);
            try { Thread.sleep(500); } catch (Exception e) {}
            synchronized(cb) { assertEquals(true, cb.completed); }
            curSeqNo = cb.seqNo;
            assertTrue(curSeqNo > 0);
        }
        
        { // waitset should pick up added user
            WaitMultipleAccounts.Callback cb = new WaitMultipleAccounts.Callback();
            
            WaitSet ws = WaitSetMgr.lookup(waitSetId);
            
            // create a new account, shouldn't trigger waitset
            Account user2Acct = TestUtil.getAccount(USER_2_NAME);
            List<WaitSetAccount> add2 = new ArrayList<WaitSetAccount>();
            add2.add(new WaitSetAccount(user2Acct.getId(), null, TypeEnum.m.getMask()));
            errors = ws.doWait(cb, curSeqNo, true, add2, null, null);
            // wait shouldn't find anything yet
            assertEquals(0, errors.size());
            synchronized(cb) { assertEquals(false, cb.completed); }
            
            // adding a message to the new account SHOULD trigger waitset
            String sender = TestUtil.getAddress(WS_USER_NAME);
            String recipient = TestUtil.getAddress(USER_2_NAME);
            String subject = NAME_PREFIX + " testWaitSet 3";
            TestUtil.insertMessageLmtp(1, subject, recipient, sender);
            try { Thread.sleep(500); } catch (Exception e) {}
            synchronized(cb) { assertEquals(true, cb.completed); }
            assertTrue(curSeqNo < cb.seqNo);
            curSeqNo = cb.seqNo;
        }
        {
            WaitSetMgr.destroy(waitSetId);
        }
    }
    
    public void runMeSecond() throws Exception {
        Pair<String, List<WaitSetError>> result = 
            WaitSetMgr.create(TypeEnum.all.getMask(), true, null);
        
        String waitSetId = result.getFirst();
        int curSeqNo = 0;
        List<WaitSetError> errors = result.getSecond();
        assertEquals(0, errors.size());
        

        { // waitset shouldn't signal until message added to a mailbox
            WaitMultipleAccounts.Callback cb = new WaitMultipleAccounts.Callback();
            
            // wait shouldn't find anything yet
            WaitSet ws = WaitSetMgr.lookup(waitSetId);
            errors = ws.doWait(cb, 0, true, null, null, null);
            assertEquals(0, errors.size());
            synchronized(cb) { assertEquals(false, cb.completed); }
            
            // inserting a message to existing account should trigger waitset
            String sender = TestUtil.getAddress(USER_1_NAME);
            String recipient = TestUtil.getAddress(USER_1_NAME);
            String subject = NAME_PREFIX + " testWaitSet 1";
            TestUtil.insertMessageLmtp(1, subject, recipient, sender);
            try { Thread.sleep(500); } catch (Exception e) {}
            synchronized(cb) { assertEquals(true, cb.completed); }
            curSeqNo = cb.seqNo;
            assertTrue(curSeqNo > 0);
        }
        
        { // waitset should remain signalled until sequence number is increased
            WaitMultipleAccounts.Callback cb = new WaitMultipleAccounts.Callback();
            WaitSet ws = WaitSetMgr.lookup(waitSetId);
            errors = ws.doWait(cb, 0, true, null, null, null);
            try { Thread.sleep(500); } catch (Exception e) {}
            assertEquals(0, errors.size());
            synchronized(cb) { assertEquals(true, cb.completed); }
            curSeqNo = cb.seqNo;
        }
        
        { // part 2: waitset for "all" should pick up new account added  
            WaitMultipleAccounts.Callback cb = new WaitMultipleAccounts.Callback();
            
            // wait shouldn't find anything yet
            WaitSet ws = WaitSetMgr.lookup(waitSetId);
            errors = ws.doWait(cb, curSeqNo, true, null, null, null);
            assertEquals(0, errors.size());
            synchronized(cb) { assertEquals(false, cb.completed); }
            
            // create a new account, shouldn't trigger waitset
            TestUtil.createAccount(WS_USER_NAME);
            synchronized(cb) { assertEquals(false, cb.completed); }
            
            // adding a message to the new account SHOULD trigger waitset
            String sender = TestUtil.getAddress(WS_USER_NAME);
            String recipient = TestUtil.getAddress(WS_USER_NAME);
            String subject = NAME_PREFIX + " testWaitSet 2";
            TestUtil.insertMessageLmtp(1, subject, recipient, sender);
            try { Thread.sleep(500); } catch (Exception e) {}
            synchronized(cb) { assertEquals(true, cb.completed); }
            assertTrue(curSeqNo < cb.seqNo);
            curSeqNo = cb.seqNo;
        }
        {
            WaitSetMgr.destroy(waitSetId);
        }
    }
}
