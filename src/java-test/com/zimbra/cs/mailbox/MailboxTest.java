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
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.ZAttrProvisioning.MailThreadingAlgorithm;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;

/**
 * Unit test for {@link Mailbox}.
 *
 * @author ysasaki
 */
public final class MailboxTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    public static final DeliveryOptions STANDARD_DELIVERY_OPTIONS = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);

    @Test
    public void browse() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test1-2@sub1.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test1-3@sub1.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test1-4@sub1.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test2-1@sub2.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test2-2@sub2.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test2-3@sub2.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test3-1@sub3.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test3-2@sub3.zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test4-1@sub4.zimbra.com".getBytes(), false), dopt, null);
        mbox.index.indexDeferredItems();

        List<BrowseTerm> terms = mbox.browse(null, Mailbox.BrowseBy.domains, null, 100);
        Assert.assertEquals(4, terms.size());
        Assert.assertEquals("sub1.zimbra.com", terms.get(0).getText());
        Assert.assertEquals("sub2.zimbra.com", terms.get(1).getText());
        Assert.assertEquals("sub3.zimbra.com", terms.get(2).getText());
        Assert.assertEquals("sub4.zimbra.com", terms.get(3).getText());
        Assert.assertEquals(8, terms.get(0).getFreq());
        Assert.assertEquals(6, terms.get(1).getFreq());
        Assert.assertEquals(4, terms.get(2).getFreq());
        Assert.assertEquals(2, terms.get(3).getFreq());
    }

    @Test
    public void threadDraft() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.subject);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // setup: add the root message
        ParsedMessage pm = MailboxTestUtil.generateMessage("test subject");
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int rootId = mbox.addMessage(null, pm, dopt, null).getId();

        // first draft explicitly references the parent by item ID (how ZWC does it)
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        Message draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT, rootId + "", MailSender.MSGTYPE_REPLY, null, null, 0);
        Message parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded explicitly", parent.getConversationId(), draft.getConversationId());

        // second draft implicitly references the parent by default threading rules
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [saveDraft]", parent.getConversationId(), draft.getConversationId());

        // threading is set up at first save time, so modifying the second draft should *not* affect threading
        pm = MailboxTestUtil.generateMessage("Re: changed the subject");
        draft = mbox.saveDraft(null, pm, draft.getId());
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [resaved]", parent.getConversationId(), draft.getConversationId());

        // third draft is like second draft, but goes via Mailbox.addMessage (how IMAP does it)
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        dopt = new DeliveryOptions().setFlags(Flag.BITMASK_DRAFT).setFolderId(Mailbox.ID_FOLDER_DRAFTS);
        draft = mbox.addMessage(null, pm, dopt, null);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [addMessage]", parent.getConversationId(), draft.getConversationId());

        // fourth draft explicitly references the parent by item ID, even though it wouldn't get threaded using the default threader
        pm = MailboxTestUtil.generateMessage("changed the subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT, rootId + "", MailSender.MSGTYPE_REPLY, null, null, 0);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded explicitly (changed subject)", parent.getConversationId(), draft.getConversationId());

        // fifth draft is not related to the parent and should not be threaded
        pm = MailboxTestUtil.generateMessage("Re: unrelated subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT);
        Assert.assertEquals("unrelated", -draft.getId(), draft.getConversationId());
    }

    @Test
    public void trimTombstones() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // add a message
        int changeId1 = mbox.getLastChangeID();
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int msgId = mbox.addMessage(null, MailboxTestUtil.generateMessage("foo"), dopt, null).getId();

        // turn on sync tracking -- tombstone table should be empty
        mbox.beginTrackingSync();
        int changeId2 = mbox.getLastChangeID();
        Assert.assertTrue("no changes", mbox.getTombstones(changeId2).isEmpty());

        // verify that we can't use a sync token from *before* sync tracking was enabled
        try {
            mbox.getTombstones(changeId1);
            Assert.fail("too-early sync token");
        } catch (MailServiceException e) {
            Assert.assertEquals("too-early sync token", e.getCode(), MailServiceException.MUST_RESYNC);
        }

        // delete the message and check that it generated a tombstone
        mbox.delete(null, msgId, MailItem.Type.MESSAGE);
        int changeId3 = mbox.getLastChangeID();
        Assert.assertTrue("deleted item in tombstones", mbox.getTombstones(changeId2).contains(msgId));
        Assert.assertTrue("no changes since delete", mbox.getTombstones(changeId3).isEmpty());

        // purge the account with the default tombstone purge lifetime (3 months)
        mbox.purgeMessages(null);
        Assert.assertTrue("deleted item still in tombstones", mbox.getTombstones(changeId2).contains(msgId));

        // purge the account and all its tombstones
        LC.tombstone_max_age_ms.setDefault(0);
        mbox.purgeMessages(null);
        try {
            mbox.getTombstones(changeId2);
            Assert.fail("sync token predates purged tombstone");
        } catch (MailServiceException e) {
            Assert.assertEquals("sync token predates purged tombstone", e.getCode(), MailServiceException.MUST_RESYNC);
        }
        Assert.assertTrue("sync token matches last purged tombstone", mbox.getTombstones(changeId3).isEmpty());
    }

    static class MockListener extends MailboxListener {
        PendingModifications pms;

        @Override public void notify(ChangeNotification notification) {
            this.pms = notification.mods;
        }
    }

    @Test
    public void notifications() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        MockListener ml = new MockListener();
        MailboxListener.register(ml);

        try {
            Folder f = mbox.createFolder(null, "foo", (byte) 0, MailItem.Type.MESSAGE);
            Folder fParent = (Folder) f.getParent();

            ModificationKey fkey = new ModificationKey(f);
            ModificationKey fParentKey = new ModificationKey(fParent);

            Assert.assertNull("no deletes after create", ml.pms.deleted);

            Assert.assertNotNull("creates aren't null", ml.pms.created);
            Assert.assertEquals("one created folder", 1, ml.pms.created.size());
            Assert.assertNotNull("created folder has entry", ml.pms.created.get(fkey));
            Assert.assertEquals("created folder matches created entry", f.getId(), ml.pms.created.get(fkey).getId());

            Assert.assertNotNull("modifications aren't null", ml.pms.modified);
            Assert.assertEquals("one modified folder", 1, ml.pms.modified.size());
            PendingModifications.Change pModification = ml.pms.modified.get(fParentKey);
            Assert.assertNotNull("parent folder modified", pModification);
            Assert.assertEquals("parent folder matches modified entry",
                                fParent.getId(), ((Folder) pModification.what).getId());
            Assert.assertNotNull("preModifyObj is not null", pModification.preModifyObj);
            Assert.assertEquals("preModifyObj is a snapshot of parent folder",
                                fParent.getId(), ((Folder) pModification.preModifyObj).getId());

            DeliveryOptions dopt = new DeliveryOptions().setFolderId(f.getId());
            Message m = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), dopt, null);
            ModificationKey mkey = new ModificationKey(m);

            mbox.delete(null, f.getId(), MailItem.Type.FOLDER);

            Assert.assertNull("no creates after delete", ml.pms.created);

            Assert.assertNotNull("deletes aren't null", ml.pms.deleted);
            Assert.assertEquals("one deleted folder, one deleted message, one deleted vconv", 3, ml.pms.deleted.size());
            PendingModifications.Change fDeletion = ml.pms.deleted.get(fkey);
            Assert.assertNotNull("deleted folder has entry", fDeletion);
            Assert.assertTrue("deleted folder matches deleted entry",
                              f.getType() == fDeletion.what && f.getId() == ((Folder) fDeletion.preModifyObj).getId());
            PendingModifications.Change mDeletion = ml.pms.deleted.get(mkey);
            Assert.assertNotNull("deleted message has entry", mDeletion);
            // Note that preModifyObj may be null for the deleted message, so just check for the type
            Assert.assertTrue("deleted message matches deleted entry", m.getType() == mDeletion.what);

            Assert.assertNotNull("modifications aren't null", ml.pms.modified);
            Assert.assertEquals("parent folder modified, mailbox size modified", 2, ml.pms.modified.size());
            pModification = ml.pms.modified.get(fParentKey);
            Assert.assertNotNull("parent folder modified", pModification);
            Assert.assertEquals("parent folder matches modified entry",
                                fParent.getId(), ((Folder) pModification.what).getId());
            Assert.assertNotNull("preModifyObj is not null", pModification.preModifyObj);
            Assert.assertEquals("preModifyObj is a snapshot of parent folder",
                                fParent.getId(), ((Folder) pModification.preModifyObj).getId());
        } finally {
            MailboxListener.unregister(ml);
        }
    }

    @Test
    public void dumpster() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        acct.setDumpsterEnabled(true);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int msgId = mbox.addMessage(null, MailboxTestUtil.generateMessage("test"), dopt, null).getId();

        mbox.index.indexDeferredItems();

        mbox.delete(null, msgId, MailItem.Type.MESSAGE);
        mbox.recover(null, new int[] { msgId }, MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_INBOX);
    }

    @Test
    public void deleteMailbox() throws Exception {
        // first test normal mailbox delete
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals("start with no blobs in the store", 0, MockStoreManager.size());

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, MailboxTestUtil.generateMessage("test"), dopt, null).getId();
        Assert.assertEquals("1 blob in the store", 1, MockStoreManager.size());

        mbox.deleteMailbox();
        Assert.assertEquals("end with no blobs in the store", 0, MockStoreManager.size());


        // then test mailbox delete without store delete
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals("start with no blobs in the store", 0, MockStoreManager.size());

        mbox.addMessage(null, MailboxTestUtil.generateMessage("test"), dopt, null).getId();
        Assert.assertEquals("1 blob in the store", 1, MockStoreManager.size());

        mbox.deleteMailbox(Mailbox.DeleteBlobs.NEVER);
        Assert.assertEquals("end with 1 blob in the store", 1, MockStoreManager.size());
        MockStoreManager.purge();


        // then do it contingent on whether the store is centralized or local
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals("start with no blobs in the store", 0, MockStoreManager.size());

        mbox.addMessage(null, MailboxTestUtil.generateMessage("test"), dopt, null).getId();
        Assert.assertEquals("1 blob in the store", 1, MockStoreManager.size());

        mbox.deleteMailbox(Mailbox.DeleteBlobs.UNLESS_CENTRALIZED);
        int expected = StoreManager.getInstance().supports(StoreManager.StoreFeature.CENTRALIZED) ? 1 : 0;
        Assert.assertEquals("end with " + expected + " blob(s) in the store", expected, MockStoreManager.size());
        MockStoreManager.purge();
    }
}
