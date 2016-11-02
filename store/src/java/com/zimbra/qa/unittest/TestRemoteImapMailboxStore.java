package com.zimbra.qa.unittest;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.cs.imap.RemoteImapMailboxStore;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;

public class TestRemoteImapMailboxStore extends TestCase {
    private static String NAME_PREFIX = "TestRemoteImapMailboxStore";
    private static final String USER_NAME = NAME_PREFIX + "_user1";

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

        // add a message to the folder
        TestUtil.addMessage(mbox, "Message 1");
        folder = mbox.getFolderById(null, folderId);
        Assert.assertEquals("After adding a message, remoteStore.getCurrentMODSEQ returns value different from folder.getImapMODSEQ", remoteStore.getCurrentMODSEQ(folderId), folder.getImapMODSEQ());
    }
}