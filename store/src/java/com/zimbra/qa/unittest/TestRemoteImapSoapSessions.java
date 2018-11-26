package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
@Ignore("For Zimbra-X just test the configured IMAP via proxy")
public class TestRemoteImapSoapSessions extends ImapTestBase {
    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        sharedSetUp();
        boolean canUseRemoteImap = false;
        boolean canUseLocalImap = imapServer.isImapServerEnabled() && imapServer.isImapCleartextLoginEnabled();
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
    }

    @Test
    public void testLogout() throws Exception {
        Collection<Session> sessionsBeforeLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        connection = connect();
        connection.login(PASS);
        connection.select("INBOX");
        Collection<Session> sessionsAfterLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsBeforeLogin = sessionsBeforeLogin == null ? 0 : sessionsBeforeLogin.size();
        int numSessionsAfterLogin = sessionsAfterLogin == null ? 0 : sessionsAfterLogin.size();
        assertEquals("Should have one more session after login", numSessionsBeforeLogin, numSessionsAfterLogin - 1);
        connection.logout();
        Thread.sleep(500);
        Collection<Session> sessionsAfterLogout = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsAfterLogout = sessionsAfterLogout == null ? 0 : sessionsAfterLogout.size();
        assertEquals("Should have as many sessions after logout as before login", numSessionsBeforeLogin, numSessionsAfterLogout);
    }

    @Test
    public void testOpenCloseFolders() throws Exception {
        Collection<Session> sessionsBeforeLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        connection = connect();
        connection.login(PASS);
        connection.select("INBOX");
        Collection<Session> sessionsAfterLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsBeforeLogin = sessionsBeforeLogin == null ? 0 : sessionsBeforeLogin.size();
        int numSessionsAfterLogin = sessionsAfterLogin == null ? 0 : sessionsAfterLogin.size();
        assertEquals("Should have one more session after login", numSessionsBeforeLogin, numSessionsAfterLogin - 1);
        MailboxInfo folderInfo = connection.select("DRAFTS");
        assertNotNull("return MailboxInfo for 'SELECT DRAFTS'");
        Collection<Session> sessionsAfterSelect = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsAfterSelect = sessionsAfterSelect == null ? 0 : sessionsAfterSelect.size();
        assertEquals("Should have as many sessions after selecting DRAFTS as after login", numSessionsAfterSelect, numSessionsAfterLogin);
        connection.close_mailbox();
        Collection<Session> sessionsAfterClose = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsAfterClose = sessionsAfterClose == null ? 0 : sessionsAfterClose.size();
        assertEquals("Should have as many sessions after closing DRAFTS as after login", numSessionsAfterClose, numSessionsAfterLogin);
        connection.create("testOpenCloseFolders");
        folderInfo = connection.select("testOpenCloseFolders");
        assertNotNull(String.format("return MailboxInfo for 'SELECT %s'", "testOpenCloseFolders"), folderInfo);
        sessionsAfterSelect = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        numSessionsAfterSelect = sessionsAfterSelect == null ? 0 : sessionsAfterSelect.size();
        assertEquals("Should have as many sessions after selecting 'testOpenCloseFolders' as after login", numSessionsAfterSelect, numSessionsAfterLogin);
    }

    @Test
    public void testTerminate() throws Exception {
        Collection<Session> sessionsBeforeLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        connection = connect();
        connection.login(PASS);
        connection.select("INBOX");
        Collection<Session> sessionsAfterLogin = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsBeforeLogin = sessionsBeforeLogin == null ? 0 : sessionsBeforeLogin.size();
        int numSessionsAfterLogin = sessionsAfterLogin == null ? 0 : sessionsAfterLogin.size();
        assertEquals("Should have one more session after login", numSessionsBeforeLogin, numSessionsAfterLogin -1);
        connection.close();
        Thread.sleep(500);
        Collection<Session> sessionsAfterClose = SessionCache.getSoapSessions(TestUtil.getAccount(USER).getId());
        int numSessionsAfterClose = sessionsAfterClose == null ? 0 : sessionsAfterClose.size();
        assertEquals("SOAP session should be dropped when IMAP client drops without logging out", numSessionsBeforeLogin, numSessionsAfterClose);
    }

    @Override
    protected int getImapPort() {
        if(imapServer.isImapServerEnabled() && imapServer.isImapCleartextLoginEnabled()) {
            return imapServer.getImapBindPort();
        } else {
            return imapServer.getRemoteImapBindPort();
        }
    }
}
