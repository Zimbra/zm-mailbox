package com.zimbra.qa.unittest;

import java.util.List;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
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

import junit.framework.TestCase;

public class TestShareNotifications extends TestCase {
    private String SENDER_NAME="sender_TestShareNotifications";
    private String RECIPIENT_NAME="recip_TestShareNotifications";
    private String CAL_NAME1 = "cal1_TestShareNotifications";
    private String CAL_NAME2 = "cal2_TestShareNotifications";
    private String CONTACTS_NAME1 = "contacts1_TestShareNotifications";

    public void setUp() throws Exception {

        cleanUp();
    }
    
    public void tearDown() throws Exception {
        cleanUp();
    }
    
    private void cleanUp() throws Exception {
        if(TestUtil.accountExists(SENDER_NAME)) {
            TestUtil.deleteAccount(SENDER_NAME);
        }
        if(TestUtil.accountExists(RECIPIENT_NAME)) {
            TestUtil.deleteAccount(RECIPIENT_NAME);
        }
    }
            
    public void testCalendarShareNotification() throws Exception {
        Account senderAccount = TestUtil.createAccount(SENDER_NAME);
        Account recipientAccount = TestUtil.createAccount(RECIPIENT_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);
        ZFolder newCal = TestUtil.createFolder(mbox, CAL_NAME1, ZFolder.View.appointment);
        String  calendarId = newCal.getId();
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
        
        ZMailbox recipientMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        GetShareNotificationsRequest getShareNotificationReq = new GetShareNotificationsRequest();
        GetShareNotificationsResponse sharesResp = recipientMbox.invokeJaxb(getShareNotificationReq);
        assertNotNull("GetShareNotificationsResponse is null", sharesResp);
        List<ShareNotificationInfo> shares = sharesResp.getShares();
        assertTrue("should have exactly one share notification", shares.size() == 1);
        assertNotNull("share grantor is null", shares.get(0).getGrantor());
        assertTrue("share grantor is not " + senderAccount.getMail(), senderAccount.getMail().equalsIgnoreCase(shares.get(0).getGrantor().getEmail()));
    }
    
    public void testMultipleCalendarShareNotifications() throws Exception {
        Account senderAccount = TestUtil.createAccount(SENDER_NAME);
        Account recipientAccount = TestUtil.createAccount(RECIPIENT_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);
        //create and share the first calendar
        ZFolder newCal = TestUtil.createFolder(mbox, CAL_NAME1, ZFolder.View.appointment);
        String  calendarId = newCal.getId();
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
        
        //create and share the second calendar
        ZFolder newCal2 = TestUtil.createFolder(mbox, CAL_NAME2, ZFolder.View.appointment);
        String  calendarId2 = newCal2.getId();
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
        
        ZMailbox recipientMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        GetShareNotificationsRequest getShareNotificationReq = new GetShareNotificationsRequest();
        GetShareNotificationsResponse sharesResp = recipientMbox.invokeJaxb(getShareNotificationReq);
        assertNotNull("GetShareNotificationsResponse is null", sharesResp);
        List<ShareNotificationInfo> shares = sharesResp.getShares();
        assertTrue("should have exactly two share notification", shares.size() == 2);
        assertNotNull("share grantor is null", shares.get(0).getGrantor());
        assertTrue("share grantor is not " + senderAccount.getMail(), senderAccount.getMail().equalsIgnoreCase(shares.get(0).getGrantor().getEmail()));
    }
    
    public void testContactAndCalendarShareNotifications() throws Exception {
        Account senderAccount = TestUtil.createAccount(SENDER_NAME);
        Account recipientAccount = TestUtil.createAccount(RECIPIENT_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);
        //create and share the first calendar
        ZFolder newCal = TestUtil.createFolder(mbox, CAL_NAME1, ZFolder.View.appointment);
        String  calendarId = newCal.getId();
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
        
        //create and share the second calendar
        ZFolder newContactsFolder = TestUtil.createFolder(mbox, CONTACTS_NAME1, ZFolder.View.contact);
        String  contactsFolderId = newContactsFolder.getId();
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
        
        ZMailbox recipientMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        GetShareNotificationsRequest getShareNotificationReq = new GetShareNotificationsRequest();
        GetShareNotificationsResponse sharesResp = recipientMbox.invokeJaxb(getShareNotificationReq);
        assertNotNull("GetShareNotificationsResponse is null", sharesResp);
        List<ShareNotificationInfo> shares = sharesResp.getShares();
        assertTrue("should have exactly two share notification", shares.size() == 2);
        assertNotNull("share grantor is null", shares.get(0).getGrantor());
        assertTrue("share grantor is not " + senderAccount.getMail(), senderAccount.getMail().equalsIgnoreCase(shares.get(0).getGrantor().getEmail()));
    }
    
    public void testContactShareNotification() throws Exception {
        Account senderAccount = TestUtil.createAccount(SENDER_NAME);
        Account recipientAccount = TestUtil.createAccount(RECIPIENT_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);
        //create and share the first calendar
        ZFolder newContactsFolder = TestUtil.createFolder(mbox, CAL_NAME1, ZFolder.View.contact);
        String  contactsFolderId = newContactsFolder.getId();
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
        
        ZMailbox recipientMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        GetShareNotificationsRequest getShareNotificationReq = new GetShareNotificationsRequest();
        GetShareNotificationsResponse sharesResp = recipientMbox.invokeJaxb(getShareNotificationReq);
        assertNotNull("GetShareNotificationsResponse is null", sharesResp);
        List<ShareNotificationInfo> shares = sharesResp.getShares();
        assertTrue("should have exactly one share notification", shares.size() == 1);
        assertNotNull("share grantor is null", shares.get(0).getGrantor());
        assertTrue("share grantor is not " + senderAccount.getMail(), senderAccount.getMail().equalsIgnoreCase(shares.get(0).getGrantor().getEmail()));
    }
}
