package com.zimbra.qa.unittest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

/**
 * This is a shell test for Local IMAP tests that does the necessary configuration to select
 * the local variant of IMAP access.
 *
 * Note: Currently bypasses Proxy, the tests connect directly to the embedded IMAP server's port
 *
 * The actual tests that are run are in {@link SharedImapTests}
 */
public class TestImapViaEmbeddedLocal extends SharedImapTests {

    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(false));
        imapServer.setReverseProxyUpstreamImapServers(new String[] {});
        super.sharedSetUp();
        TestUtil.assumeTrue("local IMAP server is not enabled", imapServer.isImapServerEnabled());
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        super.sharedTearDown();
    }

    @Override
    protected int getImapPort() {
        return imapServer.getImapBindPort();
    }

    @Test(timeout=10000)
    public void testReallyUsingLocalImap() throws ServiceException {
        Account uAcct = TestUtil.getAccount(USER);
        assertTrue("Provisioning.canUseLocalIMAP(" + USER + ")", Provisioning.canUseLocalIMAP(uAcct));
    }
}
