/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.BufferedWriter;
import org.junit.Ignore;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZContentType;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.logger.EventLogHandler;
import com.zimbra.cs.event.logger.EventLogger;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mailbox.Mailbox.MessageCallback.Type;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTest;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MessageCallbackContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.util.JMSession;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class SendMsgTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("rcpt@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except use SMTP to deliver mail
        MailboxManager.setInstance(new DirectInsertionMailboxManager());
    }

    public static class NoDeliveryMailboxManager extends MailboxManager {
        public NoDeliveryMailboxManager() throws ServiceException {
            super();
        }

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
                                if (rcptAddresses == null || rcptAddresses.length == 0) {
                                    throw new SendFailedException("No recipient addresses");
                                }
                                return Arrays.asList(rcptAddresses);
                            } catch (MessagingException e) {
                                throw new SafeMessagingException(e);
                            }
                        }
                    };
                }
            };
        }
    }

    public static class DirectInsertionMailboxManager extends MailboxManager {
        public DirectInsertionMailboxManager() throws ServiceException {
            super(true);
        }

        @Override
        protected Mailbox instantiateMailbox(MailboxData data) {
            return new Mailbox(data) {
                @Override
                public MailSender getMailSender() {
                    return new MailSender() {
                        @Override
                        protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm, Collection<RollbackData> rollbacks) {
                            List<Address> successes = new ArrayList<Address>();
                            Address[] addresses;
                            try {
                                addresses = getRecipients(mm);
                            } catch (Exception e) {
                                addresses = new Address[0];
                            }
                            DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
                            for (Address addr : addresses) {
                                try {
                                    Account acct = Provisioning.getInstance().getAccountByName(((InternetAddress) addr).getAddress());
                                    if (acct != null) {
                                        Mailbox target = MailboxManager.getInstance().getMailboxByAccount(acct);
                                        MessageCallbackContext ctxt = new MessageCallbackContext(Type.sent);
                                        dopt.setCallbackContext(ctxt);
                                        target.addMessage(null, new ParsedMessage(mm, false), dopt, null);
                                        successes.add(addr);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace(System.out);
                                }
                            }
                            if (successes.isEmpty() && !isSendPartial()) {
                                for (RollbackData rdata : rollbacks) {
                                    if (rdata != null) {
                                        rdata.rollback();
                                    }
                                }
                            }
                            return successes;
                        }
                    };
                }
            };
        }
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
    public void testSendFromDraft() throws Exception {
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

    @Test
    public void sendUpload() throws Exception {
        Assert.assertTrue("using Zimbra MIME parser", ZMimeMessage.usingZimbraParser());

        Account rcpt = Provisioning.getInstance().getAccountByName("rcpt@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(rcpt);

        // Configure test timezones.ics file.
        File tzFile = File.createTempFile("timezones-", ".ics");
        BufferedWriter writer= new BufferedWriter(new FileWriter(tzFile));
        writer.write("BEGIN:VCALENDAR\r\nEND:VCALENDAR");
        writer.close();
        LC.timezone_file.setDefault(tzFile.getAbsolutePath());

        InputStream is = getClass().getResourceAsStream("bug-69862-invite.txt");
        ParsedMessage pm = new ParsedMessage(ByteUtil.getContent(is, -1), false);
        mbox.addMessage(null, pm, MailboxTest.STANDARD_DELIVERY_OPTIONS, null);

        is = getClass().getResourceAsStream("bug-69862-reply.txt");
        FileUploadServlet.Upload up = FileUploadServlet.saveUpload(is, "lslib32.bin", "message/rfc822", rcpt.getId());

        Element request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        request.addAttribute(MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP, true).addAttribute(MailConstants.A_NO_SAVE_TO_SENT, true);
        request.addUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ATTACHMENT_ID, up.getId());
        new SendMsg().handle(request, ServiceTestUtil.getRequestContext(rcpt));

        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        Message msg = (Message) mbox.getItemList(null, MailItem.Type.MESSAGE).get(0);
        MimeMessage mm = msg.getMimeMessage();
        Assert.assertEquals("correct top-level MIME type", "multipart/alternative", new ZContentType(mm.getContentType()).getBaseType());
    }

    @Test
    public void testSendMailEventLoggerTest() throws Exception {

        EventLogHandler.Factory mockFactory = Mockito.mock(EventLogHandler.Factory.class);
        EventLogHandler mockHandler = Mockito.mock(EventLogHandler.class);
        Mockito.doReturn(mockHandler).when(mockFactory).createHandler(Mockito.anyString());

        EventLogger.registerHandlerFactory("testhandler", mockFactory);

        EventLogger.ConfigProvider mockConfigProvider = Mockito.mock(EventLogger.ConfigProvider.class);

        Multimap<String, String> mockConfigMap = ArrayListMultimap.create();
        mockConfigMap.put("testhandler", "");
        Mockito.doReturn(mockConfigMap.asMap()).when(mockConfigProvider).getHandlerConfig();

        Mockito.doReturn(1).when(mockConfigProvider).getNumThreads(); //ensures sequential event processing

        Mockito.doReturn(true).when(mockConfigProvider).isEnabled();

        EventLogger.getEventLogger(mockConfigProvider).startupEventNotifierExecutor();

        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // first, add draft message
        MimeMessage mm = new MimeMessage(Session.getInstance(new Properties()));
        mm.setRecipients(RecipientType.TO, "rcpt1@zimbra.com,rcpt2@zimbra.com");
        mm.saveChanges();
        ParsedMessage pm = new ParsedMessage(mm, false);
        int draftId = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT).getId();

        // then send a message referencing the draft
        Element request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        request.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_DRAFT_ID, draftId).addAttribute(MailConstants.A_SEND_FROM_DRAFT, true);
        Element response = new SendMsg().handle(request, ServiceTestUtil.getRequestContext(acct));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(mockHandler, Mockito.times(2)).log(captor.capture());

        Assert.assertNotNull(captor.getAllValues());
        Assert.assertEquals(2, captor.getAllValues().size());

        Assert.assertEquals(Event.EventType.SENT, captor.getAllValues().get(0).getEventType());
        Assert.assertEquals(Event.EventType.SENT, captor.getAllValues().get(1).getEventType());

        Assert.assertEquals("test@zimbra.com", captor.getAllValues().get(0).getContextField(Event.EventContextField.SENDER));
        Assert.assertEquals("test@zimbra.com", captor.getAllValues().get(1).getContextField(Event.EventContextField.SENDER));

        Assert.assertEquals("rcpt1@zimbra.com", captor.getAllValues().get(0).getContextField(Event.EventContextField.RECEIVER));
        Assert.assertEquals("rcpt2@zimbra.com", captor.getAllValues().get(1).getContextField(Event.EventContextField.RECEIVER));
    }

    @After
    public void tearDown() {
        EventLogger.getEventLogger().clearQueue();
        EventLogger.unregisterAllHandlerFactories();
    }
}
