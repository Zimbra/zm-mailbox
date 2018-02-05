package com.zimbra.cs.service.admin;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.mail.GetContactBackupList;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.ContactBackupRequest;
import com.zimbra.soap.admin.message.ContactBackupResponse;
import com.zimbra.soap.mail.message.GetContactBackupListRequest;
import com.zimbra.soap.mail.message.GetContactBackupListResponse;

import junit.framework.Assert;

public class ContactBackupApiTest {
    private static Provisioning prov = null;
    private static final String DOMAIN_NAME = "zimbra.com";
    private static final String BUG_NUMBER = "zcs3594";
    private static final String ADMIN = "admin_" + BUG_NUMBER + "@" + DOMAIN_NAME;
    private static final String TEST1 = "test1_" + BUG_NUMBER + "@"+ DOMAIN_NAME;
    private static final String TEST2 = "test2_" + BUG_NUMBER + "@"+ DOMAIN_NAME;
    private static final String TEST3 = "test3_" + BUG_NUMBER + "@"+ DOMAIN_NAME;
    private static final String TEST4 = "test4_" + BUG_NUMBER + "@"+ DOMAIN_NAME;
    private static Account admin = null;
    private static Account test1 = null;
    private static Mailbox mboxAdmin = null;
    private static Mailbox mboxTest1 = null;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraIsAdminAccount, true); // set admin account
        prov.createAccount(ADMIN, "secret", attrs);
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraFeatureContactBackupEnabled, true); // set contact backup feature enabled

        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount(TEST1, "test123", attrs);
        admin = prov.getAccountByName(ADMIN);
        test1 = prov.getAccountByName(TEST1);
        mboxAdmin = MailboxManager.getInstance().getMailboxByAccountId(admin.getId());
        mboxTest1 = MailboxManager.getInstance().getMailboxByAccountId(test1.getId());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testContactBackupApiWithStart() throws Exception {
        Folder folder = mboxTest1.createFolder(null, "Briefcase/ContactsBackup",
            new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT));
        OperationContext octxt = new OperationContext(test1);
        // Upload the contacts backup file to ContactsBackup folder in briefcase
        mboxTest1.createDocument(octxt, folder.getId(), "backup_dummy_test1.tgz",
            MimeConstants.CT_APPLICATION_ZIMBRA_DOC, "author", "description",
            new ByteArrayInputStream("dummy data".getBytes()));
        mboxTest1.createDocument(octxt, folder.getId(), "backup_dummy_test2.tgz",
            MimeConstants.CT_APPLICATION_ZIMBRA_DOC, "author", "description",
            new ByteArrayInputStream("dummy data".getBytes()));

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "test2");
        fields.put(ContactConstants.A_lastName, "testing");
        fields.put(ContactConstants.A_email, TEST2);
        mboxTest1.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);
        fields.put(ContactConstants.A_firstName, "test3");
        fields.put(ContactConstants.A_lastName, "testing");
        fields.put(ContactConstants.A_email, TEST3);
        mboxTest1.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);
        fields.put(ContactConstants.A_firstName, "test4");
        fields.put(ContactConstants.A_lastName, "testing");
        fields.put(ContactConstants.A_email, TEST4);
        mboxTest1.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        ContactBackupRequest cbReq = new ContactBackupRequest();
        cbReq.addId(1);
        Element request = JaxbUtil.jaxbToElement(cbReq);
        Element response  = null;
        ContactBackup cb = new MockContactBackup();

        try {
            response = cb.handle(request, ServiceTestUtil.getRequestContext(admin));
        } catch (ServiceException se) {
            Assert.fail("ServiceException must not be thrown.");
        }
        if (response == null) {
            Assert.fail("Response must be received.");
        }
        ContactBackupResponse cbResp = JaxbUtil.elementToJaxb(response);
        Assert.assertNull(cbResp.getDoneIds());
        Assert.assertNull(cbResp.getSkippedIds());
        Assert.assertEquals(cbResp.toString(), "ContactBackupResponse{}");
    }

    @Test
    public void verifyContactBackupWithStart() throws Exception {
        GetContactBackupListRequest gcblReq = new GetContactBackupListRequest();
        Element request = JaxbUtil.jaxbToElement(gcblReq);
        Element response  = null;
        try {
            response = new GetContactBackupList().handle(request, ServiceTestUtil.getRequestContext(test1));
        } catch (ServiceException se) {
            Assert.fail("ServiceException must not be thrown.");
        }
        GetContactBackupListResponse gclblResp = JaxbUtil.elementToJaxb(response);
        Assert.assertEquals(gclblResp.toString(), "GetContactBackupListResponse{backup=[backup_dummy_test1.tgz, backup_dummy_test2.tgz]}");
        List<String> backups = gclblResp.getBackup();
        Assert.assertEquals(backups.size(), 2);
    }

    @Test
    public void verifyContactBackupStatusWithException() throws Exception {
        ContactBackupRequest cbReq = new ContactBackupRequest();
        cbReq.setTask("start");
        Element request = JaxbUtil.jaxbToElement(cbReq);
        try {
            new MockContactBackup().handle(request, ServiceTestUtil.getRequestContext(test1));
            Assert.fail("ServiceException must  be thrown.");
        } catch (ServiceException se) {
            Assert.assertTrue(se.getMessage().contains("is not currently running"));
        }
    }

    @Test
    public void verifyContactBackupStatusWithDoneAndSkipIds() throws Exception {
        ContactBackupRequest cbReq = new ContactBackupRequest();
        cbReq.setTask("status");
        Element request = JaxbUtil.jaxbToElement(cbReq);
        try {
            Element response  = new MockContactBackup().handle(request, ServiceTestUtil.getRequestContext(test1));
            ContactBackupResponse cbResp = JaxbUtil.elementToJaxb(response);
            Assert.assertEquals(cbResp.toString(), "ContactBackupResponse{doneIds=, doneId=4, doneId=5, doneId=6, skippedIds=, skippedId=2, skippedId=9}");
            List<Integer> doneIds = cbResp.getDoneIds();
            List<Integer> skippedIds = cbResp.getSkippedIds();
            Assert.assertEquals(doneIds.size(), 3);
            Assert.assertEquals(skippedIds.size(), 2);
        } catch (ServiceException se) {
            se.printStackTrace();
            Assert.fail(" No exception should be thrown");
        }
    }

    @Test
    public void verifyContactBackupStopWithDoneAndSkipIds() throws Exception {
        ContactBackupRequest cbReq = new ContactBackupRequest();
        cbReq.setTask("stop");
        Element request = JaxbUtil.jaxbToElement(cbReq);
        try {
            Element response  = new MockContactBackup().handle(request, ServiceTestUtil.getRequestContext(test1));
            ContactBackupResponse cbResp = JaxbUtil.elementToJaxb(response);
            Assert.assertEquals(cbResp.toString(), "ContactBackupResponse{doneIds=, doneId=4, doneId=5, doneId=6, skippedIds=, skippedId=2, skippedId=9}");
            List<Integer> doneIds = cbResp.getDoneIds();
            List<Integer> skippedIds = cbResp.getSkippedIds();
            Assert.assertEquals(doneIds.size(), 3);
            Assert.assertEquals(skippedIds.size(), 2);
        } catch (ServiceException se) {
            se.printStackTrace();
            Assert.fail(" No exception should be thrown");
        }
    }

    public class MockContactBackup extends ContactBackup {
        @Override
        protected void getContactBackupStatus() throws ServiceException {
                this.doneIds = new ArrayList<Integer>();
                this.doneIds.add(4);
                this.doneIds.add(5);
                this.doneIds.add(6);

                this.skippedIds = new ArrayList<Integer>();
                this.skippedIds.add(2);
                this.skippedIds.add(9);
        }

        @Override
        protected void startContactBackup(List<Integer> ids) throws ServiceException {
            if (ids == null) {
                throw ServiceException.NOT_IN_PROGRESS(null, "ContactBackup is not running.");
            }
        }

        @Override
        protected void stopContactBackup() throws ServiceException {
            this.doneIds = new ArrayList<Integer>();
            this.doneIds.add(4);
            this.doneIds.add(5);
            this.doneIds.add(6);

            this.skippedIds = new ArrayList<Integer>();
            this.skippedIds.add(2);
            this.skippedIds.add(9);
        }
    }
}
