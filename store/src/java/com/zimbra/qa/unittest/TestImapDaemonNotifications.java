package com.zimbra.qa.unittest;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.localconfig.LC;
import org.junit.Ignore;

@Ignore("For Zimbra-X just test the configured IMAP via proxy")
public class TestImapDaemonNotifications extends SharedImapNotificationTests {

    @Before
    public void setUp() throws Exception  {
        getLocalServer();
        TestUtil.assumeTrue("remoteImapServerEnabled false for this server", imapServer.isRemoteImapServerEnabled());
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(false));
        imapServer.setReverseProxyUpstreamImapServers(new String[] {imapServer.getServiceHostname()});
        super.sharedSetUp();
        TestUtil.flushImapDaemonCache(imapServer);
    }

    @After
    public void tearDown() throws Exception  {
        super.sharedTearDown();
        TestUtil.flushImapDaemonCache(imapServer);
        getAdminConnection().reloadLocalConfig();
    }

    @Override
    protected int getImapPort() {
        return imapServer.getRemoteImapBindPort();
    }

    @Override
    protected void runOp(MailboxOperation op, ZMailbox mbox, ZFolder folder)
            throws Exception {
        op.run(mbox);
        String saved = null;
        int timeout = 6000;
        while(timeout > 0) {
            saved = op.checkResult();
            if(saved == null) {
                break;
            } else {
                //sleeping for 500ms is sometimes insufficient when these tests are run
                //on a freshly rebooted mailboxd, as it can lead to a dropped connection
                //due to imap_max_consecutive_error being reached.
                Thread.sleep(1000);
                timeout -= 1000;
            }
        }
        if(saved != null) {
            fail(saved);
        }
    }
}
