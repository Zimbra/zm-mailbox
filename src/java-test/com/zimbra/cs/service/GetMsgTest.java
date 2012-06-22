package com.zimbra.cs.service;

import java.util.HashMap;
import java.util.List;

import javax.mail.internet.MimeMessage;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.icu.util.Calendar;
import com.zimbra.common.mime.shim.JavaMailMimeMessage;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zmime.ZSharedFileInputStream;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.GetMsg;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.cs.util.JMSession;

public class GetMsgTest {

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

    @Test
    public void testMsgMaxAttr() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MimeMessage message = new JavaMailMimeMessage(JMSession.getSession(), new ZSharedFileInputStream("data/TestMailRaw/1"));
        ParsedMessage pm = new ParsedMessage(message, false);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, pm, dopt, null);

        Element request = new Element.XMLElement(MailConstants.GET_MSG_REQUEST);
        Element action = request.addElement(MailConstants.E_MSG);
        action.addAttribute(MailConstants.A_ID, msg.getId());
        action.addAttribute(MailConstants.A_MAX_INLINED_LENGTH, 10);

        Element response = new GetMsg().handle(request, ServiceTestUtil.getRequestContext(mbox.getAccount())).getElement(MailConstants.E_MSG);
        Assert.assertEquals(response.getElement(MailConstants.E_MIMEPART).getElement(MailConstants.E_CONTENT).getText().length(), 10);
    }

    @Test
    public void testMsgView() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MimeMessage message = new JavaMailMimeMessage(JMSession.getSession(), new ZSharedFileInputStream("data/unittest/email/bug_75163.txt"));
        ParsedMessage pm = new ParsedMessage(message, false);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, pm, dopt, null);

        Element request = new Element.XMLElement(MailConstants.GET_MSG_REQUEST);
        Element action = request.addElement(MailConstants.E_MSG);
        action.addAttribute(MailConstants.A_ID, msg.getId());

        Element response = new GetMsg().handle(request, ServiceTestUtil.getRequestContext(mbox.getAccount())).getElement(MailConstants.E_MSG);
        List<Element> mimeParts = response.getElement(MailConstants.E_MIMEPART).listElements();
        // test plain text view
        for (Element elt : mimeParts) {
            if (elt.getAttribute(MailConstants.A_BODY, null) != null) {
                Assert.assertEquals(elt.getAttribute(MailConstants.A_CONTENT_TYPE), "text/plain");
                break;
            }
        }

        action.addAttribute(MailConstants.A_WANT_HTML, 1);
        response = new GetMsg().handle(request, ServiceTestUtil.getRequestContext(mbox.getAccount())).getElement(MailConstants.E_MSG);
        mimeParts = response.getElement(MailConstants.E_MIMEPART).listElements();
        // test HTML view
        for (Element elt : mimeParts) {
            if (elt.getAttribute(MailConstants.A_BODY, null) != null) {
                Assert.assertEquals(elt.getAttribute(MailConstants.A_CONTENT_TYPE), "text/html");
                break;
            }
        }
    }

    @Test
    public void testMsgHeaderN() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MimeMessage message = new JavaMailMimeMessage(JMSession.getSession(), new ZSharedFileInputStream("data/unittest/email/bug_75163.txt"));
        ParsedMessage pm = new ParsedMessage(message, false);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, pm, dopt, null);

        Element request = new Element.XMLElement(MailConstants.GET_MSG_REQUEST);
        Element action = request.addElement(MailConstants.E_MSG);
        action.addAttribute(MailConstants.A_ID, msg.getId());
        action.addElement(MailConstants.A_HEADER).addAttribute(MailConstants.A_ATTRIBUTE_NAME, "Return-Path");

        Element response = new GetMsg().handle(request, ServiceTestUtil.getRequestContext(mbox.getAccount())).getElement(MailConstants.E_MSG);
        List<Element> headerN = response.listElements(MailConstants.A_HEADER);
        for (Element elt : headerN) {
            Assert.assertEquals(elt.getText(), "foo@example.com");
        }
    }
}
