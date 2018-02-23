/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util;

import java.util.HashMap;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.account.ZAttrProvisioning.ShareNotificationMtaConnectionType;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailclient.smtp.SmtpTransport;
import com.zimbra.cs.mailclient.smtp.SmtpsTransport;

/**
 * Unit test for {@link JMSession}.
 *
 * @author ysasaki
 */
public class JMSessionTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        LC.zimbra_attrs_directory.setDefault(MailboxTestUtil.getZimbraServerDir("") + "conf/attrs");
        MockProvisioning prov = new MockProvisioning();
        prov.getLocalServer().setSmtpPort(25);
        Provisioning.setInstance(prov);
    }

    @After
    public void tearDown() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.getAccount("user1@example.com");
        if (acct != null) {
            Domain domain = prov.getDomain(acct);
            prov.deleteAccount(acct.getId());
            prov.deleteDomain(domain.getId());
        }
        prov.getConfig().setSmtpStartTlsModeAsString("only");
    }
    
    @Test
    public void getTransport() throws Exception {
        Assert.assertSame(SmtpTransport.class,
                JMSession.getSession().getTransport("smtp").getClass());
        Assert.assertSame(SmtpsTransport.class,
                JMSession.getSession().getTransport("smtps").getClass());

        Assert.assertSame(SmtpTransport.class,
                JMSession.getSmtpSession().getTransport("smtp").getClass());
        Assert.assertSame(SmtpsTransport.class,
                JMSession.getSmtpSession().getTransport("smtps").getClass());
    }

    //@Test
    public void testRelayMta() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        server.setShareNotificationMtaHostname("mta02.zimbra.com");
        server.setShareNotificationMtaPort(25);
        server.setShareNotificationMtaAuthRequired(true);
        server.setShareNotificationMtaConnectionType(ShareNotificationMtaConnectionType.STARTTLS);
        server.setShareNotificationMtaAuthAccount("test-jylee");
        server.setShareNotificationMtaAuthPassword("test123");

        SMTPMessage out = new SMTPMessage(JMSession.getRelaySession());
        InternetAddress address = new JavaMailInternetAddress("test-jylee@zimbra.com");
        out.setFrom(address);

        address = new JavaMailInternetAddress("test-jylee@zimbra.com");
        out.setRecipient(javax.mail.Message.RecipientType.TO, address);

        out.setSubject("test mail");
        out.setText("hello world");

        out.saveChanges();
        ZimbraLog.smtp.setLevel(Level.trace);
        Transport.send(out);
    }

    @Test
    public void messageID() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.createDomain("example.com", new HashMap<String, Object>());
        Account account = prov.createAccount("user1@example.com", "test123", new HashMap<String, Object>());

        MimeMessage mm = new MimeMessage(JMSession.getSmtpSession(account));
        mm.saveChanges();
        Assert.assertEquals("message ID contains account domain", domain.getName() + '>', mm.getMessageID().split("@")[1]);
    }
    
    @Test
    public void testSmtpStartTlsMode() throws Exception {
        Session smtpSession;
        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.createDomain("example.com", new HashMap<String, Object>());
        String mail = "user1@example.com";
        Account account = prov.createAccount(mail, "test123", new HashMap<String, Object>());
        Server server = prov.getLocalServer();
        
        // Server:off
        server.setSmtpStartTlsModeAsString("off");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=off/-: mail.smtp.starttls.enable", "false", smtpSession.getProperty("mail.smtp.starttls.enable"));
        
        // Server:on
        server.setSmtpStartTlsModeAsString("on");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=on/-: mail.smtp.starttls.enable", "true", smtpSession.getProperty("mail.smtp.starttls.enable"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=on/-: mail.smtp.starttls.required", "false", smtpSession.getProperty("mail.smtp.starttls.required"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=on/-: mail.smtp.ssl.trust", "*", smtpSession.getProperty("mail.smtp.ssl.trust"));

        // Server:only
        server.setSmtpStartTlsModeAsString("only");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=only/-: mail.smtp.starttls.enable", "true", smtpSession.getProperty("mail.smtp.starttls.enable"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=only/-: mail.smtp.starttls.required", "true", smtpSession.getProperty("mail.smtp.starttls.required"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=only/-: mail.smtp.ssl.trust", "*", smtpSession.getProperty("mail.smtp.ssl.trust"));

        // Server:off, Domain:off
        server.setSmtpStartTlsModeAsString("off");
        domain.setSmtpStartTlsModeAsString("off");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=off/off: mail.smtp.starttls.enable", "false", smtpSession.getProperty("mail.smtp.starttls.enable"));

        // Server:off, Domain:on
        server.setSmtpStartTlsModeAsString("off");
        domain.setSmtpStartTlsModeAsString("on");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=off/on: mail.smtp.starttls.enable", "true", smtpSession.getProperty("mail.smtp.starttls.enable"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=off/on: mail.smtp.starttls.required", "false", smtpSession.getProperty("mail.smtp.starttls.required"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=off/on: mail.smtp.ssl.trust", "*", smtpSession.getProperty("mail.smtp.ssl.trust"));

        // Server:off, Domain:only
        server.setSmtpStartTlsModeAsString("off");
        domain.setSmtpStartTlsModeAsString("only");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=off/only: mail.smtp.starttls.enable", "true", smtpSession.getProperty("mail.smtp.starttls.enable"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=off/only: mail.smtp.starttls.required", "true", smtpSession.getProperty("mail.smtp.starttls.required"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=off/only: mail.smtp.ssl.trust", "*", smtpSession.getProperty("mail.smtp.ssl.trust"));

        // Server:on, Domain:off
        server.setSmtpStartTlsModeAsString("on");
        domain.setSmtpStartTlsModeAsString("off");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=on/off: mail.smtp.starttls.enable", "false", smtpSession.getProperty("mail.smtp.starttls.enable"));

        // Server:on, Domain:on
        server.setSmtpStartTlsModeAsString("on");
        domain.setSmtpStartTlsModeAsString("on");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=on/on: mail.smtp.starttls.enable", "true", smtpSession.getProperty("mail.smtp.starttls.enable"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=on/on: mail.smtp.starttls.required", "false", smtpSession.getProperty("mail.smtp.starttls.required"));

        // Server:on, Domain:only
        server.setSmtpStartTlsModeAsString("on");
        domain.setSmtpStartTlsModeAsString("only");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=on/only: mail.smtp.starttls.enable", "true", smtpSession.getProperty("mail.smtp.starttls.enable"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=on/only: mail.smtp.starttls.required", "true", smtpSession.getProperty("mail.smtp.starttls.required"));

        // Server:only, Domain:off
        server.setSmtpStartTlsModeAsString("only");
        domain.setSmtpStartTlsModeAsString("off");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=only/off: mail.smtp.starttls.enable", "false", smtpSession.getProperty("mail.smtp.starttls.enable"));

        // Server:only, Domain:on
        server.setSmtpStartTlsModeAsString("only");
        domain.setSmtpStartTlsModeAsString("on");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=only/on: mail.smtp.starttls.enable", "true", smtpSession.getProperty("mail.smtp.starttls.enable"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=only/on: mail.smtp.starttls.required", "false", smtpSession.getProperty("mail.smtp.starttls.required"));

        // Server:only, Domain:only
        server.setSmtpStartTlsModeAsString("only");
        domain.setSmtpStartTlsModeAsString("only");
        smtpSession = JMSession.getSmtpSession(account);
        Assert.assertEquals("zimbraSmtpStartTlsMode=only/only: mail.smtp.starttls.enable", "true", smtpSession.getProperty("mail.smtp.starttls.enable"));
        Assert.assertEquals("zimbraSmtpStartTlsMode=only/only: mail.smtp.starttls.required", "true", smtpSession.getProperty("mail.smtp.starttls.required"));
    }
}
