package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.imap.ImapConfig;
import com.zimbra.cs.imap.ImapCredentials;
import com.zimbra.cs.imap.ImapFolder;
import com.zimbra.cs.imap.ImapHandler;
import com.zimbra.cs.imap.ImapListener;
import com.zimbra.cs.imap.ImapMailboxStore;
import com.zimbra.cs.imap.ImapPath;
import com.zimbra.cs.imap.ImapServerListener;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;

public class TestImapServerListener {
    private static final String LOCAL_USER_NAME = "TestImapServerListener-localuser";
    private static final String REMOTE_USER_NAME = "TestImapServerListener-remoteuser";
    private static final String PASS = "test123";
    private Account remoteAccount;
    private Account localAccount;
    private String[] imapServersForLocalHost = null;
    private String[] imapServersForRemoteHost = null;
    private static Server localServer = null;
    private static Server remoteServer = null;
    private static final Provisioning prov = Provisioning.getInstance();
    private static final SoapProvisioning sp = new SoapProvisioning();
    private static List<Server> servers;
    private static AuthenticatorFactory ZIMBRA_AUTH_FACTORY = new AuthenticatorFactory();

    @BeforeClass
    public static void beforeClass() throws Exception {
        servers = prov.getAllServers(Provisioning.SERVICE_MAILBOX);
        ZIMBRA_AUTH_FACTORY.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
        localServer = prov.getLocalServer();
        sp.soapSetURI(LC.zimbra_admin_service_scheme.value() + localServer.getServiceHostname() + ":" + localServer.getAdminPort()
                + AdminConstants.ADMIN_SERVICE_URI);
        sp.soapZimbraAdminAuthenticate();
        for(Server s : servers) {
            if(!s.isLocalServer()) {
                remoteServer = s;
                break;
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        cleanup();
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, localServer.getServiceHostname());
        attrs.put(Provisioning.A_zimbraFeatureIMEnabled, ProvisioningConstants.TRUE);
        localAccount = TestUtil.createAccount(LOCAL_USER_NAME, attrs);
        Provisioning.getInstance().setPassword(localAccount, PASS);
        imapServersForLocalHost = localServer.getReverseProxyUpstreamImapServers();

        if(remoteServer != null) {
            localServer.setReverseProxyUpstreamImapServers(new String[]{remoteServer.getServiceHostname()});
            attrs = Maps.newHashMap();
            attrs.put(Provisioning.A_zimbraMailHost, remoteServer.getServiceHostname());
            attrs.put(Provisioning.A_zimbraFeatureIMEnabled, ProvisioningConstants.TRUE);
            remoteAccount = TestUtil.createAccount(REMOTE_USER_NAME, attrs);
            Provisioning.getInstance().setPassword(remoteAccount, PASS);
            imapServersForRemoteHost = remoteServer.getReverseProxyUpstreamImapServers();
            remoteServer.setReverseProxyUpstreamImapServers(new String[]{localServer.getServiceHostname()});
        }
        sp.flushCache("all", null, true);
    }

    @After
    public void tearDown() throws Exception {
        if(localServer != null) {
            localServer.setReverseProxyUpstreamImapServers(imapServersForLocalHost);
        }
        if(remoteServer != null) {
            remoteServer.setReverseProxyUpstreamImapServers(imapServersForRemoteHost);
        }
        sp.flushCache("all", null, true);
    }

    private void cleanup() throws Exception {
        if(TestUtil.accountExists(LOCAL_USER_NAME)) {
            TestUtil.deleteAccount(LOCAL_USER_NAME);
        }
        if(TestUtil.accountExists(REMOTE_USER_NAME)) {
            TestUtil.deleteAccount(REMOTE_USER_NAME);
        }
        remoteAccount = null;
        localAccount = null;
    }

    @Test
    public void testRegisterUnregister() throws ServiceException {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ImapServerListener remoteListener = new ImapServerListener(remoteServer.getServiceHostname());
        assertNotNull("ImapServerListener instance should not be null", remoteListener);
        MailboxStore mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ImapMailboxStore imapStore = ImapMailboxStore.get(mboxStore);
        assertNotNull("ImapMailboxStore instance should not be null", imapStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        assertNotNull("ImapPath instance should not be null", path);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        assertNotNull("ImapFolder instance should not be null", i4folder);
        assertNotNull("ImapFolder.getCredentials() should not return null", i4folder.getCredentials());
        assertNotNull("ImapFolder.getPath() should not return null", i4folder.getPath());
        
        ImapListener session = imapStore.createListener(i4folder, handler);
        assertNotNull("ImapListener instance should not be null", session);
        assertFalse("Expecting ImapServerListener::isListeningOn to return false before calling addListener", remoteListener.isListeningOn(session.getTargetAccountId()));
        remoteListener.addListener(session);
        assertTrue("Expecting ImapServerListener::isListeningOn to return true after calling addListener", remoteListener.isListeningOn(session.getTargetAccountId()));
        remoteListener.removeListener(session);
        assertFalse("Expecting ImapServerListener::isListeningOn to return false after calling removeListener", remoteListener.isListeningOn(session.getTargetAccountId()));
    }
    
    class MockImapHandler extends ImapHandler {

        MockImapHandler() {
            super(new ImapConfig(false));
        }

        @Override
        protected String getRemoteIp() {
            return "127.0.0.1";
        }

        @Override
        protected void sendLine(String line, boolean flush) throws IOException {
        }

        @Override
        protected void dropConnection(boolean sendBanner) {
        }

        @Override
        protected void close() {
        }

        @Override
        protected void enableInactivityTimer() throws IOException {
        }

        @Override
        protected void completeAuthentication() throws IOException {
        }

        @Override
        protected boolean doSTARTTLS(String tag) throws IOException {
            return false;
        }

        @Override
        protected InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("localhost", 0);
        }
    }
}