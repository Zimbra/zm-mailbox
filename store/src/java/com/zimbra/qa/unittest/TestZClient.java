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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZContact;
import com.zimbra.client.ZFeatures;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGetInfoResult;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Options;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZPrefs;
import com.zimbra.client.ZSignature;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.OpContext;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.imap.RemoteImapMailboxStore;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.soap.account.message.ImapMessageInfo;
import com.zimbra.soap.mail.message.ItemActionResponse;

public class TestZClient extends TestCase {
    private static String NAME_PREFIX = "TestZClient";
    private static String RECIPIENT_USER_NAME = NAME_PREFIX + "_user2";
    private static final String USER_NAME = NAME_PREFIX + "_user1";
    private static final String FOLDER_NAME = "testfolder";

    @Override
    public void setUp()
    throws Exception {
        if (!TestUtil.fromRunUnitTests) {
            TestUtil.cliSetup();
        }
        cleanUp();
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(RECIPIENT_USER_NAME);
    }

    /**
     * Confirms that the prefs accessor works (bug 51384).
     */
    public void testPrefs() throws Exception {
        Account account = TestUtil.getAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZPrefs prefs = mbox.getPrefs();
        assertEquals(account.getPrefLocale(), prefs.getLocale());
    }

    /**
     * Confirms that the features accessor doesn't throw NPE (bug 51384).
     */
    public void testFeatures() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFeatures features = mbox.getFeatures();
        features.getPop3Enabled();
    }

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
     * Confirms that the {@code List} of signatures returned by {@link ZMailbox#getSignatures}
     * is modifiable (see bug 51842).
     */
    public void testModifySignatures() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZSignature> signatures = mbox.getSignatures();
        try {
            signatures.set(signatures.size(), null);
        } catch (IndexOutOfBoundsException e) {
            // Not UnsupportedOperationException, so we're good.
        }

        ZGetInfoResult info = mbox.getAccountInfo(true);
        signatures = info.getSignatures();
        try {
            signatures.set(signatures.size(), null);
        } catch (IndexOutOfBoundsException e) {
            // Not UnsupportedOperationException, so we're good.
        }
    }

    public void testCopyItemAction() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String sender = TestUtil.getAddress(USER_NAME);
        String recipient = TestUtil.getAddress(RECIPIENT_USER_NAME);
        String subject = NAME_PREFIX + " testCopyItemAction";
        String content = new MessageBuilder().withSubject(subject).withFrom(sender).withToRecipient(recipient).create();

        // add a msg flagged as sent; filterSent=TRUE
        mbox.addMessage(Integer.toString(Mailbox.ID_FOLDER_DRAFTS), null, null, System.currentTimeMillis(), content, false, false);
        ZMessage msg = TestUtil.waitForMessage(mbox, "in:drafts " + subject);
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(Integer.parseInt(msg.getId()));
        ItemActionResponse resp = mbox.copyItemAction(Mailbox.ID_FOLDER_SENT, ids);
        Assert.assertNotNull(resp);
        Assert.assertNotNull(resp.getAction());
        Assert.assertNotNull(resp.getAction().getId());

        ZMessage copiedMessage = mbox.getMessageById(resp.getAction().getId());
        Assert.assertNotNull(copiedMessage);
        Assert.assertEquals(subject, copiedMessage.getSubject());
        //msg.getId()

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
        Assert.assertTrue(folder.isIMAPSubscribed());
        mbox.flagFolderAsUnsubscribed(null, folder);
        Assert.assertFalse(folder.isIMAPSubscribed());
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
    public void testGetMessageItemById() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);

        Integer id = Integer.valueOf(TestUtil.addMessage(zmbox, "testGetItemById test msg"));
        ItemIdentifier msgItemId = ItemIdentifier.fromAccountIdAndItemId(zmbox.getAccountId(), id);

        Contact contact = TestUtil.createContactInDefaultFolder(mbox, "testzclient@example.com");
        ItemIdentifier contactId = ItemIdentifier.fromAccountIdAndItemId(zmbox.getAccountId(), contact.getId());

        /* getting message using message id */
        ZimbraMailItem mItemAsMsg = zmbox.getItemById((OpContext) null, msgItemId, MailItemType.MESSAGE);
        Assert.assertNotNull("getItemById returned null when got with type MESSAGE", mItemAsMsg);
        Assert.assertEquals(
                "Different ID when got with type MESSAGE", id, Integer.valueOf(mItemAsMsg.getIdInMailbox()));
        Assert.assertTrue(
                String.format("%s Not a ZMessage when got with type MESSAGE", mItemAsMsg.getClass().getName()),
                mItemAsMsg instanceof ZMessage);

        /* getting item using message id */
        mItemAsMsg = zmbox.getItemById((OpContext) null, msgItemId, MailItemType.UNKNOWN);
        Assert.assertNotNull("getItemById returned null when got with type UNKNOWN", mItemAsMsg);
        Assert.assertEquals(
                "Different ID when got with type UNKNOWN", id, Integer.valueOf(mItemAsMsg.getIdInMailbox()));
        Assert.assertTrue(
                String.format("%s Not a ZMessage when got with type UNKNOWN", mItemAsMsg.getClass().getName()),
                mItemAsMsg instanceof ZMessage);


        /* getting contact using id of contact */
        ZimbraMailItem mItemAsContact = zmbox.getItemById((OpContext) null, contactId, MailItemType.CONTACT);
        Assert.assertNotNull("getItemById returned null when got with type CONTACT", mItemAsContact);
        Assert.assertEquals(
                "Different ID when got with type CONTACT", contactId.id, mItemAsContact.getIdInMailbox());
        Assert.assertTrue(
                String.format("%s Not a ZContact when got with type CONTACT", mItemAsContact.getClass().getName()),
                mItemAsContact instanceof ZContact);

        /* getting message using contact id */
        try {
            zmbox.getItemById((OpContext) null, contactId, MailItemType.MESSAGE);
            Assert.fail("ZClientNoSuchItemException was not thrown when getting contact as message");
        } catch (ZClientException.ZClientNoSuchItemException zcnsie) {
        }

        /* getting message using non-existent id */
        ItemIdentifier nonexistent = ItemIdentifier.fromAccountIdAndItemId(zmbox.getAccountId(), 9099);
        try {
            zmbox.getItemById((OpContext) null, nonexistent, MailItemType.UNKNOWN);
            Assert.fail("ZClientNoSuchItemException was not thrown");
        } catch (ZClientException.ZClientNoSuchItemException zcnsie) {
        }


        /* getting contact using id of message */
        try {
            zmbox.getItemById((OpContext) null, msgItemId, MailItemType.CONTACT);
            Assert.fail("ZClientNoSuchItemException was not thrown");
        } catch (ZClientException.ZClientNoSuchContactException zcnsce) {
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
        Assert.assertNotNull(config);
        MetadataList rlist = config.getList("subs", true);
        Assert.assertNotNull(rlist);
        Assert.assertNotNull(rlist.get(0));
        Assert.assertTrue(rlist.get(0).equalsIgnoreCase(path));

        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Set<String> subs = zmbox.listIMAPSubscriptions();
        Assert.assertNotNull(subs);
        Assert.assertFalse(subs.isEmpty());
        Assert.assertTrue(path.equalsIgnoreCase(subs.iterator().next()));
    }

    @Test
    public void testSaveIMAPSubscriptions() throws Exception {
        //check that no subscriptions are saved yet
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Set<String> subs = zmbox.listIMAPSubscriptions();
        Assert.assertNotNull(subs);
        Assert.assertTrue(subs.isEmpty());

        //save new subscription
        String path = NAME_PREFIX + "_testPath";
        HashSet<String> newSubs = new HashSet<String>();
        newSubs.add(path);
        zmbox.saveIMAPsubscriptions(newSubs);

        //verify
        Set<String> savedSubs = zmbox.listIMAPSubscriptions();
        Assert.assertNotNull(savedSubs);
        Assert.assertFalse(savedSubs.isEmpty());
        Assert.assertTrue(path.equalsIgnoreCase(savedSubs.iterator().next()));
    }

    @Test
    public void testImapMODSEQ() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, NAME_PREFIX, new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        int folderId = folder.getId();

        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder zfolder = zmbox.getFolderById(Integer.toString(folderId));
        int zmodSeq = zfolder.getImapMODSEQ();
        Assert.assertEquals("Before adding a message, ZFolder modseq is not the same as folder modseq", zmodSeq, folder.getImapMODSEQ());

        // add a message to the folder (there is a test in FolderTest which verifies that adding a message modifies imapmodseq)
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId).setFlags(Flag.BITMASK_UNREAD);
        String message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here", new Date(System.currentTimeMillis()));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        zmbox.noOp(); //get notifications and update folder cache
        zfolder = zmbox.getFolderById(Integer.toString(folderId));
        folder = mbox.getFolderById(null, folderId);
        Assert.assertEquals("After adding a message, ZFolder modseq is not the same as folder modseq", zfolder.getImapMODSEQ(), folder.getImapMODSEQ());
        Assert.assertFalse("ZFolder modseq did not change after adding a message", zmodSeq == zfolder.getImapMODSEQ());
    }

    @Test
    public void testRecentMessageCount() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Assert.assertEquals("Mailbox::getRecentMessageCount should return 0 before adding a message", 0, mbox.getRecentMessageCount());
        // add a message to the folder (there is a test in FolderTest which verifies that adding a message modifies imapmodseq)
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        String message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here", new Date(System.currentTimeMillis()));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        Assert.assertEquals("Mailbox::getRecentMessageCount should return 1 after adding a message", 1, mbox.getRecentMessageCount());
        zmbox.resetRecentMessageCount(null);
        Assert.assertEquals("Mailbox::getRecentMessageCount should return 0 after reset", 0, mbox.getRecentMessageCount());
        message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here 2", new Date(System.currentTimeMillis() + 1000));
        pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here 3", new Date(System.currentTimeMillis() + 2000));
        pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        Assert.assertEquals("Mailbox::getRecentMessageCount should return 2 after adding two messages", 2, mbox.getRecentMessageCount());
        zmbox.resetRecentMessageCount(null);
        Assert.assertEquals("Mailbox::getRecentMessageCount should return 0 after the second reset", 0, mbox.getRecentMessageCount());
    }

    @Test
    public void testOpenImapFolder() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, "TestOpenImapFolder", new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        int folderId = folder.getId();
        List<ImapMessage> expected = new LinkedList<ImapMessage>();
        for (int i = 1; i <= 5; i++) {
            Message msg = TestUtil.addMessage(mbox, folderId, String.format("imap message %s", i), System.currentTimeMillis());
            expected.add(new ImapMessage(msg));
        }

        List<ImapMessageInfo> actual = zmbox.openImapFolder(folderId, 1000);
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals("expected and actual lists have different lengths", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals((Integer) expected.get(i).getImapId(), actual.get(i).getImapId());
        }
    }

    @Override
    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        if(TestUtil.accountExists(USER_NAME)) {
            TestUtil.deleteAccount(USER_NAME);
        }
        if(TestUtil.accountExists(RECIPIENT_USER_NAME)) {
            TestUtil.deleteAccount(RECIPIENT_USER_NAME);
        }
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestZClient.class);
    }
}
