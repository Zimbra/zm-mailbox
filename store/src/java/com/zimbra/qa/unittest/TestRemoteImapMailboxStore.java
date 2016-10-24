package com.zimbra.qa.unittest;

import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.cs.imap.RemoteImapMailboxStore;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;

public class TestRemoteImapMailboxStore extends TestCase {
    private static String NAME_PREFIX = "TestRemoteImapMailboxStore";
    private static final String USER_NAME = "user1";

    public void setUp()
    throws Exception {
        cleanUp();
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    @Test
    public void test() throws Exception {
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
        RemoteImapMailboxStore localStore = new RemoteImapMailboxStore(zmbox, TestUtil.getAccount(USER_NAME).getId());
        Set<String> subs = localStore.listSubscriptions(null);
        Assert.assertNotNull(subs);
        Assert.assertEquals(1,subs.size());
        String sub = subs.iterator().next();
        Assert.assertTrue(sub.equalsIgnoreCase(path));
    }
}