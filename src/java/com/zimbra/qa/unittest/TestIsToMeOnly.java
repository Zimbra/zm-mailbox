package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Fetch;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.MessagePart;
import com.zimbra.client.ZMailbox.ZSendMessageResponse;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessageHit;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;

public class TestIsToMeOnly extends TestCase {
    private static final String SENDER = "tomeonlySender";
    private static final String RECIPIENT = "tomeonlyRecipient";
    private static final String RECIPIENT_OTHER = "tomeonlyRecipientOther";
    private static final String SUBJECT = "email with one recipient";
    private static final String SUBJECT_OTHER = "email with multiple recipients";
    private static ZMailbox sender_mbox;
    private static ZMailbox rcpt_mbox;

    @Override
    @Before
    public void setUp() throws Exception {
        TestUtil.createAccount(SENDER);
        TestUtil.createAccount(RECIPIENT);
        TestUtil.createAccount(RECIPIENT_OTHER);
        sender_mbox = TestUtil.getZMailbox(SENDER);
        rcpt_mbox = TestUtil.getZMailbox(RECIPIENT);

        ZEmailAddress recipEmailAddress = new ZEmailAddress(String.format("%s@%s", RECIPIENT, AccountTestUtil.getDomain()),
                null, null, ZEmailAddress.EMAIL_TYPE_TO);
        ZEmailAddress otherRecipEmailAddress = new ZEmailAddress(String.format("%s@%s", RECIPIENT_OTHER, AccountTestUtil.getDomain()),
                null, null, ZEmailAddress.EMAIL_TYPE_TO);
        ZOutgoingMessage msg = new ZOutgoingMessage();
        List<ZEmailAddress> addresses = new ArrayList<ZEmailAddress>(1);
        addresses.add(recipEmailAddress);
        msg.setAddresses(addresses);
        msg.setSubject(SUBJECT);
        msg.setMessagePart(new MessagePart("text/plain", ""));
        ZSendMessageResponse resp = sender_mbox.sendMessage(msg, null, false);
        msg = new ZOutgoingMessage();
        addresses = new ArrayList<ZEmailAddress>(2);
        addresses.add(recipEmailAddress);
        addresses.add(otherRecipEmailAddress);
        msg.setAddresses(addresses);
        msg.setSubject(SUBJECT_OTHER);
        msg.setMessagePart(new MessagePart("text/plain", ""));
        resp = sender_mbox.sendMessage(msg, null, false);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(SENDER);
        TestUtil.deleteAccount(RECIPIENT);
        TestUtil.deleteAccount(RECIPIENT_OTHER);
    }

    private void testQuery(String query, int numExpected, String expectedSubject) throws ServiceException {
        ZSearchParams params = new ZSearchParams(query);
        params.setTypes("message");
        params.setFetch(Fetch.all);
        ZSearchResult results = rcpt_mbox.search(params);
        assertEquals(numExpected, results.getHits().size());
        if (numExpected == 1) {
            ZMessageHit msgHit = (ZMessageHit) results.getHits().get(0);
            ZMessage msg = msgHit.getMessage();
            assertEquals(expectedSubject, msg.getSubject());
        }
    }
    @Test
    public void testToMeOnly() throws ServiceException {
        testQuery("is:tome", 2, null); //returns both
        testQuery("is:tomeonly", 1, SUBJECT);
        testQuery("is:tomeonly subject:email", 1, SUBJECT); //combining with text query works
        testQuery("is:tomeonly subject:multiple", 0, null);
        testQuery("-is:tomeonly", 1, SUBJECT_OTHER);
    }
}
