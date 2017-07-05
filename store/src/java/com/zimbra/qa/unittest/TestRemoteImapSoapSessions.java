package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
public class TestRemoteImapSoapSessions extends ImapTestBase {
    private boolean canUseLocalImap;
    private boolean canUseRemoteImap;
    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        sharedSetUp();
        saveImapConfigSettings();
        canUseLocalImap = imapServer.isImapServerEnabled() && imapServer.isImapCleartextLoginEnabled();
        if(canUseLocalImap) {
            TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(true));
        } else {
            canUseRemoteImap = imapServer.isRemoteImapServerEnabled() && imapServer.isImapCleartextLoginEnabled() && 
                    Arrays.asList(imapServer.getReverseProxyUpstreamImapServers()).contains(imapServer.getServiceHostname());
        }
        TestUtil.assumeTrue("neither embeded remote, nor standalone imapd are available", canUseRemoteImap || canUseLocalImap);
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        sharedTearDown();
        restoreImapConfigSettings();
    }

    @Test
    public void testLogout() throws Exception {
        Collection<Session> sessionsBeforeLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        ImapConnection imapConn = connect();
        imapConn.login(PASS);
        imapConn.select("INBOX");
        Collection<Session> sessionsAfterLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsBeforeLogin = sessionsBeforeLogin == null ? 0 : sessionsBeforeLogin.size();
        int numSessionsAfterLogin = sessionsAfterLogin == null ? 0 : sessionsAfterLogin.size();
        assertEquals("Should have one more session after login", numSessionsBeforeLogin, numSessionsAfterLogin - 1);
        imapConn.logout();
        Thread.sleep(500);
        Collection<Session> sessionsAfterLogout = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsAfterLogout = sessionsAfterLogout == null ? 0 : sessionsAfterLogout.size();
        assertEquals("Should have as many sessions after logout as before login", numSessionsBeforeLogin, numSessionsAfterLogout);
    }

    @Test
    public void testTerminate() throws Exception {
        Collection<Session> sessionsBeforeLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        ImapConnection imapConn = connect();
        imapConn.login(PASS);
        imapConn.select("INBOX");
        Collection<Session> sessionsAfterLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsBeforeLogin = sessionsBeforeLogin == null ? 0 : sessionsBeforeLogin.size();
        int numSessionsAfterLogin = sessionsAfterLogin == null ? 0 : sessionsAfterLogin.size();
        assertEquals("Should have one more session after login", numSessionsBeforeLogin, numSessionsAfterLogin -1);
        imapConn.close();
        Thread.sleep(500);
        Collection<Session> sessionsAfterClose = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsAfterClose = sessionsAfterClose == null ? 0 : sessionsAfterClose.size();
        assertEquals("Should have as many sessions after close as before login", numSessionsBeforeLogin, numSessionsAfterClose);
    }

    @Override
    protected int getImapPort() {
        if(canUseLocalImap) {
            return imapServer.getImapBindPort();
        } else {
            return imapServer.getRemoteImapBindPort();
        }
    }

}
