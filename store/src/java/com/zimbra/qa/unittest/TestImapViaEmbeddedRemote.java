package com.zimbra.qa.unittest;

import java.io.IOException;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;

/**
 * Test the Embedded Remote IMAP server, doing the necessary configuration to make it work.
 *
 * Note: Currently bypasses Proxy, the tests connect directly to the embedded IMAP server's port
 *
 * The actual tests that are run are in {@link SharedImapTests}
 */
public class TestImapViaEmbeddedRemote extends SharedImapTests {

    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(true));
        imapServer.setReverseProxyUpstreamImapServers(new String[] {});
        super.sharedSetUp();
        TestUtil.assumeTrue("embedded remote IMAP server is not enabled", imapServer.isImapServerEnabled());
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        super.sharedTearDown();
    }

    @Override
    protected int getImapPort() {
        return imapServer.getImapBindPort();
    }

    @Override
    @SuppressWarnings("PMD")  // checking done in called methods
    @Ignore("ZCS-3810 - range searches are broken on remote IMAP")
    @Test
    /**
     * this is ignored until ZCS-3810 is fixed
     */
    public void testUidRangeSearch() throws Exception {

    }
    @Override
    @SuppressWarnings("PMD")  // checking done in called methods
    @Ignore("ZCS-3810 - virtual folders (search folders) return only up to 1000 items")
    @Test
    /**
     * this is ignored until ZCS-3810 is fixed
     */
    public void testUidRangeSearchOnVirtualFolder() throws Exception {

    }
}
