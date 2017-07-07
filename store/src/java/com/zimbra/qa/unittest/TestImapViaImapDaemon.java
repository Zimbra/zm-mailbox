package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;
import com.zimbra.cs.service.AuthProvider;

/**
 * Test the IMAP server provided by the IMAP daemon, doing the necessary configuration to make it work.
 *
 * Note: Currently bypasses Proxy, the tests connect directly to the IMAP daemon's port
 *
 * The actual tests that are run are in {@link SharedImapTests}
 */
public class TestImapViaImapDaemon extends SharedImapTests {

    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        saveImapConfigSettings();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(false));
        getLocalServer();
        imapServer.setReverseProxyUpstreamImapServers(new String[] {imapServer.getServiceHostname()});
        super.sharedSetUp();
        TestUtil.assumeTrue("remoteImapServerEnabled false for this server", imapServer.isRemoteImapServerEnabled());
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        super.sharedTearDown();
        restoreImapConfigSettings();
    }

    @Override
    protected int getImapPort() {
        return imapServer.getRemoteImapBindPort();
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

    private ImapConnection getAdminConnection() throws Exception {
        AuthenticatorFactory authFactory = new AuthenticatorFactory();
        authFactory.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
        ImapConfig config = new ImapConfig(imapHostname);
        config.setMechanism(ZimbraAuthenticator.MECHANISM);
        config.setAuthenticatorFactory(authFactory);
        config.setPort(imapPort);
        config.setAuthenticationId(LC.zimbra_ldap_user.value());
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection conn = new ImapConnection(config);
        conn.connect();
        conn.authenticate(AuthProvider.getAdminAuthToken().getEncoded());
        return conn;
    }

    @Test
    public void testClearDaemonCache() throws Exception {
        ImapConnection adminConn = getAdminConnection();
        adminConn.flushCache("config,server");
        Account acct = TestUtil.getAccount(USER);
        CacheEntry[] acctEntries = new CacheEntry[2];
        acctEntries[0] = new CacheEntry(CacheEntryBy.name, acct.getName());
        acctEntries[1] = new CacheEntry(CacheEntryBy.id, acct.getId());
        adminConn.flushCache("account", acctEntries);
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
