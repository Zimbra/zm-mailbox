/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.client.ZContact;
import com.zimbra.client.ZFeatures;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGetInfoResult;
import com.zimbra.client.ZIdHit;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ContactSortBy;
import com.zimbra.client.ZMailbox.Fetch;
import com.zimbra.client.ZMailbox.GalEntryType;
import com.zimbra.client.ZMailbox.OpenIMAPFolderParams;
import com.zimbra.client.ZMailbox.Options;
import com.zimbra.client.ZMailbox.ZActionResult;
import com.zimbra.client.ZMailbox.ZAppointmentResult;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZSearchGalResult;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessageHit;
import com.zimbra.client.ZMountpoint;
import com.zimbra.client.ZPrefs;
import com.zimbra.client.ZSearchFolder;
import com.zimbra.client.ZSearchHit;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.client.ZSignature;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.OpContext;
import com.zimbra.common.mailbox.ZimbraFetchMode;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.mailbox.ZimbraQueryHit;
import com.zimbra.common.mailbox.ZimbraQueryHitResults;
import com.zimbra.common.mailbox.ZimbraSearchParams;
import com.zimbra.common.mailbox.ZimbraSortBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.imap.RemoteImapMailboxStore;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.mail.message.ItemActionResponse;
import com.zimbra.soap.mail.message.OpenIMAPFolderResponse;
import com.zimbra.soap.mail.type.ImapMessageInfo;
import com.zimbra.soap.type.SearchSortBy;

/* Don't think class length is an issue for tests.  More useful to have a single test that can
 * be run to exercise an area than to split it up and have to run several.
 */
@SuppressWarnings("PMD.ExcessiveClassLength")
public class TestZClient {

    @Rule
    public TestName testInfo = new TestName();
    private static String NAME_PREFIX;
    private static String RECIPIENT_USER_NAME;
    private static String USER_NAME;
    private static String FOLDER_NAME;

    @Before
    public void setUp()
    throws Exception {
        NAME_PREFIX = String.format("%s-%d", testInfo.getMethodName(), (int)Math.abs(Math.random()*100));
        RECIPIENT_USER_NAME = NAME_PREFIX + "_user2";
        USER_NAME = NAME_PREFIX + "_user1";
        FOLDER_NAME = String.format("%s-Folder", this.getClass().getSimpleName());
        if (!TestUtil.fromRunUnitTests) {
            TestUtil.cliSetup();
        }
        cleanUp();
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(RECIPIENT_USER_NAME);
    }

    @After
    public void cleanUp() throws Exception {
            TestUtil.deleteAccountIfExists(USER_NAME);
            TestUtil.deleteAccountIfExists(RECIPIENT_USER_NAME);
    }

    /**
     * Confirms that the prefs accessor works (bug 51384).
     */
    @Test
    public void testPrefs() throws Exception {
        Account account = TestUtil.getAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZPrefs prefs = mbox.getPrefs();
        assertEquals(account.getPrefLocale(), prefs.getLocale());
    }

    /**
     * Confirms that the features accessor doesn't throw NPE (bug 51384).
     */
    @Test
    public void testFeatures() {
        try {
            ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
            ZFeatures features = mbox.getFeatures();
            features.getPop3Enabled();
        } catch (Exception e) {
            fail(String.format("Unexpected exception %s thrown", e));
        }
    }

    @Test
    public void testChangePassword() throws Exception {
        Account account = TestUtil.getAccount(USER_NAME);
        Options options = new Options();
        options.setAccount(account.getName());
        options.setAccountBy(AccountBy.name);
        options.setPassword(TestUtil.DEFAULT_PASSWORD);
        options.setNewPassword("test456");
        options.setUri(TestUtil.getSoapUrl());
        ZMailbox.changePassword(options);

        try {
            TestUtil.getZMailbox(USER_NAME);
        } catch (SoapFaultException e) {
            assertEquals(AuthFailedServiceException.AUTH_FAILED, e.getCode());
        }
    }

    /**
     * Test for fix to ZCS-4341 code similar to ZJspSession.getRestMailbox
     * Prior to fix, fails with:
     *     Exception thrown getting ZMailbox:no valid authtoken present
     */
    @Test
    public void simulateGetRestMailbox() throws ServiceException, AuthTokenException {
        Account acct = TestUtil.getAccount(USER_NAME);
        AuthToken authToken = AuthProvider.getAuthToken(acct);
        ZMailbox.Options options = new ZMailbox.Options(authToken.getEncoded(),
                    AccountUtil.getSoapUri(acct));
        options.setNoSession(true);
        options.setAuthAuthToken(true);  /* validate */
        // to get a csrf token
        options.setCsrfSupported(true);
        options.setTargetAccount(acct.getId());
        options.setTargetAccountBy(Key.AccountBy.id);
        options.setClientIp("client-ip-addr");
        try {
            ZMailbox box = ZMailbox.getMailbox(options);
            assertNotNull("ZMailbox object", box);
        } catch (ServiceException se) {
            ZimbraLog.test.info("Thrown getting ZMailbox", se);
            fail("Exception thrown getting ZMailbox:" + se.getMessage());
        }
    }

    @Test
    public void testGetLastItemIdInMailbox() throws Exception {
        int numMessages = 10;
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        int lItemIdZMbox0 = zmbox.getLastItemIdInMailbox();
        for (int i=1; i<=numMessages; ++i) {
            TestUtil.addMessage(zmbox, String.format("test message %d", i));
        }
        int lItemIdZMbox1 = zmbox.getLastItemIdInMailbox();
        assertEquals(lItemIdZMbox0 + numMessages, lItemIdZMbox1);
    }

    /**
     * Confirms that the {@code List} of signatures returned by {@link ZMailbox#getSignatures}
     * is modifiable (see bug 51842).
     */
    @Test
    public void testModifySignatures() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZSignature> signatures = mbox.getSignatures();
        try {
            signatures.set(signatures.size(), null);
        } catch (IndexOutOfBoundsException e) {
            // Not UnsupportedOperationException, so we're good.
        } catch (Exception e) {
            fail(String.format("Unexpected exception %s thrown", e));
        }

