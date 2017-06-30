package com.zimbra.cs.imap;

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class ImapPathTest {

    private static final String LOCAL_USER = "localimaptest@zimbra.com";
    private Account acct = null;

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
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testWildCardStar() throws Exception {
        ImapCredentials credentials = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath i4Path = new ImapPath("*", credentials, ImapPath.Scope.UNPARSED);
        Assert.assertNotNull("Should be able to instantiate ImapPath for '*'", i4Path);
        String owner = i4Path.getOwner();
        Assert.assertNull("owner part of the path should be null. Was " + owner, owner);
        Assert.assertTrue("belongsTo should return TRUE with same credentials as were passed to the constructor", i4Path.belongsTo(credentials));
        Assert.assertEquals("Incorrect owner account", acct, i4Path.getOwnerAccount());
        Assert.assertEquals("Incorrect UTF7-encoded path", "\"*\"", i4Path.asUtf7String());
    }

    @Test
    public void testWildCardPercent() throws Exception {
        ImapCredentials credentials = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath i4Path = new ImapPath("%", credentials, ImapPath.Scope.UNPARSED);
        Assert.assertNotNull("Should be able to instantiate ImapPath for '*'", i4Path);
        String owner = i4Path.getOwner();
        Assert.assertNull("owner part of the path should be null. Was " + owner, owner);
        Assert.assertTrue("belongsTo should return TRUE with same credentials as were passed to the constructor", i4Path.belongsTo(credentials));
        Assert.assertEquals("Incorrect owner account", acct, i4Path.getOwnerAccount());
        Assert.assertEquals("Incorrect UTF7-encoded path", "\"%\"", i4Path.asUtf7String());
    }

    @Test
    public void testWildCardPercent2() throws Exception {
        ImapCredentials credentials = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath i4Path = new ImapPath("%/%", credentials, ImapPath.Scope.UNPARSED);
        Assert.assertNotNull("Should be able to instantiate ImapPath for '*'", i4Path);
        String owner = i4Path.getOwner();
        Assert.assertNull("owner part of the path should be null. Was " + owner, owner);
        Assert.assertTrue("belongsTo should return TRUE with same credentials as were passed to the constructor", i4Path.belongsTo(credentials));
        Assert.assertEquals("Incorrect owner account", acct, i4Path.getOwnerAccount());
        Assert.assertEquals("Incorrect UTF7-encoded path", "\"%/%\"", i4Path.asUtf7String());
    }

    @Test
    public void testHomeWildCard() throws Exception {
        ImapCredentials credentials = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath i4Path = new ImapPath("/home/*", credentials, ImapPath.Scope.UNPARSED);
        Assert.assertNotNull("Should be able to instantiate ImapPath for '/home/*'", i4Path);
        String owner = i4Path.getOwner();
        Assert.assertNull("owner part of the path should be null. Was " + owner, owner);
        Assert.assertTrue("belongsTo should return TRUE with same credentials as were passed to the constructor", i4Path.belongsTo(credentials));
        Assert.assertEquals("Incorrect owner account", acct, i4Path.getOwnerAccount());
        Assert.assertEquals("Incorrect UTF7-encoded path", "\"home/*\"", i4Path.asUtf7String());
    }

    @Test
    public void testHomePercent() throws Exception {
        ImapCredentials credentials = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath i4Path = new ImapPath("/home/%", credentials, ImapPath.Scope.UNPARSED);
        Assert.assertNotNull("Should be able to instantiate ImapPath for '/home/%'", i4Path);
        String owner = i4Path.getOwner();
        Assert.assertNull("owner part of the path should be null. Was " + owner, owner);
        Assert.assertTrue("belongsTo should return TRUE with same credentials as were passed to the constructor", i4Path.belongsTo(credentials));
        Assert.assertEquals("Incorrect owner account", acct, i4Path.getOwnerAccount());
        Assert.assertEquals("Incorrect UTF7-encoded path", "\"home/%\"", i4Path.asUtf7String());
    }

    @Test
    public void testHomePercent2() throws Exception {
        ImapCredentials credentials = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath i4Path = new ImapPath("/home/%/%", credentials, ImapPath.Scope.UNPARSED);
        Assert.assertNotNull("Should be able to instantiate ImapPath for '/home/%/%'", i4Path);
        String owner = i4Path.getOwner();
        Assert.assertNull("owner part of the path should be null. Was " + owner, owner);
        Assert.assertTrue("belongsTo should return TRUE with same credentials as were passed to the constructor", i4Path.belongsTo(credentials));
        Assert.assertEquals("Incorrect owner account", acct, i4Path.getOwnerAccount());
        Assert.assertEquals("Incorrect UTF7-encoded path", "\"home/%/%\"", i4Path.asUtf7String());
    }
}
