/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.base.Joiner;
import com.zimbra.client.ZConversation;
import com.zimbra.client.ZConversation.ZMessageSummary;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Fetch;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZSendMessageResponse;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessageHit;
import com.zimbra.client.ZSearchHit;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.util.ZimbraLog;

@RunWith(Parameterized.class)
public class TestSearchConv {

    private static final String TEST_CLASS_NAME = TestSearchConv.class.getSimpleName();
    private static String USER_NAME = "user1";
    private static String REMOTE_USER_NAME = "user2";
    private static String subject = TEST_CLASS_NAME;
    private static ZMailbox mbox;
    private static ZMailbox remote_mbox;
    private static String convId;
    private static ArrayList<String> msgIds = new ArrayList<String>();
    private static ZConversation conv;

    private final String query;
    private final Fetch fetch;
    private final int[] expected;
    private final int[] unread;

    public TestSearchConv(String query, Fetch fetch, int[] unread, int[] expected) {
        this.query = query;
        this.fetch = fetch;
        this.expected = expected;
        this.unread   = unread;
    }

    /** Note not using @Before annotation - see testInputs() for why */
    public static void setUp() throws Exception {
        USER_NAME = String.format("%s-user1", TEST_CLASS_NAME).toLowerCase();
        REMOTE_USER_NAME = String.format("%s-remote-user", TEST_CLASS_NAME).toLowerCase();
        cleanUp();
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        remote_mbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        setupConversation();
        conv = mbox.getConversation(convId, Fetch.all);
        List<ZMessageSummary> msgs = conv.getMessageSummaries();
        for (ZMessageSummary msg: msgs) {
            msgIds.add(msg.getId());
        }
        //getMessageSummaries are in chronological order; hits are returned latest-first, so should reverse
        Collections.reverse(msgIds);
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> testInputs() throws Exception {
        //calling setUp here instead of using @BeforeClass annotation
        //because the annotation actually causes it to run AFTER the @Parameters method,
        //and we need results from setUp to properly configure all the tests
        setUp();
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();
        String q;     //the query
        int[] unread; //indexes of messages that are unread in the conversation
        Fetch fetchLastMsg          = new Fetch(msgIds.get(3));
        Fetch fetchFirstAndThirdMsg = new Fetch(msgIds.get(0)+","+msgIds.get(2));

        //check a query that matches messages 2 and 3 (0-indexed)
        q = "dungeons or mountains";
        //first, set all messages to unread
        unread = new int[]{0,1,2,3};
        addTestCase(testCases, q, Fetch.none,              unread, new int[]{});
        addTestCase(testCases, q, Fetch.all,               unread, new int[]{0,1,2,3});
        addTestCase(testCases, q, Fetch.hits,              unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.first,             unread, new int[]{2});
        addTestCase(testCases, q, Fetch.unread,            unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.u1,                unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.first_msg,         unread, new int[]{0});
        addTestCase(testCases, q, Fetch.hits_or_first_msg, unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.u_or_first_msg,    unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.u1_or_first_msg,   unread, new int[]{2,3});
        addTestCase(testCases, q, fetchLastMsg,            unread, new int[]{3});
        addTestCase(testCases, q, fetchFirstAndThirdMsg,   unread, new int[]{0,2});

        //now test when only one of the matching hits is unread
        unread = new int[]{3};
        addTestCase(testCases, q, Fetch.none,              unread, new int[]{});
        addTestCase(testCases, q, Fetch.all,               unread, new int[]{0,1,2,3});
        addTestCase(testCases, q, Fetch.hits,              unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.first,             unread, new int[]{2});
        addTestCase(testCases, q, Fetch.unread,            unread, new int[]{3});
        addTestCase(testCases, q, Fetch.u1,                unread, new int[]{3});
        addTestCase(testCases, q, Fetch.first_msg,         unread, new int[]{0});
        addTestCase(testCases, q, Fetch.hits_or_first_msg, unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.u_or_first_msg,    unread, new int[]{3});
        addTestCase(testCases, q, Fetch.u1_or_first_msg,   unread, new int[]{3});
        addTestCase(testCases, q, fetchLastMsg,            unread, new int[]{3});
        addTestCase(testCases, q, fetchFirstAndThirdMsg,   unread, new int[]{0,2});

        //now test when all messages are read
        unread = new int[]{};
        addTestCase(testCases, q, Fetch.none,              unread, new int[]{});
        addTestCase(testCases, q, Fetch.all,               unread, new int[]{0,1,2,3});
        addTestCase(testCases, q, Fetch.hits,              unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.first,             unread, new int[]{2});
        addTestCase(testCases, q, Fetch.unread,            unread, new int[]{});
        addTestCase(testCases, q, Fetch.u1,                unread, new int[]{2});
        addTestCase(testCases, q, Fetch.first_msg,         unread, new int[]{0});
        addTestCase(testCases, q, Fetch.hits_or_first_msg, unread, new int[]{2,3});
        addTestCase(testCases, q, Fetch.u_or_first_msg,    unread, new int[]{0});
        addTestCase(testCases, q, Fetch.u1_or_first_msg,   unread, new int[]{0,2});
        addTestCase(testCases, q, fetchLastMsg,            unread, new int[]{3});
        addTestCase(testCases, q, fetchFirstAndThirdMsg,   unread, new int[]{0,2});

        //now check a query that doesn't match any messages in the conversation
        q = "thiswontmatch";
        unread = new int[]{};
        addTestCase(testCases, q, Fetch.none,              unread, new int[]{});
        addTestCase(testCases, q, Fetch.all,               unread, new int[]{0,1,2,3});
        addTestCase(testCases, q, Fetch.hits,              unread, new int[]{});
        addTestCase(testCases, q, Fetch.first,             unread, new int[]{});
        addTestCase(testCases, q, Fetch.unread,            unread, new int[]{});
        addTestCase(testCases, q, Fetch.u1,                unread, new int[]{});
        addTestCase(testCases, q, Fetch.first_msg,         unread, new int[]{0});
        addTestCase(testCases, q, Fetch.hits_or_first_msg, unread, new int[]{0});
        addTestCase(testCases, q, Fetch.u_or_first_msg,    unread, new int[]{0});
        addTestCase(testCases, q, Fetch.u1_or_first_msg,   unread, new int[]{0});
        addTestCase(testCases, q, fetchLastMsg,            unread, new int[]{3});
        addTestCase(testCases, q, fetchFirstAndThirdMsg,   unread, new int[]{0,2});

        //test when there are no matches, but some of the messages are unread.
        //this should be the same as when there are no unread messages
        unread = new int[]{2,3};
        addTestCase(testCases, q, Fetch.none,              unread, new int[]{});
        addTestCase(testCases, q, Fetch.all,               unread, new int[]{0,1,2,3});
        addTestCase(testCases, q, Fetch.hits,              unread, new int[]{});
        addTestCase(testCases, q, Fetch.first,             unread, new int[]{});
        addTestCase(testCases, q, Fetch.unread,            unread, new int[]{});
        addTestCase(testCases, q, Fetch.u1,                unread, new int[]{});
        addTestCase(testCases, q, Fetch.first_msg,         unread, new int[]{0});
        addTestCase(testCases, q, Fetch.hits_or_first_msg, unread, new int[]{0});
        addTestCase(testCases, q, Fetch.u_or_first_msg,    unread, new int[]{0});
        addTestCase(testCases, q, Fetch.u1_or_first_msg,   unread, new int[]{0});
        addTestCase(testCases, q, fetchLastMsg,            unread, new int[]{3});
        addTestCase(testCases, q, fetchFirstAndThirdMsg,   unread, new int[]{0,2});

        //test values with "!" when the first message is also a hit
        q = "enchanted or mountains"; //matches messages 0 and 3
        //all messages read
        unread = new int[]{};
        addTestCase(testCases, q, Fetch.first_msg,         unread, new int[]{0});
        addTestCase(testCases, q, Fetch.hits_or_first_msg, unread, new int[]{0,3});
        addTestCase(testCases, q, Fetch.u_or_first_msg,    unread, new int[]{0});
        addTestCase(testCases, q, Fetch.u1_or_first_msg,   unread, new int[]{0});

        //all messages unread
        unread = new int[]{0,1,2,3};
        addTestCase(testCases, q, Fetch.first_msg,         unread, new int[]{0});
        addTestCase(testCases, q, Fetch.hits_or_first_msg, unread, new int[]{0,3});
        addTestCase(testCases, q, Fetch.u_or_first_msg,    unread, new int[]{0,3});
        addTestCase(testCases, q, Fetch.u1_or_first_msg,   unread, new int[]{0,3});

        //one of the hits is unread
        unread = new int[]{2,3};
        addTestCase(testCases, q, Fetch.first_msg,         unread, new int[]{0});
        addTestCase(testCases, q, Fetch.hits_or_first_msg, unread, new int[]{0,3});
        addTestCase(testCases, q, Fetch.u_or_first_msg,    unread, new int[]{3});
        addTestCase(testCases, q, Fetch.u1_or_first_msg,   unread, new int[]{3});

        return testCases;
    }

    private static void addTestCase(ArrayList<Object[]> testCases, String query, Fetch fetch, int[] unread, int[] expected){
        Object[] args = new Object[]{query,fetch,unread,expected};
        testCases.add(args);
    }

    private static void setupConversation() throws Exception{
        ZOutgoingMessage msg;
        ZOutgoingMessage reply;
        msg = TestUtil.getOutgoingMessage(REMOTE_USER_NAME, subject, "far over the misty mountains cold",null);
        ZSendMessageResponse resp = mbox.sendMessage(msg,null,false);
        Thread.sleep(1000);
        String remoteMsgId = TestUtil.getMessage(remote_mbox, subject).getId();
        reply = TestUtil.getOutgoingMessage(USER_NAME, subject , "to dungeons deep and caverns old", null);
        reply.setOriginalMessageId(remoteMsgId);
        reply.setReplyType("r");
        remote_mbox.sendMessage(reply, null,false);
        Thread.sleep(1000);
        reply = TestUtil.getOutgoingMessage(USER_NAME, subject , "we must away ere break of day", null);
        reply.setOriginalMessageId(remoteMsgId);
        reply.setReplyType("r");
        remote_mbox.sendMessage(reply, null, false);
        Thread.sleep(1000);
        reply = TestUtil.getOutgoingMessage(USER_NAME, subject , "to seek the pale enchanted gold", null);
        reply.setOriginalMessageId(remoteMsgId);
        reply.setReplyType("r");
        remote_mbox.sendMessage(reply, null, false);
        Thread.sleep(1000);
        convId = mbox.getMessageById(resp.getId()).getConversationId();
    }

    @Test
    public void searchConversation()
    throws Exception {
        ZimbraLog.search.debug("testing query '%s' with fetch='%s', unread=%s (expecting '%s')",
                query, fetch, Arrays.toString(unread), Arrays.toString(expected));
        markMessagesUnreadByIndex(unread);
        ZSearchParams params = new ZSearchParams(query);
        params.setFetch(fetch);
        ZSearchResult result = mbox.searchConversation(convId,params);
        List<ZSearchHit> hits = result.getHits();
        List<String> expandedList = new ArrayList<String>();
        for (Integer idx: expected){
            expandedList.add(msgIds.get(idx));
        }
        for (ZSearchHit hit: hits){
            boolean expanded = isExpanded(hit);
            if(expandedList.contains(hit.getId())){
                assertTrue(expanded);
                expandedList.remove(hit.getId());
            }
            else{
                assertFalse(expanded);
            }
        }
        assertEquals(0,expandedList.size());
    }

    private void markAllMessagesRead() throws Exception {
        mbox.markMessageRead(Joiner.on(",").join(msgIds),true);
    }

    private void markMessagesUnreadByIndex(int[] indexes) throws Exception{
        ArrayList<String> ids = new ArrayList<String>();
        for (int i: indexes){
            ids.add(msgIds.get(i));
        }
        String commaSeparatedIds = Joiner.on(",").join(ids);
        markAllMessagesRead();
        if (commaSeparatedIds.length() > 0) {
        mbox.markMessageRead(commaSeparatedIds, false);
        }
        //make sure the messages are marked read/unread correctly
        for (String id: msgIds) {
            ZMessage message = mbox.getMessageById(id);
            boolean unread = message.isUnread();
            if (ids.contains(id)) {assertTrue(unread);}
            else {assertFalse(unread);}
        }
    }

    private boolean isExpanded(ZSearchHit hit){
        //expanded hits include the message object, non-expanded don't
        ZMessage msg = ((ZMessageHit) hit).getMessage();
        boolean expanded = msg != null;
        return expanded;
    }

    @AfterClass
    public static void cleanUp() throws Exception{
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(REMOTE_USER_NAME);
    }
}