        ZGetInfoResult info = mbox.getAccountInfo(true);
        signatures = info.getSignatures();
        try {
            signatures.set(signatures.size(), null);
        } catch (IndexOutOfBoundsException e) {
            // Not UnsupportedOperationException, so we're good.
        } catch (Exception e) {
            fail(String.format("Unexpected exception %s thrown", e));
        }
    }

    @Test
    public void testCopyItemAction() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String sender = TestUtil.getAddress(USER_NAME);
        String recipient = TestUtil.getAddress(RECIPIENT_USER_NAME);
        String subject = NAME_PREFIX + " testCopyItemAction";
        String content = new MessageBuilder().withSubject(subject).withFrom(sender).withToRecipient(recipient).create();

        // add a msg flagged as sent; filterSent=TRUE
        mbox.addMessage(Integer.toString(Mailbox.ID_FOLDER_DRAFTS), null, null, System.currentTimeMillis(), content, false, false);
        ZMessage msg = TestUtil.waitForMessage(mbox, "in:drafts " + subject);
        List<ItemIdentifier> ids = Lists.newArrayListWithCapacity(1);
        ids.add(new ItemIdentifier(msg.getId(), null));
        ItemActionResponse resp = mbox.copyItemAction(new ItemIdentifier(null, Mailbox.ID_FOLDER_SENT), ids);
        assertNotNull("item action response should not be null", resp);
        assertNotNull("action should not be null", resp.getAction());
        assertNotNull("action id should not be null", resp.getAction().getId());

        ZMessage copiedMessage = mbox.getMessageById(resp.getAction().getId());
        assertNotNull("copied msg should not be null", copiedMessage);
        assertEquals("subject of copied msg", subject, copiedMessage.getSubject());
        //msg.getId()

    }

    @Test
    public void testImapUIDforMsg() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        String sender = TestUtil.getAddress(USER_NAME);
        String recipient = TestUtil.getAddress(RECIPIENT_USER_NAME);
        String subject = NAME_PREFIX + " imapUID";
        String content = new MessageBuilder().withSubject(subject).withFrom(sender).withToRecipient(recipient).create();

        zmbox.addMessage(Integer.toString(Mailbox.ID_FOLDER_DRAFTS), null, null,
                System.currentTimeMillis(), content, false, false);
        ZMessage msg = TestUtil.waitForMessage(zmbox, "in:drafts " + subject);
        assertEquals("IMAP UID should be same as ID", msg.getIdInMailbox(), msg.getImapUid());
        Element reqEl = zmbox.newRequestElement(MailConstants.GET_MSG_REQUEST);
        Element msgEl = reqEl.addUniqueElement(MailConstants.E_MSG);
        msgEl.addAttribute(MailConstants.A_ID, msg.getIdInMailbox());
        // Specifically do NOT do reqEl.addAttribute(MailConstants.A_WANT_IMAP_UID, true);
        Element respEl = zmbox.invoke(reqEl);
        ZMessage zmsg2 = new ZMessage(respEl.getElement(MailConstants.E_MSG), zmbox);
        // The IMAP UID should be gotten via a refetch of the contact - asking for the IMAP UID
        assertEquals(String.format(
                "IMAP UID '%s' should be same as ID '%s' even though didn't request IMAP UID in original SOAP request",
                zmsg2.getImapUid(), zmsg2.getIdInMailbox()), zmsg2.getImapUid(), zmsg2.getIdInMailbox());
    }

    @Test
    public void testImapUIDForGalContact() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZSearchGalResult zsgr = mbox.autoCompleteGal(USER_NAME /* query */, GalEntryType.all, 20 /* limit */);
        assertNotNull("ZSearchGalResult", zsgr);
        List<ZContact> contacts = zsgr.getContacts();
        assertNotNull("ZSearchGalResult.getContacts()", contacts);
        assertTrue(String.format("Contacts size %s should be greater than 0", contacts.size()), contacts.size() > 0);
        for (ZContact contact : contacts) {
            assertEquals("IMAP UID should be zero for Gal contact", 0, contact.getImapUid());
            assertEquals("ModifiedSequence should be zero for Gal contact", 0, contact.getModifiedSequence());
        }
    }

    @Test
    public void testImapUIDForNonStandardContact() throws ServiceException {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Contact contact = TestUtil.createContactInDefaultFolder(mbox, "testzclient@example.com");
        ItemIdentifier contactId = ItemIdentifier.fromAccountIdAndItemId(zmbox.getAccountId(), contact.getId());
        Element reqEl = zmbox.newRequestElement(MailConstants.GET_CONTACTS_REQUEST);
        // Specifically do NOT do reqEl.addAttribute(MailConstants.A_WANT_IMAP_UID, true);
        reqEl.addNonUniqueElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, contactId.id);
        Element respEl = zmbox.invoke(reqEl);
        ZContact zcontact = new ZContact(respEl.getElement(MailConstants.E_CONTACT), zmbox);
        assertNotNull("ZContact object", zcontact);
        // The IMAP UID should be gotten via a refetch of the contact - asking for the IMAP UID
        assertEquals("Expected=ZContact ID Actual=ZContact IMAP UID", zcontact.getIdInMailbox(), zcontact.getImapUid());
        assertEquals("IMAP UID Expected=Contact Actual=ZContact", contact.getImapUid(), zcontact.getImapUid());
        assertEquals("ModifiedSequence Expected=Contact Actual=ZContact",
                contact.getModifiedSequence(), zcontact.getModifiedSequence());
    }

    @Test
    public void testZMailboxGetContact() throws ServiceException {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Contact contact = TestUtil.createContactInDefaultFolder(mbox, "testzclient2@example.com");
        ItemIdentifier contactId = ItemIdentifier.fromAccountIdAndItemId(zmbox.getAccountId(), contact.getId());
        ZContact zcontact = zmbox.getContact(contactId);
        assertNotNull("ZContact object", zcontact);
        assertEquals("Expected=ZContact ID Actual=ZContact IMAP UID", zcontact.getIdInMailbox(), zcontact.getImapUid());
        assertEquals("IMAP UID Expected=Contact Actual=ZContact", contact.getImapUid(), zcontact.getImapUid());
        assertEquals("ModifiedSequence Expected=Contact Actual=ZContact",
                contact.getModifiedSequence(), zcontact.getModifiedSequence());
    }

    @Test
    public void testZMailboxGetContactsForFolder() throws ServiceException {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Contact contact = TestUtil.createContactInDefaultFolder(mbox, "testzclient2@example.com");
        List<ZContact> allZContacts = zmbox.getContactsForFolder(
                ZFolder.ID_CONTACTS, null /* ids */, (ContactSortBy) null, true /* sync */, null /* attrs */);
        assertNotNull("zmbox.getAllContacts result should not be null", allZContacts);
        assertEquals("zmbox.getAllContacts result should have 1 entry", 1, allZContacts.size());
        ZContact zcontact = allZContacts.get(0);
        assertEquals("GetAllContacts:Expected=Contact IMAP UID Actual=ZContact IMAP UID",
                contact.getImapUid(), zcontact.getImapUid());
        assertEquals("GetAllContacts:Expected=Contact ModifiedSequence Actual=ZContact ModifiedSequence",
                contact.getModifiedSequence(), zcontact.getModifiedSequence());
    }

    @Test
    public void testCreateAndModifyZContact() throws ServiceException {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Map<String, String> attrs = Maps.newHashMapWithExpectedSize(3);
        attrs.put(ContactConstants.A_email, "fun@example.com");
        attrs.put(ContactConstants.A_fullName, "Barney A. Rubble");
        ZContact zcontact = zmbox.createContact(ZFolder.ID_CONTACTS, null /* tags */, attrs);
        assertNotNull("Newly created zcontact should not be null", zcontact);
        assertEquals("Expected=ZContact ID Actual=ZContact IMAP UID", zcontact.getIdInMailbox(), zcontact.getImapUid());
        Contact contact = mbox.getContactById(null, zcontact.getIdInMailbox());
        assertNotNull("Contact object gotten by ID on newly created zcontact should not be null", zcontact);
        assertEquals("IMAP UID Expected=Contact Actual=ZContact", contact.getImapUid(), zcontact.getImapUid());
        assertEquals("ModifiedSequence Expected=Contact Actual=ZContact",
                contact.getModifiedSequence(), zcontact.getModifiedSequence());
        int origModSeq = zcontact.getModifiedSequence();
        attrs.put(ContactConstants.A_company, "Acme Inc.");
        zcontact = zmbox.modifyContact(zcontact.getId(), true /* i.e replace all */, attrs);
        assertNotNull("Contact object from zmbox.modifyContact() should not be null", zcontact);
        assertEquals("After Modify:Expected=ZContact ID Actual=ZContact IMAP UID",
                zcontact.getIdInMailbox(), zcontact.getImapUid());
        contact = mbox.getContactById(null, zcontact.getIdInMailbox());
        assertNotNull("After Modify:Contact object gotten by ID on newly modified zcontact should not be null",
                contact);
        assertEquals("After Modify:IMAP UID:Expected=Contact Actual=ZContact",
                contact.getImapUid(), zcontact.getImapUid());
        assertEquals("After Modify:ModifiedSequence Expected=Contact Actual=ZContact",
                contact.getModifiedSequence(), zcontact.getModifiedSequence());
        assertTrue(String.format("After Contact modify, new modSeq=%s should be greater than old modSeq=%s",
                zcontact.getModifiedSequence(), origModSeq),
                zcontact.getModifiedSequence() > origModSeq);
        List<ZContact> allZContacts = zmbox.getAllContacts(ZFolder.ID_CONTACTS,
                (ContactSortBy)null, true /* sync */, null /* attrs */);
        assertNotNull("zmbox.getAllContacts result should not be null", allZContacts);
        assertEquals("zmbox.getAllContacts result should have 1 entry", 1, allZContacts.size());
        zcontact = allZContacts.get(0);
        assertEquals("GetAllContacts: IMAP UID Expected=Contact Actual=ZContact",
                contact.getImapUid(), zcontact.getImapUid());
        assertEquals("GetAllContacts ModifiedSequence:Expected=Contact Actual=ZContact",
                contact.getModifiedSequence(), zcontact.getModifiedSequence());
    }

    @Test
    public void testSubscribeFolder() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = null;
        try {
            folder = mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"",
                    FOLDER_NAME, ZFolder.View.unknown, ZFolder.Color.DEFAULTCOLOR, null, null);
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                folder = mbox.getFolderByPath("/"+FOLDER_NAME);
            } else {
                throw e;
            }
        }
        mbox.flagFolderAsSubscribed(null, folder);
        assertTrue(folder.isIMAPSubscribed());
        mbox.flagFolderAsUnsubscribed(null, folder);
        assertFalse(folder.isIMAPSubscribed());
    }

    @Test
    public void testResetImapUID() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<Integer> ids = new LinkedList<Integer>();
        ids.add(Integer.valueOf(TestUtil.addMessage(mbox, "imap message 1")));
        ids.add(Integer.valueOf(TestUtil.addMessage(mbox, "imap message 2")));
        RemoteImapMailboxStore store = new RemoteImapMailboxStore(mbox, TestUtil.getAccount(USER_NAME).getId());
        // TODO: change to use List<Integer> newUIDs = mbox.resetImapUid(null, ids);
        store.resetImapUid(ids);
    }

    @Test
    public void testZMailboxGetItemById() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);

        Integer id = Integer.valueOf(TestUtil.addMessage(zmbox, "testGetItemById test msg"));
        ItemIdentifier msgItemId = ItemIdentifier.fromAccountIdAndItemId(zmbox.getAccountId(), id);

        Contact contact = TestUtil.createContactInDefaultFolder(mbox, "testzclient@example.com");
        ItemIdentifier contactId = ItemIdentifier.fromAccountIdAndItemId(zmbox.getAccountId(), contact.getId());

        /* getting message using message id */
        ZimbraMailItem mItemAsMsg = zmbox.getItemById((OpContext) null, msgItemId, MailItemType.MESSAGE);
        assertNotNull("getItemById returned null when got with type MESSAGE", mItemAsMsg);
        assertEquals(
                "Different ID when got with type MESSAGE", id, Integer.valueOf(mItemAsMsg.getIdInMailbox()));
        assertTrue(
                String.format("%s Not a ZMessage when got with type MESSAGE", mItemAsMsg.getClass().getName()),
                mItemAsMsg instanceof ZMessage);

        /* getting item using message id */
        mItemAsMsg = zmbox.getItemById((OpContext) null, msgItemId, MailItemType.UNKNOWN);
        assertNotNull("getItemById returned null when got with type UNKNOWN", mItemAsMsg);
        assertEquals(
                "Different ID when got with type UNKNOWN", id, Integer.valueOf(mItemAsMsg.getIdInMailbox()));
        assertTrue(
                String.format("%s Not a ZMessage when got with type UNKNOWN", mItemAsMsg.getClass().getName()),
                mItemAsMsg instanceof ZMessage);


        /* getting contact using id of contact */
        ZimbraMailItem mItemAsContact = zmbox.getItemById((OpContext) null, contactId, MailItemType.CONTACT);
        assertNotNull("getItemById returned null when got with type CONTACT", mItemAsContact);
        assertEquals(
                "Different ID when got with type CONTACT", contactId.id, mItemAsContact.getIdInMailbox());
        assertTrue(
                String.format("%s Not a ZContact when got with type CONTACT", mItemAsContact.getClass().getName()),
                mItemAsContact instanceof ZContact);
        ZContact zContact = (ZContact) mItemAsContact;
        assertEquals("Imap UID of ZContact should be same as Contact",
                contact.getImapUid(), zContact.getImapUid());
        assertTrue(
                String.format("IMAP UID %s of ZContact not 0", zContact.getImapUid()), 0 != zContact.getImapUid());

        /* getting message using contact id */
        try {
            zmbox.getItemById((OpContext) null, contactId, MailItemType.MESSAGE);
            fail("ZClientNoSuchItemException was not thrown when getting contact as message");
        } catch (ZClientException.ZClientNoSuchItemException zcnsie) {
        }

        /* getting message using non-existent id */
        ItemIdentifier nonexistent = ItemIdentifier.fromAccountIdAndItemId(zmbox.getAccountId(), 9099);
        try {
            zmbox.getItemById((OpContext) null, nonexistent, MailItemType.UNKNOWN);
            fail("ZClientNoSuchItemException was not thrown");
        } catch (ZClientException.ZClientNoSuchItemException zcnsie) {
        }

        /* getting contact using id of message */
        try {
            zmbox.getItemById((OpContext) null, msgItemId, MailItemType.CONTACT);
            fail("ZClientNoSuchItemException was not thrown");
        } catch (ZClientException.ZClientNoSuchContactException zcnsce) {
        }

        /* getting document using id of message */
        try {
            zmbox.getItemById((OpContext) null, msgItemId, MailItemType.DOCUMENT);
            fail("ZClientNoSuchItemException was not thrown");
        } catch (ZClientException.ZClientNoSuchItemException zcnsce) {
        }
    }

    @Test
    public void testListIMAPSubscriptions() throws Exception {
        String path = NAME_PREFIX + "_testPath";
        MetadataList slist = new MetadataList();
        slist.add(path);

        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        //imitate subscription
        mbox.setConfig(null, "imap", new Metadata().put("subs", slist));

        //check that subscription was saved in mailbox configuration
        Metadata config = mbox.getConfig(null, "imap");
        assertNotNull(config);
        MetadataList rlist = config.getList("subs", true);
        assertNotNull(rlist);
        assertNotNull(rlist.get(0));
        assertTrue(rlist.get(0).equalsIgnoreCase(path));

        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Set<String> subs = zmbox.listIMAPSubscriptions();
        assertNotNull(subs);
        assertFalse(subs.isEmpty());
        assertTrue(path.equalsIgnoreCase(subs.iterator().next()));
    }

    @Test
    public void testSaveIMAPSubscriptions() throws Exception {
        //check that no subscriptions are saved yet
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Set<String> subs = zmbox.listIMAPSubscriptions();
        assertNotNull(subs);
        assertTrue(subs.isEmpty());

        //save new subscription
        String path = NAME_PREFIX + "_testPath";
        HashSet<String> newSubs = new HashSet<String>();
        newSubs.add(path);
        zmbox.saveIMAPsubscriptions(newSubs);

        //verify
        Set<String> savedSubs = zmbox.listIMAPSubscriptions();
        assertNotNull(savedSubs);
        assertFalse(savedSubs.isEmpty());
        assertTrue(path.equalsIgnoreCase(savedSubs.iterator().next()));
    }

    @Test
    public void testImapMODSEQ() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, NAME_PREFIX, new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        int folderId = folder.getId();

        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder zfolder = zmbox.getFolderById(Integer.toString(folderId));
        int zmodSeq = zfolder.getImapMODSEQ();
        assertEquals("Before adding a message, ZFolder modseq is not the same as folder modseq", zmodSeq, folder.getImapMODSEQ());

        // add a message to the folder (there is a test in FolderTest which verifies that adding a message modifies imapmodseq)
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId).setFlags(Flag.BITMASK_UNREAD);
        String message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here", new Date(System.currentTimeMillis()));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        zmbox.noOp(); //get notifications and update folder cache
        zfolder = zmbox.getFolderById(Integer.toString(folderId));
        folder = mbox.getFolderById(null, folderId);
        assertEquals("After adding a message, ZFolder modseq is not the same as folder modseq", zfolder.getImapMODSEQ(), folder.getImapMODSEQ());
        assertFalse("ZFolder modseq did not change after adding a message", zmodSeq == zfolder.getImapMODSEQ());
    }

    @Test
    public void testRecentMessageCount() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        assertEquals("Mailbox::getRecentMessageCount should return 0 before adding a message", 0, mbox.getRecentMessageCount());
        // add a message to the folder (there is a test in FolderTest which verifies that adding a message modifies imapmodseq)
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        String message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here", new Date(System.currentTimeMillis()));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        assertEquals("Mailbox::getRecentMessageCount should return 1 after adding a message", 1, mbox.getRecentMessageCount());
        zmbox.resetRecentMessageCount(null);
        assertEquals("Mailbox::getRecentMessageCount should return 0 after reset", 0, mbox.getRecentMessageCount());
        message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here 2", new Date(System.currentTimeMillis() + 1000));
        pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here 3", new Date(System.currentTimeMillis() + 2000));
        pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        assertEquals("Mailbox::getRecentMessageCount should return 2 after adding two messages", 2, mbox.getRecentMessageCount());
        zmbox.resetRecentMessageCount(null);
        assertEquals("Mailbox::getRecentMessageCount should return 0 after the second reset", 0, mbox.getRecentMessageCount());
    }

    @Test
    public void testRecordIMAPSession() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder testFolder = TestUtil.createFolder(zmbox, "foo");
        String folderId = testFolder.getId();
        ItemIdentifier folderIdent = testFolder.getFolderItemIdentifier();
        zmbox.recordImapSession(testFolder.getFolderItemIdentifier());
        int imapRecentCutoff0 = testFolder.getImapRECENTCutoff(false);
        TestUtil.addMessage(zmbox, "test Message", folderId);
        zmbox.recordImapSession(testFolder.getFolderItemIdentifier());
        // passing false here ensures that ZFolder is not manually refreshing
        // the cutoff value
        int imapRecentCutoff1 = testFolder.getImapRECENTCutoff(false);

        assertEquals(folderIdent.id, imapRecentCutoff0);
        assertEquals(folderIdent.id + 1, imapRecentCutoff1);
    }

    @Test
    public void testOpenImapFolder() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, "TestOpenImapFolder", new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        ItemIdentifier folderId = folder.getFolderItemIdentifier();
        List<ImapMessage> expected = new LinkedList<ImapMessage>();
        for (int i = 1; i <= 10; i++) {
            Message msg = TestUtil.addMessage(mbox, folderId.id,
                    String.format("imap message %s", i), System.currentTimeMillis());
            expected.add(new ImapMessage(msg));
        }

        //test pagination
        OpenIMAPFolderParams params = new OpenIMAPFolderParams(folderId);

        params.setLimit(100); //test fetching all results
        OpenIMAPFolderResponse result = zmbox.fetchImapFolderChunk(params);
        assertEquals(10, result.getImapMessageInfo().size());
        assertFalse(result.getHasMore());

        params.setLimit(5); //test fetching first 5
        result = zmbox.fetchImapFolderChunk(params);
        List<ImapMessageInfo> messages = result.getImapMessageInfo();
        assertEquals(5, messages.size());
        for (int i = 0; i < messages.size(); i++) {
            assertEquals(messages.get(i).getId(), expected.get(i).getMsgId());
        }
        assertTrue(result.getHasMore());

        Integer cursorId = expected.get(2).getMsgId(); //test fetching 5 items starting at 3rd item, (more results left)
        params.setCursorId(String.valueOf(cursorId));
        result = zmbox.fetchImapFolderChunk(params);
        messages = result.getImapMessageInfo();
        assertEquals(5, messages.size());
        for (int i = 0; i < messages.size(); i++) {
            assertEquals(messages.get(i).getId(), expected.get(i+3).getMsgId());
        }
        assertTrue(result.getHasMore());

        cursorId = expected.get(6).getMsgId();//test fetching 5 items starting at 7th item, exhausting all results
        params.setCursorId(String.valueOf(cursorId));
        result = zmbox.fetchImapFolderChunk(params);
        messages = result.getImapMessageInfo();
        assertEquals(3, messages.size());
        for (int i = 0; i < messages.size(); i++) {
            assertEquals(messages.get(i).getId(), expected.get(i+7).getMsgId());
        }
        assertFalse(result.getHasMore());

        //test getting all messages in batches of 3, so pagination is used
        List<ImapMessageInfo> actual = zmbox.openImapFolder(folderId, 3);
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals("expected and actual lists have different lengths", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getImapUid(), actual.get(i).getImapUid());
        }
    }

    @Test
    public void testGetIdsOfModifiedItemsInFolder() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, "testGetIdsOfModifiedItemsInFolder", new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        int folderId = folder.getId();
        int lastChange = mbox.getLastChangeID();
        List<Integer> modifiedIds = zmbox.getIdsOfModifiedItemsInFolder(null, lastChange, folderId);
        assertNotNull("getIdsOfModifiedItemsInFolder should not return null", modifiedIds);
        assertTrue("getIdsOfModifiedItemsInFolder should return an empty list given the last change number", modifiedIds.isEmpty());
        List<Integer> expected = new LinkedList<Integer>();
        for (int i = 1; i <= 10; i++) {
            Message msg = TestUtil.addMessage(mbox, folderId, String.format("testGetIdsOfModifiedItemsInFolder message %s", i), System.currentTimeMillis());
            expected.add(msg.getId());
        }
        modifiedIds = zmbox.getIdsOfModifiedItemsInFolder(null, lastChange, folderId);
        assertEquals(String.format("should return the same number of IDs as the number of added messages. Added %d messages, but returned %d IDs", expected.size(), modifiedIds.size()), expected.size(), modifiedIds.size());
        for(Integer mId : expected) {
            assertTrue(modifiedIds.contains(mId));
        }
        lastChange = mbox.getLastChangeID();
        Message msg = TestUtil.addMessage(mbox, folderId, "testGetIdsOfModifiedItemsInFolder last message", System.currentTimeMillis());
        modifiedIds = zmbox.getIdsOfModifiedItemsInFolder(null, lastChange, folderId);
        assertEquals(1, modifiedIds.size());
        assertEquals(Integer.valueOf(msg.getId()), modifiedIds.get(0));
    }

    @Test
    public void testBeginTrackingImap() throws ServiceException {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        try {
            zmbox.beginTrackingImap();
        } catch (ServiceException e) {
            fail("beginTrackingImap should succeed");
        }
        assertTrue(mbox.isTrackingImap());
    }

    @Test
    public void testImapRecentRequest() throws ServiceException {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);

        Folder folder = mbox.createFolder(null, "testImapRecent", new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        int folderId = folder.getId();

        try {
            TestUtil.addMessage(mbox, folderId, "testImapRecent message", System.currentTimeMillis());
            int recent = zmbox.getImapRECENT(Integer.toString(folderId));
            assertEquals(1, recent);
        } catch (ServiceException e) {
            fail("getIMAPRecent should not fail");
        }
        catch (Exception e) {
            fail("getIMAPRecent should not fail");
        }
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test
    public void testGetMessageNotRaw() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        int folderId = Mailbox.ID_FOLDER_INBOX;
        Message msg = TestUtil.addMessage(mbox, folderId,
                String.format("%s message %s", testInfo.getMethodName(), "hello"), System.currentTimeMillis());
        ZMessage zmsg = zmbox.getMessageById(ItemIdentifier.fromAccountIdAndItemId(mbox.getAccountId(), msg.getId()));
        compareMsgAndZMsg(msg, zmsg);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test
    public void testGetMessageRaw() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        int folderId = Mailbox.ID_FOLDER_INBOX;
        Message msg = TestUtil.addMessage(mbox, folderId,
                String.format("%s message %s", testInfo.getMethodName(), "hello"), System.currentTimeMillis());
        ZMessage zmsg = zmbox.getMessageById(
                ItemIdentifier.fromAccountIdAndItemId(mbox.getAccountId(), msg.getId()), true, null);
        compareMsgAndZMsg(msg, zmsg);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test
    public void testGetMessageRawUseURL() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        int folderId = Mailbox.ID_FOLDER_INBOX;
        Message msg = TestUtil.addMessage(mbox, folderId,
                String.format("%s message %s", testInfo.getMethodName(), "hello"), System.currentTimeMillis());
        ZMessage zmsg = zmbox.getMessageById(
                ItemIdentifier.fromAccountIdAndItemId(mbox.getAccountId(), msg.getId()), true, 0);
        compareMsgAndZMsg(msg, zmsg);
    }

    @Test
    public void testAlterTag() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Message msg = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, "testAlterTag message", System.currentTimeMillis());
        int msgId = msg.getId();
        String tagName = "testAlterTag tag";
        zmbox.createTag(tagName, ZTag.Color.blue);
        Collection<ItemIdentifier> ids = new ArrayList<ItemIdentifier>(1);
        ids.add(new ItemIdentifier(mbox.getAccountId(), msgId));

        //add tag via zmailbox
        zmbox.alterTag(null, ids, tagName, true);
        assertTrue("Message should be tagged", waitForTag(msgId, mbox, tagName, true, 3000));
        assertTrue(msg.isTagged(tagName));

        //remove tag via zmailbox
        zmbox.alterTag(null, ids, tagName, false);
        assertTrue("Message should NOT be tagged", waitForTag(msgId, mbox, tagName, false, 3000));
        msg = mbox.getMessageById(null, msgId);
        assertFalse(tagName + " should be removed", msg.isTagged(tagName));

        //test setting/unsetting unread flag
        zmbox.alterTag(null, ids, "\\Unread", true);
        assertTrue("Message.isUnread should return TRUE", waitForFlag(msgId, mbox, "isUnread", true, 3000));
        msg = mbox.getMessageById(null, msgId);
        assertTrue("Message should be unread", msg.isUnread());
        zmbox.alterTag(null, ids, "\\Unread", false);
        assertTrue("Message.isUnread should return FALSE", waitForFlag(msgId, mbox, "isUnread", false, 3000));
        msg = mbox.getMessageById(null, msgId);
        assertFalse("Message should be read", msg.isUnread());
    }

    @Test
    public void testSetTags() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Message msg = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, "testAlterTag message", System.currentTimeMillis());
        String tag1Name = "testSetTags tag1";
        String tag2Name = "testSetTags tag2";
        String tag3Name = "testSetTags tag3";
        int msgId = msg.getId();

        zmbox.createTag(tag1Name, ZTag.Color.blue);
        Collection<ItemIdentifier> ids = new ArrayList<ItemIdentifier>(1);
        ids.add(new ItemIdentifier(mbox.getAccountId(), msgId));

        //add tag via zmailbox
        zmbox.alterTag(null, ids, tag1Name, true);
        assertTrue("Message should be tagged with " + tag1Name, waitForTag(msgId, mbox, tag1Name, true, 3000));
        assertTrue(msg.isTagged(tag1Name));

        //override via setTags
        Collection<String> newTags = new ArrayList<String>();
        newTags.add(tag2Name);
        newTags.add(tag3Name);
        zmbox.setTags(null, ids, 0, newTags);
        assertTrue("Message should NOT be tagged with " + tag1Name, waitForTag(msgId, mbox, tag1Name, false, 3000));
        assertTrue("Message should be tagged with " + tag2Name, waitForTag(msgId, mbox, tag2Name, true, 3000));
        assertTrue("Message should be tagged with " + tag3Name, waitForTag(msgId, mbox, tag3Name, true, 3000));
        msg = mbox.getMessageById(null, msgId);
        assertFalse(msg.isTagged("testSetTags tag1"));
        assertTrue(msg.isTagged("testSetTags tag2"));
        assertTrue(msg.isTagged("testSetTags tag3"));
    }

    @Test
    public void testFlags() throws Exception {
        ZMailbox recipZMbox = TestUtil.getZMailbox(RECIPIENT_USER_NAME);
        ZMailbox senderZMbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox recipMbox = TestUtil.getMailbox(RECIPIENT_USER_NAME);
        Mailbox senderMbox = TestUtil.getMailbox(USER_NAME);

        //UNREAD
        String id = TestUtil.addMessage(recipZMbox, "testFlags unread message");
        int numId = Integer.parseInt(id);
        recipZMbox.markItemRead(id, false, null);
        assertTrue("Message.isUnread should return TRUE", waitForFlag(numId, recipMbox, "isUnread", true, 3000));
        ZMessage zmsg = recipZMbox.getMessageById(id);
        assertTrue(zmsg.isUnread());
        Message msg = recipMbox.getMessageById(null, numId);
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());
        Collection<ItemIdentifier> ids = new ArrayList<ItemIdentifier>(1);
        ids.add(new ItemIdentifier(recipMbox.getAccountId(), numId));

        recipZMbox.markItemRead(id, true, null);
        assertTrue("Message.isUnread should return FALSE", waitForFlag(numId, recipMbox, "isUnread", false, 3000));
        zmsg = recipZMbox.getMessageById(id);
        assertFalse("ZMessage.isUnread() should be FALSE", zmsg.isUnread());
        msg = recipMbox.getMessageById(null, numId);
        assertFalse("Message.isUnread() should be FALSE", msg.isUnread());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());

        //flag
        recipMbox.alterTag(null, numId, MailItem.Type.MESSAGE, Flag.FlagInfo.FLAGGED, true, null);
        msg = recipMbox.getMessageById(null, numId);
        assertTrue("Message.isFlagged() should be TRUE", msg.isFlagged());
        recipZMbox.noOp();
        zmsg = recipZMbox.getMessageById(id);
        assertTrue("ZMessage.isFlagged() should be TRUE", zmsg.isFlagged());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());

        //unflag
        recipMbox.alterTag(null, numId, MailItem.Type.MESSAGE, Flag.FlagInfo.FLAGGED, false, null);
        msg = recipMbox.getMessageById(null, numId);
        assertFalse("Message.isFlagged() should be FALSE", msg.isFlagged());
        recipZMbox.noOp();
        zmsg = recipZMbox.getMessageById(id);
        assertFalse("ZMessage.isFlagged() should be FALSE", zmsg.isFlagged());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());

        //HIGH_PRIORITY
        ZOutgoingMessage importantMsg = TestUtil.getOutgoingMessage(RECIPIENT_USER_NAME, "This is an important message", "about something", null);
        importantMsg.setPriority("!");
        ZMessage savedDraft = senderZMbox.saveDraft(importantMsg, null, null);
        numId = Integer.parseInt(savedDraft.getId());
        assertTrue("ZMessage.isHighPriority() should be TRUE", savedDraft.isHighPriority());
        assertTrue(String.format("SavedDraft modifiedSequence=%s should be > 0",
                savedDraft.getModifiedSequence()),
                savedDraft.getModifiedSequence() > 0);

        msg = senderMbox.getMessageById(null, numId);
        zmsg = senderZMbox.getMessageById(savedDraft.getId());
        assertTrue("Message should be tagged with '!' tag", msg.isTagged(Flag.FlagInfo.HIGH_PRIORITY));
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());
        assertEquals("Expected=savedDraft ModSeq, Actual=MBOX getMessageById ModSeq",
                savedDraft.getModifiedSequence(), msg.getModifiedSequence());
        assertEquals("Expected=savedDraft ModSeq, Actual=ZMBOX getMessageById ModSeq",
                savedDraft.getModifiedSequence(), zmsg.getModifiedSequence());

        //LOW_PRIORITY
        ZOutgoingMessage nonImportantMsg = TestUtil.getOutgoingMessage(RECIPIENT_USER_NAME, "This is an important message", "about something", null);
        nonImportantMsg.setPriority("?");
        savedDraft = senderZMbox.saveDraft(nonImportantMsg, null, null);
        numId = Integer.parseInt(savedDraft.getId());
        assertFalse("ZMessage.isHighPriority() should be FALSE", savedDraft.isHighPriority());
        msg = senderMbox.getMessageById(null, numId);
        zmsg = senderZMbox.getMessageById(savedDraft.getId());
        assertTrue("Message should be tagged with '?' tag", msg.isTagged(Flag.FlagInfo.LOW_PRIORITY));
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());

        //NOTIFIED
        id = TestUtil.addMessage(recipZMbox, "testFlags notified message");
        numId = Integer.parseInt(id);
        recipMbox.alterTag(null, numId, MailItem.Type.MESSAGE, Flag.FlagInfo.NOTIFIED, true, null);
        recipZMbox.clearMessageCache();
        msg = recipMbox.getMessageById(null, numId);
        zmsg = recipZMbox.getMessageById(id);
        assertTrue("Message should be tagged 'NOTIFIED'", msg.isTagged(FlagInfo.NOTIFIED));
        assertTrue("ZMessage.isNotificationSent() should be TRUE", zmsg.isNotificationSent());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());

        //unset
        recipMbox.alterTag(null, numId, MailItem.Type.MESSAGE, Flag.FlagInfo.NOTIFIED, false, null);
        recipZMbox.clearMessageCache();
        msg = recipMbox.getMessageById(null, numId);
        zmsg = recipZMbox.getMessageById(id);
        assertFalse("Message should not be tagged 'NOTIFIED' anymore", msg.isTagged(FlagInfo.NOTIFIED));
        assertFalse("ZMessage.isNotificationSent() should be FALSE", zmsg.isNotificationSent());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());

        //FORWARDED
        id = TestUtil.addMessage(recipZMbox, "testFlags forwarded message");
        numId = Integer.parseInt(id);
        recipMbox.alterTag(null, numId, MailItem.Type.MESSAGE, Flag.FlagInfo.FORWARDED, true, null);
        recipZMbox.clearMessageCache();
        msg = recipMbox.getMessageById(null, numId);
        zmsg = recipZMbox.getMessageById(id);
        assertTrue("Message should be tagged 'FORWARDED'", msg.isTagged(FlagInfo.FORWARDED));
        assertTrue("ZMessage.isForwarded() should be TRUE", zmsg.isForwarded());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());

        //unset
        numId = Integer.parseInt(id);
        recipMbox.alterTag(null, numId, MailItem.Type.MESSAGE, Flag.FlagInfo.FORWARDED, false, null);
        recipZMbox.clearMessageCache();
        msg = recipMbox.getMessageById(null, numId);
        zmsg = recipZMbox.getMessageById(id);
        assertFalse("Message should NOT be tagged 'FORWARDED'", msg.isTagged(FlagInfo.FORWARDED));
        assertFalse("ZMessage.isForwarded() should be FALSE", zmsg.isForwarded());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());

        //send an important message
        String subject = "tobesent";
        importantMsg = TestUtil.getOutgoingMessage(RECIPIENT_USER_NAME, subject, "about nothing", null);
        importantMsg.setPriority("!");
        savedDraft = senderZMbox.saveDraft(importantMsg, null, null);
        numId = Integer.parseInt(savedDraft.getId());
        msg = senderMbox.getMessageById(null, numId);
        zmsg = senderZMbox.getMessageById(savedDraft.getId());
        //send it
        senderZMbox.sendMessage(importantMsg, null, false);
        senderZMbox.clearMessageCache();
        zmsg = TestUtil.waitForMessage(senderZMbox, "in:sent subject:\"" + subject + "\"");
        numId = Integer.parseInt(zmsg.getId());
        msg = senderMbox.getMessageById(null, numId);
        assertTrue("sent message should be marked as 'from me'", msg.isFromMe());
        assertTrue("Message should be tagged with '!' tag", msg.isTagged(Flag.FlagInfo.HIGH_PRIORITY));
        assertTrue("sent ZMessage should be market as 'sent by me'", zmsg.isSentByMe());
        assertTrue("ZMessage.isHighPriority() should be TRUE", zmsg.isHighPriority());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());
        //check recipient
        recipZMbox.clearMessageCache();
        zmsg = TestUtil.waitForMessage(recipZMbox, "in:inbox subject:\"" + subject + "\"");
        numId = Integer.parseInt(zmsg.getId());
        msg = recipMbox.getMessageById(null, numId);
        assertFalse("received message should NOT be marked as 'from me'", msg.isFromMe());
        assertTrue("Message should be tagged with '!' tag", msg.isTagged(Flag.FlagInfo.HIGH_PRIORITY));
        assertFalse("received ZMessage should NOT be market as 'sent by me'", zmsg.isSentByMe());
        assertTrue("ZMessage.isHighPriority() should be TRUE", zmsg.isHighPriority());
        assertEquals("ZMessage bitmask does not match Message bitmask", msg.getFlagBitmask(), zmsg.getFlagBitmask());
    }

    @Test
    public void testLastChangeId() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        int firstChangeId = zmbox.getLastChangeID();
        assertEquals(String.format("wrong change ID before adding message. Expecting %d. Got %d", mbox.getLastChangeID(), firstChangeId), mbox.getLastChangeID(), firstChangeId);
        String msgId = TestUtil.addMessage(zmbox, "testLastChangeId message");
        ZMessage msg = zmbox.getMessageById(msgId);
        assertNotNull("msg should not be NULL", msg);
        int secondChangeId = zmbox.getLastChangeID();
        assertTrue("lastChangeId should have increased", firstChangeId < secondChangeId);
        assertEquals(String.format("wrong change ID after adding message. Expecting %d. Got %d", mbox.getLastChangeID(), secondChangeId), mbox.getLastChangeID(), secondChangeId);
    }

    public boolean waitForFlag(int msgId, Mailbox mbox, String getterName, boolean expected, int maxtimeout) throws Exception {
        int remaining = maxtimeout;
        while(remaining > 0) {
            Message msg = mbox.getMessageById(null, msgId);
            java.lang.reflect.Method getter = msg.getClass().getMethod(getterName);
            if(expected == (boolean) getter.invoke(msg)) {
                return true;
            } else {
                Thread.sleep(500);
                remaining -= 500;
            }
        }
        return false;
    }

    public boolean waitForTag(int msgId, Mailbox mbox, String tagName, boolean expected, int maxtimeout) throws Exception {
        int remaining = maxtimeout;
        while(remaining > 0) {
            Message msg = mbox.getMessageById(null, msgId);
            if(expected == msg.isTagged(tagName)) {
                return true;
            } else {
                Thread.sleep(500);
                remaining -= 500;
            }
        }
        return false;
    }

    @Test
    public void testZSearchParamsIncludeTagDeleted() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        String msgId = TestUtil.addMessage(zmbox, "testZSearchParamsIncludeTagDeleted message");
        mbox.alterTag(null, Integer.valueOf(msgId), MailItem.Type.MESSAGE, FlagInfo.DELETED, true, null);
        ZSearchParams params = new ZSearchParams("testZSearchParamsIncludeTagDeleted");
        ZSearchResult result = zmbox.search(params);
        assertEquals(0, result.getHits().size());
        params.setIncludeTagDeleted(true);
        result = zmbox.search(params);
        assertEquals(1, result.getHits().size());
    }

    @Test
    public void testZSearchParamsMailItemTypes() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_WEEK, 1);
        String msgId = TestUtil.addMessage(zmbox, "testZSearchParamsMailItemTypes message");
        ZAppointmentResult appt = TestUtil.createAppointment(zmbox, "testZSearchParamsMailItemTypes appointment", USER_NAME, new Date(), calendar.getTime());
        String apptId = appt.getCalItemId();
        Set<MailItemType> msgOnly = Sets.newHashSet(MailItemType.MESSAGE);
        Set<MailItemType> apptOnly = Sets.newHashSet(MailItemType.APPOINTMENT);
        Set<MailItemType> msgAndAppt = Sets.newHashSet(MailItemType.MESSAGE, MailItemType.APPOINTMENT);
        ZSearchParams params = new ZSearchParams("testZSearchParamsMailItemTypes");
        // search for messages only
        params.setMailItemTypes(msgOnly);
        List<ZSearchHit> results = zmbox.search(params).getHits();
        assertEquals(1, results.size());
        assertEquals(msgId, results.get(0).getId());
        // search for appointments only
        params.setMailItemTypes(apptOnly);
        results = zmbox.search(params).getHits();
        assertEquals(1, results.size());
        assertEquals(apptId, results.get(0).getId());
        // search for messages and appointments
        params.setMailItemTypes(msgAndAppt);
        results = zmbox.search(params).getHits();
        assertEquals(2, results.size());
    }

    @Test
    public void testZSearchParamsZimbraSortBy() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        String msgId1 = TestUtil.addMessage(zmbox, "testZSearchParamsZimbraSortBy msg1");
        String msgId2 = TestUtil.addMessage(zmbox, "testZSearchParamsZimbraSortBy msg2");
        ZSearchParams params = new ZSearchParams("testZSearchParamsZimbraSortBy");
        params.setTypes("message");
        params.setZimbraSortBy(ZimbraSortBy.nameAsc);
        List<ZSearchHit> results = zmbox.search(params).getHits();
        assertEquals(msgId1, results.get(0).getId());
        params.setZimbraSortBy(ZimbraSortBy.nameDesc);
        results = zmbox.search(params).getHits();
        assertEquals(msgId2, results.get(0).getId());
    }

    @Test
    public void testZSearchParamsFetchMode() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(zmbox, "testZSearchParamsFetchMode message");
        ZSearchParams params = new ZSearchParams("testZSearchParamsFetchMode");
        params.setTypes("message");
        params.setZimbraFetchMode(ZimbraFetchMode.NORMAL);
        List<ZSearchHit> results = zmbox.search(params).getHits();
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ZMessageHit);
        params.setZimbraFetchMode(ZimbraFetchMode.IDS);
        results = zmbox.search(params).getHits();
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ZIdHit);
    }

    @Test
    public void testImapSearch() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZTag tag = zmbox.createTag("testImapSearch tag", Color.blue);
        int msgId = Integer.valueOf(TestUtil.addMessage(zmbox, "testImapSearch message"));
        mbox.alterTag(null, msgId, Type.MESSAGE, FlagInfo.UNREAD, false, null);
        mbox.alterTag(null, msgId, Type.MESSAGE, tag.getName(), true, null);
        ZSearchParams params = new ZSearchParams("testImapSearch");
        params.setMailItemTypes(Sets.newHashSet(MailItemType.MESSAGE));
        params.setFetch(Fetch.all);
        params.setZimbraFetchMode(ZimbraFetchMode.IMAP);
        ZimbraQueryHitResults results = zmbox.searchImap(null, params);
        Message msg = mbox.getMessageById(null, msgId);
        verifyImapSearchResults(results, msgId, msg.getImapUid(), msg.getParentId(), msg.getModifiedSequence(),
                MailItemType.MESSAGE, msg.getFlagBitmask(), new String[] {tag.getId()}, true);
        params.setZimbraFetchMode(ZimbraFetchMode.MODSEQ);
        results = zmbox.searchImap(null, params);
        verifyImapSearchResults(results, msgId, -1, msg.getParentId(), msg.getModifiedSequence(),
                MailItemType.MESSAGE, msg.getFlagBitmask(), new String[] {tag.getId()}, true);
        params.setZimbraFetchMode(ZimbraFetchMode.IDS);
        results = zmbox.searchImap(null, params);
        verifyImapSearchResults(results, msgId, -1, -1, -1, null, -1, null, false);

        //verify that setting the expandResult parameter also returns the necessary fields
        params.setFetch(Fetch.all);
        params.setZimbraFetchMode(ZimbraFetchMode.IMAP);
        results = zmbox.searchImap(null, params);
        verifyImapSearchResults(results, msgId, msg.getImapUid(), msg.getParentId(), msg.getModifiedSequence(),
                MailItemType.MESSAGE, msg.getFlagBitmask(), new String[] {tag.getId()}, true);
    }

    @Test
    public void testImapSearchContact() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZTag tag = zmbox.createTag("testImapSearch tag", Color.blue);
        ZFolder folder = TestUtil.createFolder(zmbox, "/testImapSearch", ZFolder.View.contact);
        Contact contact = TestUtil.createContact(mbox, folder.getFolderIdInOwnerMailbox(), "testImapSearch@test.local");
        int contactId = contact.getId();
        mbox.alterTag(null, contactId, Type.CONTACT, FlagInfo.FLAGGED, true, null);
        mbox.alterTag(null, contactId, Type.CONTACT, tag.getName(), true, null);
        ZSearchParams params = new ZSearchParams("testImapSearch");
        params.setMailItemTypes(Sets.newHashSet(MailItemType.CONTACT));
        params.setZimbraFetchMode(ZimbraFetchMode.IMAP);
        ZimbraQueryHitResults results = zmbox.searchImap(null, params);
        verifyImapSearchResults(results, contactId, contact.getImapUid(), contact.getParentId(), contact.getModifiedSequence(),
                MailItemType.CONTACT, contact.getFlagBitmask(), new String[] {tag.getId()}, true);
        params.setZimbraFetchMode(ZimbraFetchMode.MODSEQ);
        results = zmbox.searchImap(null, params);
        verifyImapSearchResults(results, contactId, -1, contact.getParentId(), contact.getModifiedSequence(),
                MailItemType.CONTACT, contact.getFlagBitmask(), new String[] {tag.getId()}, true);
        params.setZimbraFetchMode(ZimbraFetchMode.IDS);
        results = zmbox.searchImap(null, params);
        verifyImapSearchResults(results, contactId, -1, -1, -1, null, -1, null, false);
    }

    private void verifyImapSearchResults(ZimbraQueryHitResults results, int id, int imapUid, int parentId, int modSeq,
            MailItemType type, int flags, String[] tags, boolean nonZeroModSeq) throws ServiceException {
        ZimbraQueryHit hit = results.getNext();
        assertNotNull("ImapSearchResults hit should not be null", hit);
        assertEquals("Expected=Id Actual=hit.getItemId()", id, hit.getItemId());
        assertEquals("Expected=parentId Actual=hit.getParentId()", parentId, hit.getParentId());
        assertEquals("Expected=imapUid Actual=hit.getImapUid()", imapUid, hit.getImapUid());
        assertEquals("Expected=modSeq Actual=hit.getModifiedSequence()", modSeq, hit.getModifiedSequence());
        assertEquals("Expected=type Actual=hit.getMailItemType()", type, hit.getMailItemType());
        assertEquals("Expected=flags Actual=hit.getFlagBitmask()", flags, hit.getFlagBitmask());
        assertArrayEquals("Expected=tags Actual=hit.getTags()", tags, hit.getTags());
        if (nonZeroModSeq) {
            assertTrue(String.format("Modified sequence %s should be > 0", modSeq), modSeq > 0);
        }
    }

    private void compareMsgAndZMsg(Message msg, ZMessage zmsg) throws IOException, ServiceException {
        assertNotNull("Message is null", msg);
        assertNotNull("ZMessage is null", zmsg);
        String msgContent = null;
        String zmsgContent = null;
        try (InputStream msgContentStream = msg.getContentStream()) {
            msgContent = IOUtils.toString(msgContentStream, StandardCharsets.UTF_8.name());
        }
        zmsgContent = zmsg.getContent();
        if (null != zmsgContent) {
            if (zmsgContent.equals(zmsgContent)) {
                ZimbraLog.test.info("%s Pleasant surprise.  ZMessage.getContent() agrees with msg.getContentStream()",
                        testInfo.getMethodName());
            } else {
                ZimbraLog.test.debug("%s ZMessage.getContent() differs from contents of msg.getContentStream()" +
                     " probably only in terms of line endings.", testInfo.getMethodName());
            }
        }
        try (InputStream zmsgContentStream = zmsg.getContentStream()) {
            zmsgContent = IOUtils.toString(zmsgContentStream, StandardCharsets.UTF_8.name());
        }
        assertEquals("Comparing getContentStream() on msg and zmsg", msgContent, zmsgContent);
        assertEquals("Imap UID of ZMessage should be same as Message",
                zmsg.getImapUid(), msg.getImapUid());
    }

    @Test
    public void testDeleteAllPresent() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Message msg = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, "testDelete message", System.currentTimeMillis());

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(msg.getId());

        List<Integer> nonExistentIds = new ArrayList<Integer>(1);
        zmbox.delete(null, ids, nonExistentIds);
        assertTrue("Non-Existent IDS should be empty: ", nonExistentIds.isEmpty());
    }

    @Test
    public void testDelete() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Message msg = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, "testDelete message", System.currentTimeMillis());

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(msg.getId());
        ids.add(300);

        List<Integer> nonExistentIds = new ArrayList<Integer>(1);
        zmbox.delete(null, ids, nonExistentIds);
        List<Integer> expectedNonExistentIds = new ArrayList<Integer>(1);
        expectedNonExistentIds.add(300);
        assertEquals("Non-Existent IDs should be: ", expectedNonExistentIds, nonExistentIds);
    }

    @Test
    public void testDeletableFolder() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder zf = zmbox.createFolder(Integer.toString(Mailbox.ID_FOLDER_USER_ROOT),
                "deleteme", ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
        assertTrue("folder created by user should be deletable", zf.isDeletable());
        zf = zmbox.createFolder(Integer.toString(Mailbox.ID_FOLDER_INBOX),
                "deleteme", ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
        assertTrue("folder created by user under /Inbox should be deletable", zf.isDeletable());
    }

    @Test
    public void testImmutableFolder() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        byte immutable = Folder.FOLDER_IS_IMMUTABLE;
        //create an immutable folder under user root
        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setAttributes(immutable).setDefaultView(MailItem.Type.MESSAGE).setFlags(0).setColor(MailItem.DEFAULT_COLOR_RGB);
        Folder testFolder = mbox.createFolder(null, "nonDeletable", Mailbox.ID_FOLDER_USER_ROOT, fopt);
        assertNotNull("testFolder (/nonDeletable) should not be null", testFolder);
        assertFalse("folder::isDeletable should return false for 'immutable' folder", testFolder.isDeletable());
        int folderId = testFolder.getId();
        assertTrue("folderId should be higher than default system IDs", folderId >= Mailbox.FIRST_USER_ID);
        String szFolderId = Integer.toString(folderId);
        assertEquals("unexpected folder name", "nonDeletable", testFolder.getName());
        assertEquals("unexpected folder path", "/nonDeletable", testFolder.getPath());
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder zf = zmbox.getFolderById(szFolderId);
        assertNotNull("ZMailbox should be able to get new folder by ID", zf);
        zf = zmbox.getFolderRequestById(szFolderId);
        assertNotNull("ZMailbox::getFolderRequestById should be able to get new folder by ID", zf);
        assertFalse("zFolder::isDeletable should return false for 'immutable' folder", zf.isDeletable());
        assertEquals("unexpected zfolder ID", szFolderId, zf.getId());
        assertEquals("unexpected zfolder name", "nonDeletable", zf.getName());
        assertEquals("unexpected zfolder path", "/nonDeletable", zf.getPath());
    }

    @Test
    public void testHiddenFolder() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        byte immutable = Folder.FOLDER_IS_IMMUTABLE;
        //create a hidden folder under root
        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setAttributes(immutable).setDefaultView(MailItem.Type.MESSAGE).setFlags(0).setColor(MailItem.DEFAULT_COLOR_RGB);
        Folder testFolder = mbox.createFolder(null, "rootNonDeletable", Mailbox.ID_FOLDER_ROOT, fopt);
        assertNotNull("testFolder (/rootNonDeletable) should not be null", testFolder);
        assertFalse("folder::isDeletable should return false for 'immutable' folder", testFolder.isDeletable());
        int folderId = testFolder.getId();
        assertTrue("folderId for /rootNonDeletable should be higher than default system IDs", folderId >= Mailbox.FIRST_USER_ID);
        assertEquals("unexpected folder name", "rootNonDeletable", testFolder.getName());
        assertEquals("unexpected folder path", "/rootNonDeletable", testFolder.getPath());
        String szFolderId = Integer.toString(folderId);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder zf = zmbox.getFolderById(szFolderId);
        assertNull("ZMailbox should not cache /rootNonDeletable by default", zf);

        zf = zmbox.getFolderRequestById(szFolderId);
        assertNotNull("ZMailbox::getFolderRequestById should be able to get /rootNonDeletable folder by ID", zf);
        assertFalse("zFolder::isDeletable should return false for 'immutable' folder", zf.isDeletable());
        assertEquals("unexpected zfolder ID", szFolderId, zf.getId());
        assertEquals("unexpected zfolder name", "rootNonDeletable", zf.getName());
        assertEquals("unexpected zfolder path", "/rootNonDeletable", zf.getPath());
    }

    @Test
    public void testVisibleFolders() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        List<ZFolder> folders = zmbox.getAllFolders();
        assertFalse("list of folders in ZMailbox should not be empty", folders.isEmpty());
        assertEquals("Should have 12 folders", 12, folders.size());
        ArrayList<Integer> folderIds = Lists.newArrayList();
        for(ZFolder zf : folders) {
            ZimbraLog.test.debug("Found folder %s with ID: %s", zf.getPath(), zf.getId());
            assertFalse(String.format("Folder %s should not be deletable", zf.getPath()), zf.isDeletable());
            folderIds.add(Integer.parseInt(zf.getId()));
        }
        assertTrue("did not find /Calendar", folderIds.contains(Mailbox.ID_FOLDER_CALENDAR));
        assertTrue("did not find /Contacts", folderIds.contains(Mailbox.ID_FOLDER_CONTACTS));
        assertTrue("did not find /Trash", folderIds.contains(Mailbox.ID_FOLDER_TRASH));
        assertTrue("did not find /Junk", folderIds.contains(Mailbox.ID_FOLDER_SPAM));
        assertTrue("did not find /Sent", folderIds.contains(Mailbox.ID_FOLDER_SENT));
        assertTrue("did not find /Drafts", folderIds.contains(Mailbox.ID_FOLDER_DRAFTS));
        assertTrue("did not find /Inbox", folderIds.contains(Mailbox.ID_FOLDER_INBOX));
        assertTrue("did not find /Emailed Contacts", folderIds.contains(Mailbox.ID_FOLDER_AUTO_CONTACTS));
        assertTrue("did not find /",folderIds.contains( Mailbox.ID_FOLDER_USER_ROOT));
        assertTrue("did not find /Tasks", folderIds.contains(Mailbox.ID_FOLDER_TASKS));
        assertTrue("did not find /Briefcase", folderIds.contains(Mailbox.ID_FOLDER_BRIEFCASE));
        assertTrue("did not find /Chats", folderIds.contains(Mailbox.ID_FOLDER_IM_LOGS));
    }

    @Test
    public void testRenameFolder() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setDefaultView(MailItem.Type.MESSAGE).setFlags(0).setColor(MailItem.DEFAULT_COLOR_RGB);
        Folder testFolder = mbox.createFolder(null, "testRenameFolderOriginal", Mailbox.ID_FOLDER_USER_ROOT, fopt);
        assertNotNull("testFolder (/testRenameFolderOriginal) should not be null", testFolder);
        int folderId = testFolder.getId();
        String szFolderId = Integer.toString(folderId);
        assertEquals("unexpected folder name", "testRenameFolderOriginal", testFolder.getName());
        assertEquals("unexpected folder path", "/testRenameFolderOriginal", testFolder.getPath());
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder zf = zmbox.getFolderById(szFolderId);
        assertNotNull("ZMailbox should be able to get new folder by ID", zf);
        assertEquals("unexpected zfolder ID", szFolderId, zf.getId());
        assertEquals("unexpected zfolder name", "testRenameFolderOriginal", zf.getName());
        assertEquals("unexpected zfolder path", "/testRenameFolderOriginal", zf.getPath());

        //rename folder using ZMailbox
        zmbox.renameFolder(szFolderId, "testFolderRenamedOnce");
        zf = zmbox.getFolderById(szFolderId);
        assertNotNull("ZMailbox should be able to get renamed folder by ID", zf);
        assertEquals("unexpected renamed zfolder ID", szFolderId, zf.getId());
        assertEquals("unexpected renamed zfolder name", "testFolderRenamedOnce", zf.getName());
        assertEquals("unexpected renamed zfolder path", "/testFolderRenamedOnce", zf.getPath());

        //rename folder outside of ZMailbox
        mbox.renameFolder(null, testFolder, "/testFolderRenamedTwice");
        zf = zmbox.getFolderById(szFolderId);
        assertNotNull("ZMailbox should be able to get renamed folder by ID", zf);
        assertEquals("unexpected renamed zfolder ID", szFolderId, zf.getId());
        assertEquals("zfolder should not be aware of new name yet", "testFolderRenamedOnce", zf.getName());
        assertEquals("zfolder should not be aware of new path yet", "/testFolderRenamedOnce", zf.getPath());

        //get notifications
        zmbox.noOp();
        zf = zmbox.getFolderById(szFolderId);
        assertNotNull("ZMailbox should be able to get renamed folder by ID", zf);
        assertEquals("unexpected renamed zfolder ID", szFolderId, zf.getId());
        assertEquals("zfolder should pick up new name for renamed folder after NoOp", "testFolderRenamedTwice", zf.getName());
        assertEquals("zfolder should pick up new path for renamed folder after NoOp", "/testFolderRenamedTwice", zf.getPath());
    }

    private ZSearchFolder doCreateSearchFolder(ZMailbox zmbox, String parentId, String name,
            String query, String types, SearchSortBy sortBy, ZFolder.Color color) throws ServiceException {
        try {
            ZSearchFolder sf = zmbox.createSearchFolder(parentId, name, query, types, sortBy, color);
            assertNotNull(String.format("createSearchFolder failed for '%s' returned null ZSearchFolder", name), sf);
            assertEquals("Name of folder created by createSearchFolder", name, sf.getName());
            assertEquals("Query of folder created by createSearchFolder", query, sf.getQuery());
            if (null != color) {
                assertEquals("Color of folder created by createSearchFolder", color, sf.getColor());
            }
            if (null != sortBy) {
                assertEquals("SortBy of folder created by createSearchFolder", sortBy, sf.getSortBy());
            }
        } catch (Exception e) {
            fail(String.format("createSearchFolder failed for '%s'", name));
        }
        return null;
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test(timeout=100000)
    public void createSearchFolder() throws ServiceException {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        doCreateSearchFolder(zmbox, ZFolder.ID_USER_ROOT, "isReadSearch-msg-SortAsc-green",
                    "is:read", MailItemType.MESSAGE.name(), SearchSortBy.nameAsc, ZFolder.Color.GREEN);
        doCreateSearchFolder(zmbox, ZFolder.ID_USER_ROOT, "isReadSearch-msg-SortAsc-NO-COLOR",
                    "is:read", MailItemType.MESSAGE.name(), SearchSortBy.nameAsc, (ZFolder.Color)null);
        doCreateSearchFolder(zmbox, ZFolder.ID_USER_ROOT, "isReadSearch-msg-NO-SORT-green",
                    "is:read", MailItemType.MESSAGE.name(), (SearchSortBy)null, ZFolder.Color.GREEN);
        doCreateSearchFolder(zmbox, ZFolder.ID_USER_ROOT, "isReadSearch-NO-TYPE-SortAsc-green",
                    "is:read", (String)null, SearchSortBy.nameAsc, ZFolder.Color.GREEN);
    }

    @Test(timeout=100000)
    public void copyMsgToMountpoint() throws ServiceException, IOException, MessagingException {
        String sharedFolder = "shared";
        String mountpoint = String.format("shared-", testInfo.getMethodName());
        ZMailbox sharerZmbox = TestUtil.getZMailbox(RECIPIENT_USER_NAME);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(USER_NAME);
        ZMountpoint mp = TestUtil.createMountpoint(sharerZmbox, "/" + sharedFolder, shareeZmbox, mountpoint);
        String msgId = TestUtil.addMessage(shareeZmbox, String.format("test message for %s",
                testInfo.getMethodName()));
        ZActionResult result = shareeZmbox.moveMessage(msgId, mp.getFolderIdAsString());
        assertNotNull("ZActionResult for move message", result);
        assertNotNull("ZActionResult Ids array for move message", result.getIdsAsArray());
        assertEquals("ZActionResult Ids array length for move message", 1, result.getIdsAsArray().length);
    }

    @Test(timeout=50000)
    public void searchImapWithCursor() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        assertNotNull("ZMailbox", zmbox);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        int numMsgs = 400;
        for (int i=0;i<numMsgs;i++) {
            TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, "same subject");
        }
        // A simple in:INBOX search worked fine but this complex search doesn't
        ZimbraSearchParams params = zmbox.createSearchParams("in:\"INBOX\" (item:{266--399} -tag:\\Deleted)");
        params.setIncludeTagDeleted(true);
        params.setMailItemTypes(MailItem.Type.toCommon(ImapMessage.SUPPORTED_TYPES));
        params.setZimbraSortBy(ZimbraSortBy.dateAsc);
        params.setLimit(10);  /* Small sized window */
        params.setPrefetch(false);
        params.setZimbraFetchMode(ZimbraFetchMode.IDS);
        int numHits = 0;
        try (ZimbraQueryHitResults zqr = zmbox.searchImap((OpContext)null, params)) {
            for (ZimbraQueryHit hit = zqr.getNext(); hit != null; hit = zqr.getNext()) {
                numHits++;
            }
        } catch (Exception e) {
            throw ServiceException.FAILURE("failure opening search folder", e);
        }
        assertEquals("Number of hits", 399 - 266 + 1, numHits);
    }

    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestZClient.class);
    }
}
