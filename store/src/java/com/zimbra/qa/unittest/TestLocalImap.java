package com.zimbra.qa.unittest;

import java.io.IOException;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;

/**
 * This is a shell test for Local IMAP tests that does the necessary configuration to select
 * the local variant of IMAP access.
 *
 * The Remote equivalent is {@Link TestRemoteImapShared}
 *
 * The actual tests that are run are in {@link SharedImapTests}
 */
public class TestLocalImap extends SharedImapTests {
    private static boolean saved_imap_always_use_remote_store;

    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        saved_imap_always_use_remote_store = LC.imap_always_use_remote_store.booleanValue();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(false));
        USER = "TestLocalImap-user";
        super.sharedSetUp();
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        super.sharedTearDown();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(saved_imap_always_use_remote_store));
    }
}
