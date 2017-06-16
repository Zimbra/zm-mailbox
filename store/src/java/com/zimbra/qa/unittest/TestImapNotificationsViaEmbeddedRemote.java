package com.zimbra.qa.unittest;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Set;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.imap.ImapRemoteSession;
import com.zimbra.cs.imap.ImapServerListener;
import com.zimbra.cs.imap.ImapServerListenerPool;

public class TestImapNotificationsViaEmbeddedRemote extends SharedImapNotificationTests {
    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        saveImapConfigSettings();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(true));
        imapServer.setReverseProxyUpstreamImapServers(new String[] {});
        super.sharedSetUp();
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        super.sharedTearDown();
        restoreImapConfigSettings();
    }

    @Override
    protected int getImapPort() {
        return imapServer.getImapBindPort();
    }

    @Override
    protected ZMailbox getImapZMailboxForFolder(ZMailbox zmbox, ZFolder folder)
            throws ServiceException {
        ImapServerListener listener = ImapServerListenerPool.getInstance().get(zmbox);
        Set<ImapRemoteSession> sessions = listener.getListeners(zmbox.getAccountId(), Integer.valueOf(folder.getId()));
        assertFalse(String.format("Folder %s does not have any IMAP listeners", folder.getPath()), sessions.isEmpty());
        ImapRemoteSession session = sessions.iterator().next();
        return (ZMailbox) session.getMailbox();
    }
}
