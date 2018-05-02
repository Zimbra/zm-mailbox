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
package com.zimbra.qa.unittest;

import java.util.List;

import junit.framework.Assert;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.mail.message.FolderActionRequest;
import com.zimbra.soap.mail.message.FolderActionResponse;
import com.zimbra.soap.mail.message.GetShareNotificationsRequest;
import com.zimbra.soap.mail.message.GetShareNotificationsResponse;
import com.zimbra.soap.mail.message.SendShareNotificationRequest;
import com.zimbra.soap.mail.message.SendShareNotificationResponse;
import com.zimbra.soap.mail.type.ActionGrantSelector;
import com.zimbra.soap.mail.type.EmailAddrInfo;
import com.zimbra.soap.mail.type.FolderActionSelector;
import com.zimbra.soap.mail.type.ShareNotificationInfo;

public class TestShareNotifications {
    private String SENDER_NAME = "sender_TestShareNotifications";
    private String RECIPIENT_NAME = "recip_TestShareNotifications";
    private String CAL_NAME1 = "cal1_TestShareNotifications";
    private String CAL_NAME2 = "cal2_TestShareNotifications";
    private String CONTACTS_NAME1 = "contacts1_TestShareNotifications";

    private Account senderAccount;
    private Account recipientAccount;

    @Before
    public void setUp() throws Exception {
        cleanUp();
        senderAccount = TestUtil.createAccount(SENDER_NAME);
        recipientAccount = TestUtil.createAccount(RECIPIENT_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        if (TestUtil.accountExists(SENDER_NAME)) {
            TestUtil.deleteAccount(SENDER_NAME);
        }
        if (TestUtil.accountExists(RECIPIENT_NAME)) {
            TestUtil.deleteAccount(RECIPIENT_NAME);
        }
    }

    @Test
    public void testCalendarShareNotification() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);

        // check that there are no share notifications in the recipient's mailbox
        ZMailbox recipientMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        List<ShareNotificationInfo> shares = waitForShareNotifications(recipientMbox, 0, 100);
        assertEquals("Recipient should have exactly 0 share notification", 0, shares.size());

        ZFolder newCal = TestUtil.createFolder(mbox, CAL_NAME1, ZFolder.View.appointment);
        String calendarId = newCal.getId();
        FolderActionSelector action = new FolderActionSelector(calendarId, "grant");
        ActionGrantSelector grant = new ActionGrantSelector("r", "usr");
        grant.setDisplayName(recipientAccount.getName());
        grant.setPassword("");
        action.setGrant(grant);
        FolderActionRequest folderActionReq = new FolderActionRequest(action);
        FolderActionResponse folderActionResp = mbox.invokeJaxb(folderActionReq);
        assertNotNull("FolderActionResponse is null", folderActionResp);
        SendShareNotificationRequest shareNotificationReq = new SendShareNotificationRequest();
        shareNotificationReq.setItem(new com.zimbra.soap.type.Id(calendarId));
        shareNotificationReq.addEmailAddress(new EmailAddrInfo(recipientAccount.getMail()));
        SendShareNotificationResponse resp = mbox.invokeJaxb(shareNotificationReq);
        assertNotNull("ShareNotificationResponse is null", resp);

