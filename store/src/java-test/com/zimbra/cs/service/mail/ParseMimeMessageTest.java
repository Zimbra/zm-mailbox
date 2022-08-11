/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import org.dom4j.QName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.zmime.ZMimeUtility;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ParseMimeMessage}.
 *
 * @author ysasaki
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ZimbraSoapContext.class})
public final class ParseMimeMessageTest {
    
    
    ZimbraSoapContext zsc = PowerMockito.mock(ZimbraSoapContext.class);
    
    Provisioning prov = Provisioning.getInstance();

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        Account account = prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        AuthToken authToken = new ZimbraAuthToken(account);
        zsc = getMockSoapContext();
        Whitebox.setInternalState(zsc, AuthToken.class, authToken);
    }

    static ZimbraSoapContext getMockSoapContext() throws ServiceException {
        ZimbraSoapContext parent = new ZimbraSoapContext((Element) null, (QName) null, (DocumentHandler) null,
                Collections.<String, Object>emptyMap(), SoapProtocol.SoapJS);
        return new ZimbraSoapContext(parent, MockProvisioning.DEFAULT_ACCOUNT_ID, null);
    }

    @Test
    public void parseMimeMsgSoap() throws Exception {
        Element el = new Element.JSONElement(MailConstants.E_MSG);
        Element elParent = new Element.JSONElement(MailConstants.E_MSG);
        elParent.addUniqueElement(el);
        el.addAttribute(MailConstants.E_SUBJECT, "dinner appt");
        el.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, "foo bar");
        el.addElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");

        Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
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
    public void customMimeHeader() throws Exception {
        Element el = new Element.JSONElement(MailConstants.E_MSG);
        Element elParent = new Element.JSONElement(MailConstants.E_MSG);
        elParent.addUniqueElement(el);
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
        Element elParent = new Element.JSONElement(MailConstants.E_MSG);
        elParent.addUniqueElement(el);
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
        Element elParent = new Element.JSONElement(MailConstants.E_MSG);
        elParent.addUniqueElement(el);
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
        Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        OperationContext octxt = new OperationContext(acct);
        Document doc = mbox.createDocument(octxt, Mailbox.ID_FOLDER_BRIEFCASE, "testdoc",
                MimeConstants.CT_APPLICATION_ZIMBRA_DOC, "author", "description",
                new ByteArrayInputStream("test123".getBytes()));

        Element el = new Element.JSONElement(MailConstants.E_MSG);
        Element elParent = new Element.JSONElement(MailConstants.E_MSG);
        elParent.addUniqueElement(el);
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
        Element elParent = new Element.JSONElement(MailConstants.E_MSG);
        elParent.addUniqueElement(el);
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
