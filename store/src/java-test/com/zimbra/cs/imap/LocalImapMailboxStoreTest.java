package com.zimbra.cs.imap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;

public class LocalImapMailboxStoreTest {
    private static final String LOCAL_USER = "localimaptest@zimbra.com";
    private Account acct = null;
    private Mailbox mbox = null;
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        acct = prov.createAccount(LOCAL_USER, "secret", attrs);
        mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testListSubscriptions() throws Exception {
        String path = "testPath";
        MetadataList slist = new MetadataList();
        slist.add(path);
        
        //imitate subscription
        mbox.setConfig(null, "imap", new Metadata().put("subs", slist));
        
        //check that subscription was saved in mailbox configuration
        Metadata config = mbox.getConfig(null, "imap");
        Assert.assertNotNull(config);
        MetadataList rlist = config.getList("subs", true);
        Assert.assertNotNull(rlist);
        Assert.assertNotNull(rlist.get(0));
        Assert.assertTrue(rlist.get(0).equalsIgnoreCase(path));

        //test listSubscriptions method
        LocalImapMailboxStore localStore = new LocalImapMailboxStore(mbox);
        Set<String> subs = localStore.listSubscriptions(null);
        Assert.assertNotNull(subs);
        Assert.assertEquals(1,subs.size());
        String sub = subs.iterator().next();
        Assert.assertTrue(sub.equalsIgnoreCase(path));
    }

    @Test
    public void testSaveSubscriptions() throws Exception {
        //verify that no subscriptions are saved yet
        LocalImapMailboxStore localStore = new LocalImapMailboxStore(mbox);
        Set<String> savedSubscriptions = localStore.listSubscriptions(null);
        Assert.assertNull(savedSubscriptions);

        String path = "testPath";
        HashSet<String> subscriptions = new HashSet<String>();
        subscriptions.add(path);

        //test saving subscriptions
        localStore.saveSubscriptions(null, subscriptions);
        savedSubscriptions = localStore.listSubscriptions(null);
        Assert.assertNotNull(savedSubscriptions);
        Assert.assertEquals(1,savedSubscriptions.size());
        String sub = savedSubscriptions.iterator().next();
        Assert.assertTrue(sub.equalsIgnoreCase(path));
    }
}