        shares = waitForShareNotifications(recipientMbox, 1, 1000);
        assertTrue("should have exactly one share notification", shares.size() == 1);
        assertNotNull("share grantor is null", shares.get(0).getGrantor());
        assertTrue("share grantor is not " + senderAccount.getMail(),
                senderAccount.getMail().equalsIgnoreCase(shares.get(0).getGrantor().getEmail()));
    }

    @Test
    public void testMultipleCalendarShareNotifications() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);

        // check that there are no share notifications in the recipient's mailbox
        ZMailbox recipientMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        List<ShareNotificationInfo> shares = waitForShareNotifications(recipientMbox, 0, 100);
        assertEquals("Recipient should have exactly 0 share notification", 0, shares.size());

        // create and share the first calendar
        ZFolder newCal = TestUtil.createFolder(mbox, CAL_NAME1, ZFolder.View.appointment);
        String calendarId = newCal.getId();
        FolderActionSelector action = new FolderActionSelector(calendarId, "grant");
        ActionGrantSelector grant = new ActionGrantSelector("r", "usr");
        grant.setDisplayName(recipientAccount.getName());
        grant.setPassword("");
        action.setGrant(grant);
        FolderActionRequest folderActionReq = new FolderActionRequest(action);
        FolderActionResponse folderActionResp = mbox.invokeJaxb(folderActionReq);
        assertNotNull("FolderActionResponse is null", folderActionResp);
        SendShareNotificationRequest shareNotificationReq = new SendShareNotificationRequest();
        shareNotificationReq.setItem(new com.zimbra.soap.type.Id(calendarId));
        shareNotificationReq.addEmailAddress(new EmailAddrInfo(recipientAccount.getMail()));
        SendShareNotificationResponse resp = mbox.invokeJaxb(shareNotificationReq);
        assertNotNull("ShareNotificationResponse is null", resp);

        // create and share the second calendar
        ZFolder newCal2 = TestUtil.createFolder(mbox, CAL_NAME2, ZFolder.View.appointment);
        String calendarId2 = newCal2.getId();
        action = new FolderActionSelector(calendarId2, "grant");
        grant = new ActionGrantSelector("r", "usr");
        grant.setDisplayName(recipientAccount.getName());
        grant.setPassword("");
        action.setGrant(grant);
        folderActionReq = new FolderActionRequest(action);
        folderActionResp = mbox.invokeJaxb(folderActionReq);
        assertNotNull("FolderActionResponse is null", folderActionResp);
        shareNotificationReq = new SendShareNotificationRequest();
        shareNotificationReq.setItem(new com.zimbra.soap.type.Id(calendarId2));
        shareNotificationReq.addEmailAddress(new EmailAddrInfo(recipientAccount.getMail()));
        resp = mbox.invokeJaxb(shareNotificationReq);
        assertNotNull("ShareNotificationResponse is null", resp);

        shares = waitForShareNotifications(recipientMbox, 2, 1000);
        assertEquals("should have exactly two share notification", 2, shares.size());
        assertNotNull("share grantor is null", shares.get(0).getGrantor());
        assertTrue("share grantor is not " + senderAccount.getMail(),
                senderAccount.getMail().equalsIgnoreCase(shares.get(0).getGrantor().getEmail()));
    }

    @Test
    public void testContactAndCalendarShareNotifications() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);

        // check that there are no share notifications in the recipient's mailbox
        ZMailbox recipientMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        List<ShareNotificationInfo> shares = waitForShareNotifications(recipientMbox, 0, 100);
        assertEquals("Recipient should have exactly 0 share notification", 0, shares.size());

        // create and share the first calendar
        ZFolder newCal = TestUtil.createFolder(mbox, CAL_NAME1, ZFolder.View.appointment);
        String calendarId = newCal.getId();
        FolderActionSelector action = new FolderActionSelector(calendarId, "grant");
        ActionGrantSelector grant = new ActionGrantSelector("r", "usr");
        grant.setDisplayName(recipientAccount.getName());
        grant.setPassword("");
        action.setGrant(grant);
        FolderActionRequest folderActionReq = new FolderActionRequest(action);
        FolderActionResponse folderActionResp = mbox.invokeJaxb(folderActionReq);
        assertNotNull("FolderActionResponse is null", folderActionResp);
        SendShareNotificationRequest shareNotificationReq = new SendShareNotificationRequest();
        shareNotificationReq.setItem(new com.zimbra.soap.type.Id(calendarId));
        shareNotificationReq.addEmailAddress(new EmailAddrInfo(recipientAccount.getMail()));
        SendShareNotificationResponse resp = mbox.invokeJaxb(shareNotificationReq);
        assertNotNull("ShareNotificationResponse is null", resp);

        // create and share the second calendar
        ZFolder newContactsFolder = TestUtil.createFolder(mbox, CONTACTS_NAME1, ZFolder.View.contact);
        String contactsFolderId = newContactsFolder.getId();
        action = new FolderActionSelector(contactsFolderId, "grant");
        grant = new ActionGrantSelector("r", "usr");
        grant.setDisplayName(recipientAccount.getName());
        grant.setPassword("");
        action.setGrant(grant);
        folderActionReq = new FolderActionRequest(action);
        folderActionResp = mbox.invokeJaxb(folderActionReq);
        assertNotNull("FolderActionResponse is null", folderActionResp);
        shareNotificationReq = new SendShareNotificationRequest();
        shareNotificationReq.setItem(new com.zimbra.soap.type.Id(contactsFolderId));
        shareNotificationReq.addEmailAddress(new EmailAddrInfo(recipientAccount.getMail()));
        resp = mbox.invokeJaxb(shareNotificationReq);
        assertNotNull("ShareNotificationResponse is null", resp);

        shares = waitForShareNotifications(recipientMbox, 2, 1000);
        assertEquals("should have exactly two share notification", 2, shares.size());
        assertNotNull("share grantor is null", shares.get(0).getGrantor());
        assertTrue("share grantor is not " + senderAccount.getMail(),
                senderAccount.getMail().equalsIgnoreCase(shares.get(0).getGrantor().getEmail()));
    }

    @Test
    public void testContactShareNotification() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);

        // check that there are no share notifications in the recipient's mailbox
        ZMailbox recipientMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        List<ShareNotificationInfo> shares = waitForShareNotifications(recipientMbox, 0, 100);
        assertEquals("Recipient should have exactly 0 share notification", 0, shares.size());

        // create and share the first calendar
        ZFolder newContactsFolder = TestUtil.createFolder(mbox, CAL_NAME1, ZFolder.View.contact);
        String contactsFolderId = newContactsFolder.getId();
        FolderActionSelector action = new FolderActionSelector(contactsFolderId, "grant");
        ActionGrantSelector grant = new ActionGrantSelector("r", "usr");
        grant.setDisplayName(recipientAccount.getName());
        grant.setPassword("");
        action.setGrant(grant);
        FolderActionRequest folderActionReq = new FolderActionRequest(action);
        FolderActionResponse folderActionResp = mbox.invokeJaxb(folderActionReq);
        assertNotNull("FolderActionResponse is null", folderActionResp);
        SendShareNotificationRequest shareNotificationReq = new SendShareNotificationRequest();
        shareNotificationReq.setItem(new com.zimbra.soap.type.Id(contactsFolderId));
        shareNotificationReq.addEmailAddress(new EmailAddrInfo(recipientAccount.getMail()));
        SendShareNotificationResponse resp = mbox.invokeJaxb(shareNotificationReq);
        assertNotNull("ShareNotificationResponse is null", resp);

        shares = waitForShareNotifications(recipientMbox, 1, 1000);
        assertEquals("should have exactly one share notification", 1, shares.size());
        assertNotNull("share grantor is null", shares.get(0).getGrantor());
        assertTrue("share grantor is not " + senderAccount.getMail(),
                senderAccount.getMail().equalsIgnoreCase(shares.get(0).getGrantor().getEmail()));
    }

    static List<ShareNotificationInfo> waitForShareNotifications(ZMailbox mbox, int numExpected, int timeout_millis)
            throws ServiceException {
        int orig_timeout_millis = timeout_millis;
        List<ShareNotificationInfo> shares = Lists.newArrayListWithExpectedSize(0);
        while (timeout_millis > 0) {
            GetShareNotificationsRequest getShareNotificationReq = new GetShareNotificationsRequest();
            GetShareNotificationsResponse sharesResp = mbox.invokeJaxb(getShareNotificationReq);
            assertNotNull("GetShareNotificationsResponse is null", sharesResp);
            shares = sharesResp.getShares();
            if (shares.size() == numExpected) {
                return shares;
            }
            if (shares.size() > numExpected) {
                Assert.fail("Unexpected number of share notifications (" + shares.size() + ")");
            }
            try {
                if (timeout_millis > 100) {
                    Thread.sleep(100);
                    timeout_millis = timeout_millis - 100;
                } else {
                    Thread.sleep(timeout_millis);
                    timeout_millis = 0;

                }
            } catch (InterruptedException e) {
                ZimbraLog.test.debug("sleep got interrupted", e);
            }
        }
        if (numExpected > 0) {
            Assert.fail(String.format("Waited for %d share notifications for %d millis. Found %d share notifications",
                    numExpected, orig_timeout_millis, shares.size()));
        }
        return shares;
    }
}
