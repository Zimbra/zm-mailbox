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
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;

public class SendMsgTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new MailboxManager() {
            @Override
            protected Mailbox instantiateMailbox(MailboxData data) {
                return new Mailbox(data) {
                    @Override
                    public MailSender getMailSender() {
                        return new MailSender() {
                            @Override
                            protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm, Collection<RollbackData> rollbacks)
                            throws SafeMessagingException, IOException {
                                try {
                                    Address[] rcptAddresses = getRecipients(mm);
                                    if (rcptAddresses == null || rcptAddresses.length == 0)
                                        throw new SendFailedException("No recipient addresses");
                                    return Arrays.asList(rcptAddresses);
                                } catch (MessagingException e) {
                                    throw new SafeMessagingException(e);
                                }
                            }
                        };
                    }
                };
            }
        });
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void deleteDraft() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // first, add draft message
        MimeMessage mm = new MimeMessage(JMSession.getSmtpSession(acct));
        mm.setText("foo");
        ParsedMessage pm = new ParsedMessage(mm, false);
        int draftId = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT).getId();

        // then send a message referencing the draft
        Element request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        Element m = request.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_DRAFT_ID, draftId).addAttribute(MailConstants.E_SUBJECT, "dinner appt");
        m.addUniqueElement(MailConstants.E_MIMEPART).addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain").addAttribute(MailConstants.E_CONTENT, "foo bar");
        m.addElement(MailConstants.E_EMAIL).addAttribute(MailConstants.A_ADDRESS_TYPE, ToXML.EmailType.TO.toString()).addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");
        new SendMsg().handle(request, ServiceTestUtil.getRequestContext(acct));

        // finally, verify that the draft is gone
        try {
            mbox.getMessageById(null, draftId);
            Assert.fail("draft message not deleted");
        } catch (NoSuchItemException nsie) {
        }
    }

    @Test
    public void sendFromDraft() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // first, add draft message
        MimeMessage mm = new MimeMessage(Session.getInstance(new Properties()));
        mm.setRecipients(RecipientType.TO, "rcpt@zimbra.com");
        mm.saveChanges();
        ParsedMessage pm = new ParsedMessage(mm, false);
        int draftId = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT).getId();

        // then send a message referencing the draft
        Element request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        request.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_DRAFT_ID, draftId).addAttribute(MailConstants.A_SEND_FROM_DRAFT, true);
        Element response = new SendMsg().handle(request, ServiceTestUtil.getRequestContext(acct));

        // make sure sent message exists
        int sentId = (int) response.getElement(MailConstants.E_MSG).getAttributeLong(MailConstants.A_ID);
        Message sent = mbox.getMessageById(null, sentId);
        Assert.assertEquals(pm.getRecipients(), sent.getRecipients());

        // finally, verify that the draft is gone
        try {
            mbox.getMessageById(null, draftId);
            Assert.fail("draft message not deleted");
        } catch (NoSuchItemException nsie) {
        }
    }
}
