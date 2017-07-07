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

    @Test
    public void testClearDaemonCache() throws Exception {
        AuthenticatorFactory authFactory = new AuthenticatorFactory();
        authFactory.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
        Account acct = TestUtil.getAccount(USER);
        ImapConfig config = new ImapConfig(imapHostname);
        config.setMechanism(ZimbraAuthenticator.MECHANISM);
        config.setAuthenticatorFactory(authFactory);
        config.setPort(imapPort);
        config.setAuthenticationId(LC.zimbra_ldap_user.value());
        config.getLogger().setLevel(Log.Level.trace);
        connection = new ImapConnection(config);
        connection.connect();
        connection.authenticate(AuthProvider.getAdminAuthToken().getEncoded());
        connection.flushCache("config,server");
        CacheEntry[] acctEntries = new CacheEntry[2];
        acctEntries[0] = new CacheEntry(CacheEntryBy.name, acct.getName());
        acctEntries[1] = new CacheEntry(CacheEntryBy.id, acct.getId());
        connection.flushCache("account", acctEntries);
    }
}
