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
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import org.junit.Ignore;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.ZAttrProvisioning.ShareNotificationMtaConnectionType;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.JMSession;
import com.zimbra.qa.unittest.MessageBuilder;

/**
 * Unit test for {@link MailSender}.
 *
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class MailSenderTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.deleteAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAllowFromAddress, "test-alias@zimbra.com");
        attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, "test@zimbra.com");
        attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, "test-alias@zimbra.com");
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Test
    public void getSenderHeadersSimpleAuth() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MailSender mailSender = new MailSender();
        Pair<InternetAddress, InternetAddress> pair;
        String mail = "test@zimbra.com";
        String alias = "test-alias@zimbra.com";
        String invalid1 = "foo@zimbra.com";
        String invalid2 = "bar@zimbra.com";

        pair = mailSender.getSenderHeaders(null, null, account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(mail), null, account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(null, new InternetAddress(mail), account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(mail), new InternetAddress(mail), account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(alias), null, account, account, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(null, new InternetAddress(alias), account, account, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(alias), new InternetAddress(alias), account, account, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(invalid1), null, account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(null, new InternetAddress(invalid1), account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(invalid1), new InternetAddress(invalid2), account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(alias), new InternetAddress(mail), account, account, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertEquals(mail, pair.getSecond().toString());

        pair = mailSender.getSenderHeaders(new InternetAddress(mail), new InternetAddress(alias), account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(alias), new InternetAddress(invalid1), account, account, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertEquals(mail, pair.getSecond().toString());

        pair = mailSender.getSenderHeaders(new InternetAddress(invalid1), new InternetAddress(alias), account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(mail), new InternetAddress(invalid1), account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(invalid1), new InternetAddress(mail), account, account, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());
    }

    @Test
    public void getSenderHeadersDelegatedAuth() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account account2 = prov.createAccount("test2@zimbra.com", "secret", attrs);
        MailSender mailSender = new MailSender();
        Pair<InternetAddress, InternetAddress> pair;
        String target = "test@zimbra.com";
        String mail = "test2@zimbra.com";
        String alias = "test-alias@zimbra.com";
        String invalid1 = "foo@zimbra.com";
        String invalid2 = "bar@zimbra.com";

        Right right = RightManager.getInstance().getUserRight("sendOnBehalfOf");
        ZimbraACE ace = new ZimbraACE(account2.getId(), GranteeType.GT_USER, right, null, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(ace);
        ACLUtil.grantRight(Provisioning.getInstance(), account, aces);

        pair = mailSender.getSenderHeaders(null, null, account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(mail), null, account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(null, new InternetAddress(mail), account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(mail), new InternetAddress(mail), account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(alias), null, account, account2, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertEquals(mail, pair.getSecond().toString());

        pair = mailSender.getSenderHeaders(null, new InternetAddress(alias), account, account2, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertEquals(mail, pair.getSecond().toString());

        pair = mailSender.getSenderHeaders(new InternetAddress(alias), new InternetAddress(alias), account, account2, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertEquals(mail, pair.getSecond().toString());

        pair = mailSender.getSenderHeaders(new InternetAddress(invalid1), null, account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(null, new InternetAddress(invalid1), account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(invalid1), new InternetAddress(invalid2), account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(alias), new InternetAddress(mail), account, account2, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertEquals(mail, pair.getSecond().toString());

        pair = mailSender.getSenderHeaders(new InternetAddress(mail), new InternetAddress(alias), account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(alias), new InternetAddress(invalid1), account, account2, false);
        Assert.assertEquals(alias, pair.getFirst().toString());
        Assert.assertEquals(mail, pair.getSecond().toString());

        pair = mailSender.getSenderHeaders(new InternetAddress(invalid1), new InternetAddress(alias), account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(mail), new InternetAddress(invalid1), account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress(invalid1), new InternetAddress(mail), account, account2, false);
        Assert.assertEquals(mail, pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());
    }

    @Test
    public void getCalSenderHeaders() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MailSender calSender = new MailSender().setCalendarMode(true);
        Pair<InternetAddress, InternetAddress> pair;

        // Calendar mode allows send-obo without grants.
        pair = calSender.getSenderHeaders(new InternetAddress("foo@zimbra.com"), new InternetAddress("test@zimbra.com"), account, account, false);
        Assert.assertEquals("foo@zimbra.com", pair.getFirst().toString());
        Assert.assertEquals("test@zimbra.com", pair.getSecond().toString());

        // Even in calendar mode, Sender must be the user's own address.
        pair = calSender.getSenderHeaders(new InternetAddress("foo@zimbra.com"), new InternetAddress("bar@zimbra.com"), account, account, false);
        Assert.assertEquals("foo@zimbra.com", pair.getFirst().toString());
        Assert.assertEquals("test@zimbra.com", pair.getSecond().toString());
    }

    @Test
    public void updateReferenceHeaders() throws Exception {
        MailSender sender = new MailSender();
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        String from = "sender@example.com";
        String to = "recipient@example.com";
        String subject = "test subject";
        MessageBuilder builder = new MessageBuilder().withFrom(from).withToRecipient(to)
            .withSubject(subject).withBody("test body");
        String content = builder.create();
        MimeMessage msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));

        //default
        Assert.assertNull(msg.getHeader("Thread-Index"));
        Assert.assertNull(msg.getHeader("Thread-Topic"));
        sender.updateReferenceHeaders(msg, null, account);
        Assert.assertEquals(1, msg.getHeader("Thread-Index").length);
        Assert.assertEquals(1, msg.getHeader("Thread-Topic").length);
        Assert.assertEquals(subject, msg.getHeader("Thread-Topic")[0]); //technically the normalized subject, but our test message already had normalized subject

        //index set by sending agent (bg 96107)
        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        String presetIndex = "1234";
        msg.setHeader("Thread-Index", presetIndex);
        Assert.assertNull(msg.getHeader("Thread-Topic"));
        sender.updateReferenceHeaders(msg, null, account);
        Assert.assertEquals(1, msg.getHeader("Thread-Index").length);
        Assert.assertEquals(1, msg.getHeader("Thread-Topic").length);
        Assert.assertEquals(subject, msg.getHeader("Thread-Topic")[0]);
        Assert.assertEquals(presetIndex, msg.getHeader("Thread-Index")[0]);

        //topic set by sending agent (bug 96107)
        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        Assert.assertNull(msg.getHeader("Thread-Index"));
        String presetTopic = "mytopic";
        msg.setHeader("Thread-Topic", presetTopic);
        sender.updateReferenceHeaders(msg, null, account);
        Assert.assertEquals(1, msg.getHeader("Thread-Index").length);
        Assert.assertEquals(1, msg.getHeader("Thread-Topic").length);
        Assert.assertEquals(presetTopic, msg.getHeader("Thread-Topic")[0]);

        //both
        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        msg.setHeader("Thread-Topic", presetTopic);
        msg.setHeader("Thread-Index", presetIndex);
        sender.updateReferenceHeaders(msg, null, account);
        Assert.assertEquals(1, msg.getHeader("Thread-Index").length);
        Assert.assertEquals(1, msg.getHeader("Thread-Topic").length);
        Assert.assertEquals(presetTopic, msg.getHeader("Thread-Topic")[0]);
        Assert.assertEquals(presetIndex, msg.getHeader("Thread-Index")[0]);


        //subject changed from orig parent subject (bug 96954)
        String parentSubject = "parent subject";
        builder = new MessageBuilder().withFrom(from).withToRecipient(to)
                .withSubject(parentSubject).withBody("parent body");
        String parentContent = builder.create();

        MimeMessage parentMsg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(parentContent.getBytes()));
        sender.updateReferenceHeaders(parentMsg, null, account);
        ParsedMessage pm = new ParsedMessage(parentMsg, false);
        Message mboxMessage = mbox.addMessage(null, pm, MailboxTest.STANDARD_DELIVERY_OPTIONS, null);

        sender = new MailSender();
        sender.setOriginalMessageId(new ItemId(mboxMessage));
        sender.setReplyType(MailSender.MSGTYPE_REPLY);
        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        sender.updateReferenceHeaders(msg, null, account);
        Assert.assertEquals(1, msg.getHeader("Thread-Index").length);
        Assert.assertEquals(1, msg.getHeader("Thread-Topic").length);
        Assert.assertEquals(subject, msg.getHeader("Thread-Topic")[0]);

        //keep agent specified topic and/or index even when parent specified
        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        msg.setHeader("Thread-Topic", presetTopic);
        sender.updateReferenceHeaders(msg, null, account);
        Assert.assertEquals(1, msg.getHeader("Thread-Index").length);
        Assert.assertEquals(1, msg.getHeader("Thread-Topic").length);
        Assert.assertEquals(presetTopic, msg.getHeader("Thread-Topic")[0]);

        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        msg.setHeader("Thread-Index", presetIndex);
        sender.updateReferenceHeaders(msg, null, account);
        Assert.assertEquals(1, msg.getHeader("Thread-Index").length);
        Assert.assertEquals(1, msg.getHeader("Thread-Topic").length);
        Assert.assertEquals(subject, msg.getHeader("Thread-Topic")[0]);
        Assert.assertEquals(presetIndex, msg.getHeader("Thread-Index")[0]);

        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        msg.setHeader("Thread-Topic", presetTopic);
        msg.setHeader("Thread-Index", presetIndex);
        sender.updateReferenceHeaders(msg, null, account);
        Assert.assertEquals(1, msg.getHeader("Thread-Index").length);
        Assert.assertEquals(1, msg.getHeader("Thread-Topic").length);
        Assert.assertEquals(presetTopic, msg.getHeader("Thread-Topic")[0]);
        Assert.assertEquals(presetIndex, msg.getHeader("Thread-Index")[0]);
    }

    //@Test
    public void sendExternalMessage() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        server.setSmtpHostname(new String[] {"bogusname.test"});
        server.setSmtpPort(25);
        server.setSmtpTimeout(60);
        server.setSmtpSendPartial(true);
        server.setShareNotificationMtaHostname("mta02.zimbra.com");
        server.setShareNotificationMtaPort(25);
        server.setShareNotificationMtaAuthRequired(true);
        server.setShareNotificationMtaConnectionType(ShareNotificationMtaConnectionType.STARTTLS);
        server.setShareNotificationMtaAuthAccount("test-jylee");
        server.setShareNotificationMtaAuthPassword("test123");

        MimeMessage mm = new MimeMessage(JMSession.getSmtpSession());
        InternetAddress address = new JavaMailInternetAddress("test-jylee@zimbra.com");
        mm.setFrom(address);

        address = new JavaMailInternetAddress("test-jylee@zimbra.com");
        mm.setRecipient(javax.mail.Message.RecipientType.TO, address);

        mm.setSubject("test mail");
        mm.setText("hello world");

        mm.saveChanges();
        ZimbraLog.smtp.setLevel(Level.trace);
        MailSender.relayMessage(mm);
    }
}
