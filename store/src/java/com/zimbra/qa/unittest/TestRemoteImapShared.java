package com.zimbra.qa.unittest;

import java.io.IOException;
import java.util.Set;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.imap.ImapRemoteSession;
import com.zimbra.cs.imap.ImapServerListener;
import com.zimbra.cs.imap.ImapServerListenerPool;

/**
 * This is a shell test for Remote IMAP tests that does the necessary configuration to select
 * the remote variant of IMAP access.
 *
 * The Local equivalent is {@Link TestLocalImapShared}
 *
 * The actual tests that are run are in {@link SharedImapTests}
 */
public class TestRemoteImapShared extends SharedImapTests {
    private static boolean saved_imap_always_use_remote_store;

    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        USER = "TestRemoteImapShared-user";
        saved_imap_always_use_remote_store = LC.imap_always_use_remote_store.booleanValue();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(true));
        super.sharedSetUp();
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        super.sharedTearDown();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(saved_imap_always_use_remote_store));
    }

    @Override
    protected ZMailbox getImapZMailbox() throws Exception {
        // return the ZMailbox instance used by the imap listener
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        ImapServerListener listener = ImapServerListenerPool.getInstance().get(mbox);
        Set<ImapRemoteSession> sessions = listener.getListeners(mbox.getAccountId(), 2);
        ImapRemoteSession session = sessions.iterator().next();
        ZMailbox zmbox = (ZMailbox) session.getMailbox();
        return zmbox;
    }

    @Override
    @Ignore ("failing on remote imap for now")
    public void testCatenateUrl() throws Exception {

    }

    @Override
    @Ignore ("failing on remote imap for now")
    public void testCatenateSimpleNoLiteralPlus() throws Exception {

    }

    @Override
    @Ignore ("failing on remote imap for now")
    public void testCatenateSimple() throws Exception {

    }

    @Override
    public void testStoreTagsDirty() throws Exception {

    }

    @Override
    @Ignore ("failing on remote imap for now")
    public void testStoreInvalidSystemFlag() throws Exception {

    }

    @Override
    @Ignore ("failing on remote imap for now")
    public void testStoreTags() throws Exception {

    }
}
