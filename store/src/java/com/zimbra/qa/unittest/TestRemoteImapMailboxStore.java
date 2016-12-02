package com.zimbra.qa.unittest;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.imap.RemoteImapMailboxStore;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mime.ParsedMessage;

public class TestRemoteImapMailboxStore extends TestCase {
    private static String NAME_PREFIX = "TestRemoteImapMailboxStore";
    private static final String USER_NAME = NAME_PREFIX + "_user1";

    @Override
    public void setUp() throws Exception {
        cleanUp();
        TestUtil.createAccount(USER_NAME);
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        if(TestUtil.accountExists(USER_NAME)) {
            TestUtil.deleteAccount(USER_NAME);
        }
    }

    @Test
    public void testSaveIMAPSubscriptions() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        RemoteImapMailboxStore remoteStore = new RemoteImapMailboxStore(zmbox, TestUtil.getAccount(USER_NAME).getId());

        //check that there are no subscriptions saved yet
        Set<String> subs = remoteStore.listSubscriptions(null);
        Assert.assertNull(subs);

        String path = NAME_PREFIX + "_testPath";
        Set<String> newSubs = new HashSet<String>();
        newSubs.add(path);

        //save subscriptions
        remoteStore.saveSubscriptions(null, newSubs);

        //verify that subscriptions were saved
        subs = remoteStore.listSubscriptions(null);
        Assert.assertNotNull(subs);
        Assert.assertEquals(1,subs.size());
        String sub = subs.iterator().next();
        Assert.assertTrue(sub.equalsIgnoreCase(path));
    }

    @Test
    public void testListIMAPSubscriptions() throws Exception {
        String path = NAME_PREFIX + "_testPath";
        MetadataList slist = new MetadataList();
        slist.add(path);

        //imitate subscription
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        mbox.setConfig(null, "imap", new Metadata().put("subs", slist));

        //check that subscription was saved in mailbox configuration
        Metadata config = mbox.getConfig(null, "imap");
        Assert.assertNotNull(config);
        MetadataList rlist = config.getList("subs", true);
        Assert.assertNotNull(rlist);
        Assert.assertNotNull(rlist.get(0));
        Assert.assertTrue(rlist.get(0).equalsIgnoreCase(path));

        //test listSubscriptions method
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        RemoteImapMailboxStore remoteStore = new RemoteImapMailboxStore(zmbox, TestUtil.getAccount(USER_NAME).getId());
        Set<String> subs = remoteStore.listSubscriptions(null);
        Assert.assertNotNull(subs);
        Assert.assertEquals(1,subs.size());
        String sub = subs.iterator().next();
        Assert.assertTrue(sub.equalsIgnoreCase(path));
    }

    @Test
    public void testGetCurrentModseq() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, NAME_PREFIX, new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        int folderId = folder.getId();

        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        RemoteImapMailboxStore remoteStore = new RemoteImapMailboxStore(zmbox, TestUtil.getAccount(USER_NAME).getId());
        Assert.assertEquals("Before adding a message, remoteStore.getCurrentMODSEQ returns value different from folder.getImapMODSEQ", remoteStore.getCurrentMODSEQ(folderId), folder.getImapMODSEQ());
        int oldModSeq = remoteStore.getCurrentMODSEQ(folderId);

        // add a message to the folder
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId).setFlags(Flag.BITMASK_UNREAD);
        String message = TestUtil.getTestMessage(NAME_PREFIX, mbox.getAccount().getName(), "someone@zimbra.com", "nothing here", new Date(System.currentTimeMillis()));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        mbox.addMessage(null, pm, dopt, null);
        zmbox.noOp();
        folder = mbox.getFolderById(null, folderId);
        Assert.assertEquals("After adding a message, remoteStore.getCurrentMODSEQ returns value different from folder.getImapMODSEQ", remoteStore.getCurrentMODSEQ(folderId), folder.getImapMODSEQ());
        Assert.assertFalse("Modseq should have changed after adding a message", remoteStore.getCurrentMODSEQ(folderId) == oldModSeq);
    }

    @Test
    public void testOpenImapFolder() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, "TestOpenImapFolder", new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        int folderId = folder.getId();
        List<ImapMessage> expected = new LinkedList<ImapMessage>();
        for (int i = 1; i <= 3; i++) {
            Message msg = TestUtil.addMessage(mbox, folderId, String.format("imap message %s", i), System.currentTimeMillis());
            expected.add(new ImapMessage(msg));
        }
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        RemoteImapMailboxStore remoteStore = new RemoteImapMailboxStore(zmbox, TestUtil.getAccount(USER_NAME).getId());
        List<ImapMessage> actual = remoteStore.openImapFolder(null, new ItemIdentifier(mbox.getAccountId(), folderId));
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals("expected and actual ImapMessage lists have different lengths", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i));
        }
    }
}