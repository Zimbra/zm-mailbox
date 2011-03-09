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

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.MockMimeTypeInfo;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;
import com.zimbra.cs.service.mail.ParseMimeMessage;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Unit test for {@link ParseMimeMessage}.
 *
 * @author ysasaki
 */
public final class ParseMimeMessageTest {

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();

        MockMimeTypeInfo mime = new MockMimeTypeInfo();
        mime = new MockMimeTypeInfo();
        mime.setHandlerClass(UnknownTypeHandler.class.getName());
        prov.addMimeType("all", mime);

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "0-0-0");
        attrs.put(Provisioning.A_zimbraMailHost, "localhost");
        prov.createAccount("test@zimbra.com", "secret", attrs);
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();

        LC.zimbra_class_store.setDefault(MockStoreManager.class.getName());
        StoreManager.getInstance().startup();
    }

    @Before
    public void setUp() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
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

        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, "test@zimbra.com");
        ZimbraSoapContext zsc = new ZimbraSoapContext(new ZimbraSoapContext(null,
                Collections.<String, Object>emptyMap(), SoapProtocol.SoapJS), "0-0-0", null);
        OperationContext octxt = new OperationContext(acct);

        MimeMessage mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null,
                new ParseMimeMessage.MimeMessageData());
        Assert.assertEquals("text/plain; charset=utf-8", mm.getContentType());
        Assert.assertEquals("dinner appt", mm.getSubject());
        Assert.assertEquals("rcpt@zimbra.com", mm.getHeader("To", ","));
        Assert.assertEquals("7bit", mm.getHeader("Content-Transfer-Encoding", ","));
        Assert.assertEquals("foo bar", mm.getContent());
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

        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, "test@zimbra.com");
        ZimbraSoapContext zsc = new ZimbraSoapContext(new ZimbraSoapContext(null,
                Collections.<String, Object>emptyMap(), SoapProtocol.SoapJS), "0-0-0", null);
        OperationContext octxt = new OperationContext(acct);

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
        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId("0-0-0");
        ZimbraSoapContext zsc = new ZimbraSoapContext(new ZimbraSoapContext(null,
                Collections.<String, Object>emptyMap(), SoapProtocol.SoapJS), "0-0-0", null);
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

        MimeMessage mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null,
                new ParseMimeMessage.MimeMessageData());
        MimeMultipart mmp = (MimeMultipart) mm.getContent();
        MimeBodyPart part = (MimeBodyPart) mmp.getBodyPart(1);
        Assert.assertEquals(MimeConstants.CT_APPLICATION_PDF, new ContentType(part.getContentType()).getContentType());
    }

    @Test
    public void attachZimbraDocument() throws Exception {
        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId("0-0-0");
        ZimbraSoapContext zsc = new ZimbraSoapContext(new ZimbraSoapContext(null,
                Collections.<String, Object>emptyMap(), SoapProtocol.SoapJS), "0-0-0", null);
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

        MimeMessage mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, null, el, null,
                new ParseMimeMessage.MimeMessageData());
        MimeMultipart mmp = (MimeMultipart) mm.getContent();
        MimeBodyPart part = (MimeBodyPart) mmp.getBodyPart(1);
        Assert.assertEquals(MimeConstants.CT_TEXT_HTML, new ContentType(part.getContentType()).getContentType());
    }

}
