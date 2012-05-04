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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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
import com.zimbra.cs.util.JMSession;

/**
 * Unit test for {@link MailSender}.
 *
 * @author ysasaki
 */
public final class MailSenderTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
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
