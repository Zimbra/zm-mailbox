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

public class TestLocalImapNotifications extends SharedImapNotificationTests {

    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        saveImapConfigSettings();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(false));
        imapServer.setReverseProxyUpstreamImapServers(new String[] {});
        super.sharedSetUp();
        TestUtil.assumeTrue("local IMAP server is not enabled", imapServer.isImapServerEnabled());
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
    protected void runOp(MailboxOperation op, ZMailbox mbox, ZFolder folder)
            throws Exception {
        op.run(mbox);
        op.checkResult();
    }
}
