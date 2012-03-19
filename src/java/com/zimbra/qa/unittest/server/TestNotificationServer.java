/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.qa.unittest.TestUtil;

/**
 * These tests cause a change in state to the out_of_office table.  This data needs to
 * be cleaned up after the tests runs, so we categorize these tests as server-only.
 */
public class TestNotificationServer
extends TestCase {

    private static String RECIPIENT_NAME = "user1";
    private static String SENDER_NAME = "user2";

    private static String[] ALL_TEST_USERS = { "user1", "user2" };

    private static String NAME_PREFIX = TestNotificationServer.class.getSimpleName();

    private boolean originalReplyEnabled;
    private String originalReply;
    private boolean originalNotificationEnabled;
    private String originalNotificationAddress;
    private String originalNotificationSubject;
    private String originalNotificationBody;
    private String[] originalInterceptAddresses;
    private String originalInterceptSendHeadersOnly;
    private String originalSaveToSent;
    private String originalMailForwardingAddress;
    private boolean originalLocalDeliveryDisabled;
    private boolean isServerTest;

    protected void setUp() throws Exception
    {
        super.setUp();
        cleanUp();

        Account account = TestUtil.getAccount(RECIPIENT_NAME);
        originalReplyEnabled = account.getBooleanAttr(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, false);
        originalReply = account.getAttr(Provisioning.A_zimbraPrefOutOfOfficeReply, "");
        originalNotificationEnabled = account.getBooleanAttr(Provisioning.A_zimbraPrefNewMailNotificationEnabled, false);
        originalNotificationAddress = account.getAttr(Provisioning.A_zimbraPrefNewMailNotificationAddress, "");
        originalNotificationSubject = account.getAttr(Provisioning.A_zimbraNewMailNotificationSubject, "");
        originalNotificationBody = account.getAttr(Provisioning.A_zimbraNewMailNotificationBody, "");
        originalInterceptAddresses = account.getMultiAttr(Provisioning.A_zimbraInterceptAddress);
        originalInterceptSendHeadersOnly = account.getAttr(Provisioning.A_zimbraInterceptSendHeadersOnly, "");
        originalSaveToSent = account.getAttr(Provisioning.A_zimbraPrefSaveToSent, "");
        originalMailForwardingAddress = account.getAttr(Provisioning.A_zimbraPrefMailForwardingAddress, "");
        originalLocalDeliveryDisabled = account.getBooleanAttr(Provisioning.A_zimbraPrefMailLocalDeliveryDisabled, false);
    }

    /**
     * Confirms that the subject and body of the out of office and new mail
     * notification can contain UTF-8 characters.
     *
     * @throws Exception
     */
    public void testUtf8()
    throws Exception {
        isServerTest = true;

        String NEW_MAIL_SUBJECT =
                NAME_PREFIX + " \u041a\u0440\u043e\u043a\u043e\u0434\u0438\u043b"; // Krokodil
        String NEW_MAIL_BODY =
                NAME_PREFIX + " \u0427\u0435\u0440\u0435\u043f\u0430\u0445\u0430"; // Cherepaha
        String OUT_OF_OFFICE_SUBJECT =
                NAME_PREFIX + " \u041e\u0431\u0435\u0437\u044c\u044f\u043d\u0430"; // Obezyana
        String OUT_OF_OFFICE_BODY =
                NAME_PREFIX + " \u0416\u0438\u0440\u0430\u0444";                   // Jiraf

        // Turn on auto-reply and notification
        Account account = TestUtil.getAccount(RECIPIENT_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, OUT_OF_OFFICE_BODY);
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationEnabled, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationAddress, TestUtil.getAddress(SENDER_NAME));
        attrs.put(Provisioning.A_zimbraNewMailNotificationSubject, NEW_MAIL_SUBJECT);
        attrs.put(Provisioning.A_zimbraNewMailNotificationBody, NEW_MAIL_BODY);
        Provisioning.getInstance().modifyAttrs(account, attrs);

        ZMailbox senderMbox = TestUtil.getZMailbox(SENDER_NAME);
        ZMailbox recipMbox = TestUtil.getZMailbox(RECIPIENT_NAME);

        // Make sure messages don't exist
        List<ZMessage> messages = TestUtil.search(recipMbox, "in:inbox subject:" + OUT_OF_OFFICE_SUBJECT);
        assertEquals("Messages in recipient mailbox", 0, messages.size());
        messages = TestUtil.search(senderMbox, "in:inbox subject:" + OUT_OF_OFFICE_SUBJECT);
        assertEquals("Messages in sender mailbox", 0, messages.size());
        messages = TestUtil.search(senderMbox, NEW_MAIL_SUBJECT);
        assertEquals("New mail reply", 0, messages.size());

        // Send the message
        TestUtil.sendMessage(senderMbox, RECIPIENT_NAME, OUT_OF_OFFICE_SUBJECT, "testing");

        // Make sure the recipient received it
        TestUtil.waitForMessage(recipMbox, "in:inbox subject:" + OUT_OF_OFFICE_SUBJECT);

        // Check for out-of-office
        TestUtil.waitForMessage(senderMbox, "in:inbox subject:" + OUT_OF_OFFICE_SUBJECT);
        messages = TestUtil.search(senderMbox, "in:inbox content:" + OUT_OF_OFFICE_BODY);
        assertEquals("Out-of-office body not found", 1, messages.size());

        // Check for new mail notification
        TestUtil.waitForMessage(senderMbox, "in:inbox subject:" + NEW_MAIL_SUBJECT);
        messages = TestUtil.search(senderMbox, "in:inbox content:" + NEW_MAIL_BODY);
        assertEquals("New mail notification body not found", 1, messages.size());
    }

    /**
     * Tests fix for bug 26818 (OOO message does not work when "Don't keep a local copy of messages" is checked)
     * 
     * @throws Exception
     */
    public void testOOOWhenForwardNoDelivery()
    throws Exception {
        isServerTest = true;

        Account account = TestUtil.getAccount(RECIPIENT_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, "I am OOO");
        attrs.put(Provisioning.A_zimbraPrefMailForwardingAddress, "abc@xyz.com");
        attrs.put(Provisioning.A_zimbraPrefMailLocalDeliveryDisabled, ProvisioningConstants.TRUE);
        Provisioning.getInstance().modifyAttrs(account, attrs);

        ZMailbox senderMbox = TestUtil.getZMailbox(SENDER_NAME);
        ZMailbox recipMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        
        String subject = NAME_PREFIX + " testOOOWhenForwardNoDelivery";

        // Send the message
        TestUtil.sendMessage(senderMbox, RECIPIENT_NAME, subject, "testing");

        // Make sure message was not delivered since local delivery is disabled
        assertEquals(0, TestUtil.search(recipMbox, "in:inbox subject:" + subject).size());

        // But check for out-of-office reply
        TestUtil.waitForMessage(senderMbox, "in:inbox subject:" + subject);
    }

    public void tearDown()
    throws Exception {
        cleanUp();

        // Revert to original values for out-of-office and notification
        Account account = TestUtil.getAccount(RECIPIENT_NAME);

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled,
            LdapUtil.getLdapBooleanString(originalReplyEnabled));
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, originalReply);
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationEnabled,
            LdapUtil.getLdapBooleanString(originalNotificationEnabled));
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationAddress, originalNotificationAddress);
        attrs.put(Provisioning.A_zimbraNewMailNotificationSubject, originalNotificationSubject);
        attrs.put(Provisioning.A_zimbraNewMailNotificationBody, originalNotificationBody);
        if (originalInterceptAddresses != null && originalInterceptAddresses.length == 0) {
            attrs.put(Provisioning.A_zimbraInterceptAddress, "");
        } else {
            attrs.put(Provisioning.A_zimbraInterceptAddress, originalInterceptAddresses);
        }
        attrs.put(Provisioning.A_zimbraInterceptSendHeadersOnly, originalInterceptSendHeadersOnly);
        attrs.put(Provisioning.A_zimbraPrefSaveToSent, originalSaveToSent);
        attrs.put(Provisioning.A_zimbraPrefMailForwardingAddress, originalMailForwardingAddress);
        attrs.put(Provisioning.A_zimbraPrefMailLocalDeliveryDisabled,
                LdapUtil.getLdapBooleanString(originalLocalDeliveryDisabled));
        Provisioning.getInstance().modifyAttrs(account, attrs);

        super.tearDown();
    }

    /**
     * Deletes rows from the <tt>out_of_office</tt> table and cleans up data
     * created by the test.
     */
    private void cleanUp()
    throws Exception {
        if (isServerTest) {
            DbConnection conn = DbPool.getConnection();
            Mailbox mbox = TestUtil.getMailbox(RECIPIENT_NAME);
            DbOutOfOffice.clear(conn, mbox);
            conn.commit();
            DbPool.quietClose(conn);
        }

        // Clean up temporary data
        for (String userName : ALL_TEST_USERS) {
            TestUtil.deleteTestData(userName, NAME_PREFIX);
        }
    }
}
