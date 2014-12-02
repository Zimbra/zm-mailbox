package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public class TestSearchConvDate {
    private static String USER_NAME = "testSearchConvDate";
    private static String REMOTE_USER_NAME = "testSearchConvDateRemote";
    private static ZMailbox mbox;
    private static ZMailbox remote_mbox;
    private static long convStart = 1388577600L; // 1/1/2014 at noon GMT
    private static String mId;

    @BeforeClass
    public static void setupConversation() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        remote_mbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        //inject a message with timestamp of Jan 1st 2014, then reply to it at the current time
        String message = "From: " + mbox.getAccountInfo(true).getName() + "\n"
                + "Subject: some conversation\n\n"
                + "First message in conversation";
        mId = mbox.addMessage(String.valueOf(Mailbox.ID_FOLDER_INBOX), null, null, convStart, message, true);
        ZOutgoingMessage reply = TestUtil.getOutgoingMessage(REMOTE_USER_NAME, "some conversation" , "second message in conversation", null);
        reply.setOriginalMessageId(mId);
        reply.setReplyType("r");
        mbox.sendMessage(reply, null,false);
        Thread.sleep(1000);

        /* Inject another message that will not be part of a conversation,
         * with the same timestamp.
         */
        message = "From: " + mbox.getAccountInfo(true).getName() + "\n"
                + "Subject: standalone message\n\n"
                + "No replies here";
        mbox.addMessage(String.valueOf(Mailbox.ID_FOLDER_INBOX), null, null, convStart, message, true);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
        TestUtil.deleteAccount(REMOTE_USER_NAME);
    }

    @Test
    public void testConvRangeQueries() throws ServiceException {
        ZSearchResult res;
        res = mbox.search(new ZSearchParams("conv-start:1/1/2014"));
        assertEquals(2, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-start:1/1/2014 conv-count:1"));
        assertEquals(1, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-start:>12/30/2013"));
        assertEquals(2, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-end:>1/1/2014"));
        assertEquals(1, res.getHits().size()); //standalone message doesn't get returned here
        res = mbox.search(new ZSearchParams("conv-start:>1/1/2014"));
        assertEquals(0, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-start:>=1/1/2014"));
        assertEquals(2, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-start:<1/2/2014"));
        assertEquals(2, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-start:<=1/1/2014"));
        assertEquals(2, res.getHits().size());
        res = mbox.search(new ZSearchParams("conv-end:<1/2/2014"));
        assertEquals(1, res.getHits().size()); //standalone message gets returned here, but not the conv
    }

}
