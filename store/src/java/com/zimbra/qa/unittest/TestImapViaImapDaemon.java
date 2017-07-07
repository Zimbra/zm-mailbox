package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.soap.admin.type.CacheEntryType;

/**
 * Test the IMAP server provided by the IMAP daemon, doing the necessary configuration to make it work.
 *
 * Note: Currently bypasses Proxy, the tests connect directly to the IMAP daemon's port
 *
 * The actual tests that are run are in {@link SharedImapTests}
 */
public class TestImapViaImapDaemon extends SharedImapTests {

    @Before
    public void setUp() throws Exception  {
        saveImapConfigSettings();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(false));
        getLocalServer();
        imapServer.setReverseProxyUpstreamImapServers(new String[] {imapServer.getServiceHostname()});
        super.sharedSetUp();
        TestUtil.flushImapDaemonCache(imapServer);
        TestUtil.assumeTrue("remoteImapServerEnabled false for this server", imapServer.isRemoteImapServerEnabled());
    }

    @After
    public void tearDown() throws Exception  {
        super.sharedTearDown();
        restoreImapConfigSettings();
        TestUtil.flushImapDaemonCache(imapServer);
        getAdminConnection().reloadLocalConfig();
    }

    @Override
    protected int getImapPort() {
        return imapServer.getRemoteImapBindPort();
    }

    @Override
    protected void flushCacheIfNecessary() throws Exception {
        TestUtil.flushImapDaemonCache(imapServer);
    }

    @Test
    public void testClearDaemonCacheWrongAuthenticator() throws Exception {
        connection = connect();
        try {
            connection.flushCache(CacheEntryType.config.toString());
            fail("should not be able to flush the cache without authenticating");
        } catch (CommandFailedException cfe) {
            assertEquals("must be in AUTHENTICATED or SELECTED state", cfe.getError());
        }
        connection.login(PASS);
        try {
            connection.flushCache(CacheEntryType.config.toString());
            fail("should not be able to flush the cache without using X-ZIMBRA auth mechanism");
        } catch (CommandFailedException cfe) {
            assertEquals("must be authenticated with X-ZIMBRA auth mechanism", cfe.getError());
        }
    }

    private void tryConnect(boolean shouldSucceed, String message) throws Exception {
        try {
            connection = connect(USER);
            connection.login(PASS);
            if (!shouldSucceed) {
                fail(message);
            }
        } catch (CommandFailedException e) {
            if (shouldSucceed) {
                fail(message);
            }
        }
    }

    private void enableImapAndCheckCachedValue(Account acct) throws Exception {
        acct.setImapEnabled(true);
        tryConnect(false, "should not be able to log in since imapd has old value of zimbraImapEnabled=FALSE");
    }

    private void disableImapAndCheckCachedValue(Account acct) throws Exception {
        acct.setImapEnabled(false);
        tryConnect(true, "should be able to log in since imapd has old value of zimbraImapEnabled=TRUE");
    }

    @Test
    public void testClearDaemonCache() throws Exception {
        tryConnect(true, "should be able to log in initially"); //loads the account into the imapd cache

        Account acct = TestUtil.getAccount(USER);
        CacheEntry acctByName = new CacheEntry(CacheEntryBy.name, acct.getName());
        CacheEntry acctById = new CacheEntry(CacheEntryBy.id, acct.getId());
        ImapConnection adminConn = getAdminConnection();

        //verify that we can flush an account cache by name
        disableImapAndCheckCachedValue(acct);
        adminConn.flushCache("account", new CacheEntry[] { acctByName });
        tryConnect(false, "should not be able to log in after flushing LDAP cache by account name");

        //verify that we can flush an account cache by ID
        enableImapAndCheckCachedValue(acct);
        adminConn.flushCache("account", new CacheEntry[] { acctById });
        tryConnect(true, "should be able to log in after flushing LDAP cache by account ID");

        //verify that we can flush an account cache with multiple CacheEntries
        disableImapAndCheckCachedValue(acct);
        adminConn.flushCache("account", new CacheEntry[] { acctByName, acctById });
        tryConnect(false, "should not be able to log in after flushing LDAP cache by account name and ID");

        //verify that we can flush an account cache without a CacheEntry value
        enableImapAndCheckCachedValue(acct);
        adminConn.flushCache("account");
        tryConnect(true, "should be able to log in after flushing entire LDAP account cache");

        //verify that we can flush multiple cache types at once
        disableImapAndCheckCachedValue(acct);
        adminConn.flushCache("config,account");
        tryConnect(false, "should not be able to log in after flushing multiple cache types");

        //verify that we can flush all caches
        enableImapAndCheckCachedValue(acct);
        adminConn.flushCache("all");
        tryConnect(true, "should be able to log in after flushing all caches");
    }

    @Test
    public void testReloadLocalConfig() throws Exception {
        //to test this, we set imap_max_consecutive_error to 1 and make sure imapd disconnects after
        //the first failure
        ImapConnection adminConn = getAdminConnection();
        int savedMaxConsecutiveError = LC.imap_max_consecutive_error.intValue();
        TestUtil.setLCValue(LC.imap_max_consecutive_error, "1");
        connection = connect(USER);
        connection.login(PASS);
        try {
            //sanity check: even though the LC value is changed, imapd should still be using the old value
            for (int i = 0; i < savedMaxConsecutiveError; i++) {
                try {
                    connection.select("BAD");
                } catch (CommandFailedException e) {
                    assertEquals("expected 'SELECT failed' error", "SELECT failed", e.getError());
                }
            }
            try {
                connection.select("INBOX");
                fail("session should be disconnected due to too many consecutive errors");
            } catch (CommandFailedException e) {}

            //reload LC and reconnect; imapd should now be using the new value
            adminConn.reloadLocalConfig();
            connection = connect(USER);
            connection.login(PASS);
            try {
                connection.select("BAD");
            } catch (CommandFailedException e) {
                assertEquals("expected 'SELECT failed' error", "SELECT failed", e.getError());
            }
            try {
                connection.select("INBOX");
                fail("session should be disconnected after 1 error");
            } catch (CommandFailedException e) {}
        } finally {
            TestUtil.setLCValue(LC.imap_max_consecutive_error, String.valueOf(savedMaxConsecutiveError));
            adminConn.reloadLocalConfig();
        }
    }
}
