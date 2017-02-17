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
import com.zimbra.qa.unittest.TestUtil;

public class TestNewMailNotification
extends TestCase {

    private static String MESSAGE_RECIPIENT = "testnotification-recipient1";
    private static String NOTIFICATION_RECIPIENT = "testnotification-recipient2";
    private static String MESSAGE_SENDER = "testnotification-sender1";

    private static String[] ALL_TEST_USERS = { MESSAGE_RECIPIENT, NOTIFICATION_RECIPIENT, MESSAGE_SENDER };

    private static String NAME_PREFIX = TestNewMailNotification.class.getSimpleName();

    protected void setUp() throws Exception
    {
        cleanUp();
        TestUtil.createAccount(MESSAGE_RECIPIENT);
        TestUtil.createAccount(NOTIFICATION_RECIPIENT);
        TestUtil.createAccount(MESSAGE_SENDER);
    }

    /**
     * Confirms that the subject and body of the out of office and new mail
     * notification can contain UTF-8 characters.
     *
     * @throws Exception
     */
    public void testUtf8()
    throws Exception {

        String NEW_MAIL_SUBJECT =
                NAME_PREFIX + " \u041a\u0440\u043e\u043a\u043e\u0434\u0438\u043b"; // Krokodil
        String NEW_MAIL_BODY =
                NAME_PREFIX + " \u0427\u0435\u0440\u0435\u043f\u0430\u0445\u0430"; // Cherepaha
        String OUT_OF_OFFICE_SUBJECT =
                NAME_PREFIX + " \u041e\u0431\u0435\u0437\u044c\u044f\u043d\u0430"; // Obezyana
        String OUT_OF_OFFICE_BODY =
                NAME_PREFIX + " \u0416\u0438\u0440\u0430\u0444";                   // Jiraf

        // Turn on auto-reply and notification
        Account recipientAcct = TestUtil.getAccount(MESSAGE_RECIPIENT);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, OUT_OF_OFFICE_BODY);
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationEnabled, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationAddress, TestUtil.getAddress(NOTIFICATION_RECIPIENT));
        attrs.put(Provisioning.A_zimbraNewMailNotificationSubject, NEW_MAIL_SUBJECT);
        attrs.put(Provisioning.A_zimbraNewMailNotificationBody, NEW_MAIL_BODY);
        Provisioning.getInstance().modifyAttrs(recipientAcct, attrs);

        ZMailbox notificationMbox = TestUtil.getZMailbox(NOTIFICATION_RECIPIENT);
        ZMailbox recipientMbox = TestUtil.getZMailbox(MESSAGE_RECIPIENT);
        ZMailbox senderMbox = TestUtil.getZMailbox(MESSAGE_SENDER);

        // Send the message
        TestUtil.sendMessage(senderMbox, MESSAGE_RECIPIENT, OUT_OF_OFFICE_SUBJECT, "testing");

        // Make sure the recipient received it
        TestUtil.waitForMessage(recipientMbox, "in:inbox subject:" + OUT_OF_OFFICE_SUBJECT);

        // Check for out-of-office
        TestUtil.waitForMessage(senderMbox, "in:inbox subject:" + OUT_OF_OFFICE_SUBJECT);
        List<ZMessage> messages = TestUtil.search(senderMbox, "in:inbox content:" + OUT_OF_OFFICE_BODY);
        assertEquals("Out-of-office body not found", 1, messages.size());

        // Check for new mail notification
        TestUtil.waitForMessage(notificationMbox, "in:inbox subject:" + NEW_MAIL_SUBJECT);
        messages = TestUtil.search(notificationMbox, "in:inbox content:" + NEW_MAIL_BODY);
        assertEquals("New mail notification body not found", 1, messages.size());
    }


    /**
     * Verify that when zimbraNewMailNotificationMessage is set it overwrites
     * zimbraNewMailNotificationBody, zimbraNewMailNotificationSubject and
     * zimbraNewMailNotificationFrom
     * @throws Exception
     */
    public void testNotificationMessageOverwrite() throws Exception {
        String notificationTemplate = "From: Postmaster &lt;postmaster@${RECIPIENT_DOMAIN}&gt;${NEWLINE}To: &lt;${RECIPIENT_ADDRESS}&gt;${NEWLINE}Subject: New message received at ${RECIPIENT_ADDRESS}${NEWLINE}Date: ${DATE}${NEWLINE}Content-Type: text/plain${NEWLINE}${NEWLINE}New message received at ${RECIPIENT_ADDRESS}.${NEWLINE}Sender: ${SENDER_ADDRESS}${NEWLINE}Subject: ${SUBJECT}";
        String notificationSubject = "Even old New York was once New Amsterdam";
        String notificationBody = "On ne passe pas";

        String messageSubject = "Istanbul was Constantinople";
        String messageBody = "Now it's Istanbul, not Constantinople";
        String notifierAddress = "che@burashka.com";

        Account recipientAcct = TestUtil.getAccount(MESSAGE_RECIPIENT);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationEnabled, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationAddress, TestUtil.getAddress(NOTIFICATION_RECIPIENT));
        attrs.put(Provisioning.A_zimbraNewMailNotificationSubject, notificationSubject);
        attrs.put(Provisioning.A_zimbraNewMailNotificationBody, notificationBody);
        attrs.put(Provisioning.A_zimbraNewMailNotificationMessage, notificationTemplate);
        attrs.put(Provisioning.A_zimbraNewMailNotificationFrom, notifierAddress);
        Provisioning.getInstance().modifyAttrs(recipientAcct, attrs);

        ZMailbox notificationMbox = TestUtil.getZMailbox(NOTIFICATION_RECIPIENT);
        ZMailbox recipientMbox = TestUtil.getZMailbox(MESSAGE_RECIPIENT);
        ZMailbox senderMbox = TestUtil.getZMailbox(MESSAGE_SENDER);

        // Send the message
        TestUtil.sendMessage(senderMbox, MESSAGE_RECIPIENT, messageSubject, messageBody);

        // Make sure the recipient received it
        TestUtil.waitForMessage(recipientMbox, "in:inbox subject:'" + messageSubject + "'");

        // Check for new mail notification
        TestUtil.waitForMessage(notificationMbox,
                "in:inbox subject: New message received at " + TestUtil.getAddress(MESSAGE_RECIPIENT));
        List<ZMessage> messages = TestUtil.search(notificationMbox, "in:inbox content: New message received at "
                + TestUtil.getAddress(MESSAGE_RECIPIENT));
        assertEquals("New mail notification body not found", 1, messages.size());

        messages = TestUtil.search(notificationMbox, "in:inbox content:'" + notificationBody + "'");
        assertEquals("found new mail notification with wrong body", 0, messages.size());

        messages = TestUtil.search(notificationMbox, "in:inbox subject:'" + notificationSubject + "'");
        assertEquals("found new mail notification with wrong subject", 0, messages.size());

        messages = TestUtil.search(notificationMbox, "in:inbox from:" + notifierAddress);
        assertEquals("found new mail notification with wrong sender", 0, messages.size());
    }

    /**
     * Verify that when zimbraNewMailNotificationMessage is set it overwrites
     * zimbraNewMailNotificationBody, zimbraNewMailNotificationSubject and
     * zimbraNewMailNotificationFrom
     * @throws Exception
     */
    public void testNotificationSubjBodyFrom() throws Exception {
        String notificationSubject = "Even old New York was once New Amsterdam";
        String notificationBody = "On ne passe pas";

        String messageSubject = "Istanbul was Constantinople";
        String messageBody = "Now it's Istanbul, not Constantinople";
        String notifierAddress = "che@burashka.com";

        Account recipientAcct = TestUtil.getAccount(MESSAGE_RECIPIENT);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationEnabled, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationAddress, TestUtil.getAddress(NOTIFICATION_RECIPIENT));
        attrs.put(Provisioning.A_zimbraNewMailNotificationSubject, notificationSubject);
        attrs.put(Provisioning.A_zimbraNewMailNotificationBody, notificationBody);
        attrs.put(Provisioning.A_zimbraNewMailNotificationFrom, notifierAddress);
        Provisioning.getInstance().modifyAttrs(recipientAcct, attrs);

        ZMailbox notificationMbox = TestUtil.getZMailbox(NOTIFICATION_RECIPIENT);
        ZMailbox recipientMbox = TestUtil.getZMailbox(MESSAGE_RECIPIENT);
        ZMailbox senderMbox = TestUtil.getZMailbox(MESSAGE_SENDER);

        // Send the message
        TestUtil.sendMessage(senderMbox, MESSAGE_RECIPIENT, messageSubject, messageBody);

        // Make sure the recipient received it
        TestUtil.waitForMessage(recipientMbox, "in:inbox subject:'" + messageSubject + "'");

        // Check for new mail notification
        TestUtil.waitForMessage(notificationMbox, "in:inbox subject:'" + notificationSubject + "'");
        List<ZMessage> messages = TestUtil.search(notificationMbox, "in:inbox content:'" + notificationBody + "'");
        assertEquals("found new mail notification with wrong body", 1, messages.size());

        messages = TestUtil.search(notificationMbox, "in:inbox subject:'" + notificationSubject + "'");
        assertEquals("found new mail notification with wrong subject", 1, messages.size());

        messages = TestUtil.search(notificationMbox, "in:inbox from:" + notifierAddress);
        assertEquals("found new mail notification with wrong sender", 1, messages.size());
    }

    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        // Clean up temporary data
        for (String userName : ALL_TEST_USERS) {
            if (TestUtil.accountExists(userName)) {
                TestUtil.deleteAccount(userName);
            }
        }
    }
}
