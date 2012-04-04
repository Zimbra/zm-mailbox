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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.zmime.ZMimeUtility;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Unit test for {@link ParseMimeMessage}.
 *
 * @author ysasaki
 */
public final class ParseMimeMessageTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    static ZimbraSoapContext getMockSoapContext() throws ServiceException {
        ZimbraSoapContext parent = new ZimbraSoapContext(null, Collections.<String, Object>emptyMap(), SoapProtocol.SoapJS);
        return new ZimbraSoapContext(parent, MockProvisioning.DEFAULT_ACCOUNT_ID, null);
    }

    @Test
    public void parseMimeMsgSoap() throws Exception {
        Element el = new Element.JSONElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, "dinner appt");
        el.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, "foo bar");
        el.addElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");

        Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        OperationContext octxt = new OperationContext(acct);
        ZimbraSoapContext zsc = getMockSoapContext();

        MimeMessage mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null,
                new ParseMimeMessage.MimeMessageData());
        Assert.assertEquals("text/plain; charset=utf-8", mm.getContentType());
        Assert.assertEquals("dinner appt", mm.getSubject());
        Assert.assertEquals("rcpt@zimbra.com", mm.getHeader("To", ","));
        Assert.assertEquals("7bit", mm.getHeader("Content-Transfer-Encoding", ","));
        Assert.assertEquals("foo bar", mm.getContent());
    }

    @Test
    public void customMimeHeader() throws Exception {
        Element el = new Element.JSONElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, "subject");
        el.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, "body");
        el.addElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");
        el.addElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, "X-Zimbra-Test")
            .setText("custom");
        el.addElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, "X-Zimbra-Test")
            .setText("\u30ab\u30b9\u30bf\u30e0");

        Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        OperationContext octxt = new OperationContext(acct);
        ZimbraSoapContext zsc = getMockSoapContext();

        MimeMessage mm;
        try {
            mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null, new ParseMimeMessage.MimeMessageData());
            Assert.fail();
        } catch (ServiceException expected) {
            Assert.assertEquals("invalid request: header 'X-Zimbra-Test' not allowed", expected.getMessage());
        }

        Provisioning.getInstance().getConfig().setCustomMimeHeaderNameAllowed(new String[] {"X-Zimbra-Test"});
        mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null, new ParseMimeMessage.MimeMessageData());
        Assert.assertEquals("custom, =?utf-8?B?44Kr44K544K/44Og?=", mm.getHeader("X-Zimbra-Test", ", "));
    }

    @Test
    public void attachedMessage() throws Exception {
        Element el = new Element.JSONElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, "attach message");
        el.addElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");
        Element mp = el.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "multipart/mixed;");
        mp.addElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, "This is the outer message.");
        mp.addElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "message/rfc822")
            .addAttribute(MailConstants.E_CONTENT,
                    "From: inner-sender@zimbra.com\r\n" +
                    "To: inner-rcpt@zimbra.com\r\n" +
                    "Subject: inner-message\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Transfer-Encoding: 7bit\r\n" +
                    "MIME-Version: 1.0\r\n\r\n" +
                    "This is the inner message.");

        Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        OperationContext octxt = new OperationContext(acct);
        ZimbraSoapContext zsc = getMockSoapContext();

        MimeMessage mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null,
                new ParseMimeMessage.MimeMessageData());
        Assert.assertTrue(mm.getContentType().startsWith("multipart/mixed;"));
        Assert.assertEquals("attach message", mm.getSubject());
        Assert.assertEquals("rcpt@zimbra.com", mm.getHeader("To", ","));
        MimeMultipart mmp = (MimeMultipart) mm.getContent();
        Assert.assertEquals(2, mmp.getCount());
        Assert.assertTrue(mmp.getContentType().startsWith("multipart/mixed;"));

        MimeBodyPart part = (MimeBodyPart) mmp.getBodyPart(0);
        Assert.assertEquals("text/plain; charset=utf-8", part.getContentType());
        Assert.assertEquals("7bit", part.getHeader("Content-Transfer-Encoding", ","));
        Assert.assertEquals("This is the outer message.", part.getContent());

        part = (MimeBodyPart) mmp.getBodyPart(1);
        Assert.assertEquals("message/rfc822; charset=utf-8", part.getContentType());
        MimeMessage msg = (MimeMessage) part.getContent();
        Assert.assertEquals("text/plain", msg.getContentType());
        Assert.assertEquals("inner-message", msg.getSubject());
        Assert.assertEquals("This is the inner message.", msg.getContent());
    }

    @Test
    public void attachPdfDocument() throws Exception {
        Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        OperationContext octxt = new OperationContext(acct);
        Document doc = mbox.createDocument(octxt, Mailbox.ID_FOLDER_BRIEFCASE, "testdoc",
                MimeConstants.CT_APPLICATION_PDF, "author", "description",
                new ByteArrayInputStream("test123".getBytes()));

        Element el = new Element.JSONElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, "attach message");
        el.addElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");
        el.addElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, "This is the content.");
        el.addElement(MailConstants.E_ATTACH)
            .addElement(MailConstants.E_DOC)
            .addAttribute(MailConstants.A_ID, doc.getId());

        ZimbraSoapContext zsc = getMockSoapContext();
        MimeMessage mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null,
                new ParseMimeMessage.MimeMessageData());
        MimeMultipart mmp = (MimeMultipart) mm.getContent();
        MimeBodyPart part = (MimeBodyPart) mmp.getBodyPart(1);
        Assert.assertEquals(MimeConstants.CT_APPLICATION_PDF, new ContentType(part.getContentType()).getContentType());
    }

    @Test
    public void attachZimbraDocument() throws Exception {
        Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        OperationContext octxt = new OperationContext(acct);
        Document doc = mbox.createDocument(octxt, Mailbox.ID_FOLDER_BRIEFCASE, "testdoc",
                MimeConstants.CT_APPLICATION_ZIMBRA_DOC, "author", "description",
                new ByteArrayInputStream("test123".getBytes()));

        Element el = new Element.JSONElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, "attach message");
        el.addElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");
        el.addElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, "This is the content.");
        el.addElement(MailConstants.E_ATTACH)
            .addElement(MailConstants.E_DOC)
            .addAttribute(MailConstants.A_ID, doc.getId());

        ZimbraSoapContext zsc = getMockSoapContext();
        MimeMessage mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null,
                new ParseMimeMessage.MimeMessageData());
        MimeMultipart mmp = (MimeMultipart) mm.getContent();
        MimeBodyPart part = (MimeBodyPart) mmp.getBodyPart(1);
        Assert.assertEquals(MimeConstants.CT_TEXT_HTML, new ContentType(part.getContentType()).getContentType());
    }

    private ByteArrayInputStream randomContent(String prefix, int length) {
        ZMimeUtility.ByteBuilder bb = new ZMimeUtility.ByteBuilder();
        Random rnd = new Random();
        bb.append(prefix).append("\n");
        for (int i = prefix.length() + 2; i < length; i++) {
            int r = rnd.nextInt(55);
            if (r < 26) {
                bb.append((char) ('A' + r));
            } else if (r < 52) {
                bb.append((char) ('a' + r));
            } else {
                bb.append(' ');
            }
        }
        return new ByteArrayInputStream(bb.toByteArray());
    }

    private String firstLine(MimePart part) throws IOException, MessagingException {
        return new BufferedReader(new InputStreamReader(part.getInputStream())).readLine();
    }

    @Test
    public void staleReference() throws Exception {
        Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // first, create the original draft
        OperationContext octxt = new OperationContext(acct);
        Document doc = mbox.createDocument(octxt, Mailbox.ID_FOLDER_BRIEFCASE, "testdoc",
                MimeConstants.CT_TEXT_PLAIN, null, null, randomContent("test1", 8192));
        Document doc2 = mbox.createDocument(octxt, Mailbox.ID_FOLDER_BRIEFCASE, "testdoc2",
                MimeConstants.CT_TEXT_PLAIN, null, null, randomContent("test2", 8192));

        Element el = new Element.JSONElement(MailConstants.E_MSG), attach;
        el.addAttribute(MailConstants.E_SUBJECT, "has attachment");
        el.addElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");
        el.addElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, "This is the content.");
        attach = el.addElement(MailConstants.E_ATTACH);
        attach.addElement(MailConstants.E_DOC)
            .addAttribute(MailConstants.A_ID, doc.getId());
        attach.addElement(MailConstants.E_DOC)
            .addAttribute(MailConstants.A_ID, doc2.getId());

        ZimbraSoapContext zsc = getMockSoapContext();
        MimeMessage mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null, new ParseMimeMessage.MimeMessageData());
        Message draft = mbox.saveDraft(octxt, new ParsedMessage(mm, false), -1);


        // then, create a new draft that references one of the original draft's attachments
        attach.detach();
        (attach = el.addElement(MailConstants.E_ATTACH))
            .addElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_MESSAGE_ID, draft.getId())
            .addAttribute(MailConstants.A_PART, "3");
        mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null, new ParseMimeMessage.MimeMessageData());

        // delete the draft itself and then try to save the new draft
        mbox.delete(octxt, draft.getId(), MailItem.Type.MESSAGE);
        Message draft2 = mbox.saveDraft(octxt, new ParsedMessage(mm, false), -1);

        // check that the attachment's content is present and correct
        MimeMultipart multi = (MimeMultipart) (draft2.getMimeMessage().getContent());
        Assert.assertEquals("2 parts in draft", 2, multi.getCount());
        Assert.assertEquals("attached part content", "test2", firstLine((MimeBodyPart) multi.getBodyPart(1)));
    }
}
