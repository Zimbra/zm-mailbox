package com.zimbra.cs.imap;

import java.util.HashMap;
import org.junit.Ignore;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class ImapFolderTest {
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
    public void testGetSubsequence() throws Exception {
        ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath path = new ImapPath("trash", creds);
        byte params = 0;
        
        ImapFolder i4folder = new ImapFolder(path, params, null);
        i4folder.cache(new ImapMessage(1, Type.of((byte) 5), 11, 0, null), true);
        i4folder.cache(new ImapMessage(2, Type.of((byte) 5), 12, 0, null), true);
        i4folder.cache(new ImapMessage(3, Type.of((byte) 5), 13, 0, null), true);
        LocalImapMailboxStore localStore = new LocalImapMailboxStore(mbox);
        Set<ImapMessage> i4set = i4folder.getSubsequence(null, "1,2", false);
        Assert.assertNotNull(i4set);
        Assert.assertEquals(2, i4set.size());
        
        i4set = i4folder.getSubsequence(null, "1:3", false);
        Assert.assertNotNull(i4set);
        Assert.assertEquals(3, i4set.size());
    }
}
