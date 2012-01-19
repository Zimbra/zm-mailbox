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
package com.zimbra.cs.mailbox;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.ZAttrProvisioning.MailThreadingAlgorithm;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Threader.ThreadIndex;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;

public final class ThreaderTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.getInstance().createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setup() throws Exception {
        MailboxTestUtil.clearData();
    }

    private static final String ROOT_SUBJECT = "sdkljfh sdjhfg kjdshkj iu 8 skfjd";
    private static final String ROOT_MESSAGE_ID = "<sakfuslkdhflskjch@sdkf.example.com>";
    private static final String ROOT_THREAD_TOPIC = ROOT_SUBJECT;
    private static final String ROOT_THREAD_INDEX = Threader.ThreadIndex.newThreadIndex();

    private static final String OTHER_SUBJECT = "kjsdfhg sdf sdgf asa aa sadfkjha 345";
    private static final String OTHER_MESSAGE_ID = "<lsdfkjghkds.afas.sdf@sdkf.example.com>";

    private static final String THIRD_MESSAGE_ID = "<dkjhgf.w98yerg.ksj72@sdkf.example.com>";
    private static final String FOURTH_MESSAGE_ID = "<783246tygirufhmnasdb@sdkf.example.com>";
    private static final String FIFTH_MESSAGE_ID = "<kjsdfg.45wy.setrhye.g@sdkf.example.com>";

    private Account getAccount() throws Exception {
        return Provisioning.getInstance().getAccount("test@zimbra.com");
    }

    public static ParsedMessage getRootMessage() throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "Bob Evans <bob@example.com>");
        mm.setHeader("To", "Jimmy Dean <jdean@example.com>");
        mm.setHeader("Subject", ROOT_SUBJECT);
        mm.setHeader("Message-ID", ROOT_MESSAGE_ID);
        mm.setHeader("Thread-Topic", ROOT_THREAD_TOPIC);
        mm.setHeader("Thread-Index", ROOT_THREAD_INDEX);
        mm.setText("nothing to see here");

        return new ParsedMessage(mm, false);
    }

    static MimeMessage getSecondMessage() throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "Bob Evans <bob@example.com>");
        mm.setHeader("To", "Jimmy Dean <jdean@example.com>");
        mm.setHeader("Message-ID", OTHER_MESSAGE_ID);
        mm.setText("still nothing to see here");
        return mm;
    }

    private void threadMessage(String msg, MailThreadingAlgorithm mode, ParsedMessage pm, Mailbox mbox, List<Integer> expectedMatches) throws Exception {
        mbox.beginTransaction("ThreaderTest", null);
        try {
            getAccount().setMailThreadingAlgorithm(mode);
            Threader threader = new Threader(mbox, pm);
            List<Integer> matches = MailItem.toId(threader.lookupConversation());
            Assert.assertEquals(msg + " (threading: " + mode + ")", expectedMatches, matches);
        } finally {
            mbox.endTransaction(false);
        }
    }

    @Test
    public void unrelated() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        mbox.addMessage(null, getRootMessage(), MailboxTest.STANDARD_DELIVERY_OPTIONS, null);

        // unrelated, not a reply
        MimeMessage mm = getSecondMessage();
        mm.setHeader("Subject", OTHER_SUBJECT);
        ParsedMessage pm = new ParsedMessage(mm, false);

        threadMessage("unrelated", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("unrelated", MailThreadingAlgorithm.subject, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("unrelated", MailThreadingAlgorithm.references, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("unrelated", MailThreadingAlgorithm.subjrefs, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("unrelated", MailThreadingAlgorithm.strict, pm, mbox, Collections.<Integer>emptyList());

        // unrelated, reply to some other message
        mm = getSecondMessage();
        mm.setHeader("Subject", "Re: " + OTHER_SUBJECT);
        mm.setHeader("In-Reply-To", THIRD_MESSAGE_ID);
        mm.setHeader("References", THIRD_MESSAGE_ID);
        pm = new ParsedMessage(mm, false);

        threadMessage("unrelated reply", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("unrelated reply", MailThreadingAlgorithm.subject, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("unrelated reply", MailThreadingAlgorithm.references, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("unrelated reply", MailThreadingAlgorithm.subjrefs, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("unrelated reply", MailThreadingAlgorithm.strict, pm, mbox, Collections.<Integer>emptyList());
    }

    @Test
    public void followup() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Message msg = mbox.addMessage(null, getRootMessage(), MailboxTest.STANDARD_DELIVERY_OPTIONS, null);
        List<Integer> match = Arrays.asList(msg.getConversationId());

        // References and In-Reply-To set
        MimeMessage mm = getSecondMessage();
        mm.setHeader("Subject", "Re: " + ROOT_SUBJECT);
        mm.setHeader("In-Reply-To", ROOT_MESSAGE_ID);
        mm.setHeader("References", ROOT_MESSAGE_ID);
        ParsedMessage pm = new ParsedMessage(mm, false);

        threadMessage("followup", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("followup", MailThreadingAlgorithm.subject, pm, mbox, match);
        threadMessage("followup", MailThreadingAlgorithm.references, pm, mbox, match);
        threadMessage("followup", MailThreadingAlgorithm.subjrefs, pm, mbox, match);
        threadMessage("followup", MailThreadingAlgorithm.strict, pm, mbox, match);

        // only In-Reply-To set
        mm = getSecondMessage();
        mm.setHeader("Subject", "Re: " + ROOT_SUBJECT);
        mm.setHeader("In-Reply-To", ROOT_MESSAGE_ID);
        pm = new ParsedMessage(mm, false);

        threadMessage("followup [irt]", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("followup [irt]", MailThreadingAlgorithm.subject, pm, mbox, match);
        threadMessage("followup [irt]", MailThreadingAlgorithm.references, pm, mbox, match);
        threadMessage("followup [irt]", MailThreadingAlgorithm.subjrefs, pm, mbox, match);
        threadMessage("followup [irt]", MailThreadingAlgorithm.strict, pm, mbox, match);

        // only References set
        mm = getSecondMessage();
        mm.setHeader("Subject", "Re: " + ROOT_SUBJECT);
        mm.setHeader("References", ROOT_MESSAGE_ID);
        pm = new ParsedMessage(mm, false);

        threadMessage("followup [refs]", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("followup [refs]", MailThreadingAlgorithm.subject, pm, mbox, match);
        threadMessage("followup [refs]", MailThreadingAlgorithm.references, pm, mbox, match);
        threadMessage("followup [refs]", MailThreadingAlgorithm.subjrefs, pm, mbox, match);
        threadMessage("followup [refs]", MailThreadingAlgorithm.strict, pm, mbox, match);
    }

    @Test
    public void missingHeaders() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Message msg = mbox.addMessage(null, getRootMessage(), MailboxTest.STANDARD_DELIVERY_OPTIONS, null);
        List<Integer> match = Arrays.asList(msg.getConversationId());

        // reply without any of the threading headers
        MimeMessage mm = getSecondMessage();
        mm.setHeader("Subject", "Re: " + ROOT_SUBJECT);
        ParsedMessage pm = new ParsedMessage(mm, false);

        threadMessage("followup [nohdr]", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("followup [nohdr]", MailThreadingAlgorithm.subject, pm, mbox, match);
        threadMessage("followup [nohdr]", MailThreadingAlgorithm.references, pm, mbox, match);
        threadMessage("followup [nohdr]", MailThreadingAlgorithm.subjrefs, pm, mbox, match);
        threadMessage("followup [nohdr]", MailThreadingAlgorithm.strict, pm, mbox, Collections.<Integer>emptyList());
    }

    @Test
    public void nonreply() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Message msg = mbox.addMessage(null, getRootMessage(), MailboxTest.STANDARD_DELIVERY_OPTIONS, null);
        List<Integer> match = Arrays.asList(msg.getConversationId());

        // not a reply, but matching Subject
        MimeMessage mm = getSecondMessage();
        mm.setHeader("Subject", ROOT_SUBJECT);
        ParsedMessage pm = new ParsedMessage(mm, false);

        threadMessage("matching subject", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("matching subject", MailThreadingAlgorithm.subject, pm, mbox, match);
        threadMessage("matching subject", MailThreadingAlgorithm.references, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("matching subject", MailThreadingAlgorithm.subjrefs, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("matching subject", MailThreadingAlgorithm.strict, pm, mbox, Collections.<Integer>emptyList());
    }

    @Test
    public void changedSubject() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Message msg = mbox.addMessage(null, getRootMessage(), MailboxTest.STANDARD_DELIVERY_OPTIONS, null);
        List<Integer> match = Arrays.asList(msg.getConversationId());

        // reply with different Subject
        MimeMessage mm = getSecondMessage();
        mm.setHeader("Subject", OTHER_SUBJECT);
        mm.setHeader("In-Reply-To", ROOT_MESSAGE_ID);
        mm.setHeader("References", ROOT_MESSAGE_ID);
        ParsedMessage pm = new ParsedMessage(mm, false);

        threadMessage("changed subject", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("changed subject", MailThreadingAlgorithm.subject, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("changed subject", MailThreadingAlgorithm.references, pm, mbox, match);
        threadMessage("changed subject", MailThreadingAlgorithm.subjrefs, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("changed subject", MailThreadingAlgorithm.strict, pm, mbox, match);
    }

    @Test
    public void crossedThread() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Message msg = mbox.addMessage(null, getRootMessage(), MailboxTest.STANDARD_DELIVERY_OPTIONS, null);
        List<Integer> match = Arrays.asList(msg.getConversationId());

        // reply with the same normalized subject, but not the same thread as the original message
        MimeMessage mm = getSecondMessage();
        mm.setHeader("Subject", "Re: " + ROOT_SUBJECT);
        mm.setHeader("In-Reply-To", THIRD_MESSAGE_ID);
        mm.setHeader("References", THIRD_MESSAGE_ID);
        ParsedMessage pm = new ParsedMessage(mm, false);

        threadMessage("crossed threads", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("crossed threads", MailThreadingAlgorithm.subject, pm, mbox, match);
        threadMessage("crossed threads", MailThreadingAlgorithm.references, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("crossed threads", MailThreadingAlgorithm.subjrefs, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("crossed threads", MailThreadingAlgorithm.strict, pm, mbox, Collections.<Integer>emptyList());
    }

    @Test
    public void outlook() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Message msg = mbox.addMessage(null, getRootMessage(), MailboxTest.STANDARD_DELIVERY_OPTIONS, null);
        List<Integer> match = Arrays.asList(msg.getConversationId());

        // reply from Outlook (no In-Reply-To or References header)
        MimeMessage mm = getSecondMessage();
        mm.setHeader("Subject", "Re: " + ROOT_SUBJECT);
        mm.setHeader("Thread-Topic", ROOT_THREAD_TOPIC);
        mm.setHeader("Thread-Index", Threader.ThreadIndex.addChild(Threader.ThreadIndex.parseHeader(ROOT_THREAD_INDEX)));
        ParsedMessage pm = new ParsedMessage(mm, false);

        threadMessage("outlook", MailThreadingAlgorithm.none, pm, mbox, Collections.<Integer>emptyList());
        threadMessage("outlook", MailThreadingAlgorithm.subject, pm, mbox, match);
        threadMessage("outlook", MailThreadingAlgorithm.references, pm, mbox, match);
        threadMessage("outlook", MailThreadingAlgorithm.subjrefs, pm, mbox, match);
        threadMessage("outlook", MailThreadingAlgorithm.strict, pm, mbox, Collections.<Integer>emptyList());
    }

    @Test
    public void threadIndex() throws Exception {
        Assert.assertEquals("new thread index length", 32, ThreadIndex.newThreadIndex().length());

        byte[] oldIndex = new byte[82];
        new Random().nextBytes(oldIndex);
        String newIndex = ThreadIndex.addChild(oldIndex);
        Assert.assertEquals("child index length", 116, newIndex.length());
        byte[] head = new byte[oldIndex.length];
        System.arraycopy(ThreadIndex.parseHeader(newIndex), 0, head, 0, oldIndex.length);
        Assert.assertArrayEquals("preserving old index", oldIndex, head);
    }

    private void checkConversations(Mailbox mbox, int msgidA, int msgidB, boolean match) throws ServiceException {
        Message msgA = mbox.getMessageById(null, msgidA);
        Message msgB = mbox.getMessageById(null, msgidB);
        if (match) {
            Assert.assertTrue("in same conversation", msgA.getConversationId() == msgB.getConversationId());
        } else {
            Assert.assertFalse("in different conversations", msgA.getConversationId() == msgB.getConversationId());
        }
    }

    @Test
    public void redelivery() throws Exception {
        Account acct = getAccount();
        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.references);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // add thread starter
        int msgid1 = mbox.addMessage(null, getRootMessage(), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        // add second message, which should get slotted with the first message
        MimeMessage mm = getSecondMessage();
        mm.setHeader("Subject", "Re: " + ROOT_SUBJECT);
        mm.setHeader("In-Reply-To", ROOT_MESSAGE_ID);
        int msgid2 = mbox.addMessage(null, new ParsedMessage(mm, false), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        checkConversations(mbox, msgid1, msgid2, true);

        // add fourth message
        mm.setHeader("Message-ID", FOURTH_MESSAGE_ID);
        mm.setHeader("In-Reply-To", THIRD_MESSAGE_ID);
        int msgid4 = mbox.addMessage(null, new ParsedMessage(mm, false), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        // add fifth message, which should get slotted with the fourth message
        mm.setHeader("Message-ID", FIFTH_MESSAGE_ID);
        mm.setHeader("In-Reply-To", FOURTH_MESSAGE_ID);
        int msgid5 = mbox.addMessage(null, new ParsedMessage(mm, false), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        checkConversations(mbox, msgid4, msgid5, true);
        checkConversations(mbox, msgid1, msgid5, false);

        // add third message, joining the two conversations
        mm.setHeader("Message-ID", THIRD_MESSAGE_ID);
        mm.setHeader("In-Reply-To", OTHER_MESSAGE_ID);
        ParsedMessage pm = new ParsedMessage(mm, false);
        int msgid3 = mbox.addMessage(null, pm, MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        checkConversations(mbox, msgid3, msgid5, true);
        checkConversations(mbox, msgid3, msgid1, true);

        // redeliver the same message to the mailbox (e.g. two different FILEINTO filters)
        int msgid3A = mbox.addMessage(null, pm, MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        checkConversations(mbox, msgid3A, msgid3, true);
    }
}
