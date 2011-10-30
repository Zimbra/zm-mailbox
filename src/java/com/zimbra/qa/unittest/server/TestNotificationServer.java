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


public class TestNotificationServer
extends TestCase {

    private static String RECIPIENT_NAME = "user1";
    private static String SENDER_NAME = "user2";

    private static String[] ALL_TEST_USERS = { "user1", "user2", "user3" };

    private static String NAME_PREFIX = TestNotificationServer.class.getSimpleName();

    private static String NEW_MAIL_SUBJECT =
        NAME_PREFIX + " \u041a\u0440\u043e\u043a\u043e\u0434\u0438\u043b"; // Krokodil
    private static String NEW_MAIL_BODY =
        NAME_PREFIX + " \u0427\u0435\u0440\u0435\u043f\u0430\u0445\u0430"; // Cherepaha
    private static String OUT_OF_OFFICE_SUBJECT =
        NAME_PREFIX + " \u041e\u0431\u0435\u0437\u044c\u044f\u043d\u0430"; // Obezyana
    private static String OUT_OF_OFFICE_BODY =
        NAME_PREFIX + " \u0416\u0438\u0440\u0430\u0444";                   // Jiraf

    private boolean mOriginalReplyEnabled;
    private String mOriginalReply;
    private boolean mOriginalNotificationEnabled;
    private String mOriginalNotificationAddress;
    private String mOriginalNotificationSubject;
    private String mOriginalNotificationBody;
    private String[] mOriginalInterceptAddresses;
    private String mOriginalInterceptSendHeadersOnly;
    private String mOriginalSaveToSent;
    private boolean mIsServerTest = false;

    protected void setUp() throws Exception
    {
        super.setUp();
        cleanUp();

        Account account = TestUtil.getAccount(RECIPIENT_NAME);
        mOriginalReplyEnabled = account.getBooleanAttr(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, false);
        mOriginalReply = account.getAttr(Provisioning.A_zimbraPrefOutOfOfficeReply, "");
        mOriginalNotificationEnabled = account.getBooleanAttr(Provisioning.A_zimbraPrefNewMailNotificationEnabled, false);
        mOriginalNotificationAddress = account.getAttr(Provisioning.A_zimbraPrefNewMailNotificationAddress, "");
        mOriginalNotificationSubject = account.getAttr(Provisioning.A_zimbraNewMailNotificationSubject, "");
        mOriginalNotificationBody = account.getAttr(Provisioning.A_zimbraNewMailNotificationBody, "");
        mOriginalInterceptAddresses = account.getMultiAttr(Provisioning.A_zimbraInterceptAddress);
        mOriginalInterceptSendHeadersOnly = account.getAttr(Provisioning.A_zimbraInterceptSendHeadersOnly, "");
        mOriginalSaveToSent = account.getAttr(Provisioning.A_zimbraPrefSaveToSent, "");
    }

    /**
     * Confirms that the subject and body of the out of office and new mail
     * notification can contain UTF-8 characters.
     *
     * This test causes a change in state to the out_of_office table.  This data
     * needs to be cleaned up after the test runs, so we categorize this test
     * as server-only.
     *
     * @throws Exception
     */
    public void testUtf8()
    throws Exception {
        mIsServerTest = true;

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


    public void tearDown()
    throws Exception {
        cleanUp();

        // Revert to original values for out-of-office and notification
        Account account = TestUtil.getAccount(RECIPIENT_NAME);

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled,
            LdapUtil.getLdapBooleanString(mOriginalReplyEnabled));
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, mOriginalReply);
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationEnabled,
            LdapUtil.getLdapBooleanString(mOriginalNotificationEnabled));
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationAddress, mOriginalNotificationAddress);
        attrs.put(Provisioning.A_zimbraNewMailNotificationSubject, mOriginalNotificationSubject);
        attrs.put(Provisioning.A_zimbraNewMailNotificationBody, mOriginalNotificationBody);
        if (mOriginalInterceptAddresses != null && mOriginalInterceptAddresses.length == 0) {
            attrs.put(Provisioning.A_zimbraInterceptAddress, "");
        } else {
            attrs.put(Provisioning.A_zimbraInterceptAddress, mOriginalInterceptAddresses);
        }
        attrs.put(Provisioning.A_zimbraInterceptSendHeadersOnly, mOriginalInterceptSendHeadersOnly);
        attrs.put(Provisioning.A_zimbraPrefSaveToSent, mOriginalSaveToSent);
        Provisioning.getInstance().modifyAttrs(account, attrs);

        super.tearDown();
    }

    /**
     * Deletes rows from the <tt>out_of_office</tt> table and cleans up data
     * created by the test.
     */
    private void cleanUp()
    throws Exception {
        if (mIsServerTest) {
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
