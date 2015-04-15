/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.MailThreadingAlgorithm;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexingQueueAdapter;
import com.zimbra.cs.index.IndexingService;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link Mailbox}.
 *
 * @author ysasaki
 */
public final class MailboxTest {
    int defaultCacheSizeSetting = 1024;
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret",
                new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        defaultCacheSizeSetting =  Provisioning.getInstance().getLocalServer().getIndexTermsCacheSize();
        MailboxTestUtil.clearData();
    }

    public static final DeliveryOptions STANDARD_DELIVERY_OPTIONS = new DeliveryOptions()
            .setFolderId(Mailbox.ID_FOLDER_INBOX);

    @Test
    public void threadDraft() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.subject);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // setup: add the root message
        ParsedMessage pm = MailboxTestUtil.generateMessage("test subject");
        int rootId = mbox.addMessage(null, pm, STANDARD_DELIVERY_OPTIONS, null)
                .getId();

        // first draft explicitly references the parent by item ID (how ZWC does
        // it)
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        Message draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT,
                rootId + "", MailSender.MSGTYPE_REPLY, null, null, 0);
        Message parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded explicitly", parent.getConversationId(),
                draft.getConversationId());

        // second draft implicitly references the parent by default threading
        // rules
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [saveDraft]",
                parent.getConversationId(), draft.getConversationId());

        // threading is set up at first save time, so modifying the second draft
        // should *not* affect threading
        pm = MailboxTestUtil.generateMessage("Re: changed the subject");
        draft = mbox.saveDraft(null, pm, draft.getId());
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [resaved]",
                parent.getConversationId(), draft.getConversationId());

        // third draft is like second draft, but goes via Mailbox.addMessage
        // (how IMAP does it)
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        DeliveryOptions dopt = new DeliveryOptions().setFlags(
                Flag.BITMASK_DRAFT).setFolderId(Mailbox.ID_FOLDER_DRAFTS);
        draft = mbox.addMessage(null, pm, dopt, null);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [addMessage]",
                parent.getConversationId(), draft.getConversationId());

        // fourth draft explicitly references the parent by item ID, even though
        // it wouldn't get threaded using the default threader
        pm = MailboxTestUtil.generateMessage("changed the subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT,
                rootId + "", MailSender.MSGTYPE_REPLY, null, null, 0);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded explicitly (changed subject)",
                parent.getConversationId(), draft.getConversationId());

        // fifth draft is not related to the parent and should not be threaded
        pm = MailboxTestUtil.generateMessage("Re: unrelated subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT);
        Assert.assertEquals("unrelated", -draft.getId(),
                draft.getConversationId());
    }

    @Test
    public void trimTombstones() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // add a message
        int changeId1 = mbox.getLastChangeID();
        int msgId = mbox.addMessage(null,
                MailboxTestUtil.generateMessage("foo"),
                STANDARD_DELIVERY_OPTIONS, null).getId();

        // turn on sync tracking -- tombstone table should be empty
        mbox.beginTrackingSync();
        int changeId2 = mbox.getLastChangeID();
        Assert.assertTrue("no changes", mbox.getTombstones(changeId2).isEmpty());

        // verify that we can't use a sync token from *before* sync tracking was
        // enabled
        try {
            mbox.getTombstones(changeId1);
            Assert.fail("too-early sync token");
        } catch (MailServiceException e) {
            Assert.assertEquals("too-early sync token", e.getCode(),
                    MailServiceException.MUST_RESYNC);
        }

        // delete the message and check that it generated a tombstone
        mbox.delete(null, msgId, MailItem.Type.MESSAGE);
        int changeId3 = mbox.getLastChangeID();
        Assert.assertTrue("deleted item in tombstones",
                mbox.getTombstones(changeId2).contains(msgId));
        Assert.assertTrue("no changes since delete",
                mbox.getTombstones(changeId3).isEmpty());

        // purge the account with the default tombstone purge lifetime (3
        // months)
        mbox.purgeMessages(null);
        Assert.assertTrue("deleted item still in tombstones", mbox
                .getTombstones(changeId2).contains(msgId));

        // purge the account and all its tombstones
        Provisioning.getInstance().getLocalServer().setMailboxTombstoneMaxAge("0ms");
        mbox.purgeMessages(null);
        try {
            mbox.getTombstones(changeId2);
            Assert.fail("sync token predates purged tombstone");
        } catch (MailServiceException e) {
            Assert.assertEquals("sync token predates purged tombstone",
                    e.getCode(), MailServiceException.MUST_RESYNC);
        }
        Assert.assertTrue("sync token matches last purged tombstone", mbox
                .getTombstones(changeId3).isEmpty());
    }

    static class MockListener implements MailboxListener {
        /**
         * Information on creations/modifications and deletions seen since
         * {@link clear} was last called (or listener was instantiated)
         */
        PendingModifications pms;

        @Override
        public Set<MailItem.Type> notifyForItemTypes() {
            return MailboxListener.ALL_ITEM_TYPES;
        }

        @Override
        public void notify(ChangeNotification notification) {
            PendingModifications newPms = notification.mods;

            if (this.pms == null) {
                this.pms = newPms;
            } else {
                if (newPms.created != null) {
                    if (pms.created == null) {
                        pms.created = Maps.newLinkedHashMap();
                    }
                    pms.created.putAll(newPms.created);
                }
                if (newPms.modified != null) {
                    if (pms.modified == null) {
                        pms.modified = Maps.newHashMap();
                    }
                    pms.modified.putAll(newPms.modified);
                }
                if (newPms.deleted != null) {
                    if (pms.deleted == null) {
                        pms.deleted = Maps.newHashMap();
                    }
                    pms.deleted.putAll(newPms.deleted);
                }
            }
        }

        public PendingModifications getPms() {
            return pms;
        }

        /**
         * Regard all previously items seen during listening as processed.
         */
        public void clear() {
            pms = null;
        }
    }

    @Test
    public void notifications() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        MockListener ml = new MockListener();
        MailboxListenerManager.getInstance().register(ml);

        try {
            Folder f = mbox.createFolder(null, "foo",
                    new Folder.FolderOptions()
                            .setDefaultView(MailItem.Type.MESSAGE));
            Folder fParent = (Folder) f.getParent();

            ModificationKey fkey = new ModificationKey(f);
            ModificationKey fParentKey = new ModificationKey(fParent);

            Assert.assertNull("no deletes after create", ml.getPms().deleted);

            Assert.assertNotNull("creates aren't null", ml.getPms().created);
            Assert.assertEquals("one created folder", 1,
                    ml.getPms().created.size());
            Assert.assertNotNull("created folder has entry",
                    ml.getPms().created.get(fkey));
            Assert.assertEquals("created folder matches created entry",
                    f.getId(), ml.getPms().created.get(fkey).getId());

            Assert.assertNotNull("modifications aren't null",
                    ml.getPms().modified);
            Assert.assertEquals("one modified folder", 1,
                    ml.getPms().modified.size());
            PendingModifications.Change pModification = ml.getPms().modified
                    .get(fParentKey);
            Assert.assertNotNull("parent folder modified", pModification);
            Assert.assertEquals("parent folder matches modified entry",
                    fParent.getId(), ((Folder) pModification.what).getId());
            Assert.assertNotNull("preModifyObj is not null",
                    pModification.preModifyObj);
            Assert.assertEquals("preModifyObj is a snapshot of parent folder",
                    fParent.getId(),
                    ((Folder) pModification.preModifyObj).getId());

            DeliveryOptions dopt = new DeliveryOptions().setFolderId(f.getId());
            Message m = mbox
                    .addMessage(null,
                            MailboxTestUtil.generateMessage("test subject"),
                            dopt, null);
            ModificationKey mkey = new ModificationKey(m);

            ml.clear();
            mbox.delete(null, f.getId(), MailItem.Type.FOLDER);

            Assert.assertNull("no creates after delete", ml.getPms().created);

            Assert.assertNotNull("deletes aren't null", ml.getPms().deleted);
            Assert.assertEquals(
                    "1 deleted folder, 1 deleted message, 1 deleted vconv", 3,
                    ml.getPms().deleted.size());
            PendingModifications.Change fDeletion = ml.getPms().deleted
                    .get(fkey);
            Assert.assertNotNull("deleted folder has entry", fDeletion);
            Assert.assertTrue(
                    "deleted folder matches deleted entry",
                    f.getType() == fDeletion.what
                            && f.getId() == ((Folder) fDeletion.preModifyObj)
                                    .getId());
            PendingModifications.Change mDeletion = ml.getPms().deleted
                    .get(mkey);
            Assert.assertNotNull("deleted message has entry", mDeletion);
            // Note that preModifyObj may be null for the deleted message, so
            // just check for the type
            Assert.assertTrue("deleted message matches deleted entry",
                    m.getType() == mDeletion.what);

            Assert.assertNotNull("modifications aren't null",
                    ml.getPms().modified);
            // Bug 80980 "folder size modified" notification present because
            // folder delete is now a 2 stage operation.
            // Empty folder, then delete it.
            Assert.assertEquals(
                    "parent folder modified, mailbox size modified, folder size modified",
                    3, ml.getPms().modified.size());
            pModification = ml.getPms().modified.get(fParentKey);
            Assert.assertNotNull("parent folder modified", pModification);
            Assert.assertEquals("parent folder matches modified entry",
                    fParent.getId(), ((Folder) pModification.what).getId());
            Assert.assertNotNull("preModifyObj is not null",
                    pModification.preModifyObj);
            Assert.assertEquals("preModifyObj is a snapshot of parent folder",
                    fParent.getId(),
                    ((Folder) pModification.preModifyObj).getId());
        } finally {
            MailboxListenerManager.getInstance().unregister(ml);
        }
    }

    @Test
    public void dumpster() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        acct.setDumpsterEnabled(true);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        int msgId = mbox.addMessage(null,
                MailboxTestUtil.generateMessage("test"),
                STANDARD_DELIVERY_OPTIONS, null).getId();

        mbox.delete(null, msgId, MailItem.Type.MESSAGE);
        mbox.recover(null, new int[] { msgId }, MailItem.Type.MESSAGE,
                Mailbox.ID_FOLDER_INBOX);
    }

    @Test
    public void moveOutOfSpam() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        mbox.getAccount().setJunkMessagesIndexingEnabled(false);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_SPAM);
        Message msg = mbox.addMessage(null, new ParsedMessage(
                "From: spammer@zimbra.com\r\nTo: test@zimbra.com".getBytes(), false), opt, null);

        SearchParams params = new SearchParams();
        params.setSortBy(SortBy.NONE);
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setQueryString("from:spammer");
        ZimbraQueryResults result = mbox.index.search(SoapProtocol.Soap12, new OperationContext(mbox), params);
        Assert.assertFalse(result.hasNext());

        mbox.move(new OperationContext(mbox), msg.getId(), MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_INBOX);

        result = mbox.index.search(SoapProtocol.Soap12, new OperationContext(mbox), params);
        Assert.assertTrue(result.hasNext());
        Assert.assertEquals(msg.getId(), result.getNext().getItemId());
    }

    @Test
    public void deleteMailbox() throws Exception {
        MockStoreManager sm = (MockStoreManager) StoreManager.getInstance();

        // first test normal mailbox delete
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals("start with no blobs in the store", 0, sm.size());

        MailItem item = mbox.addMessage(null,
                MailboxTestUtil.generateMessage("test"),
                STANDARD_DELIVERY_OPTIONS, null);
        Assert.assertEquals("1 blob in the store", 1, sm.size());
        // Index the mailbox so that mime message gets cached
        // make sure digest is in message cache.
        Assert.assertTrue(MessageCache.contains(item.getDigest()));

        mbox.deleteMailbox();
        Assert.assertEquals("end with no blobs in the store", 0, sm.size());
        // make sure digest is removed from message cache.
        Assert.assertFalse(MessageCache.contains(item.getDigest()));

        // then test mailbox delete without store delete
        mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals("start with no blobs in the store", 0, sm.size());

        item = mbox.addMessage(null, MailboxTestUtil.generateMessage("test"),
                STANDARD_DELIVERY_OPTIONS, null);
        Assert.assertEquals("1 blob in the store", 1, sm.size());

        // Index the mailbox so that mime message gets cached
        // make sure digest is in message cache.
        Assert.assertTrue(MessageCache.contains(item.getDigest()));

        mbox.deleteMailbox(Mailbox.DeleteBlobs.NEVER);
        Assert.assertEquals("end with 1 blob in the store", 1, sm.size());
        // make sure digest is still present in message cache.
        Assert.assertTrue(MessageCache.contains(item.getDigest()));
        sm.purge();

        // then do it contingent on whether the store is centralized or local
        mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals("start with no blobs in the store", 0, sm.size());

        mbox.addMessage(null, MailboxTestUtil.generateMessage("test"),
                STANDARD_DELIVERY_OPTIONS, null).getId();
        Assert.assertEquals("1 blob in the store", 1, sm.size());

        mbox.deleteMailbox(Mailbox.DeleteBlobs.UNLESS_CENTRALIZED);
        int expected = StoreManager.getInstance().supports(
                StoreManager.StoreFeature.CENTRALIZED) ? 1 : 0;
        Assert.assertEquals("end with " + expected + " blob(s) in the store",
                expected, sm.size());
        sm.purge();
    }

    @Test
    public void muted() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);

        // root message
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(
                Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        Message msg = mbox.addMessage(null,
                MailboxTestUtil.generateMessage("test subject"), dopt, null);
        Assert.assertTrue("root unread", msg.isUnread());
        Assert.assertFalse("root not muted", msg.isTagged(Flag.FlagInfo.MUTED));
        Assert.assertTrue("root in virtual conv", msg.getConversationId() < 0);

        // mark root muted
        mbox.alterTag(null, msg.getId(), MailItem.Type.MESSAGE,
                Flag.FlagInfo.MUTED, true, null);
        msg = mbox.getMessageById(null, msg.getId());
        Assert.assertTrue("root unread", msg.isUnread());
        Assert.assertTrue("root muted", msg.isTagged(Flag.FlagInfo.MUTED));
        Assert.assertTrue("root in virtual conv", msg.getConversationId() < 0);
        Assert.assertTrue("virtual conv muted",
                mbox.getConversationById(null, msg.getConversationId())
                        .isTagged(Flag.FlagInfo.MUTED));

        // add a reply to the muted virtual conversation
        dopt.setConversationId(msg.getConversationId());
        Message msg2 = mbox
                .addMessage(null,
                        MailboxTestUtil.generateMessage("Re: test subject"),
                        dopt, null);
        Assert.assertFalse("reply read", msg2.isUnread());
        Assert.assertTrue("reply muted", msg2.isTagged(Flag.FlagInfo.MUTED));
        Assert.assertFalse("reply in real conv", msg2.getConversationId() < 0);
        Assert.assertTrue("real conversation muted",
                mbox.getConversationById(null, msg2.getConversationId())
                        .isTagged(Flag.FlagInfo.MUTED));

        // add another reply to the now-real still-muted conversation
        dopt.setConversationId(msg2.getConversationId());
        Message msg3 = mbox
                .addMessage(null,
                        MailboxTestUtil.generateMessage("Re: test subject"),
                        dopt, null);
        Assert.assertFalse("second reply read", msg3.isUnread());
        Assert.assertTrue("second reply muted",
                msg3.isTagged(Flag.FlagInfo.MUTED));
        Assert.assertFalse("second reply in real conv",
                msg3.getConversationId() < 0);
        Assert.assertTrue("real conversation muted",
                mbox.getConversationById(null, msg3.getConversationId())
                        .isTagged(Flag.FlagInfo.MUTED));

        // unmute conversation
        mbox.alterTag(null, msg3.getConversationId(),
                MailItem.Type.CONVERSATION, Flag.FlagInfo.MUTED, false, null);
        msg3 = mbox.getMessageById(null, msg3.getId());
        Assert.assertFalse("second reply not muted",
                msg3.isTagged(Flag.FlagInfo.MUTED));
        Assert.assertFalse("real conversation not muted",
                mbox.getConversationById(null, msg3.getConversationId())
                        .isTagged(Flag.FlagInfo.MUTED));

        // add a last reply to the now-unmuted conversation
        Message msg4 = mbox
                .addMessage(null,
                        MailboxTestUtil.generateMessage("Re: test subject"),
                        dopt, null);
        Assert.assertTrue("third reply unread", msg4.isUnread());
        Assert.assertFalse("third reply not muted",
                msg4.isTagged(Flag.FlagInfo.MUTED));
        Assert.assertFalse("third reply in real conv",
                msg4.getConversationId() < 0);
        Assert.assertFalse("real conversation not muted",
                mbox.getConversationById(null, msg4.getConversationId())
                        .isTagged(Flag.FlagInfo.MUTED));
    }

    @Test
    public void tombstones() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        mbox.beginTrackingSync();
        int token = mbox.getLastChangeID();

        Document doc1 = DocumentTest.createDocument(mbox, "doc1", "abcdefg",null,
                false);
        Document doc2 = DocumentTest.createDocument(mbox, "doc2", "tuvwxyz",null,
                false);

        Set<Integer> ids = Sets.newHashSet(doc1.getId(), doc2.getId());
        Set<String> uuids = Sets.newHashSet(doc1.getUuid(), doc2.getUuid());
        Assert.assertEquals("2 different UUIDs", 2, uuids.size());

        mbox.delete(null, ArrayUtil.toIntArray(ids), MailItem.Type.DOCUMENT,
                null);

        TypedIdList tombstones = mbox.getTombstones(token);
        Assert.assertEquals("2 tombstones", 2, tombstones.size());
        for (Map.Entry<MailItem.Type, List<TypedIdList.ItemInfo>> row : tombstones) {
            Assert.assertEquals("all tombstones are for Documents",
                    MailItem.Type.DOCUMENT, row.getKey());
            for (TypedIdList.ItemInfo iinfo : row.getValue()) {
                Assert.assertTrue(iinfo + ": id contained in set",
                        ids.remove(iinfo.getId()));
                Assert.assertTrue(iinfo + ": uuid contained in set",
                        uuids.remove(iinfo.getUuid()));
            }
        }

        Collection<InternetAddress> addrs = new ArrayList<InternetAddress>();
        addrs.add(new InternetAddress("user2@email.com"));
        addrs.add(new InternetAddress("user3@email.com"));
        addrs.add(new InternetAddress("user4@email.com"));
        List<Contact> contactList = mbox.createAutoContact(null, addrs);
        Assert.assertEquals(3, addrs.size());

        int[] contacts = new int[addrs.size()];
        int i = 0;
        for (Contact c : contactList) {
            contacts[i++] = c.mId;
        }
        mbox.delete(null, contacts, MailItem.Type.CONTACT, null);
        Set<MailItem.Type> types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.CONTACT);
        List<Integer> contactTombstones = mbox.getTombstones(token, types);
        Assert.assertEquals(addrs.size(), contactTombstones.size());
        types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.DOCUMENT);
        List<Integer> docTombstones = mbox.getTombstones(token, types);
        Assert.assertEquals(2, docTombstones.size());
        types.add(MailItem.Type.CONTACT);
        Assert.assertEquals(5, mbox.getTombstones(token, types).size());

        token = mbox.getLastChangeID();

        Message msg = mbox.addMessage(null,
                MailboxTestUtil.generateMessage("test subject"),
                STANDARD_DELIVERY_OPTIONS, null);
        Document doc3 = DocumentTest.createDocument(mbox, "doc3", "lmnop",null,
                false);
        Folder folder = mbox.createFolder(null, "test",
                new Folder.FolderOptions());

        ids = Sets.newHashSet(doc3.getId(), msg.getId(), folder.getId());
        uuids = Sets
                .newHashSet(doc3.getUuid(), msg.getUuid(), folder.getUuid());
        Assert.assertEquals("3 different UUIDs", 3, uuids.size());

        mbox.move(null, new int[] { doc3.getId(), msg.getId() },
                MailItem.Type.UNKNOWN, folder.getId(), null);
        mbox.delete(null, folder.getId(), MailItem.Type.FOLDER, null);

        types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.MESSAGE);
        Assert.assertEquals(1, mbox.getTombstones(token, types).size());
        types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.CONTACT);
        Assert.assertEquals(0, mbox.getTombstones(token, types).size());

        tombstones = mbox.getTombstones(token);
        Assert.assertEquals("3 tombstones", 3, tombstones.size());
        for (Map.Entry<MailItem.Type, List<TypedIdList.ItemInfo>> row : tombstones) {
            Assert.assertTrue(
                    "expected tombstone types",
                    EnumSet.of(MailItem.Type.FOLDER, MailItem.Type.MESSAGE,
                            MailItem.Type.DOCUMENT).contains(row.getKey()));
            Assert.assertEquals("1 tombstone per type", 1, row.getValue()
                    .size());
            for (TypedIdList.ItemInfo iinfo : row.getValue()) {
                Assert.assertTrue(iinfo + ": id contained in set",
                        ids.remove(iinfo.getId()));
                Assert.assertTrue(iinfo + ": uuid contained in set",
                        uuids.remove(iinfo.getUuid()));
            }
        }
    }

    @Test
    public void createAutoContactTestWhenMaxEntriesLimitIsReached()
            throws Exception {

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        Collection<InternetAddress> addrs = new ArrayList<InternetAddress>();
        addrs.add(new InternetAddress("user2@email.com"));
        addrs.add(new InternetAddress("user3@email.com"));
        addrs.add(new InternetAddress("user4@email.com"));

        Provisioning prov = Provisioning.getInstance();
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.id,
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraContactMaxNumEntries,
                Integer.toString(2));
        prov.modifyAttrs(acct1, attrs);
        List<Contact> contactList = mbox.createAutoContact(null, addrs);
        assertEquals(2, contactList.size());

        attrs.put(Provisioning.A_zimbraContactMaxNumEntries,
                Integer.toString(10));
        prov.modifyAttrs(acct1, attrs);
        addrs = new ArrayList<InternetAddress>();
        addrs.add(new InternetAddress("user2@email.com"));
        addrs.add(new InternetAddress("user3@email.com"));
        addrs.add(new InternetAddress("user4@email.com"));
        contactList = mbox.createAutoContact(null, addrs);
        assertEquals(3, contactList.size());
    }

    @Test
    public void createAutoContactTestForDisplayNameFormat() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.id,
                MockProvisioning.DEFAULT_ACCOUNT_ID);

        Collection<InternetAddress> addrs = new ArrayList<InternetAddress>();
        addrs.add(new InternetAddress("\"First Last\" <user@email.com>"));
        List<Contact> contactList = mbox.createAutoContact(null, addrs);
        Contact contact = contactList.get(0);
        assertEquals("First", contact.get("firstName"));
        assertEquals("Last", contact.get("lastName"));

        addrs = new ArrayList<InternetAddress>();
        addrs.add(new InternetAddress("\"Last First\" <user@email.com>"));
        acct1.setPrefLocale("ja");
        ;
        contactList = mbox
                .createAutoContact(new OperationContext(acct1), addrs);
        contact = contactList.get(0);
        assertEquals("First", contact.get("firstName"));
        assertEquals("Last", contact.get("lastName"));
    }

    @Test
    public void getVisibleFolders() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        mbox.getVisibleFolders(new OperationContext(mbox));
    }

    @Test
    public void testUpdateVersion() throws Exception {
        //don't use default account to avoid messing up other tests
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acc = Provisioning.getInstance().createAccount("upgradetest@zimbra.com", "secret", attrs);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acc);

        short currentMajor = MailboxVersion.CURRENT.getMajor();
        short currentMinor = MailboxVersion.CURRENT.getMinor();
        short oldMajor = (short)(currentMajor-1);
        short oldMinor = (short)1;
        short newMajor = (short)(currentMajor+1);
        short newMinor = (short)(currentMinor+1);

        //check that mailbox has latest version upon creation
        assertEquals("wrong minor version", currentMinor,mbox.getVersion().getMinor());
        assertEquals("wrong minor version", currentMajor,mbox.getVersion().getMajor());

        //test downgrading version
        MailboxVersion olderVersion = new MailboxVersion(oldMajor,oldMinor);
        mbox.updateVersion(olderVersion);
        MailboxVersion updatedVersion = mbox.getVersion();
        assertEquals("wrong major version after downgrading version",oldMajor,updatedVersion.getMajor());
        assertEquals("wrong minor version after downgrading version",oldMinor,updatedVersion.getMinor());

        //test upgrading version
        MailboxVersion newerVersion = new MailboxVersion(newMajor,newMinor);
        mbox.updateVersion(newerVersion);
        updatedVersion = mbox.getVersion();
        assertEquals("wrong major version after upgrading ersion",newMajor,updatedVersion.getMajor());
        assertEquals("wrong minor version after upgrading ersion",newMinor,updatedVersion.getMinor());
    }

    @Test
    public void testUpgradePreKissMailbox() throws Exception {
        //don't use default account to avoid messing up other tests
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acc = Provisioning.getInstance().createAccount("upgradetest@zimbra.com", "secret", attrs);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acc);

        //indexing service should not be running at the beginning of this test
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();

        MailboxVersion olderVersion = new MailboxVersion((short)2,(short)7);
        mbox.updateVersion(olderVersion);
        MailboxVersion updatedVersion = mbox.getVersion();
        assertEquals("wrong major version after downgrading version",2,updatedVersion.getMajor());
        assertEquals("wrong minor version after downgrading version",7,updatedVersion.getMinor());

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Shall I compare thee to a summer's day".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Thou art more lovely and more temperate".getBytes(), false), dopt, null);

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 0", 0, queueAdapter.getTotalMailboxTaskCount(acc.getId()));
        assertEquals("at this point succeeded count should be 0", 0, queueAdapter.getSucceededMailboxTaskCount(acc.getId()));

        mbox.index.deleteIndex();
        mbox.migrate();

        assertEquals("at this point total count should be 2", 2, queueAdapter.getTotalMailboxTaskCount(acc.getId()));
        assertEquals("at this point succeeded count should be 0", 0, queueAdapter.getSucceededMailboxTaskCount(acc.getId()));
        //start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);
        assertEquals("at this point total count should be 2", 2, queueAdapter.getTotalMailboxTaskCount(acc.getId()));
        assertEquals("at this point succeeded count should be 2", 2, queueAdapter.getSucceededMailboxTaskCount(acc.getId()));

        assertEquals("wrong major version after downgrading version",MailboxVersion.CURRENT.getMajor(),mbox.getVersion().getMajor());
        assertEquals("wrong minor version after downgrading version",MailboxVersion.CURRENT.getMinor(),mbox.getVersion().getMinor());
    }
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
        Provisioning.getInstance().getLocalServer().setIndexTermsCacheSize(defaultCacheSizeSetting);
    }
}
