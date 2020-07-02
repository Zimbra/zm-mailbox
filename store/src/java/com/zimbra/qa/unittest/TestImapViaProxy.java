package com.zimbra.qa.unittest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.MailConfig;
import com.zimbra.cs.mailclient.imap.ImapConfig;
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
 * This is a shell test for IMAP tests which use the default IMAP configuration.
 * The actual tests that are run are in {@link SharedImapTests}
 */
public class TestImapViaProxy extends SharedImapTests {

    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        super.sharedSetUp();
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        super.sharedTearDown();
    }

    @Override
    protected ImapConfig getImapConfig() {
        ImapConfig config = null;
        try {
            List<Server> proxies = Provisioning.getInstance().getAllServers(Provisioning.SERVICE_PROXY);
            if (proxies != null && proxies.size() > 0) {
                config = new ImapConfig(proxies.get(0).getServiceHostname());
            } else {
                ZimbraLog.test.warn("No proxy servers found - assuming zmc-proxy!");
                config = new ImapConfig("zmc-proxy");
            }
        } catch (ServiceException e) {
            ZimbraLog.test.warn("Problem finding the name of a proxy", e);
            config = new ImapConfig("zmc-proxy");
        }
        config.setSecurity(MailConfig.Security.SSL);
        return config;
    }

    @Override
    protected int getImapPort() {
        try {
            return Provisioning.getInstance().getConfig().getImapSSLProxyBindPort();
        } catch (ServiceException e) {
            return ImapConfig.DEFAULT_SSL_PORT;
        }
    }
}
