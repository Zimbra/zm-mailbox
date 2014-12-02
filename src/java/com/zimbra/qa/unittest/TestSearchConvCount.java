package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZSendMessageResponse;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapFaultException;

public class TestSearchConvCount {
    private static String USER_NAME = "testSearchConvSize";
    private static String REMOTE_USER_NAME = "testSearchConvSize2";
    private static ZMailbox mbox;
    private static ZMailbox remote_mbox;
    private static String convId;

    @BeforeClass
    public static void setupConversation() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        remote_mbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ZOutgoingMessage msg;
        ZOutgoingMessage reply;
        msg = TestUtil.getOutgoingMessage(REMOTE_USER_NAME, "conversation 1", "far over the misty mountains cold",null);
        ZSendMessageResponse resp = mbox.sendMessage(msg,null,false);
        Thread.sleep(1000);
        String remoteMsgId = TestUtil.getMessage(remote_mbox, "conversation 1").getId();
        reply = TestUtil.getOutgoingMessage(USER_NAME, "conversation 1" , "to dungeons deep and caverns old", null);
        reply.setOriginalMessageId(remoteMsgId);
        reply.setReplyType("r");
        remote_mbox.sendMessage(reply, null,false);
        Thread.sleep(1000);
        reply = TestUtil.getOutgoingMessage(USER_NAME, "conversation 1" , "we must away ere break of day", null);
        reply.setOriginalMessageId(remoteMsgId);
        reply.setReplyType("r");
        remote_mbox.sendMessage(reply, null, false);
        Thread.sleep(1000);
        reply = TestUtil.getOutgoingMessage(USER_NAME, "conversation 1" , "to seek the pale enchanted gold", null);
        reply.setOriginalMessageId(remoteMsgId);
        reply.setReplyType("r");
        remote_mbox.sendMessage(reply, null, false);
        Thread.sleep(1000);
        convId = mbox.getMessageById(resp.getId()).getConversationId();
    }

    @Test
    public void test() throws ServiceException {
        ZSearchResult res = mbox.search(new ZSearchParams("conv-count:4"));
        assertEquals(1, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-count:1"));
        assertEquals(0, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-count:>1"));
        assertEquals(1, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-count:>=1"));
        assertEquals(1, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-count:5"));
        assertEquals(0, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-count:<5"));
        assertEquals(1, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-count:<=5"));
        assertEquals(1, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-minm:5"));
        assertEquals(0, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-maxm:5"));
        assertEquals(1, res.getHits().size());
        try {
            res = mbox.search(new ZSearchParams("conv-count:abc"));
        } catch (SoapFaultException e) {
            assertEquals("Couldn't parse query: conv-count:abc", e.getMessage());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
        TestUtil.deleteAccount(REMOTE_USER_NAME);
    }
}
