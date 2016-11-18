package com.zimbra.qa.unittest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;

public class TestRemoteImap {
    private static final String USER = "TestRemoteImap-user";
    private static final String PASS = "test123";
    private String[] imapServersForLocalhost = null;
    private Server homeServer = null;
    private Server imapServer = null;
    private Account acc = null;
    private ImapConnection connection;
    private static final Provisioning prov = Provisioning.getInstance();
    private static List<Server> servers;

    @BeforeClass
    public static void beforeClass() throws Exception {
        servers = prov.getAllServers(Provisioning.SERVICE_MAILBOX);
    }

    @Before
    public void setUp() throws ServiceException, IOException  {
        Assume.assumeTrue(servers.size() > 1);
        cleanup();
        //if(servers.size() > 1) {
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
        //}
    }

    private void cleanup() throws ServiceException {
        if(TestUtil.accountExists(USER)) {
            TestUtil.deleteAccount(USER);
        }
    }

    @After
    public void tearDown() throws ServiceException  {
        cleanup();
        if(homeServer != null) {
            homeServer.setReverseProxyUpstreamImapServers(imapServersForLocalhost);
        }
    }

    @Test
    public void testAUTHENTICATE() throws IOException  {
        Assume.assumeTrue(servers.size() > 1);
        //if(imapServer != null && homeServer != null) {
            connection.login(PASS);
            assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        //}
    }

    @Test
    public void testLIST() throws IOException  {
        Assume.assumeTrue(servers.size() > 1);
        //if(imapServer != null && homeServer != null) {
            connection.login(PASS);
            Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
            List<ListData> listResult = connection.list("", "*");
            assertNotNull(listResult);
        //} 
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