package com.zimbra.qa.unittest;

import java.io.IOException;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;

public class TestImapDaemonNotifications extends SharedImapNotificationTests {

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

    @Override
    protected void runOp(MailboxOperation op, ZMailbox mbox, ZFolder folder)
            throws Exception {
        op.run(mbox);
        AssertionError saved = null;
        int timeout = 6000;
        while(timeout > 0) {
            try {
                op.checkResult();
                return;
            } catch (AssertionError e) {
                saved = e;
                Thread.sleep(500);
                timeout -= 500;
            }
        }
        throw saved; //re-raise failed assertion
    }
}
