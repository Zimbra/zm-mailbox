package com.zimbra.qa.unittest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;

public class TestRemoteImap extends TestCase {
    private static final String USER = "TestRemoteImap-user";
    private static final String PASS = "test123";
    private String[] imapServersForLocalhost = null;
    private Server homeServer = null;
    private Server imapServer = null;
    private Account acc = null;
    private ImapConnection connection;
    
    @Override
    public void setUp() throws Exception {
        cleanup();
        Provisioning prov  = Provisioning.getInstance();
        List<Server> servers = prov.getAllServers(Provisioning.SERVICE_MAILBOX);
        if(servers.size() > 1) {
            homeServer = servers.get(0);
            imapServer = servers.get(1);
            Map<String, Object> attrs = Maps.newHashMap();
            attrs.put(Provisioning.A_zimbraMailHost, homeServer.getServiceHostname());
            attrs.put(Provisioning.A_zimbraFeatureIMEnabled, ProvisioningConstants.TRUE);
            acc = TestUtil.createAccount(USER, attrs);
            Provisioning.getInstance().setPassword(acc, PASS);

            //preserve original setting, so we can set it back on tearDown
            imapServersForLocalhost = homeServer.getReverseProxyUpstreamImapServers();

            //homeServer will be the mailbox server and imapServer will be the remote IMAP
            homeServer.setReverseProxyUpstreamImapServers(new String[]{imapServer.getServiceHostname()});
            connection = connect();
        }
    }

    private void cleanup() throws Exception {
        if(TestUtil.accountExists(USER)) {
            TestUtil.deleteAccount(USER);
        }
    }

    @Override
    public void tearDown() throws Exception {
        cleanup();
        if(homeServer != null) {
            homeServer.setReverseProxyUpstreamImapServers(imapServersForLocalhost);
        }
    }

    @Test
    public void testAUTHENTICATE() throws Exception {
        if(imapServer != null && homeServer != null) {
            connection.login(PASS);
            Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        }
    }

    @Test
    public void testLIST() throws Exception {
        if(imapServer != null && homeServer != null) {
            connection.login(PASS);
            Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
            List<ListData> listResult = connection.list("", "*");
            assertNotNull(listResult);
        } 
    }

    private ImapConnection connect() throws IOException {
        ImapConfig config = new ImapConfig(imapServer.getServiceHostname());
        config.setPort(imapServer.getImapBindPort());
        config.setAuthenticationId(USER);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection connection = new ImapConnection(config);
        connection.connect();
        return connection;
    }
}
