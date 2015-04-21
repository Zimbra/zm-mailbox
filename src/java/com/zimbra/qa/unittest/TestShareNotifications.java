package com.zimbra.qa.unittest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
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
/**
 * @author Greg Solovyev
 * bug 97791
 */
public class TestShareNotifications  {
    private String SENDER_NAME="sender_TestShareNotifications";
    private String RECIPIENT_NAME="recip_TestShareNotifications";
    private String CAL_NAME = "cal1_TestShareNotifications";
    
    @Before
    public void setUp() throws Exception {
        cleanUp();
    }
    
    @After
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
            
    @Test            
    public void testGetShareNotifications() throws Exception {
        Account senderAccount = TestUtil.createAccount(SENDER_NAME);
        Account recipientAccount = TestUtil.createAccount(RECIPIENT_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(SENDER_NAME);
        ZFolder newCal = TestUtil.createFolder(mbox, CAL_NAME, ZFolder.View.appointment);
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
}
