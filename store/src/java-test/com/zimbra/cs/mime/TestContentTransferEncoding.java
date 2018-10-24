/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.mail.ParseMimeMessage;
import com.zimbra.cs.service.mail.ParseMimeMessage.MimeMessageData;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SendMsgRequest;
import com.zimbra.soap.mail.type.AttachmentsInfo;
import com.zimbra.soap.mail.type.MimePartAttachSpec;
import com.zimbra.soap.mail.type.MimePartInfo;
import com.zimbra.soap.mail.type.MsgToSend;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class TestContentTransferEncoding {

    Mailbox mbox;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        MailboxTestUtil.cleanupIndexStore(
                MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID));
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
    }

    private MimeMessage sendForwardedMessage(SendMsgRequest req, Message origMsg) throws Exception {
        Element reqElt = JaxbUtil.jaxbToElement(req);
        Element msgElt = reqElt.getElement(MailConstants.E_MSG);
        AuthToken at = AuthProvider.getAuthToken(mbox.getAccount());
        ZimbraSoapContext zsc = new ZimbraSoapContext(at, mbox.getAccountId(), SoapProtocol.Soap12, SoapProtocol.Soap12);
        return ParseMimeMessage.parseMimeMsgSoap(zsc, null, mbox, msgElt, null, new MimeMessageData());
    }

    /*
     * Tests that the message from bug 98015 retains the correct Content Transfer Encoding via the mechanism of
     * inferring it from the existing message referenced by the "origid" parameter.
     * This scenario also happens to test the case when a CTE header on a sub-part is inherited from the top-level.
     */
    @Ignore("disabled until bug 98015 is fixed")
    @Test
    public void testBug98015() throws Exception {
        MimeMessage mimeMsg = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(getBug98015MimeString().getBytes()));
        ParsedMessage pm = new ParsedMessage(mimeMsg, true);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, pm, dopt, null);
        MsgToSend msgToSend = new MsgToSend();
        msgToSend.setOrigId(String.valueOf(msg.getId()));
        msgToSend.setReplyType("w");
        msgToSend.setSubject("Fwd: QP bug");
        msgToSend.setMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", "\n\n----- Forwarded Message -----\nSubject: QP Bug\n\n\\ö/\nid=577281"));
        AttachmentsInfo attach = new AttachmentsInfo();
        attach.addAttachment(new MimePartAttachSpec(String.valueOf(msg.getId()), "2"));
        msgToSend.setAttachments(attach);
        SendMsgRequest req = new SendMsgRequest();
        req.setMsg(msgToSend);
        MimeMessage parsed = sendForwardedMessage(req, msg);
        ZMimeMultipart mmp = (ZMimeMultipart) parsed.getContent();
        assertEquals("8bit", mmp.getBodyPart(0).getHeader("Content-Transfer-Encoding")[0]);
        assertEquals("base64", mmp.getBodyPart(1).getHeader("Content-Transfer-Encoding")[0]);
    }

    /*
     * This tests the CTE header of a forwarded message being inferred from the existing message when the message is a simple MIME message
     */
    @Ignore("disabled until bug 98015 is fixed")
    @Test
    public void testSimpleMimeMessage() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MimeMessage mimeMsg = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(getSimpleMimeString().getBytes()));
        ParsedMessage pm = new ParsedMessage(mimeMsg, true);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, pm, dopt, null);
        MsgToSend msgToSend = new MsgToSend();
        msgToSend.setOrigId(String.valueOf(msg.getId()));
        msgToSend.setReplyType("w");
        msgToSend.setSubject("Fwd: Simple Test");
        msgToSend.setMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", "simple test"));
        SendMsgRequest req = new SendMsgRequest();
        req.setMsg(msgToSend);
        MimeMessage parsed = sendForwardedMessage(req, msg);
        assertEquals("test", parsed.getHeader("Content-Transfer-Encoding")[0]);
    }

    /*
     * This tests the CTE header of a forwarded message being inferred from the existing message when the message is a multipart message.
     */
    @Ignore("disabled until bug 98015 is fixed")
    @Test
    public void testMultipartMimeMessage() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MimeMessage mimeMsg = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(getMultipartMimeString().getBytes()));
        ParsedMessage pm = new ParsedMessage(mimeMsg, true);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, pm, dopt, null);
        MsgToSend msgToSend = new MsgToSend();
        msgToSend.setOrigId(String.valueOf(msg.getId()));
        msgToSend.setReplyType("w");
        msgToSend.setSubject("Fwd: Multipart Test");
        MimePartInfo mpi = new MimePartInfo();
        mpi.setContentType("multipart/alternative");
        List<MimePartInfo> mimeParts = new LinkedList<MimePartInfo>();
        mimeParts.add(MimePartInfo.createForContentTypeAndContent("text/plain", "multipart test"));
        mimeParts.add(MimePartInfo.createForContentTypeAndContent("text/html", "multipart test"));
        mpi.setMimeParts(mimeParts);
        msgToSend.setMimePart(mpi);
        SendMsgRequest req = new SendMsgRequest();
        req.setMsg(msgToSend);
        MimeMessage parsed = sendForwardedMessage(req, msg);
        ZMimeMultipart mmp = (ZMimeMultipart) parsed.getContent();
        assertEquals("test1", mmp.getBodyPart(0).getHeader("Content-Transfer-Encoding")[0]);
        assertEquals("test2", mmp.getBodyPart(1).getHeader("Content-Transfer-Encoding")[0]);
    }

    /*
     * Tests bug 103193, which is a regression introduced by bugfix for 98015.
     * The problem seems to be that the MIME structure of the forwarded message doesn't match the structure
     * of the original, which is assumed by the bugfix. Specifically, it lacks the top-level multipart/mixed parent.
     */

    @Test
    public void testNestedMultipartMessage() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MimeMessage mimeMsg = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(getNestedMimeString().getBytes()));
        ParsedMessage pm = new ParsedMessage(mimeMsg, true);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, pm, dopt, null);
        MsgToSend msgToSend = new MsgToSend();
        msgToSend.setOrigId(String.valueOf(msg.getId()));
        msgToSend.setReplyType("w");
        msgToSend.setSubject("Fwd: Multipart Test");
        MimePartInfo mpi = new MimePartInfo();
        mpi.setContentType("multipart/alternative");
        List<MimePartInfo> mimeParts = new LinkedList<MimePartInfo>();
        mimeParts.add(MimePartInfo.createForContentTypeAndContent("text/plain", "text content"));
        mimeParts.add(MimePartInfo.createForContentTypeAndContent("text/html", "html content"));
        mpi.setMimeParts(mimeParts);
        msgToSend.setMimePart(mpi);
        SendMsgRequest req = new SendMsgRequest();
        req.setMsg(msgToSend);
        try {
            MimeMessage parsed = sendForwardedMessage(req, msg);
        } catch (ArrayIndexOutOfBoundsException e) {
            fail("could not build MIME message");
        }
    }

    private String getBug98015MimeString() {
        return "Subject: QP Bug\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: multipart/mixed; boundary=\"--\"\n" +
                "Content-Transfer-Encoding: 8bit\n" +
                "\n" +
                "----\n" +
                "Content-Type: text/plain; charset=\"utf-8\"\n" +
                "\n" +
                " \\ö/\n" +
                "id=577281\n" +
                "\n" +
                "----\n" +
                "Content-Type: text/plain\n" +
                "Content-Disposition: attachment\n" +
                "Content-Transfer-Encoding: base64\n" +
                "\n" +
                "VGVzdAo=\n" +
                "\n" +
                "------\n" +
                "\n";
    }

    private String getMultipartMimeString() {
        return "Subject: Multipart Test\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: multipart/alternative; \n" +
                "    boundary=\"=_a722260f-6205-4da3-8a5b-b8cfc9b124db\"\n" +
                "\n" +
                "--=_a722260f-6205-4da3-8a5b-b8cfc9b124db\n" +
                "Content-Type: text/plain; charset=utf-8\n" +
                "Content-Transfer-Encoding: test1\n" +
                "\n" +
                "multipart \n" +
                "\n" +
                "--=_a722260f-6205-4da3-8a5b-b8cfc9b124db\n" +
                "Content-Type: text/html; charset=utf-8\n" +
                "Content-Transfer-Encoding: test2\n" +
                "\n" +
                "<html><body>multipart test</body></html>\n" +
                "--=_a722260f-6205-4da3-8a5b-b8cfc9b124db--\n";
    }

    private String getSimpleMimeString() {
        return "Subject: Simple test\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: text/plain; charset=utf-8\n" +
                "Content-Transfer-Encoding: test\n" +
                "\n" +
                "simple test";
    }

    private String getNestedMimeString() {
        return "Date: Thu, 7 Jan 2016 01:21:48 -0800 (PST)\n" +
                "Subject: Nested Mime test" +
                "MIME-Version: 1.0\n" +
                "Content-Type: multipart/mixed; \n" +
                "    boundary=\"----=_Part_2216_1104390902.1452158508524\"\n" +
                "\n" +
                "------=_Part_2216_1104390902.1452158508524\n" +
                "Content-Type: multipart/alternative; \n" +
                "    boundary=\"=_d8139c58-fec7-4357-8f97-e79402d7cd0f\"\n" +
                "\n" +
                "--=_d8139c58-fec7-4357-8f97-e79402d7cd0f\n" +
                "Content-Type: text/plain; charset=utf-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "text content\n" +
                "--=_d8139c58-fec7-4357-8f97-e79402d7cd0f\n" +
                "Content-Type: text/html; charset=utf-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "<html><body>html content</body></html>\n" +
                "--=_d8139c58-fec7-4357-8f97-e79402d7cd0f--\n" +
                "\n" +
                "------=_Part_2216_1104390902.1452158508524--";
    }

}
